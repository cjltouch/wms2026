package com.example.wms.business.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.business.purchase.dto.req.PurchaseAuditReq;
import com.example.wms.business.purchase.dto.req.PurchaseItemSaveReq;
import com.example.wms.business.purchase.dto.req.PurchasePageReq;
import com.example.wms.business.purchase.dto.req.PurchaseSaveReq;
import com.example.wms.business.purchase.dto.rsp.PurchaseDetailRsp;
import com.example.wms.business.purchase.entity.WmsPurchaseOrder;
import com.example.wms.business.purchase.entity.WmsPurchaseOrderItem;
import com.example.wms.business.purchase.entity.WmsPurchaseStatusLog;
import com.example.wms.business.purchase.mapper.WmsPurchaseOrderItemMapper;
import com.example.wms.business.purchase.mapper.WmsPurchaseOrderMapper;
import com.example.wms.business.purchase.mapper.WmsPurchaseStatusLogMapper;
import com.example.wms.business.purchase.service.WmsPurchaseOrderService;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;
import com.example.wms.common.ResultCode;
import com.example.wms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WmsPurchaseOrderServiceImpl extends ServiceImpl<WmsPurchaseOrderMapper, WmsPurchaseOrder> implements WmsPurchaseOrderService {

    private final WmsPurchaseOrderItemMapper itemMapper;
    private final WmsPurchaseStatusLogMapper statusLogMapper;
    private final com.example.wms.system.service.SysUserService sysUserService;

    /** 批量回填创建人/审核人/采购员姓名 */
    private void fillUserNames(List<WmsPurchaseOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        for (WmsPurchaseOrder o : orders) {
            if (o.getCreateBy() != null) userIds.add(o.getCreateBy());
            if (o.getAuditBy() != null) userIds.add(o.getAuditBy());
            if (o.getPurchaseBy() != null) userIds.add(o.getPurchaseBy());
        }
        if (userIds.isEmpty()) return;
        Map<Long, String> nameMap = new java.util.HashMap<>();
        sysUserService.listByIds(userIds).forEach(u -> nameMap.put(u.getUserId(),
                org.springframework.util.StringUtils.hasText(u.getRealName())
                        ? u.getRealName()
                        : (org.springframework.util.StringUtils.hasText(u.getNickName())
                                ? u.getNickName() : u.getUserName())));
        for (WmsPurchaseOrder o : orders) {
            o.setCreateName(o.getCreateBy() != null ? nameMap.get(o.getCreateBy()) : null);
            o.setAuditName(o.getAuditBy() != null ? nameMap.get(o.getAuditBy()) : null);
            o.setPurchaserName(o.getPurchaseBy() != null ? nameMap.get(o.getPurchaseBy()) : null);
        }
    }

    @Override
    public PageRsp<WmsPurchaseOrder> pagePurchase(PurchasePageReq req) {
        LambdaQueryWrapper<WmsPurchaseOrder> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsPurchaseOrder::getCreateTime);
        Page<WmsPurchaseOrder> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        fillUserNames(page.getRecords());
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public PurchaseDetailRsp getDetailById(Long id) {
        PurchaseDetailRsp rsp = new PurchaseDetailRsp();
        WmsPurchaseOrder order = this.getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        fillUserNames(java.util.Collections.singletonList(order));
        rsp.setOrder(order);
        rsp.setItems(itemMapper.selectByPurchaseId(id));
        rsp.setStatusLogs(statusLogMapper.selectByBillId(id));
        return rsp;
    }

    @Override
    public WmsPurchaseOrder getByPurchaseNo(String purchaseNo) {
        return this.lambdaQuery()
                .eq(WmsPurchaseOrder::getPurchaseNo, purchaseNo)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePurchase(PurchaseSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsPurchaseOrder::getPurchaseNo, req.getPurchaseNo())
                .count();
        if (count > 0) {
            throw new BizException("采购单号已存在");
        }
        WmsPurchaseOrder order = new WmsPurchaseOrder();
        copyOrderFields(order, req);
        order.setStatus(0);
        order.setDeliveredQty(0);
        order.setUnreceivedQty(req.getTotalQty() != null ? req.getTotalQty() : 0);
        order.setReturnedQty(0);
        this.save(order);

        saveItems(order.getPurchaseId(), order.getPurchaseNo(), req.getItems());

        insertStatusLog(order.getPurchaseId(), order.getPurchaseNo(), null, 0, "保存草稿", "CREATE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchase(PurchaseSaveReq req) {
        if (req.getPurchaseId() == null) {
            throw new BizException("采购单ID不能为空");
        }
        WmsPurchaseOrder exist = this.getById(req.getPurchaseId());
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0 && exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        if (!exist.getPurchaseNo().equals(req.getPurchaseNo())) {
            long count = this.lambdaQuery()
                    .eq(WmsPurchaseOrder::getPurchaseNo, req.getPurchaseNo())
                    .ne(WmsPurchaseOrder::getPurchaseId, req.getPurchaseId())
                    .count();
            if (count > 0) {
                throw new BizException("采购单号已存在");
            }
        }
        Integer fromStatus = exist.getStatus();
        copyOrderFields(exist, req);
        exist.setUnreceivedQty(req.getTotalQty() != null ? req.getTotalQty() - (exist.getDeliveredQty() != null ? exist.getDeliveredQty() : 0) : 0);
        this.updateById(exist);

        itemMapper.deleteByPurchaseId(req.getPurchaseId());
        saveItems(req.getPurchaseId(), req.getPurchaseNo(), req.getItems());

        if (!fromStatus.equals(exist.getStatus())) {
            insertStatusLog(exist.getPurchaseId(), exist.getPurchaseNo(), fromStatus, exist.getStatus(), req.getRemark(), "UPDATE");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchase(Long id) {
        WmsPurchaseOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可删除");
        }
        this.removeById(id);
        itemMapper.deleteByPurchaseId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPurchase(Long id) {
        WmsPurchaseOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        List<WmsPurchaseOrderItem> items = itemMapper.selectByPurchaseId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        exist.setStatus(1);
        this.updateById(exist);
        insertStatusLog(id, exist.getPurchaseNo(), 0, 1, "提交审核", "SUBMIT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPurchase(PurchaseAuditReq req) {
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
            WmsPurchaseOrder exist = this.getById(id);
            if (exist == null) {
                throw new BizException(ResultCode.DATA_NOT_FOUND);
            }
            if (exist.getStatus() != 1) {
                throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "采购单[" + exist.getPurchaseNo() + "]状态不允许审核");
            }
            int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
            int fromStatus = 1;
            if (pass) {
                exist.setStatus(2);
                exist.setAuditBy(AuthContextHolder.getUserId());
                exist.setAuditTime(LocalDateTime.now());
            } else {
                exist.setStatus(0);
            }
            LambdaQueryWrapper<WmsPurchaseOrder> lockWrapper = new LambdaQueryWrapper<>();
            lockWrapper.eq(WmsPurchaseOrder::getPurchaseId, id)
                    .eq(WmsPurchaseOrder::getVersion, oldVersion);
            boolean updated = this.update(exist, lockWrapper);
            if (!updated) {
                throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
            }
            insertStatusLog(id, exist.getPurchaseNo(), fromStatus, exist.getStatus(),
                    pass ? remark : "审核不通过：" + remark, pass ? "AUDIT" : "REJECT");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditPurchase(Long id, String remark) {
        WmsPurchaseOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 2) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        if (exist.getDeliveredQty() != null && exist.getDeliveredQty() > 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已到货采购单不允许反审核");
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(1);
        exist.setAuditBy(null);
        exist.setAuditTime(null);
        LambdaQueryWrapper<WmsPurchaseOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsPurchaseOrder::getPurchaseId, id)
                .eq(WmsPurchaseOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        insertStatusLog(id, exist.getPurchaseNo(), 2, 1, remark, "REJECT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidPurchase(Long id, String remark) {
        WmsPurchaseOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getDeliveredQty() != null && exist.getDeliveredQty() > 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已到货采购单不允许作废");
        }
        if (exist.getStatus() == 5) {
            return;
        }
        Integer fromStatus = exist.getStatus();
        exist.setStatus(5);
        this.updateById(exist);
        insertStatusLog(id, exist.getPurchaseNo(), fromStatus, 5, remark, "CANCEL");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(BatchAuditReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        PurchaseAuditReq auditReq = new PurchaseAuditReq();
        auditReq.setIds(req.getIds().stream().map(Long::valueOf).toList());
        auditReq.setRemark(req.getRemark());
        auditPurchase(auditReq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchVoid(BatchAuditReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        for (String idStr : req.getIds()) {
            voidPurchase(Long.valueOf(idStr), req.getRemark());
        }
    }

    @Override
    public PageRsp<Map<String, Object>> pageReconcile(PurchasePageReq req) {
        IPage<Map<String, Object>> page = statusLogMapper.pageReconcile(
                new Page<>(req.getPageNum(), req.getPageSize()), req);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    private LambdaQueryWrapper<WmsPurchaseOrder> buildQueryWrapper(PurchasePageReq req) {
        LambdaQueryWrapper<WmsPurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getPurchaseNo())) {
            wrapper.like(WmsPurchaseOrder::getPurchaseNo, req.getPurchaseNo());
        }
        if (req.getSupplierId() != null) {
            wrapper.eq(WmsPurchaseOrder::getSupplierId, req.getSupplierId());
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsPurchaseOrder::getWarehouseId, req.getWarehouseId());
        }
        if (req.getStatus() != null) {
            wrapper.eq(WmsPurchaseOrder::getStatus, req.getStatus());
        }
        if (req.getPurchaseBy() != null) {
            wrapper.eq(WmsPurchaseOrder::getPurchaseBy, req.getPurchaseBy());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(WmsPurchaseOrder::getCreateTime, req.getDateRangeStart());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(WmsPurchaseOrder::getCreateTime, req.getDateRangeEnd());
        }
        return wrapper;
    }

    private void copyOrderFields(WmsPurchaseOrder order, PurchaseSaveReq req) {
        order.setPurchaseNo(req.getPurchaseNo());
        order.setSupplierId(req.getSupplierId());
        order.setWarehouseId(req.getWarehouseId());
        order.setPurchaseBy(req.getPurchaseBy() != null ? req.getPurchaseBy() : AuthContextHolder.getUserId());
        order.setExpectDate(req.getExpectDate());
        order.setTaxRate(req.getTaxRate() != null ? req.getTaxRate() : BigDecimal.ZERO);
        order.setFreight(req.getFreight() != null ? req.getFreight() : BigDecimal.ZERO);
        order.setDiscountRate(req.getDiscountRate() != null ? req.getDiscountRate() : BigDecimal.ZERO);
        order.setOtherAmount(req.getOtherAmount() != null ? req.getOtherAmount() : BigDecimal.ZERO);
        // 总数量以明细行汇总为准，避免前端漏传导致主表数量失真（影响采购入库回写的状态判断）
        int totalQty = 0;
        if (!CollectionUtils.isEmpty(req.getItems())) {
            totalQty = req.getItems().stream()
                    .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
        }
        order.setTotalQty(totalQty);
        order.setSubtotal(req.getSubtotal() != null ? req.getSubtotal() : BigDecimal.ZERO);
        order.setTaxAmount(req.getTaxAmount() != null ? req.getTaxAmount() : BigDecimal.ZERO);
        order.setTotalAmount(req.getTotalAmount() != null ? req.getTotalAmount() : BigDecimal.ZERO);
        order.setRemark(req.getRemark());
    }

    private void saveItems(Long purchaseId, String purchaseNo, List<PurchaseItemSaveReq> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        List<WmsPurchaseOrderItem> itemList = new ArrayList<>();
        for (PurchaseItemSaveReq itemReq : items) {
            WmsPurchaseOrderItem item = new WmsPurchaseOrderItem();
            item.setPurchaseId(purchaseId);
            item.setPurchaseNo(purchaseNo);
            item.setLineNo(itemReq.getLineNo());
            item.setSkuId(itemReq.getSkuId());
            item.setSkuCode(itemReq.getSkuCode());
            item.setInnerCode(itemReq.getInnerCode());
            item.setSkuName(itemReq.getSkuName());
            item.setSpecText(itemReq.getSpecText());
            item.setUnitId(itemReq.getUnitId());
            item.setUnitName(itemReq.getUnitName());
            item.setQuantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 0);
            item.setPurchasePrice(itemReq.getPurchasePrice() != null ? itemReq.getPurchasePrice() : BigDecimal.ZERO);
            item.setTaxRate(itemReq.getTaxRate() != null ? itemReq.getTaxRate() : BigDecimal.ZERO);
            item.setTaxAmount(itemReq.getTaxAmount() != null ? itemReq.getTaxAmount() : BigDecimal.ZERO);
            item.setSubtotal(itemReq.getSubtotal() != null ? itemReq.getSubtotal() : BigDecimal.ZERO);
            item.setDeliveredQty(0);
            item.setUnreceivedQty(itemReq.getQuantity() != null ? itemReq.getQuantity() : 0);
            item.setReturnedQty(0);
            item.setExpectDate(itemReq.getExpectDate());
            item.setSuggestBatch(itemReq.getSuggestBatch());
            item.setRemark(itemReq.getRemark());
            itemList.add(item);
        }
        for (WmsPurchaseOrderItem item : itemList) {
            itemMapper.insert(item);
        }
    }

    private void insertStatusLog(Long billId, String billNo, Integer fromStatus, Integer toStatus, String remark, String operateType) {
        WmsPurchaseStatusLog log = new WmsPurchaseStatusLog();
        log.setBillId(billId);
        log.setBillNo(billNo);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperateType(operateType);
        log.setOperateBy(AuthContextHolder.getUserId());
        log.setOperateTime(LocalDateTime.now());
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }
}
