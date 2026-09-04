package com.example.wms.business.sale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.business.inventory.handler.ChangeItem;
import com.example.wms.business.inventory.handler.ChangeType;
import com.example.wms.business.inventory.handler.InventoryChangeHandler;
import com.example.wms.business.inventory.handler.InventoryChangeHandlerFactory;
import com.example.wms.business.sale.dto.req.SaleAuditReq;
import com.example.wms.business.sale.dto.req.SaleItemSaveReq;
import com.example.wms.business.sale.dto.req.SalePageReq;
import com.example.wms.business.sale.dto.req.SaleSaveReq;
import com.example.wms.business.sale.dto.rsp.SaleDetailRsp;
import com.example.wms.business.sale.entity.WmsSaleOrder;
import com.example.wms.business.sale.entity.WmsSaleOrderItem;
import com.example.wms.business.sale.entity.WmsSaleStatusLog;
import com.example.wms.business.sale.mapper.WmsSaleOrderItemMapper;
import com.example.wms.business.sale.mapper.WmsSaleOrderMapper;
import com.example.wms.business.sale.mapper.WmsSaleStatusLogMapper;
import com.example.wms.business.sale.service.WmsSaleOrderService;
import com.example.wms.business.stockout.entity.WmsStockOut;
import com.example.wms.business.stockout.entity.WmsStockOutItem;
import com.example.wms.business.stockout.mapper.WmsStockOutItemMapper;
import com.example.wms.business.stockout.mapper.WmsStockOutMapper;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;
import com.example.wms.common.ResultCode;
import com.example.wms.common.exception.BizException;
import com.example.wms.common.utils.SnowflakeId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsSaleOrderServiceImpl extends ServiceImpl<WmsSaleOrderMapper, WmsSaleOrder>
        implements WmsSaleOrderService {

    private final WmsSaleOrderItemMapper itemMapper;
    private final WmsSaleStatusLogMapper statusLogMapper;
    private final WmsStockOutMapper stockOutMapper;
    private final WmsStockOutItemMapper stockOutItemMapper;
    private final InventoryChangeHandlerFactory inventoryHandlerFactory;

    @Override
    public PageRsp<WmsSaleOrder> pageSale(SalePageReq req) {
        LambdaQueryWrapper<WmsSaleOrder> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsSaleOrder::getCreateTime);
        Page<WmsSaleOrder> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public SaleDetailRsp getDetailById(Long id) {
        SaleDetailRsp rsp = new SaleDetailRsp();
        WmsSaleOrder order = this.getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        rsp.setOrder(order);
        rsp.setItems(itemMapper.selectBySaleId(id));
        rsp.setStatusLogs(statusLogMapper.selectByBillId(id));
        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSale(SaleSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsSaleOrder::getSaleNo, req.getSaleNo())
                .count();
        if (count > 0) {
            throw new BizException("销售单号已存在");
        }
        WmsSaleOrder order = new WmsSaleOrder();
        copyOrderFields(order, req);
        order.setStatus(0);
        order.setPayStatus(0);
        order.setReceivedAmount(BigDecimal.ZERO);
        this.save(order);
        saveItems(order.getSaleId(), order.getSaleNo(), req.getItems());
        insertStatusLog(order.getSaleId(), order.getSaleNo(), null, 0, "保存草稿");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSale(SaleSaveReq req) {
        if (req.getSaleId() == null) {
            throw new BizException("销售单ID不能为空");
        }
        WmsSaleOrder exist = this.getById(req.getSaleId());
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可修改");
        }
        if (!exist.getSaleNo().equals(req.getSaleNo())) {
            long count = this.lambdaQuery()
                    .eq(WmsSaleOrder::getSaleNo, req.getSaleNo())
                    .ne(WmsSaleOrder::getSaleId, req.getSaleId())
                    .count();
            if (count > 0) {
                throw new BizException("销售单号已存在");
            }
        }
        copyOrderFields(exist, req);
        this.updateById(exist);
        itemMapper.deleteBySaleId(req.getSaleId());
        saveItems(req.getSaleId(), req.getSaleNo(), req.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSale(Long id) {
        WmsSaleOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可删除");
        }
        this.removeById(id);
        itemMapper.deleteBySaleId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSale(Long id) {
        WmsSaleOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可提交");
        }
        List<WmsSaleOrderItem> items = itemMapper.selectBySaleId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        exist.setStatus(1);
        this.updateById(exist);
        insertStatusLog(id, exist.getSaleNo(), 0, 1, "提交");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSale(SaleAuditReq req) {
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
        WmsSaleOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "销售单[" + exist.getSaleNo() + "]状态不允许审核");
        }
        List<WmsSaleOrderItem> items = itemMapper.selectBySaleId(id);
        if (pass && CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        if (pass) {
            exist.setStatus(2);
            exist.setAuditBy(AuthContextHolder.getUserId());
            exist.setAuditTime(LocalDateTime.now());
            insertStatusLog(id, exist.getSaleNo(), 1, 2, remark);
        } else {
            exist.setStatus(0);
            insertStatusLog(id, exist.getSaleNo(), 1, 0, "审核不通过：" + remark);
        }
        LambdaQueryWrapper<WmsSaleOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsSaleOrder::getSaleId, id)
                .eq(WmsSaleOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOut(Long id) {
        WmsSaleOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已审核状态可确认出库");
        }
        List<WmsSaleOrderItem> items = itemMapper.selectBySaleId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        LocalDateTime outTime = LocalDateTime.now();
        Long stockOutId = SnowflakeId.getInstance().nextId();
        String stockOutNo = exist.getSaleNo() + "-OUT";

        WmsStockOut stockOut = new WmsStockOut();
        stockOut.setStockOutId(stockOutId);
        stockOut.setStockOutNo(stockOutNo);
        stockOut.setType(1);
        stockOut.setSourceBillNo(exist.getSaleNo());
        stockOut.setWarehouseId(exist.getWarehouseId());
        stockOut.setCustomerId(exist.getCustomerId());
        stockOut.setOutBy(AuthContextHolder.getUserId());
        stockOut.setAllocationRule(1);
        int totalQty = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalSale = BigDecimal.ZERO;
        for (WmsSaleOrderItem item : items) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            BigDecimal cost = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
            BigDecimal sale = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
            totalQty += qty;
            totalCost = totalCost.add(cost.multiply(BigDecimal.valueOf(qty)));
            totalSale = totalSale.add(sale);
        }
        stockOut.setTotalQty(totalQty);
        stockOut.setTotalCost(totalCost);
        stockOut.setTotalSale(totalSale);
        stockOut.setStatus(4);
        stockOut.setAuditBy(AuthContextHolder.getUserId());
        stockOut.setAuditTime(outTime);
        stockOut.setRemark("销售出库，源单：" + exist.getSaleNo());
        stockOutMapper.insert(stockOut);

        List<WmsStockOutItem> stockOutItems = new ArrayList<>();
        for (WmsSaleOrderItem item : items) {
            WmsStockOutItem soItem = new WmsStockOutItem();
            soItem.setStockOutId(stockOutId);
            soItem.setStockOutNo(stockOutNo);
            soItem.setSourceItemId(item.getItemId());
            soItem.setLineNo(item.getLineNo());
            soItem.setSkuId(item.getSkuId());
            soItem.setSkuCode(item.getSkuCode());
            soItem.setSkuName(item.getSkuName());
            soItem.setSpecText(item.getSpecText());
            soItem.setUnitId(item.getUnitId());
            soItem.setUnitName(item.getUnitName());
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            soItem.setExpectedQty(qty);
            soItem.setActualQty(qty);
            soItem.setPickedQty(qty);
            soItem.setCostPrice(item.getCostPrice());
            soItem.setSubtotalCost(item.getCostPrice() != null
                    ? item.getCostPrice().multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO);
            soItem.setSalePrice(item.getSalePrice());
            soItem.setSubtotalSale(item.getSubtotal());
            soItem.setDiscountRate(item.getDiscountRate());
            soItem.setAllocationRule(1);
            soItem.setRemark(item.getRemark());
            stockOutItemMapper.insert(soItem);
            stockOutItems.add(soItem);

            int oldDelivered = item.getDeliveredQty() != null ? item.getDeliveredQty() : 0;
            item.setDeliveredQty(oldDelivered + qty);
            itemMapper.updateById(item);
        }

        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(3);
        exist.setStockOutId(stockOutId);
        LambdaQueryWrapper<WmsSaleOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsSaleOrder::getSaleId, id)
                .eq(WmsSaleOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }

        InventoryChangeHandler handler = inventoryHandlerFactory.getHandler(ChangeType.STOCK_OUT_CONFIRM);
        List<ChangeItem> changeItems = new ArrayList<>();
        for (WmsStockOutItem soItem : stockOutItems) {
            changeItems.add(new SaleStockOutChangeItem(soItem, exist.getWarehouseId()));
        }
        handler.handle(stockOutId, stockOutNo, outTime, changeItems);

        insertStatusLog(id, exist.getSaleNo(), 2, 3, "确认出库，生成出库单：" + stockOutNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPay(Long id, BigDecimal receivedAmount) {
        WmsSaleOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() < 2) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已审核之后的订单可确认收款");
        }
        BigDecimal oldReceived = exist.getReceivedAmount() != null ? exist.getReceivedAmount() : BigDecimal.ZERO;
        BigDecimal newReceived = receivedAmount != null ? oldReceived.add(receivedAmount) : oldReceived;
        exist.setReceivedAmount(newReceived);
        BigDecimal saleAmount = exist.getSaleAmount() != null ? exist.getSaleAmount() : BigDecimal.ZERO;
        if (newReceived.compareTo(saleAmount) >= 0) {
            exist.setPayStatus(2);
        } else if (newReceived.compareTo(BigDecimal.ZERO) > 0) {
            exist.setPayStatus(1);
        } else {
            exist.setPayStatus(0);
        }
        this.updateById(exist);
        insertStatusLog(id, exist.getSaleNo(), exist.getStatus(), exist.getStatus(),
                "确认收款：" + (receivedAmount != null ? receivedAmount : BigDecimal.ZERO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSale(Long id) {
        WmsSaleOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已出库状态可完成");
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(4);
        LambdaQueryWrapper<WmsSaleOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsSaleOrder::getSaleId, id)
                .eq(WmsSaleOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        insertStatusLog(id, exist.getSaleNo(), 3, 4, "完成订单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidSale(Long id, String remark) {
        WmsSaleOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() == 5) {
            return;
        }
        if (exist.getStatus() != null && exist.getStatus() >= 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已出库的销售单不允许作废");
        }
        Integer fromStatus = exist.getStatus();
        exist.setStatus(5);
        this.updateById(exist);
        insertStatusLog(id, exist.getSaleNo(), fromStatus, 5, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(BatchAuditReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        SaleAuditReq auditReq = new SaleAuditReq();
        auditReq.setIds(req.getIds().stream().map(Long::valueOf).toList());
        auditReq.setRemark(req.getRemark());
        auditSale(auditReq);
    }

    @Override
    public byte[] export(SalePageReq req) {
        return new byte[0];
    }

    @Override
    public Map<String, Object> profitAnalysis(LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<WmsSaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(WmsSaleOrder::getStatus, 2);
        if (startDate != null) {
            wrapper.ge(WmsSaleOrder::getSaleDate, startDate.toLocalDate());
        }
        if (endDate != null) {
            wrapper.le(WmsSaleOrder::getSaleDate, endDate.toLocalDate());
        }
        List<WmsSaleOrder> orders = this.list(wrapper);
        Map<String, Object> result = new HashMap<>();
        BigDecimal totalSale = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int orderCount = orders.size();
        for (WmsSaleOrder order : orders) {
            BigDecimal sale = order.getSaleAmount() != null ? order.getSaleAmount() : BigDecimal.ZERO;
            totalSale = totalSale.add(sale);
        }
        LambdaQueryWrapper<WmsSaleOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        List<Long> saleIds = orders.stream().map(WmsSaleOrder::getSaleId).toList();
        if (!saleIds.isEmpty()) {
            itemWrapper.in(WmsSaleOrderItem::getSaleId, saleIds);
            List<WmsSaleOrderItem> items = itemMapper.selectList(itemWrapper);
            for (WmsSaleOrderItem item : items) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal cost = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
                BigDecimal profit = item.getProfit() != null ? item.getProfit() : BigDecimal.ZERO;
                totalCost = totalCost.add(cost.multiply(BigDecimal.valueOf(qty)));
                totalProfit = totalProfit.add(profit);
            }
        }
        result.put("orderCount", orderCount);
        result.put("totalSale", totalSale);
        result.put("totalCost", totalCost);
        result.put("totalProfit", totalProfit);
        return result;
    }

    private LambdaQueryWrapper<WmsSaleOrder> buildQueryWrapper(SalePageReq req) {
        LambdaQueryWrapper<WmsSaleOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getSaleNo())) {
            wrapper.like(WmsSaleOrder::getSaleNo, req.getSaleNo());
        }
        if (req.getCustomerId() != null) {
            wrapper.eq(WmsSaleOrder::getCustomerId, req.getCustomerId());
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsSaleOrder::getWarehouseId, req.getWarehouseId());
        }
        if (req.getStatus() != null) {
            wrapper.eq(WmsSaleOrder::getStatus, req.getStatus());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(WmsSaleOrder::getCreateTime, req.getDateRangeStart());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(WmsSaleOrder::getCreateTime, req.getDateRangeEnd());
        }
        return wrapper;
    }

    private void copyOrderFields(WmsSaleOrder order, SaleSaveReq req) {
        order.setSaleNo(req.getSaleNo());
        order.setCustomerId(req.getCustomerId());
        order.setCustomerName(req.getCustomerName());
        order.setWarehouseId(req.getWarehouseId());
        order.setWarehouseName(req.getWarehouseName());
        order.setSaleType(req.getSaleType() != null ? req.getSaleType() : 1);
        order.setTotalQty(req.getTotalQty() != null ? req.getTotalQty() : 0);
        order.setGoodsAmount(req.getGoodsAmount() != null ? req.getGoodsAmount() : BigDecimal.ZERO);
        order.setDiscountAmount(req.getDiscountAmount() != null ? req.getDiscountAmount() : BigDecimal.ZERO);
        order.setSaleAmount(req.getSaleAmount() != null ? req.getSaleAmount() : BigDecimal.ZERO);
        order.setReceivedAmount(req.getReceivedAmount() != null ? req.getReceivedAmount() : BigDecimal.ZERO);
        order.setPayStatus(req.getPayStatus() != null ? req.getPayStatus() : 0);
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setReceiverAddress(req.getReceiverAddress());
        order.setExpressCompany(req.getExpressCompany());
        order.setExpressNo(req.getExpressNo());
        order.setExpressFee(req.getExpressFee());
        order.setSaleDate(req.getSaleDate());
        order.setRemark(req.getRemark());
    }

    private void saveItems(Long saleId, String saleNo, List<SaleItemSaveReq> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        for (SaleItemSaveReq itemReq : items) {
            WmsSaleOrderItem item = new WmsSaleOrderItem();
            item.setSaleId(saleId);
            item.setSaleNo(saleNo);
            item.setLineNo(itemReq.getLineNo());
            item.setSkuId(itemReq.getSkuId());
            item.setSkuCode(itemReq.getSkuCode());
            item.setInnerCode(itemReq.getInnerCode());
            item.setSkuName(itemReq.getSkuName());
            item.setSpecText(itemReq.getSpecText());
            item.setUnitId(itemReq.getUnitId());
            item.setUnitName(itemReq.getUnitName());
            item.setQuantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 0);
            item.setSalePrice(itemReq.getSalePrice() != null ? itemReq.getSalePrice() : BigDecimal.ZERO);
            item.setCostPrice(itemReq.getCostPrice() != null ? itemReq.getCostPrice() : BigDecimal.ZERO);
            item.setDiscountRate(itemReq.getDiscountRate() != null ? itemReq.getDiscountRate() : BigDecimal.ZERO);
            item.setSubtotal(itemReq.getSubtotal() != null ? itemReq.getSubtotal() : BigDecimal.ZERO);
            item.setProfit(itemReq.getProfit() != null ? itemReq.getProfit() : BigDecimal.ZERO);
            item.setDeliveredQty(itemReq.getDeliveredQty() != null ? itemReq.getDeliveredQty() : 0);
            item.setRemark(itemReq.getRemark());
            itemMapper.insert(item);
        }
    }

    private void insertStatusLog(Long billId, String billNo, Integer fromStatus, Integer toStatus, String remark) {
        WmsSaleStatusLog logEntity = new WmsSaleStatusLog();
        logEntity.setBillId(billId);
        logEntity.setBillNo(billNo);
        logEntity.setFromStatus(fromStatus);
        logEntity.setToStatus(toStatus);
        logEntity.setOperateBy(AuthContextHolder.getUserId());
        logEntity.setOperateTime(LocalDateTime.now());
        logEntity.setRemark(remark);
        statusLogMapper.insert(logEntity);
    }

    private static class SaleStockOutChangeItem implements ChangeItem {
        private final WmsStockOutItem item;
        private final Long warehouseId;

        SaleStockOutChangeItem(WmsStockOutItem item, Long warehouseId) {
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
            return null;
        }

        @Override
        public String getBatchNo() {
            return "";
        }

        @Override
        public Integer getQty() {
            return item.getActualQty() != null ? item.getActualQty() : 0;
        }

        @Override
        public BigDecimal getCostPrice() {
            return item.getCostPrice();
        }

        @Override
        public String getRemark() {
            return item.getRemark();
        }

        @Override
        public String getInnerCode() {
            return item.getInnerCode();
        }
    }
}
