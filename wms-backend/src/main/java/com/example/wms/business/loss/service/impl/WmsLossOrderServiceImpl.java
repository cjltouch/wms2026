package com.example.wms.business.loss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.business.inventory.handler.ChangeType;
import com.example.wms.business.inventory.handler.InventoryChangeHandler;
import com.example.wms.business.inventory.handler.InventoryChangeHandlerFactory;
import com.example.wms.business.loss.dto.req.LossAuditReq;
import com.example.wms.business.loss.dto.req.LossHandleReq;
import com.example.wms.business.loss.dto.req.LossItemSaveReq;
import com.example.wms.business.loss.dto.req.LossPageReq;
import com.example.wms.business.loss.dto.req.LossSaveReq;
import com.example.wms.business.loss.dto.rsp.LossDetailRsp;
import com.example.wms.business.loss.entity.WmsLossOrder;
import com.example.wms.business.loss.entity.WmsLossOrderItem;
import com.example.wms.business.loss.entity.WmsLossStatusLog;
import com.example.wms.business.loss.mapper.WmsLossOrderItemMapper;
import com.example.wms.business.loss.mapper.WmsLossOrderMapper;
import com.example.wms.business.loss.mapper.WmsLossStatusLogMapper;
import com.example.wms.business.loss.service.WmsLossOrderService;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsLossOrderServiceImpl extends ServiceImpl<WmsLossOrderMapper, WmsLossOrder> implements WmsLossOrderService {

    private final WmsLossOrderItemMapper itemMapper;
    private final WmsLossStatusLogMapper statusLogMapper;
    private final InventoryChangeHandlerFactory inventoryHandlerFactory;

    @Override
    public PageRsp<WmsLossOrder> pageLoss(LossPageReq req) {
        LambdaQueryWrapper<WmsLossOrder> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsLossOrder::getCreateTime);
        Page<WmsLossOrder> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public LossDetailRsp getDetailById(Long id) {
        LossDetailRsp rsp = new LossDetailRsp();
        WmsLossOrder order = this.getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        rsp.setOrder(order);
        rsp.setItems(itemMapper.selectByLossId(id));
        rsp.setStatusLogs(statusLogMapper.selectByBillId(id));
        return rsp;
    }

    @Override
    public WmsLossOrder getByLossNo(String lossNo) {
        return this.lambdaQuery()
                .eq(WmsLossOrder::getLossNo, lossNo)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLoss(LossSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsLossOrder::getLossNo, req.getLossNo())
                .count();
        if (count > 0) {
            throw new BizException("报损单号已存在");
        }
        WmsLossOrder order = new WmsLossOrder();
        copyOrderFields(order, req);
        order.setStatus(0);
        this.save(order);
        saveItems(order.getLossId(), order.getLossNo(), req.getItems());
        insertStatusLog(order.getLossId(), order.getLossNo(), null, 0, "保存草稿", "CREATE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLoss(LossSaveReq req) {
        if (req.getLossId() == null) {
            throw new BizException("报损单ID不能为空");
        }
        WmsLossOrder exist = this.getById(req.getLossId());
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0 && exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        Integer fromStatus = exist.getStatus();
        copyOrderFields(exist, req);
        this.updateById(exist);
        itemMapper.deleteByLossId(req.getLossId());
        saveItems(req.getLossId(), exist.getLossNo(), req.getItems());
        if (!fromStatus.equals(exist.getStatus())) {
            insertStatusLog(exist.getLossId(), exist.getLossNo(), fromStatus, exist.getStatus(), req.getRemark(), "UPDATE");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLoss(Long id) {
        WmsLossOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可删除");
        }
        this.removeById(id);
        itemMapper.deleteByLossId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLoss(Long id) {
        WmsLossOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        List<WmsLossOrderItem> items = itemMapper.selectByLossId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        exist.setStatus(1);
        this.updateById(exist);
        log.info("报损单{}提交成功，状态:0→1", exist.getLossNo());
        insertStatusLog(id, exist.getLossNo(), 0, 1, "提交", "SUBMIT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditLoss(LossAuditReq req) {
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
        WmsLossOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "报损单[" + exist.getLossNo() + "]状态不允许审核");
        }
        List<WmsLossOrderItem> items = itemMapper.selectByLossId(id);
        if (pass && CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        if (pass) {
            exist.setStatus(2);
            exist.setAuditBy(AuthContextHolder.getUserId());
            exist.setAuditTime(LocalDateTime.now());
            this.updateById(exist);
            log.info("报损单{}审核完成，状态:1→2", exist.getLossNo());
            insertStatusLog(id, exist.getLossNo(), 1, 2, remark, "AUDIT");
        } else {
            exist.setStatus(0);
            this.updateById(exist);
            log.info("报损单{}审核不通过，状态:1→0，原因:{}", exist.getLossNo(), remark);
            insertStatusLog(id, exist.getLossNo(), 1, 0, "审核不通过：" + remark, "REJECT");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleLoss(LossHandleReq req) {
        WmsLossOrder exist = this.getById(req.getId());
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已审核状态可处理");
        }
        List<WmsLossOrderItem> items = itemMapper.selectByLossId(req.getId());
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        for (WmsLossOrderItem item : items) {
            item.setWarehouseId(exist.getWarehouseId());
        }
        exist.setStatus(3);
        exist.setHandleBy(AuthContextHolder.getUserId());
        exist.setHandleTime(LocalDateTime.now());
        this.updateById(exist);
        InventoryChangeHandler handler = inventoryHandlerFactory.getHandler(ChangeType.LOSS);
        handler.handle(exist.getLossId(), exist.getLossNo(), exist.getHandleTime(), items);
        log.info("报损单{}处理完成，状态:2→3，库存扣减成功", exist.getLossNo());
        insertStatusLog(req.getId(), exist.getLossNo(), 2, 3, req.getRemark(), "HANDLE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidLoss(Long id, String remark) {
        WmsLossOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() == 5) {
            return;
        }
        if (exist.getStatus() != null && exist.getStatus() >= 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已处理报损单不允许作废");
        }
        Integer fromStatus = exist.getStatus();
        exist.setStatus(5);
        this.updateById(exist);
        log.info("报损单{}作废，状态:{}→5", exist.getLossNo(), fromStatus);
        insertStatusLog(id, exist.getLossNo(), fromStatus, 5, remark, "VOID");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(BatchAuditReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        LossAuditReq auditReq = new LossAuditReq();
        auditReq.setIds(req.getIds().stream().map(Long::valueOf).toList());
        auditReq.setRemark(req.getRemark());
        auditLoss(auditReq);
    }

    private LambdaQueryWrapper<WmsLossOrder> buildQueryWrapper(LossPageReq req) {
        LambdaQueryWrapper<WmsLossOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getLossNo())) {
            wrapper.like(WmsLossOrder::getLossNo, req.getLossNo());
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsLossOrder::getWarehouseId, req.getWarehouseId());
        }
        if (req.getLossType() != null) {
            wrapper.eq(WmsLossOrder::getLossType, req.getLossType());
        }
        if (req.getStatus() != null) {
            wrapper.eq(WmsLossOrder::getStatus, req.getStatus());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(WmsLossOrder::getCreateTime, req.getDateRangeStart());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(WmsLossOrder::getCreateTime, req.getDateRangeEnd());
        }
        return wrapper;
    }

    private void copyOrderFields(WmsLossOrder order, LossSaveReq req) {
        order.setLossNo(req.getLossNo());
        order.setWarehouseId(req.getWarehouseId());
        order.setWarehouseName(req.getWarehouseName());
        order.setLossType(req.getLossType());
        order.setTotalQty(req.getTotalQty() != null ? req.getTotalQty() : 0);
        order.setTotalAmount(req.getTotalAmount() != null ? req.getTotalAmount() : BigDecimal.ZERO);
        order.setLossDate(req.getLossDate());
        order.setRemark(req.getRemark());
    }

    private void saveItems(Long lossId, String lossNo, List<LossItemSaveReq> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        for (LossItemSaveReq itemReq : items) {
            WmsLossOrderItem item = new WmsLossOrderItem();
            item.setLossId(lossId);
            item.setLossNo(lossNo);
            item.setLineNo(itemReq.getLineNo());
            item.setSkuId(itemReq.getSkuId());
            item.setSkuCode(itemReq.getSkuCode());
            item.setInnerCode(itemReq.getInnerCode());
            item.setSkuName(itemReq.getSkuName());
            item.setSpecText(itemReq.getSpecText());
            item.setUnitId(itemReq.getUnitId());
            item.setUnitName(itemReq.getUnitName());
            item.setLossQty(itemReq.getLossQty() != null ? itemReq.getLossQty() : 0);
            item.setCostPrice(itemReq.getCostPrice() != null ? itemReq.getCostPrice() : BigDecimal.ZERO);
            item.setSubtotal(itemReq.getSubtotal() != null ? itemReq.getSubtotal() : BigDecimal.ZERO);
            item.setBatchNo(itemReq.getBatchNo());
            item.setLocationId(itemReq.getLocationId());
            item.setLocationCode(itemReq.getLocationCode());
            item.setLossReason(itemReq.getLossReason());
            item.setRemark(itemReq.getRemark());
            itemMapper.insert(item);
        }
    }

    private void insertStatusLog(Long billId, String billNo, Integer fromStatus, Integer toStatus, String remark, String operateType) {
        WmsLossStatusLog logEntity = new WmsLossStatusLog();
        logEntity.setBillId(billId);
        logEntity.setBillNo(billNo);
        logEntity.setFromStatus(fromStatus);
        logEntity.setToStatus(toStatus);
        logEntity.setOperateType(operateType);
        logEntity.setOperateBy(AuthContextHolder.getUserId());
        logEntity.setOperateTime(LocalDateTime.now());
        logEntity.setRemark(remark);
        statusLogMapper.insert(logEntity);
    }
}
