package com.example.wms.business.stockin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.business.inventory.handler.ChangeItem;
import com.example.wms.business.inventory.handler.ChangeType;
import com.example.wms.business.inventory.handler.InventoryChangeHandler;
import com.example.wms.business.inventory.handler.InventoryChangeHandlerFactory;
import com.example.wms.business.purchase.entity.WmsPurchaseOrder;
import com.example.wms.business.purchase.entity.WmsPurchaseOrderItem;
import com.example.wms.business.purchase.mapper.WmsPurchaseOrderItemMapper;
import com.example.wms.business.purchase.mapper.WmsPurchaseOrderMapper;
import com.example.wms.business.stockin.dto.req.StockInAuditReq;
import com.example.wms.business.stockin.dto.req.StockInItemSaveReq;
import com.example.wms.business.stockin.dto.req.StockInPageReq;
import com.example.wms.business.stockin.dto.req.StockInSaveReq;
import com.example.wms.business.stockin.dto.rsp.StockInDetailRsp;
import com.example.wms.business.stockin.entity.WmsStockIn;
import com.example.wms.business.stockin.entity.WmsStockInItem;
import com.example.wms.business.stockin.entity.WmsStockInStatusLog;
import com.example.wms.business.stockin.mapper.WmsStockInItemMapper;
import com.example.wms.business.stockin.mapper.WmsStockInMapper;
import com.example.wms.business.stockin.mapper.WmsStockInStatusLogMapper;
import com.example.wms.business.stockin.service.WmsStockInService;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WmsStockInServiceImpl extends ServiceImpl<WmsStockInMapper, WmsStockIn> implements WmsStockInService {

    private final WmsStockInItemMapper itemMapper;
    private final WmsStockInStatusLogMapper statusLogMapper;
    private final WmsPurchaseOrderMapper purchaseOrderMapper;
    private final WmsPurchaseOrderItemMapper purchaseOrderItemMapper;
    private final InventoryChangeHandlerFactory inventoryHandlerFactory;
    private final com.example.wms.system.service.SysUserService sysUserService;
    private final com.example.wms.business.common.SkuMasterDataLoader skuMasterDataLoader;

    @Override
    public PageRsp<WmsStockIn> pageStockIn(StockInPageReq req) {
        LambdaQueryWrapper<WmsStockIn> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsStockIn::getCreateTime);
        Page<WmsStockIn> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        fillOperatorNames(page.getRecords());
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    /**
     * 批量填入库人/审核人姓名（优先真实姓名，其次昵称，最后用户名）
     */
    private void fillOperatorNames(List<WmsStockIn> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        List<Long> userIds = new ArrayList<>();
        for (WmsStockIn o : orders) {
            if (o.getInBy() != null) {
                userIds.add(o.getInBy());
            }
            if (o.getCreateBy() != null) {
                userIds.add(o.getCreateBy());
            }
            if (o.getAuditBy() != null) {
                userIds.add(o.getAuditBy());
            }
        }
        List<Long> distinct = userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>();
        sysUserService.listByIds(distinct).forEach(u -> nameMap.put(u.getUserId(),
                StringUtils.hasText(u.getRealName()) ? u.getRealName()
                        : (StringUtils.hasText(u.getNickName()) ? u.getNickName() : u.getUserName())));
        for (WmsStockIn o : orders) {
            Long operatorId = o.getInBy() != null ? o.getInBy() : o.getCreateBy();
            o.setInByName(operatorId != null ? nameMap.get(operatorId) : null);
            o.setAuditName(o.getAuditBy() != null ? nameMap.get(o.getAuditBy()) : null);
        }
    }

    /**
     * 状态日志操作人ID解析为真实姓名
     */
    private void fillLogOperateNames(List<WmsStockInStatusLog> logs) {
        if (CollectionUtils.isEmpty(logs)) {
            return;
        }
        List<Long> userIds = logs.stream().map(WmsStockInStatusLog::getOperateBy)
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>();
        sysUserService.listByIds(userIds).forEach(u -> nameMap.put(u.getUserId(),
                StringUtils.hasText(u.getRealName()) ? u.getRealName()
                        : (StringUtils.hasText(u.getNickName()) ? u.getNickName() : u.getUserName())));
        for (WmsStockInStatusLog l : logs) {
            if (l.getOperateBy() != null) {
                l.setOperateName(nameMap.get(l.getOperateBy()));
            }
        }
    }

    @Override
    public StockInDetailRsp getDetailById(Long id) {
        StockInDetailRsp rsp = new StockInDetailRsp();
        WmsStockIn order = this.getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        rsp.setOrder(order);
        fillOperatorNames(List.of(order));
        List<WmsStockInStatusLog> logs = statusLogMapper.selectByBillId(id);
        fillLogOperateNames(logs);
        List<WmsStockInItem> items = itemMapper.selectByStockInId(id);
        enrichItemMasterData(items, order.getSupplierName());
        rsp.setItems(items);
        rsp.setStatusLogs(logs);
        return rsp;
    }

    /**
     * 明细行按SKU主数据补齐规格/商品名/单位/供应商名称（手工建单未落库这些冗余字段时）
     */
    private void enrichItemMasterData(List<WmsStockInItem> items, String orderSupplierName) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Map<Long, com.example.wms.business.common.SkuMasterDataLoader.SkuMaster> master =
                skuMasterDataLoader.load(items.stream().map(WmsStockInItem::getSkuId).toList());
        for (WmsStockInItem item : items) {
            com.example.wms.business.common.SkuMasterDataLoader.SkuMaster md = master.get(item.getSkuId());
            if (md == null) {
                continue;
            }
            if (!StringUtils.hasText(item.getSpecText())) {
                item.setSpecText(md.specText);
            }
            if (!StringUtils.hasText(item.getSkuName())) {
                item.setSkuName(md.skuName);
            }
            if (!StringUtils.hasText(item.getUnitName())) {
                item.setUnitName(md.unitName);
            }
            // 供应商优先取SKU主数据，缺失时回退到单据头供应商
            item.setSupplierName(StringUtils.hasText(md.supplierName) ? md.supplierName : orderSupplierName);
        }
    }

    @Override
    public WmsStockIn getByStockInNo(String stockInNo) {
        return this.lambdaQuery()
                .eq(WmsStockIn::getStockInNo, stockInNo)
                .one();
    }

    @Override
    public StockInDetailRsp fromSource(String sourceBillNo) {
        StockInDetailRsp rsp = new StockInDetailRsp();
        LambdaQueryWrapper<WmsPurchaseOrder> pw = new LambdaQueryWrapper<>();
        pw.eq(WmsPurchaseOrder::getPurchaseNo, sourceBillNo);
        WmsPurchaseOrder purchase = purchaseOrderMapper.selectOne(pw);
        if (purchase == null) {
            rsp.setOrder(new WmsStockIn());
            rsp.setItems(new ArrayList<>());
            return rsp;
        }
        WmsStockIn order = new WmsStockIn();
        order.setSourceBillNo(sourceBillNo);
        order.setWarehouseId(purchase.getWarehouseId());
        order.setSupplierId(purchase.getSupplierId());
        order.setType(1);
        order.setRemark(purchase.getRemark());
        rsp.setOrder(order);

        List<WmsPurchaseOrderItem> purchaseItems = purchaseOrderItemMapper.selectByPurchaseId(purchase.getPurchaseId());
        List<WmsStockInItem> stockInItems = new ArrayList<>();
        int lineNo = 1;
        for (WmsPurchaseOrderItem pi : purchaseItems) {
            int remaining = (pi.getQuantity() != null ? pi.getQuantity() : 0)
                    - (pi.getDeliveredQty() != null ? pi.getDeliveredQty() : 0);
            if (remaining <= 0) {
                continue;
            }
            WmsStockInItem item = new WmsStockInItem();
            item.setSourceItemId(pi.getItemId());
            item.setLineNo(lineNo++);
            item.setSkuId(pi.getSkuId());
            item.setSkuCode(pi.getSkuCode());
            item.setInnerCode(pi.getInnerCode());
            item.setSkuName(pi.getSkuName());
            item.setSpecText(pi.getSpecText());
            item.setUnitId(pi.getUnitId());
            item.setUnitName(pi.getUnitName());
            item.setExpectedQty(remaining);
            item.setActualQty(remaining);
            item.setCostPrice(pi.getPurchasePrice());
            if (pi.getPurchasePrice() != null && remaining > 0) {
                item.setSubtotal(pi.getPurchasePrice().multiply(BigDecimal.valueOf(remaining)));
            }
            item.setBatchNo(pi.getSuggestBatch());
            stockInItems.add(item);
        }
        rsp.setItems(stockInItems);
        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveStockIn(StockInSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsStockIn::getStockInNo, req.getStockInNo())
                .count();
        if (count > 0) {
            throw new BizException("入库单号已存在");
        }
        WmsStockIn order = new WmsStockIn();
        copyOrderFields(order, req);
        order.setStatus(0);
        this.save(order);
        saveItems(order, req.getItems());
        this.updateById(order);
        insertStatusLog(order.getStockInId(), order.getStockInNo(), null, 0, "保存草稿", "CREATE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockIn(StockInSaveReq req) {
        if (req.getStockId() == null) {
            throw new BizException("入库单ID不能为空");
        }
        WmsStockIn exist = this.getById(req.getStockId());
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0 && exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        if (!exist.getStockInNo().equals(req.getStockInNo())) {
            long count = this.lambdaQuery()
                    .eq(WmsStockIn::getStockInNo, req.getStockInNo())
                    .ne(WmsStockIn::getStockInId, req.getStockId())
                    .count();
            if (count > 0) {
                throw new BizException("入库单号已存在");
            }
        }
        Integer fromStatus = exist.getStatus();
        copyOrderFields(exist, req);
        itemMapper.deleteByStockInId(req.getStockId());
        saveItems(exist, req.getItems());
        this.updateById(exist);
        if (!fromStatus.equals(exist.getStatus())) {
            insertStatusLog(exist.getStockInId(), exist.getStockInNo(), fromStatus, exist.getStatus(), req.getRemark(), "UPDATE");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockIn(Long id) {
        WmsStockIn exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可删除");
        }
        this.removeById(id);
        itemMapper.deleteByStockInId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitStockIn(Long id) {
        WmsStockIn exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        List<WmsStockInItem> items = itemMapper.selectByStockInId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        validateLineNo(items);
        exist.setStatus(1);
        this.updateById(exist);
        insertStatusLog(id, exist.getStockInNo(), 0, 1, "提交", "SUBMIT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditStockIn(StockInAuditReq req) {
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
        WmsStockIn exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        // 已提交(1)或已验收(2)均可验收上架，避免状态机断裂卡在"已提交"
        if (exist.getStatus() == null || (exist.getStatus() != 1 && exist.getStatus() != 2)) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "入库单[" + exist.getStockInNo() + "]状态不允许验收上架");
        }
        int fromStatus = exist.getStatus();
        List<WmsStockInItem> items = itemMapper.selectByStockInId(id);
        if (pass && CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        if (pass) {
            for (WmsStockInItem item : items) {
                item.setWarehouseId(exist.getWarehouseId());
            }
            exist.setStatus(3);
            exist.setAuditBy(AuthContextHolder.getUserId());
            exist.setAuditTime(LocalDateTime.now());
        } else {
            // 驳回回退到草稿，允许修改后重新提交
            exist.setStatus(0);
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        LambdaQueryWrapper<WmsStockIn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsStockIn::getStockInId, id)
                .eq(WmsStockIn::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        if (pass) {
            InventoryChangeHandler handler = inventoryHandlerFactory.getHandler(ChangeType.STOCK_IN);
            handler.handle(exist.getStockInId(), exist.getStockInNo(), exist.getAuditTime(), items);
            if (exist.getType() != null && exist.getType() == 1) {
                writeBackPurchase(exist, items);
            }
            insertStatusLog(id, exist.getStockInNo(), fromStatus, 3, remark, "AUDIT");
        } else {
            insertStatusLog(id, exist.getStockInNo(), fromStatus, 0, "审核不通过：" + remark, "REJECT");
        }
    }

    private void writeBackPurchase(WmsStockIn stockIn, List<WmsStockInItem> items) {
        if (!StringUtils.hasText(stockIn.getSourceBillNo())) {
            return;
        }
        LambdaQueryWrapper<WmsPurchaseOrder> pw = new LambdaQueryWrapper<>();
        pw.eq(WmsPurchaseOrder::getPurchaseNo, stockIn.getSourceBillNo());
        WmsPurchaseOrder order = purchaseOrderMapper.selectOne(pw);
        if (order == null) {
            return;
        }
        // 预加载采购单明细：手工创建的采购入库单明细没有 sourceItemId，需按 SKU 编码兜底匹配
        List<WmsPurchaseOrderItem> poItems = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<WmsPurchaseOrderItem>()
                        .eq(WmsPurchaseOrderItem::getPurchaseId, order.getPurchaseId()));
        for (WmsStockInItem item : items) {
            if (item.getActualQty() == null) {
                continue;
            }
            WmsPurchaseOrderItem poi = null;
            if (item.getSourceItemId() != null) {
                poi = purchaseOrderItemMapper.selectById(item.getSourceItemId());
            }
            if (poi == null && StringUtils.hasText(item.getSkuCode())) {
                // 兜底：优先找还有未收数量的同 SKU 采购明细行
                poi = poItems.stream()
                        .filter(p -> item.getSkuCode().equals(p.getSkuCode()))
                        .filter(p -> p.getQuantity() != null && p.getDeliveredQty() != null
                                && p.getDeliveredQty() < p.getQuantity())
                        .findFirst()
                        .orElse(poItems.stream()
                                .filter(p -> item.getSkuCode().equals(p.getSkuCode()))
                                .findFirst()
                                .orElse(null));
            }
            if (poi == null) {
                continue;
            }
            int oldDelivered = poi.getDeliveredQty() != null ? poi.getDeliveredQty() : 0;
            int qty = item.getActualQty();
            poi.setDeliveredQty(oldDelivered + qty);
            int unreceived = (poi.getQuantity() != null ? poi.getQuantity() : 0) - poi.getDeliveredQty();
            poi.setUnreceivedQty(Math.max(unreceived, 0));
            purchaseOrderItemMapper.updateById(poi);
        }
        // 主表数量以采购明细行汇总为准（单一事实来源，避免主表 total_quantity 不准导致状态误判）
        int orderDelivered = poItems.stream()
                .mapToInt(p -> p.getDeliveredQty() != null ? p.getDeliveredQty() : 0).sum();
        order.setDeliveredQty(orderDelivered);
        int orderTotal = poItems.stream()
                .mapToInt(p -> p.getQuantity() != null ? p.getQuantity() : 0).sum();
        order.setUnreceivedQty(Math.max(orderTotal - orderDelivered, 0));
        // 采购入库联动采购单状态：全部到货→已完成(4)；部分到货→部分到货(3)
        if (order.getUnreceivedQty() <= 0 && (order.getStatus() == null || order.getStatus() < 4)) {
            order.setStatus(4);
        } else if (order.getDeliveredQty() > 0 && (order.getStatus() == null || order.getStatus() < 3)) {
            order.setStatus(3);
        }
        purchaseOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditStockIn(Long id, String remark) {
        WmsStockIn exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已上架状态可反审核");
        }
        if (exist.getAuditTime() != null) {
            long minutes = Duration.between(exist.getAuditTime(), LocalDateTime.now()).toMinutes();
            if (minutes > 5) {
                throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已超过5分钟，不允许反审核");
            }
        }
        List<WmsStockInItem> items = itemMapper.selectByStockInId(id);
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(2);
        exist.setAuditBy(null);
        exist.setAuditTime(null);
        LambdaQueryWrapper<WmsStockIn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsStockIn::getStockInId, id)
                .eq(WmsStockIn::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        InventoryChangeHandler handler = inventoryHandlerFactory.getHandler(ChangeType.STOCK_IN);
        List<StockInReverseItem> reverseItems = new ArrayList<>();
        for (WmsStockInItem item : items) {
            reverseItems.add(new StockInReverseItem(item, exist.getWarehouseId()));
        }
        handler.handle(exist.getStockInId(), exist.getStockInNo(), LocalDateTime.now(), reverseItems);
        if (exist.getType() != null && exist.getType() == 1) {
            rollbackPurchase(exist, items);
        }
        insertStatusLog(id, exist.getStockInNo(), 3, 2, remark, "REJECT");
    }

    private void rollbackPurchase(WmsStockIn stockIn, List<WmsStockInItem> items) {
        if (!StringUtils.hasText(stockIn.getSourceBillNo())) {
            return;
        }
        LambdaQueryWrapper<WmsPurchaseOrder> pw = new LambdaQueryWrapper<>();
        pw.eq(WmsPurchaseOrder::getPurchaseNo, stockIn.getSourceBillNo());
        WmsPurchaseOrder order = purchaseOrderMapper.selectOne(pw);
        if (order == null) {
            return;
        }
        // 预加载采购单明细：与 writeBackPurchase 相同，按 SKU 编码兜底匹配
        List<WmsPurchaseOrderItem> poItems = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<WmsPurchaseOrderItem>()
                        .eq(WmsPurchaseOrderItem::getPurchaseId, order.getPurchaseId()));
        for (WmsStockInItem item : items) {
            if (item.getActualQty() == null) {
                continue;
            }
            WmsPurchaseOrderItem poi = null;
            if (item.getSourceItemId() != null) {
                poi = purchaseOrderItemMapper.selectById(item.getSourceItemId());
            }
            if (poi == null && StringUtils.hasText(item.getSkuCode())) {
                // 兜底：优先找已有到货数量可回滚的同 SKU 采购明细行
                poi = poItems.stream()
                        .filter(p -> item.getSkuCode().equals(p.getSkuCode()))
                        .filter(p -> p.getDeliveredQty() != null && p.getDeliveredQty() > 0)
                        .findFirst()
                        .orElse(poItems.stream()
                                .filter(p -> item.getSkuCode().equals(p.getSkuCode()))
                                .findFirst()
                                .orElse(null));
            }
            if (poi == null) {
                continue;
            }
            int oldDelivered = poi.getDeliveredQty() != null ? poi.getDeliveredQty() : 0;
            int qty = item.getActualQty();
            poi.setDeliveredQty(Math.max(oldDelivered - qty, 0));
            int unreceived = (poi.getQuantity() != null ? poi.getQuantity() : 0) - poi.getDeliveredQty();
            poi.setUnreceivedQty(Math.max(unreceived, 0));
            purchaseOrderItemMapper.updateById(poi);
        }
        // 主表数量以采购明细行汇总为准（重新查询确保拿到回滚后的最新值）
        List<WmsPurchaseOrderItem> latestItems = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<WmsPurchaseOrderItem>()
                        .eq(WmsPurchaseOrderItem::getPurchaseId, order.getPurchaseId()));
        int orderDelivered = latestItems.stream()
                .mapToInt(p -> p.getDeliveredQty() != null ? p.getDeliveredQty() : 0).sum();
        order.setDeliveredQty(orderDelivered);
        int orderTotal = latestItems.stream()
                .mapToInt(p -> p.getQuantity() != null ? p.getQuantity() : 0).sum();
        order.setUnreceivedQty(Math.max(orderTotal - orderDelivered, 0));
        // 采购入库反审核联动采购单状态：仍有到货→部分到货(3)；全部回滚→已审核(2)
        if (order.getStatus() != null && order.getStatus() >= 3 && order.getUnreceivedQty() > 0) {
            order.setStatus(order.getDeliveredQty() > 0 ? 3 : 2);
        }
        purchaseOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidStockIn(Long id, String remark) {
        WmsStockIn exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() == 4) {
            return;
        }
        if (exist.getStatus() != null && exist.getStatus() >= 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已上架入库单不允许作废");
        }
        Integer fromStatus = exist.getStatus();
        exist.setStatus(4);
        this.updateById(exist);
        insertStatusLog(id, exist.getStockInNo(), fromStatus, 4, remark, "VOID");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(BatchAuditReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        StockInAuditReq auditReq = new StockInAuditReq();
        auditReq.setIds(req.getIds().stream().map(Long::valueOf).toList());
        auditReq.setRemark(req.getRemark());
        auditStockIn(auditReq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void allocateLocation(Long id) {
        WmsStockIn exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || (exist.getStatus() != 1 && exist.getStatus() != 2)) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已提交或已验收状态可分配库位");
        }
        List<WmsStockInItem> items = itemMapper.selectByStockInId(id);
        for (WmsStockInItem item : items) {
            if (item.getLocationId() == null && exist.getWarehouseId() != null) {
                item.setLocationId(exist.getWarehouseId());
                itemMapper.updateById(item);
            }
        }
    }

    @Override
    public void exportStockInItems(StockInPageReq req, jakarta.servlet.http.HttpServletResponse response) {
        // 强制只导出已上架（status=3）的入库单
        req.setStatus(3);
        LambdaQueryWrapper<WmsStockIn> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsStockIn::getAuditTime);
        List<WmsStockIn> orders = this.list(wrapper);
        if (orders.isEmpty()) {
            try {
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setCharacterEncoding("utf-8");
                com.alibaba.excel.EasyExcel.write(response.getOutputStream(),
                                com.example.wms.business.stockin.dto.rsp.StockInItemExportVo.class)
                        .sheet("入库明细").doWrite(java.util.Collections.emptyList());
            } catch (java.io.IOException e) {
                throw new RuntimeException("导出入库明细失败: " + e.getMessage(), e);
            }
            return;
        }
        List<Long> orderIds = orders.stream().map(WmsStockIn::getStockInId).toList();
        List<com.example.wms.business.stockin.entity.WmsStockInItem> items = itemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.wms.business.stockin.entity.WmsStockInItem>()
                        .in(com.example.wms.business.stockin.entity.WmsStockInItem::getStockInId, orderIds)
                        .orderByAsc(com.example.wms.business.stockin.entity.WmsStockInItem::getStockInId)
                        .orderByAsc(com.example.wms.business.stockin.entity.WmsStockInItem::getLineNo));
        java.util.Map<Long, WmsStockIn> orderMap = orders.stream()
                .collect(java.util.stream.Collectors.toMap(WmsStockIn::getStockInId, o -> o));
        java.time.format.DateTimeFormatter dtFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<com.example.wms.business.stockin.dto.rsp.StockInItemExportVo> voList = items.stream().map(item -> {
            com.example.wms.business.stockin.dto.rsp.StockInItemExportVo vo =
                    new com.example.wms.business.stockin.dto.rsp.StockInItemExportVo();
            WmsStockIn order = orderMap.get(item.getStockInId());
            if (order != null) {
                vo.setStockInNo(order.getStockInNo());
                vo.setTypeText(inTypeText(order.getType()));
                vo.setWarehouseName(order.getWarehouseName());
                vo.setSupplierName(order.getSupplierName());
                vo.setAuditTime(order.getAuditTime() != null ? order.getAuditTime().format(dtFmt) : "");
            }
            vo.setLineNo(item.getLineNo());
            vo.setSkuCode(item.getSkuCode());
            vo.setInnerCode(item.getInnerCode());
            vo.setSkuName(item.getSkuName());
            vo.setSpecText(item.getSpecText());
            vo.setUnitName(item.getUnitName());
            vo.setExpectedQty(item.getExpectedQty());
            vo.setActualQty(item.getActualQty());
            vo.setBatchNo(item.getBatchNo());
            vo.setLocationCode(item.getLocationCode());
            vo.setCostPrice(item.getCostPrice());
            vo.setSubtotal(item.getSubtotal());
            return vo;
        }).toList();
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode(
                    "入库明细_" + java.time.LocalDate.now().toString() + ".xlsx",
                    java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            com.alibaba.excel.EasyExcel.write(response.getOutputStream(),
                            com.example.wms.business.stockin.dto.rsp.StockInItemExportVo.class)
                    .sheet("入库明细")
                    .doWrite(voList);
        } catch (java.io.IOException e) {
            throw new RuntimeException("导出入库明细失败: " + e.getMessage(), e);
        }
    }

    /** 入库类型文本 */
    private String inTypeText(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "采购入库";
            case 2 -> "调拨入库";
            case 3 -> "退货入库";
            case 4 -> "其他入库";
            default -> String.valueOf(type);
        };
    }

    private LambdaQueryWrapper<WmsStockIn> buildQueryWrapper(StockInPageReq req) {
        LambdaQueryWrapper<WmsStockIn> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getStockInNo())) {
            wrapper.like(WmsStockIn::getStockInNo, req.getStockInNo());
        }
        if (req.getType() != null) {
            wrapper.eq(WmsStockIn::getType, req.getType());
        }
        if (StringUtils.hasText(req.getSourceBillNo())) {
            wrapper.like(WmsStockIn::getSourceBillNo, req.getSourceBillNo());
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsStockIn::getWarehouseId, req.getWarehouseId());
        }
        if (req.getSupplierId() != null) {
            wrapper.eq(WmsStockIn::getSupplierId, req.getSupplierId());
        }
        if (req.getStatus() != null) {
            wrapper.eq(WmsStockIn::getStatus, req.getStatus());
        }
        if (req.getInBy() != null) {
            wrapper.eq(WmsStockIn::getInBy, req.getInBy());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(WmsStockIn::getCreateTime, req.getDateRangeStart());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(WmsStockIn::getCreateTime, req.getDateRangeEnd());
        }
        return wrapper;
    }

    private void copyOrderFields(WmsStockIn order, StockInSaveReq req) {
        order.setStockInNo(req.getStockInNo());
        order.setType(req.getType());
        order.setSourceBillNo(req.getSourceBillNo());
        order.setSourceItemId(req.getSourceItemId());
        order.setWarehouseId(req.getWarehouseId());
        order.setWarehouseName(req.getWarehouseName());
        order.setSupplierId(req.getSupplierId());
        order.setSupplierName(req.getSupplierName());
        order.setInBy(req.getInBy() != null ? req.getInBy() : AuthContextHolder.getUserId());
        order.setTotalQty(req.getTotalQty() != null ? req.getTotalQty() : 0);
        order.setTotalAmount(req.getTotalAmount() != null ? req.getTotalAmount() : BigDecimal.ZERO);
        order.setRemark(req.getRemark());
    }

    private void saveItems(WmsStockIn order, List<StockInItemSaveReq> items) {
        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(items)) {
            Long stockInId = order.getStockInId();
            String stockInNo = order.getStockInNo();
            int lineNo = 1;
            for (StockInItemSaveReq itemReq : items) {
                WmsStockInItem item = new WmsStockInItem();
                item.setStockInId(stockInId);
                item.setStockInNo(stockInNo);
                item.setSourceItemId(itemReq.getSourceItemId());
                item.setLineNo(itemReq.getLineNo() != null ? itemReq.getLineNo() : lineNo);
                item.setSkuId(itemReq.getSkuId());
                item.setSkuCode(itemReq.getSkuCode());
                item.setInnerCode(itemReq.getInnerCode());
                item.setSkuName(itemReq.getSkuName());
                item.setSpecText(itemReq.getSpecText());
                item.setUnitId(itemReq.getUnitId());
                item.setUnitName(itemReq.getUnitName());
                int expectedQty = itemReq.getExpectedQty() != null ? itemReq.getExpectedQty() : 0;
                int actualQty = itemReq.getActualQty() != null ? itemReq.getActualQty() : expectedQty;
                // 入库数量以实到为准，未填实到数量时取预期数量；小计/汇总一律后端重算，不信任前端传值
                int qty = actualQty > 0 ? actualQty : expectedQty;
                item.setExpectedQty(expectedQty);
                item.setActualQty(actualQty);
                item.setDiffQty(itemReq.getDiffQty() != null ? itemReq.getDiffQty() : (actualQty - expectedQty));
                BigDecimal costPrice = itemReq.getCostPrice() != null ? itemReq.getCostPrice() : BigDecimal.ZERO;
                BigDecimal subtotal = costPrice.multiply(BigDecimal.valueOf(qty));
                item.setCostPrice(costPrice);
                item.setSubtotal(subtotal);
                // 批次空串归一为 null，与库存维度保持一致
                item.setBatchNo(StringUtils.hasText(itemReq.getBatchNo()) ? itemReq.getBatchNo().trim() : null);
                item.setProduceDate(itemReq.getProduceDate());
                item.setExpireDate(itemReq.getExpireDate());
                item.setSupplierBatch(itemReq.getSupplierBatch());
                item.setLocationId(itemReq.getLocationId());
                item.setLocationCode(itemReq.getLocationCode());
                item.setSnList(itemReq.getSnList());
                item.setRemark(itemReq.getRemark());
                itemMapper.insert(item);
                totalQty += qty;
                totalAmount = totalAmount.add(subtotal);
                lineNo++;
            }
        }
        // 表头总数量/总金额由明细汇总得出
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
    }

    private void validateLineNo(List<WmsStockInItem> items) {
        Set<Integer> lineNos = new HashSet<>();
        for (WmsStockInItem item : items) {
            if (item.getLineNo() != null && !lineNos.add(item.getLineNo())) {
                throw new BizException(ResultCode.LINE_NO_DUPLICATE);
            }
        }
    }

    private void insertStatusLog(Long billId, String billNo, Integer fromStatus, Integer toStatus, String remark, String operateType) {
        WmsStockInStatusLog log = new WmsStockInStatusLog();
        log.setBillId(billId);
        log.setBillNo(billNo);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperateType(operateType);
        log.setOperateBy(AuthContextHolder.getUserId());
        var loginUser = AuthContextHolder.get();
        if (loginUser != null && StringUtils.hasText(loginUser.getNickname())) {
            log.setOperateName(loginUser.getNickname());
        }
        log.setOperateTime(LocalDateTime.now());
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }

    private static class StockInReverseItem implements ChangeItem {
        private final WmsStockInItem item;
        private final Long warehouseId;

        StockInReverseItem(WmsStockInItem item, Long warehouseId) {
            this.item = item;
            this.warehouseId = warehouseId;
        }

        @Override
        public Long getWarehouseId() {
            return warehouseId;
        }

        @Override
        public Long getSkuId() {
            return item.getSkuId();
        }

        @Override
        public Long getLocationId() {
            return item.getLocationId();
        }

        @Override
        public String getBatchNo() {
            return item.getBatchNo();
        }

        @Override
        public Integer getQty() {
            return item.getActualQty() != null ? -item.getActualQty() : 0;
        }

        @Override
        public BigDecimal getCostPrice() {
            return item.getCostPrice();
        }

        @Override
        public String getRemark() {
            return "反入库冲销";
        }

        @Override
        public String getInnerCode() {
            return item.getInnerCode();
        }
    }
}
