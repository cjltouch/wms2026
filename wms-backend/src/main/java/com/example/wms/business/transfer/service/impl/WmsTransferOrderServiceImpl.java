package com.example.wms.business.transfer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.business.inventory.handler.ChangeItem;
import com.example.wms.business.inventory.handler.ChangeType;
import com.example.wms.business.inventory.handler.InventoryChangeHandler;
import com.example.wms.business.inventory.handler.InventoryChangeHandlerFactory;
import com.example.wms.business.transfer.dto.req.TransferAuditReq;
import com.example.wms.business.transfer.dto.req.TransferItemSaveReq;
import com.example.wms.business.transfer.dto.req.TransferPageReq;
import com.example.wms.business.transfer.dto.req.TransferSaveReq;
import com.example.wms.business.transfer.dto.rsp.TransferDetailRsp;
import com.example.wms.business.transfer.entity.WmsTransferOrder;
import com.example.wms.business.transfer.entity.WmsTransferOrderItem;
import com.example.wms.business.transfer.entity.WmsTransferStatusLog;
import com.example.wms.business.transfer.mapper.WmsTransferOrderItemMapper;
import com.example.wms.business.transfer.mapper.WmsTransferOrderMapper;
import com.example.wms.business.transfer.mapper.WmsTransferStatusLogMapper;
import com.example.wms.business.transfer.service.WmsTransferOrderService;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;
import com.example.wms.common.ResultCode;
import com.example.wms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsTransferOrderServiceImpl extends ServiceImpl<WmsTransferOrderMapper, WmsTransferOrder>
        implements WmsTransferOrderService {

    private final WmsTransferOrderItemMapper itemMapper;
    private final WmsTransferStatusLogMapper statusLogMapper;
    private final InventoryChangeHandlerFactory inventoryHandlerFactory;

    @Override
    public PageRsp<WmsTransferOrder> pageTransfer(TransferPageReq req) {
        LambdaQueryWrapper<WmsTransferOrder> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsTransferOrder::getCreateTime);
        Page<WmsTransferOrder> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public TransferDetailRsp getDetailById(Long id) {
        TransferDetailRsp rsp = new TransferDetailRsp();
        WmsTransferOrder order = this.getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        rsp.setOrder(order);
        rsp.setItems(itemMapper.selectByTransferId(id));
        rsp.setStatusLogs(statusLogMapper.selectByBillId(id));
        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTransfer(TransferSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsTransferOrder::getTransferNo, req.getTransferNo())
                .count();
        if (count > 0) {
            throw new BizException("调拨单号已存在");
        }
        WmsTransferOrder order = new WmsTransferOrder();
        copyOrderFields(order, req);
        order.setStatus(0);
        this.save(order);
        saveItems(order.getTransferId(), order.getTransferNo(), req.getItems());
        insertStatusLog(order.getTransferId(), order.getTransferNo(), null, 0, "保存草稿");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTransfer(TransferSaveReq req) {
        if (req.getTransferId() == null) {
            throw new BizException("调拨单ID不能为空");
        }
        WmsTransferOrder exist = this.getById(req.getTransferId());
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可修改");
        }
        if (!exist.getTransferNo().equals(req.getTransferNo())) {
            long count = this.lambdaQuery()
                    .eq(WmsTransferOrder::getTransferNo, req.getTransferNo())
                    .ne(WmsTransferOrder::getTransferId, req.getTransferId())
                    .count();
            if (count > 0) {
                throw new BizException("调拨单号已存在");
            }
        }
        copyOrderFields(exist, req);
        this.updateById(exist);
        itemMapper.deleteByTransferId(req.getTransferId());
        saveItems(req.getTransferId(), req.getTransferNo(), req.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransfer(Long id) {
        WmsTransferOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可删除");
        }
        this.removeById(id);
        itemMapper.deleteByTransferId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTransfer(Long id) {
        WmsTransferOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可提交");
        }
        List<WmsTransferOrderItem> items = itemMapper.selectByTransferId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        exist.setStatus(1);
        this.updateById(exist);
        insertStatusLog(id, exist.getTransferNo(), 0, 1, "提交审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditTransfer(TransferAuditReq req) {
        List<Long> effectiveIds = req.effectiveIds();
        if (CollectionUtils.isEmpty(effectiveIds)) {
            return;
        }
        boolean pass = req.getPass() == null || req.getPass();
        String remark = req.getRemark();
        if (!pass && (remark == null || remark.isBlank())) {
            throw new BizException(ResultCode.PARAM_ERROR, "审核不通过必须填写审核原因");
        }
        for (Long id : effectiveIds) {
            auditOne(id, pass, remark);
        }
    }

    private void auditOne(Long id, boolean pass, String remark) {
        WmsTransferOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "调拨单[" + exist.getTransferNo() + "]状态不允许审核");
        }
        List<WmsTransferOrderItem> items = itemMapper.selectByTransferId(id);
        if (pass && CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        if (pass) {
            exist.setStatus(2);
            exist.setAuditBy(AuthContextHolder.getUserId());
            exist.setAuditTime(LocalDateTime.now());
            insertStatusLog(id, exist.getTransferNo(), 1, 2, remark);
        } else {
            exist.setStatus(0);
            insertStatusLog(id, exist.getTransferNo(), 1, 0, "审核不通过：" + remark);
        }
        LambdaQueryWrapper<WmsTransferOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsTransferOrder::getTransferId, id)
                .eq(WmsTransferOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditTransfer(Long id, String remark) {
        WmsTransferOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已审核状态可反审核");
        }
        if (exist.getOutTime() != null) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已出库的调拨单不允许反审核");
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(1);
        exist.setAuditBy(null);
        exist.setAuditTime(null);
        LambdaQueryWrapper<WmsTransferOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsTransferOrder::getTransferId, id)
                .eq(WmsTransferOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        insertStatusLog(id, exist.getTransferNo(), 2, 1, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOut(Long id) {
        WmsTransferOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已审核状态可确认出库");
        }
        List<WmsTransferOrderItem> items = itemMapper.selectByTransferId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        LocalDateTime outTime = LocalDateTime.now();
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(3);
        exist.setOutBy(AuthContextHolder.getUserId());
        exist.setOutTime(outTime);
        LambdaQueryWrapper<WmsTransferOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsTransferOrder::getTransferId, id)
                .eq(WmsTransferOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        InventoryChangeHandler handler = inventoryHandlerFactory.getHandler(ChangeType.TRANSFER_OUT);
        List<ChangeItem> changeItems = new ArrayList<>();
        for (WmsTransferOrderItem item : items) {
            item.setWarehouseId(exist.getOutWarehouseId());
            item.setLocationId(item.getOutLocationId());
            changeItems.add(item);
        }
        handler.handle(exist.getTransferId(), exist.getTransferNo(), outTime, changeItems);
        insertStatusLog(id, exist.getTransferNo(), 2, 3, "确认出库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmIn(Long id) {
        WmsTransferOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已出库状态可确认入库");
        }
        List<WmsTransferOrderItem> items = itemMapper.selectByTransferId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        LocalDateTime inTime = LocalDateTime.now();
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(4);
        exist.setInBy(AuthContextHolder.getUserId());
        exist.setInTime(inTime);
        LambdaQueryWrapper<WmsTransferOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsTransferOrder::getTransferId, id)
                .eq(WmsTransferOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        for (WmsTransferOrderItem item : items) {
            int transferQty = item.getTransferQty() != null ? item.getTransferQty() : 0;
            int received = transferQty;
            item.setReceivedQty(received);
            item.setDiffQty(received - transferQty);
            itemMapper.updateById(item);
        }
        InventoryChangeHandler handler = inventoryHandlerFactory.getHandler(ChangeType.TRANSFER_IN);
        List<ChangeItem> changeItems = new ArrayList<>();
        for (WmsTransferOrderItem item : items) {
            item.setWarehouseId(exist.getInWarehouseId());
            item.setLocationId(item.getInLocationId());
            changeItems.add(item);
        }
        handler.handle(exist.getTransferId(), exist.getTransferNo(), inTime, changeItems);
        insertStatusLog(id, exist.getTransferNo(), 3, 4, "确认入库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidTransfer(Long id, String remark) {
        WmsTransferOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() == 5) {
            return;
        }
        if (exist.getStatus() != null && exist.getStatus() >= 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已出库的调拨单不允许作废");
        }
        Integer fromStatus = exist.getStatus();
        exist.setStatus(5);
        this.updateById(exist);
        insertStatusLog(id, exist.getTransferNo(), fromStatus, 5, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(BatchAuditReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        TransferAuditReq auditReq = new TransferAuditReq();
        auditReq.setIds(req.getIds().stream().map(Long::valueOf).toList());
        auditReq.setRemark(req.getRemark());
        auditTransfer(auditReq);
    }

    @Override
    public byte[] export(TransferPageReq req) {
        return new byte[0];
    }

    private LambdaQueryWrapper<WmsTransferOrder> buildQueryWrapper(TransferPageReq req) {
        LambdaQueryWrapper<WmsTransferOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getTransferNo())) {
            wrapper.like(WmsTransferOrder::getTransferNo, req.getTransferNo());
        }
        if (req.getOutWarehouseId() != null) {
            wrapper.eq(WmsTransferOrder::getOutWarehouseId, req.getOutWarehouseId());
        }
        if (req.getInWarehouseId() != null) {
            wrapper.eq(WmsTransferOrder::getInWarehouseId, req.getInWarehouseId());
        }
        if (req.getStatus() != null) {
            wrapper.eq(WmsTransferOrder::getStatus, req.getStatus());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(WmsTransferOrder::getCreateTime, req.getDateRangeStart());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(WmsTransferOrder::getCreateTime, req.getDateRangeEnd());
        }
        return wrapper;
    }

    private void copyOrderFields(WmsTransferOrder order, TransferSaveReq req) {
        order.setTransferNo(req.getTransferNo());
        order.setOutWarehouseId(req.getOutWarehouseId());
        order.setOutWarehouseName(req.getOutWarehouseName());
        order.setInWarehouseId(req.getInWarehouseId());
        order.setInWarehouseName(req.getInWarehouseName());
        order.setTransferType(req.getTransferType() != null ? req.getTransferType() : 1);
        order.setTotalQty(req.getTotalQty() != null ? req.getTotalQty() : 0);
        order.setTotalAmount(req.getTotalAmount() != null ? req.getTotalAmount() : BigDecimal.ZERO);
        order.setExpectDate(req.getExpectDate());
        order.setRemark(req.getRemark());
    }

    private void saveItems(Long transferId, String transferNo, List<TransferItemSaveReq> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        for (TransferItemSaveReq itemReq : items) {
            WmsTransferOrderItem item = new WmsTransferOrderItem();
            item.setTransferId(transferId);
            item.setTransferNo(transferNo);
            item.setLineNo(itemReq.getLineNo());
            item.setSkuId(itemReq.getSkuId());
            item.setSkuCode(itemReq.getSkuCode());
            item.setInnerCode(itemReq.getInnerCode());
            item.setSkuName(itemReq.getSkuName());
            item.setSpecText(itemReq.getSpecText());
            item.setUnitId(itemReq.getUnitId());
            item.setUnitName(itemReq.getUnitName());
            item.setTransferQty(itemReq.getTransferQty() != null ? itemReq.getTransferQty() : 0);
            item.setReceivedQty(itemReq.getReceivedQty() != null ? itemReq.getReceivedQty() : 0);
            item.setDiffQty(itemReq.getDiffQty() != null ? itemReq.getDiffQty() : 0);
            item.setCostPrice(itemReq.getCostPrice() != null ? itemReq.getCostPrice() : BigDecimal.ZERO);
            item.setSubtotal(itemReq.getSubtotal() != null ? itemReq.getSubtotal() : BigDecimal.ZERO);
            item.setBatchNo(itemReq.getBatchNo());
            item.setOutLocationId(itemReq.getOutLocationId());
            item.setInLocationId(itemReq.getInLocationId());
            item.setRemark(itemReq.getRemark());
            itemMapper.insert(item);
        }
    }

    private void insertStatusLog(Long billId, String billNo, Integer fromStatus, Integer toStatus, String remark) {
        WmsTransferStatusLog logEntity = new WmsTransferStatusLog();
        logEntity.setBillId(billId);
        logEntity.setBillNo(billNo);
        logEntity.setFromStatus(fromStatus);
        logEntity.setToStatus(toStatus);
        logEntity.setOperateBy(AuthContextHolder.getUserId());
        logEntity.setOperateTime(LocalDateTime.now());
        logEntity.setRemark(remark);
        statusLogMapper.insert(logEntity);
    }
}
