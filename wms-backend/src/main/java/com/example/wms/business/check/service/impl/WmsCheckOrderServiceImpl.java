package com.example.wms.business.check.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import com.example.wms.business.basedata.goods.mapper.WmsGoodsSkuMapper;
import com.example.wms.business.check.dto.req.CheckAuditReq;
import com.example.wms.business.check.dto.req.CheckItemInputReq;
import com.example.wms.business.check.dto.req.CheckPageReq;
import com.example.wms.business.check.dto.req.CheckSaveReq;
import com.example.wms.business.check.dto.rsp.CheckDetailRsp;
import com.example.wms.business.check.entity.WmsCheckOrder;
import com.example.wms.business.check.entity.WmsCheckOrderItem;
import com.example.wms.business.check.entity.WmsCheckStatusLog;
import com.example.wms.business.check.mapper.WmsCheckOrderItemMapper;
import com.example.wms.business.check.mapper.WmsCheckOrderMapper;
import com.example.wms.business.check.mapper.WmsCheckStatusLogMapper;
import com.example.wms.business.check.service.WmsCheckOrderService;
import com.example.wms.business.inventory.entity.WmsInventory;
import com.example.wms.business.inventory.handler.ChangeType;
import com.example.wms.business.inventory.handler.InventoryChangeHandler;
import com.example.wms.business.inventory.handler.InventoryChangeHandlerFactory;
import com.example.wms.business.inventory.mapper.WmsInventoryMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WmsCheckOrderServiceImpl extends ServiceImpl<WmsCheckOrderMapper, WmsCheckOrder> implements WmsCheckOrderService {

    private final WmsCheckOrderItemMapper itemMapper;
    private final WmsCheckStatusLogMapper statusLogMapper;
    private final WmsInventoryMapper inventoryMapper;
    private final WmsGoodsSkuMapper goodsSkuMapper;
    private final InventoryChangeHandlerFactory inventoryHandlerFactory;

    @Override
    public PageRsp<WmsCheckOrder> pageCheck(CheckPageReq req) {
        LambdaQueryWrapper<WmsCheckOrder> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsCheckOrder::getCreateTime);
        Page<WmsCheckOrder> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public CheckDetailRsp getDetailById(Long id) {
        CheckDetailRsp rsp = new CheckDetailRsp();
        WmsCheckOrder order = this.getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        rsp.setOrder(order);
        rsp.setItems(itemMapper.selectByCheckId(id));
        rsp.setStatusLogs(statusLogMapper.selectByBillId(id));
        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCheck(CheckSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsCheckOrder::getCheckNo, req.getCheckNo())
                .count();
        if (count > 0) {
            throw new BizException("盘点单号已存在");
        }
        WmsCheckOrder order = new WmsCheckOrder();
        copyOrderFields(order, req);
        order.setStatus(0);
        order.setProfitQty(0);
        order.setLossQty(0);
        this.save(order);
        insertStatusLog(order.getCheckId(), order.getCheckNo(), null, 0, "保存草稿");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckDetailRsp loadInventory(Long checkId, Long warehouseId, Long areaId) {
        WmsCheckOrder order = this.getById(checkId);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可加载库存");
        }
        Long queryWarehouse = warehouseId != null ? warehouseId : order.getWarehouseId();
        LambdaQueryWrapper<WmsInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsInventory::getWarehouseId, queryWarehouse);
        if (areaId != null) {
            wrapper.inSql(WmsInventory::getLocationId,
                    "SELECT location_id FROM wms_location WHERE area_id = " + areaId + " AND deleted = 0");
        }
        wrapper.gt(WmsInventory::getQuantity, 0);
        List<WmsInventory> invList = inventoryMapper.selectList(wrapper);

        itemMapper.deleteByCheckId(checkId);

        Set<Long> skuIds = invList.stream()
                .map(WmsInventory::getSkuId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WmsGoodsSku> skuMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<WmsGoodsSku> skus = goodsSkuMapper.selectBatchIds(skuIds);
            for (WmsGoodsSku sku : skus) {
                skuMap.put(sku.getSkuId(), sku);
            }
        }

        int lineNo = 1;
        for (WmsInventory inv : invList) {
            WmsCheckOrderItem item = new WmsCheckOrderItem();
            item.setCheckId(checkId);
            item.setCheckNo(order.getCheckNo());
            item.setLineNo(lineNo++);
            item.setSkuId(inv.getSkuId());
            WmsGoodsSku sku = skuMap.get(inv.getSkuId());
            if (sku != null) {
                item.setSkuCode(sku.getSkuCode());
                item.setInnerCode(sku.getInnerCode());
                item.setSpecText(sku.getSpecText());
            }
            item.setBatchNo(inv.getBatchNo());
            item.setLocationId(inv.getLocationId());
            item.setBookQty(inv.getQuantity());
            item.setActualQty(null);
            item.setDiffQty(0);
            item.setCostPrice(inv.getCostPrice());
            item.setDiffAmount(BigDecimal.ZERO);
            itemMapper.insert(item);
        }

        order.setTotalSkuCount(invList.size());
        order.setWarehouseId(queryWarehouse);
        if (areaId != null) {
            order.setAreaId(areaId);
        }
        this.updateById(order);

        CheckDetailRsp rsp = new CheckDetailRsp();
        rsp.setOrder(this.getById(checkId));
        rsp.setItems(itemMapper.selectByCheckId(checkId));
        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startCheck(Long id) {
        WmsCheckOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        List<WmsCheckOrderItem> items = itemMapper.selectByCheckId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        exist.setStatus(1);
        exist.setCheckStartTime(LocalDateTime.now());
        this.updateById(exist);
        insertStatusLog(id, exist.getCheckNo(), 0, 1, "开始盘点");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inputActual(CheckItemInputReq req) {
        if (req.getCheckId() == null) {
            throw new BizException("盘点单ID不能为空");
        }
        WmsCheckOrder exist = this.getById(req.getCheckId());
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅盘点中状态可录入实盘数据");
        }
        if (CollectionUtils.isEmpty(req.getItems())) {
            return;
        }
        for (CheckItemInputReq.ItemInput input : req.getItems()) {
            if (input.getItemId() == null) {
                continue;
            }
            WmsCheckOrderItem item = itemMapper.selectById(input.getItemId());
            if (item == null || (item.getCheckId() != null && !item.getCheckId().equals(req.getCheckId()))) {
                continue;
            }
            int bookQty = item.getBookQty() != null ? item.getBookQty() : 0;
            int actualQty = input.getActualQty() != null ? input.getActualQty() : 0;
            int diffQty = actualQty - bookQty;
            BigDecimal price = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
            BigDecimal diffAmount = price.multiply(BigDecimal.valueOf(diffQty));
            item.setActualQty(actualQty);
            item.setDiffQty(diffQty);
            item.setDiffAmount(diffAmount);
            itemMapper.updateById(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishCheck(Long id) {
        WmsCheckOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅盘点中状态可完成盘点");
        }
        List<WmsCheckOrderItem> items = itemMapper.selectByCheckId(id);
        int profitQty = 0;
        int lossQty = 0;
        BigDecimal profitAmount = BigDecimal.ZERO;
        BigDecimal lossAmount = BigDecimal.ZERO;
        for (WmsCheckOrderItem item : items) {
            int diff = item.getDiffQty() != null ? item.getDiffQty() : 0;
            BigDecimal amount = item.getDiffAmount() != null ? item.getDiffAmount() : BigDecimal.ZERO;
            if (diff > 0) {
                profitQty += diff;
                profitAmount = profitAmount.add(amount);
            } else if (diff < 0) {
                lossQty += -diff;
                lossAmount = lossAmount.add(amount.abs());
            }
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(2);
        exist.setCheckEndTime(LocalDateTime.now());
        exist.setProfitQty(profitQty);
        exist.setLossQty(lossQty);
        exist.setProfitAmount(profitAmount);
        exist.setLossAmount(lossAmount);
        LambdaQueryWrapper<WmsCheckOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsCheckOrder::getCheckId, id)
                .eq(WmsCheckOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        insertStatusLog(id, exist.getCheckNo(), 1, 2, "完成盘点");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditCheck(CheckAuditReq req) {
        List<Long> effectiveIds = req.effectiveIds();
        if (effectiveIds.isEmpty()) {
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
        WmsCheckOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "盘点单[" + exist.getCheckNo() + "]状态不允许审核");
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        if (pass) {
            exist.setStatus(3);
            exist.setAuditBy(AuthContextHolder.getUserId());
            exist.setAuditTime(LocalDateTime.now());
            LambdaQueryWrapper<WmsCheckOrder> lockWrapper = new LambdaQueryWrapper<>();
            lockWrapper.eq(WmsCheckOrder::getCheckId, id)
                    .eq(WmsCheckOrder::getVersion, oldVersion);
            boolean updated = this.update(exist, lockWrapper);
            if (!updated) {
                throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
            }
            insertStatusLog(id, exist.getCheckNo(), 2, 3, remark);
        } else {
            exist.setStatus(1);
            exist.setAuditBy(null);
            exist.setAuditTime(null);
            LambdaQueryWrapper<WmsCheckOrder> lockWrapper = new LambdaQueryWrapper<>();
            lockWrapper.eq(WmsCheckOrder::getCheckId, id)
                    .eq(WmsCheckOrder::getVersion, oldVersion);
            boolean updated = this.update(exist, lockWrapper);
            if (!updated) {
                throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
            }
            insertStatusLog(id, exist.getCheckNo(), 2, 1, "审核不通过：" + remark);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCheck(Long id, String remark) {
        WmsCheckOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已审核状态可处理");
        }
        List<WmsCheckOrderItem> items = itemMapper.selectByCheckId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(4);
        LambdaQueryWrapper<WmsCheckOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsCheckOrder::getCheckId, id)
                .eq(WmsCheckOrder::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        List<WmsCheckOrderItem> diffItems = new ArrayList<>();
        for (WmsCheckOrderItem item : items) {
            int diff = item.getDiffQty() != null ? item.getDiffQty() : 0;
            if (diff == 0) {
                continue;
            }
            item.setWarehouseId(exist.getWarehouseId());
            diffItems.add(item);
        }
        if (!diffItems.isEmpty()) {
            InventoryChangeHandler handler = inventoryHandlerFactory.getHandler(ChangeType.CHECK);
            handler.handle(exist.getCheckId(), exist.getCheckNo(), LocalDateTime.now(), diffItems);
        }
        insertStatusLog(id, exist.getCheckNo(), 3, 4, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidCheck(Long id, String remark) {
        WmsCheckOrder exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() == 5) {
            return;
        }
        if (exist.getStatus() != null && exist.getStatus() >= 4) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已处理盘点单不允许作废");
        }
        Integer fromStatus = exist.getStatus();
        exist.setStatus(5);
        this.updateById(exist);
        insertStatusLog(id, exist.getCheckNo(), fromStatus, 5, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(BatchAuditReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        CheckAuditReq auditReq = new CheckAuditReq();
        auditReq.setIds(req.getIds().stream().map(Long::valueOf).toList());
        auditReq.setRemark(req.getRemark());
        auditCheck(auditReq);
    }

    @Override
    public byte[] export(CheckPageReq req) {
        return new byte[0];
    }

    @Override
    public List<Map<String, Object>> diffSummary(Long warehouseId) {
        LambdaQueryWrapper<WmsCheckOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WmsCheckOrder::getStatus, 2, 3, 4);
        if (warehouseId != null) {
            wrapper.eq(WmsCheckOrder::getWarehouseId, warehouseId);
        }
        List<WmsCheckOrder> orders = this.list(wrapper);
        Map<String, Map<String, Object>> grouped = new HashMap<>();
        for (WmsCheckOrder o : orders) {
            String key = (o.getWarehouseId() != null ? o.getWarehouseId() : 0) + "_"
                    + (o.getAreaId() != null ? o.getAreaId() : 0);
            Map<String, Object> row = grouped.computeIfAbsent(key, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("warehouseId", o.getWarehouseId());
                m.put("warehouseName", o.getWarehouseName());
                m.put("areaId", o.getAreaId());
                m.put("areaName", o.getAreaName());
                m.put("orderCount", 0);
                m.put("profitQty", 0);
                m.put("lossQty", 0);
                m.put("profitAmount", BigDecimal.ZERO);
                m.put("lossAmount", BigDecimal.ZERO);
                return m;
            });
            row.put("orderCount", (Integer) row.get("orderCount") + 1);
            row.put("profitQty", (Integer) row.get("profitQty") + (o.getProfitQty() != null ? o.getProfitQty() : 0));
            row.put("lossQty", (Integer) row.get("lossQty") + (o.getLossQty() != null ? o.getLossQty() : 0));
            row.put("profitAmount", ((BigDecimal) row.get("profitAmount"))
                    .add(o.getProfitAmount() != null ? o.getProfitAmount() : BigDecimal.ZERO));
            row.put("lossAmount", ((BigDecimal) row.get("lossAmount"))
                    .add(o.getLossAmount() != null ? o.getLossAmount() : BigDecimal.ZERO));
        }
        return new ArrayList<>(grouped.values());
    }

    private LambdaQueryWrapper<WmsCheckOrder> buildQueryWrapper(CheckPageReq req) {
        LambdaQueryWrapper<WmsCheckOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getCheckNo())) {
            wrapper.like(WmsCheckOrder::getCheckNo, req.getCheckNo());
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsCheckOrder::getWarehouseId, req.getWarehouseId());
        }
        if (req.getCheckType() != null) {
            wrapper.eq(WmsCheckOrder::getCheckType, req.getCheckType());
        }
        if (req.getStatus() != null) {
            wrapper.eq(WmsCheckOrder::getStatus, req.getStatus());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(WmsCheckOrder::getCheckDate, req.getDateRangeStart().toLocalDate());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(WmsCheckOrder::getCheckDate, req.getDateRangeEnd().toLocalDate());
        }
        return wrapper;
    }

    private void copyOrderFields(WmsCheckOrder order, CheckSaveReq req) {
        order.setCheckNo(req.getCheckNo());
        order.setWarehouseId(req.getWarehouseId());
        order.setWarehouseName(req.getWarehouseName());
        order.setAreaId(req.getAreaId());
        order.setAreaName(req.getAreaName());
        order.setCheckType(req.getCheckType());
        order.setCheckDate(req.getCheckDate());
        order.setCheckerId(req.getCheckerId() != null ? req.getCheckerId() : AuthContextHolder.getUserId());
        order.setCheckerName(req.getCheckerName());
        order.setRemark(req.getRemark());
    }

    private void insertStatusLog(Long billId, String billNo, Integer fromStatus, Integer toStatus, String remark) {
        WmsCheckStatusLog log = new WmsCheckStatusLog();
        log.setBillId(billId);
        log.setBillNo(billNo);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperateBy(AuthContextHolder.getUserId());
        log.setOperateTime(LocalDateTime.now());
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }
}
