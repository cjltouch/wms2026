package com.example.wms.business.stockout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.business.inventory.entity.WmsInventory;
import com.example.wms.business.inventory.entity.WmsInventoryLog;
import com.example.wms.business.inventory.handler.ChangeType;
import com.example.wms.business.inventory.handler.InventoryChangeHandler;
import com.example.wms.business.inventory.handler.InventoryChangeHandlerFactory;
import com.example.wms.business.inventory.mapper.WmsInventoryLogMapper;
import com.example.wms.business.inventory.mapper.WmsInventoryMapper;
import com.example.wms.common.utils.SnowflakeId;
import com.example.wms.business.stockout.dto.req.StockOutAuditReq;
import com.example.wms.business.stockout.dto.req.StockOutItemSaveReq;
import com.example.wms.business.stockout.dto.req.StockOutPageReq;
import com.example.wms.business.stockout.dto.req.StockOutSaveReq;
import com.example.wms.business.stockout.dto.rsp.StockOutDetailRsp;
import com.example.wms.business.stockout.entity.WmsStockOut;
import com.example.wms.business.stockout.entity.WmsStockOutItem;
import com.example.wms.business.stockout.entity.WmsStockOutStatusLog;
import com.example.wms.business.stockout.mapper.WmsStockOutItemMapper;
import com.example.wms.business.stockout.mapper.WmsStockOutMapper;
import com.example.wms.business.stockout.mapper.WmsStockOutStatusLogMapper;
import com.example.wms.business.stockout.service.WmsStockOutService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsStockOutServiceImpl extends ServiceImpl<WmsStockOutMapper, WmsStockOut> implements WmsStockOutService {

    private final WmsStockOutItemMapper itemMapper;
    private final WmsStockOutStatusLogMapper statusLogMapper;
    private final InventoryChangeHandlerFactory inventoryHandlerFactory;
    private final WmsInventoryMapper inventoryMapper;
    private final WmsInventoryLogMapper inventoryLogMapper;
    private final com.example.wms.system.service.SysUserService sysUserService;
    private final com.example.wms.business.common.SkuMasterDataLoader skuMasterDataLoader;

    @Override
    public PageRsp<WmsStockOut> pageStockOut(StockOutPageReq req) {
        LambdaQueryWrapper<WmsStockOut> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsStockOut::getCreateTime);
        Page<WmsStockOut> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        fillOperatorNames(page.getRecords());
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    /**
     * 批量填充出库人/审核人姓名（优先真实姓名，其次昵称，最后用户名）
     */
    private void fillOperatorNames(List<WmsStockOut> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        List<Long> userIds = new ArrayList<>();
        for (WmsStockOut o : orders) {
            if (o.getCreateBy() != null) {
                userIds.add(o.getCreateBy());
            }
            if (o.getAuditBy() != null) {
                userIds.add(o.getAuditBy());
            }
        }
        Map<Long, String> nameMap = loadUserNameMap(userIds);
        for (WmsStockOut o : orders) {
            o.setOutByName(o.getCreateBy() != null ? nameMap.get(o.getCreateBy()) : null);
            o.setAuditName(o.getAuditBy() != null ? nameMap.get(o.getAuditBy()) : null);
        }
    }

    private Map<Long, String> loadUserNameMap(List<Long> userIds) {
        List<Long> distinct = userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, String> map = new HashMap<>();
        sysUserService.listByIds(distinct).forEach(u -> map.put(u.getUserId(),
                StringUtils.hasText(u.getRealName()) ? u.getRealName()
                        : (StringUtils.hasText(u.getNickName()) ? u.getNickName() : u.getUserName())));
        return map;
    }

    @Override
    public StockOutDetailRsp getDetailById(Long id) {
        StockOutDetailRsp rsp = new StockOutDetailRsp();
        WmsStockOut order = this.getById(id);
        if (order == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        rsp.setOrder(order);
        fillOperatorNames(List.of(order));
        List<WmsStockOutStatusLog> logs = statusLogMapper.selectByBillId(id);
        fillLogOperateNames(logs);
        List<WmsStockOutItem> items = itemMapper.selectByStockOutId(id);
        enrichItemMasterData(items);
        rsp.setItems(items);
        rsp.setStatusLogs(logs);
        return rsp;
    }

    /**
     * 明细行按SKU主数据补齐规格/商品名/单位/供应商名称（手工建单未落库这些冗余字段时）
     */
    private void enrichItemMasterData(List<WmsStockOutItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Map<Long, com.example.wms.business.common.SkuMasterDataLoader.SkuMaster> master =
                skuMasterDataLoader.load(items.stream().map(WmsStockOutItem::getSkuId).toList());
        for (WmsStockOutItem item : items) {
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
            item.setSupplierName(md.supplierName);
        }
    }

    /**
     * 状态日志操作人ID解析为真实姓名
     */
    private void fillLogOperateNames(List<WmsStockOutStatusLog> logs) {
        if (CollectionUtils.isEmpty(logs)) {
            return;
        }
        List<Long> userIds = logs.stream().map(WmsStockOutStatusLog::getOperateBy)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = loadUserNameMap(userIds);
        for (WmsStockOutStatusLog l : logs) {
            if (l.getOperateBy() != null) {
                l.setOperateName(nameMap.get(l.getOperateBy()));
            }
        }
    }

    @Override
    public WmsStockOut getByStockOutNo(String stockOutNo) {
        return this.lambdaQuery()
                .eq(WmsStockOut::getStockOutNo, stockOutNo)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveStockOut(StockOutSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsStockOut::getStockOutNo, req.getStockOutNo())
                .count();
        if (count > 0) {
            throw new BizException("出库单号已存在");
        }
        WmsStockOut order = new WmsStockOut();
        copyOrderFields(order, req);
        order.setStatus(0);
        this.save(order);
        saveItems(order, req.getItems());
        this.updateById(order);
        insertStatusLog(order.getStockOutId(), order.getStockOutNo(), null, 0, "保存草稿", "CREATE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockOut(StockOutSaveReq req) {
        if (req.getStockId() == null) {
            throw new BizException("出库单ID不能为空");
        }
        WmsStockOut exist = this.getById(req.getStockId());
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0 && exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        Integer fromStatus = exist.getStatus();
        copyOrderFields(exist, req);
        itemMapper.deleteByStockOutId(req.getStockId());
        saveItems(exist, req.getItems());
        this.updateById(exist);
        if (!fromStatus.equals(exist.getStatus())) {
            insertStatusLog(exist.getStockOutId(), exist.getStockOutNo(), fromStatus, exist.getStatus(), req.getRemark(), "UPDATE");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockOut(Long id) {
        WmsStockOut exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅草稿状态可删除");
        }
        this.removeById(id);
        itemMapper.deleteByStockOutId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitStockOut(Long id) {
        WmsStockOut exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != 0) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED);
        }
        List<WmsStockOutItem> items = itemMapper.selectByStockOutId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        // 提交前库存校验：同一SKU数量合并后与仓库可用库存比较，不足则禁止提交并明确提示
        Map<Long, Integer> needBySku = new HashMap<>();
        Map<Long, String> codeBySku = new HashMap<>();
        for (WmsStockOutItem item : items) {
            int qty = item.getActualQty() != null && item.getActualQty() > 0
                    ? item.getActualQty()
                    : (item.getExpectedQty() != null ? item.getExpectedQty() : 0);
            if (qty <= 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "商品[" + item.getSkuCode() + "]出库数量必须大于0");
            }
            needBySku.merge(item.getSkuId(), qty, Integer::sum);
            codeBySku.put(item.getSkuId(), item.getSkuCode());
        }
        for (Map.Entry<Long, Integer> entry : needBySku.entrySet()) {
            Long skuId = entry.getKey();
            int need = entry.getValue();
            // 汇总该仓库下该SKU所有库存行的可用量
            List<WmsInventory> invs = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, exist.getWarehouseId())
                    .eq(WmsInventory::getSkuId, skuId));
            int available = invs.stream()
                    .mapToInt(i -> i.getAvailableQty() != null ? i.getAvailableQty() : 0).sum();
            if (need > available) {
                throw new BizException(ResultCode.STOCK_INSUFFICIENT, "商品[" + codeBySku.get(skuId) + "]申请出库" + need
                        + "件，当前可用库存仅" + available + "件，库存不足，不可提交出库");
            }
        }
        exist.setStatus(1);
        this.updateById(exist);
        log.info("出库单{}提交成功，状态:0→1", exist.getStockOutNo());
        insertStatusLog(id, exist.getStockOutNo(), 0, 1, "提交", "SUBMIT");
    }

    @Override
    public List<Map<String, Object>> previewAllocation(Long id) {
        WmsStockOut exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() < 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已提交状态可预览分配");
        }
        List<WmsStockOutItem> items = itemMapper.selectByStockOutId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (WmsStockOutItem item : items) {
            Map<String, Object> map = new HashMap<>();
            map.put("itemId", item.getItemId());
            map.put("skuId", item.getSkuId());
            map.put("skuCode", item.getSkuCode());
            map.put("skuName", item.getSkuName());
            map.put("expectedQty", item.getExpectedQty());
            map.put("allocationRule", item.getAllocationRule() != null ? item.getAllocationRule() : exist.getAllocationRule());
            map.put("allocationDetail", new ArrayList<>());
            result.add(map);
        }
        log.info("出库单{}分配预览完成，共{}行", exist.getStockOutNo(), result.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockInventory(Long id) {
        WmsStockOut exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 1) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已提交状态可锁定库存");
        }
        List<WmsStockOutItem> items = itemMapper.selectByStockOutId(id);
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(2);
        LambdaQueryWrapper<WmsStockOut> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsStockOut::getStockOutId, id)
                .eq(WmsStockOut::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        // 真实锁定：可用量转锁定量（quantity 不变），防止其他出库单超卖
        lockItems(exist, items);
        log.info("出库单{}锁定库存，状态:1→2", exist.getStockOutNo());
        insertStatusLog(id, exist.getStockOutNo(), 1, 2, "锁定库存", "LOCK");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickConfirm(Long id) {
        WmsStockOut exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已锁定状态可拣货确认");
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(3);
        LambdaQueryWrapper<WmsStockOut> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsStockOut::getStockOutId, id)
                .eq(WmsStockOut::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        log.info("出库单{}拣货确认，状态:2→3", exist.getStockOutNo());
        insertStatusLog(id, exist.getStockOutNo(), 2, 3, "拣货确认", "PICK");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditStockOut(StockOutAuditReq req) {
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
        WmsStockOut exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 3) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "出库单[" + exist.getStockOutNo() + "]状态不允许审核");
        }
        List<WmsStockOutItem> items = itemMapper.selectByStockOutId(id);
        if (pass && CollectionUtils.isEmpty(items)) {
            throw new BizException(ResultCode.DETAIL_EMPTY);
        }
        if (pass) {
            for (WmsStockOutItem item : items) {
                item.setWarehouseId(exist.getWarehouseId());
            }
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        if (pass) {
            exist.setStatus(4);
            exist.setAuditBy(AuthContextHolder.getUserId());
            exist.setAuditTime(LocalDateTime.now());
        } else {
            exist.setStatus(2);
        }
        LambdaQueryWrapper<WmsStockOut> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsStockOut::getStockOutId, id)
                .eq(WmsStockOut::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        if (pass) {
            // 锁定转出库：quantity 与 locked_qty 同减（available 已在锁定阶段扣减）
            confirmLockedOut(exist, items);
            log.info("出库单{}审核完成，状态:3→4，锁定库存已转出库", exist.getStockOutNo());
            insertStatusLog(id, exist.getStockOutNo(), 3, 4, remark, "AUDIT");
        } else {
            log.info("出库单{}审核不通过，状态:3→2，原因:{}", exist.getStockOutNo(), remark);
            insertStatusLog(id, exist.getStockOutNo(), 3, 2, "审核不通过：" + remark, "REJECT");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditStockOut(Long id, String remark) {
        WmsStockOut exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() == null || exist.getStatus() != 4) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已审核状态可反审核");
        }
        int oldVersion = exist.getVersion() != null ? exist.getVersion() : 0;
        exist.setStatus(3);
        exist.setAuditBy(null);
        exist.setAuditTime(null);
        LambdaQueryWrapper<WmsStockOut> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(WmsStockOut::getStockOutId, id)
                .eq(WmsStockOut::getVersion, oldVersion);
        boolean updated = this.update(exist, lockWrapper);
        if (!updated) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        // 反审核回补：quantity 与 locked_qty 同加，库存回到锁定占用状态（单据可再次审核）
        List<WmsStockOutItem> items = itemMapper.selectByStockOutId(id);
        if (!CollectionUtils.isEmpty(items)) {
            restoreLockedItems(exist, items);
        }
        log.info("出库单{}反审核，状态:4→3，库存已回补至锁定状态", exist.getStockOutNo());
        insertStatusLog(id, exist.getStockOutNo(), 4, 3, remark, "REJECT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidStockOut(Long id, String remark) {
        WmsStockOut exist = this.getById(id);
        if (exist == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (exist.getStatus() != null && exist.getStatus() == 5) {
            return;
        }
        if (exist.getStatus() != null && exist.getStatus() >= 4) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已审核出库单不允许作废");
        }
        Integer fromStatus = exist.getStatus();
        exist.setStatus(5);
        this.updateById(exist);
        // 已锁定的单据作废时需要释放锁定量回可用量
        if (fromStatus != null && (fromStatus == 2 || fromStatus == 3)) {
            List<WmsStockOutItem> items = itemMapper.selectByStockOutId(id);
            if (!CollectionUtils.isEmpty(items)) {
                unlockItems(exist, items);
            }
        }
        log.info("出库单{}作废，状态:{}→5", exist.getStockOutNo(), fromStatus);
        insertStatusLog(id, exist.getStockOutNo(), fromStatus, 5, remark, "VOID");
    }

    /**
     * 锁定库存：按明细将可用量转锁定量，可用不足时抛异常回滚。
     * 锁定是预占动作，不产生库存流水。
     */
    private void lockItems(WmsStockOut order, List<WmsStockOutItem> items) {
        for (WmsStockOutItem item : items) {
            Integer qty = item.getQty() != null ? item.getQty() : item.getExpectedQty();
            if (qty == null || qty <= 0) {
                continue;
            }
            WmsInventory inv = findInventory(order.getWarehouseId(), item);
            if (inv == null || (inv.getAvailableQty() != null && inv.getAvailableQty() < qty)) {
                throw new BizException("库存不足或库存记录不存在，SKU[" + item.getSkuCode() + "]无法锁定");
            }
            int rows = inventoryMapper.lockStock(order.getWarehouseId(), item.getSkuId(),
                    inv.getLocationId(), inv.getBatchNo(), qty);
            if (rows == 0) {
                throw new BizException("库存不足或库存记录不存在，SKU[" + item.getSkuCode() + "]无法锁定");
            }
            // 将实际锁定的库存行库位/批次回写明细，确保后续确认/解锁/回补定位到同一行
            item.setLocationId(inv.getLocationId());
            item.setBatchNo(inv.getBatchNo());
            item.setLocationCode(inv.getLocationCode());
            itemMapper.updateById(item);
        }
    }

    /**
     * 出库确认：锁定量转真实出库（quantity/locked_qty 同减），并记录库存流水。
     */
    private void confirmLockedOut(WmsStockOut order, List<WmsStockOutItem> items) {
        LocalDateTime now = order.getAuditTime() != null ? order.getAuditTime() : LocalDateTime.now();
        for (WmsStockOutItem item : items) {
            Integer qty = item.getQty();
            if (qty == null || qty <= 0) {
                continue;
            }
            WmsInventory inv = findInventory(order.getWarehouseId(), item);
            int beforeQty = inv != null && inv.getQuantity() != null ? inv.getQuantity() : 0;
            int rows = inventoryMapper.confirmLockStock(order.getWarehouseId(), item.getSkuId(),
                    inv != null ? inv.getLocationId() : item.getLocationId(),
                    inv != null ? inv.getBatchNo() : item.getBatchNo(), qty, now);
            if (rows == 0) {
                throw new BizException("锁定库存异常，SKU[" + item.getSkuCode() + "]无法出库");
            }
            insertOutLog(order, item, qty, beforeQty, now, "出库确认扣减");
        }
    }

    /** 解锁：锁定量释放回可用量（作废时调用），不产生库存流水 */
    private void unlockItems(WmsStockOut order, List<WmsStockOutItem> items) {
        for (WmsStockOutItem item : items) {
            Integer qty = item.getQty() != null ? item.getQty() : item.getExpectedQty();
            if (qty == null || qty <= 0) {
                continue;
            }
            WmsInventory inv = findInventory(order.getWarehouseId(), item);
            if (inv == null) {
                log.warn("出库单{}作废解锁未匹配到库存行，SKU[{}]", order.getStockOutNo(), item.getSkuCode());
                continue;
            }
            int rows = inventoryMapper.unlockStock(order.getWarehouseId(), item.getSkuId(),
                    inv.getLocationId(), inv.getBatchNo(), qty);
            if (rows == 0) {
                log.warn("出库单{}作废解锁未命中锁定量，SKU[{}] 位置[{}]/批次[{}]",
                        order.getStockOutNo(), item.getSkuCode(), inv.getLocationId(), inv.getBatchNo());
            }
        }
    }

    /** 出库反审核回补：quantity/locked_qty 同加，写正向库存流水 */
    private void restoreLockedItems(WmsStockOut order, List<WmsStockOutItem> items) {
        LocalDateTime now = LocalDateTime.now();
        for (WmsStockOutItem item : items) {
            Integer qty = item.getQty();
            if (qty == null || qty <= 0) {
                continue;
            }
            WmsInventory inv = findInventory(order.getWarehouseId(), item);
            if (inv == null) {
                log.warn("出库单{}反审核回补未匹配到库存行，SKU[{}]", order.getStockOutNo(), item.getSkuCode());
                continue;
            }
            int beforeQty = inv.getQuantity() != null ? inv.getQuantity() : 0;
            int rows = inventoryMapper.restoreLockedStock(order.getWarehouseId(), item.getSkuId(),
                    inv.getLocationId(), inv.getBatchNo(), qty);
            if (rows == 0) {
                log.warn("出库单{}反审核回补未命中库存行，SKU[{}] 位置[{}]/批次[{}]",
                        order.getStockOutNo(), item.getSkuCode(), inv.getLocationId(), inv.getBatchNo());
                continue;
            }
            insertOutLog(order, item, qty, beforeQty, now, "出库反审核回补");
        }
    }

    /**
     * 定位库存行：优先按明细的库位/批次精确匹配；
     * 明细未指定时回退到同仓库+SKU 下可用量最大的一行（不区分批次/库位）。
     */
    private WmsInventory findInventory(Long warehouseId, WmsStockOutItem item) {
        // 批次空串/纯空格统一按 null 处理，避免无批次库存（NULL 或 ''）匹配失败
        Long locationId = item.getLocationId();
        String batchNo = item.getBatchNo() == null || item.getBatchNo().trim().isEmpty()
                ? null : item.getBatchNo().trim();
        WmsInventory exact = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseId, warehouseId)
                .eq(WmsInventory::getSkuId, item.getSkuId())
                .eq(locationId != null, WmsInventory::getLocationId, locationId)
                .isNull(locationId == null, WmsInventory::getLocationId)
                .eq(batchNo != null, WmsInventory::getBatchNo, batchNo)
                .isNull(batchNo == null, WmsInventory::getBatchNo));
        if (exact != null) {
            return exact;
        }
        if (locationId == null && batchNo == null) {
            List<WmsInventory> candidates = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, warehouseId)
                    .eq(WmsInventory::getSkuId, item.getSkuId())
                    .orderByDesc(WmsInventory::getAvailableQty));
            return candidates.isEmpty() ? null : candidates.get(0);
        }
        return null;
    }

    private void insertOutLog(WmsStockOut order, WmsStockOutItem item, int qty,
                              int beforeQty, LocalDateTime now, String remark) {
        WmsInventoryLog logRow = new WmsInventoryLog();
        logRow.setLogId(SnowflakeId.getInstance().nextId());
        logRow.setBillId(order.getStockOutId());
        logRow.setBillNo(order.getStockOutNo());
        logRow.setBillTypeCode(ChangeType.STOCK_OUT_CONFIRM.getCode());
        logRow.setWarehouseId(order.getWarehouseId());
        logRow.setSkuId(item.getSkuId());
        logRow.setLocationId(item.getLocationId());
        logRow.setBatchNo(item.getBatchNo());
        logRow.setDirection(-1);
        logRow.setQtyChange(-qty);
        logRow.setUnitPrice(item.getCostPrice());
        logRow.setAmountChange((item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(qty)));
        logRow.setOperateBy(AuthContextHolder.getUserId());
        logRow.setOperateTime(now);
        logRow.setBeforeQty(beforeQty);
        logRow.setAfterQty(beforeQty - qty);
        logRow.setRemark(remark);
        inventoryLogMapper.insert(logRow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(BatchAuditReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        StockOutAuditReq auditReq = new StockOutAuditReq();
        auditReq.setIds(req.getIds().stream().map(Long::valueOf).toList());
        auditReq.setRemark(req.getRemark());
        auditStockOut(auditReq);
    }

    private LambdaQueryWrapper<WmsStockOut> buildQueryWrapper(StockOutPageReq req) {
        LambdaQueryWrapper<WmsStockOut> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getStockOutNo())) {
            wrapper.like(WmsStockOut::getStockOutNo, req.getStockOutNo());
        }
        if (req.getType() != null) {
            wrapper.eq(WmsStockOut::getType, req.getType());
        }
        if (StringUtils.hasText(req.getSourceBillNo())) {
            wrapper.like(WmsStockOut::getSourceBillNo, req.getSourceBillNo());
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsStockOut::getWarehouseId, req.getWarehouseId());
        }
        if (req.getCustomerId() != null) {
            wrapper.eq(WmsStockOut::getCustomerId, req.getCustomerId());
        }
        if (req.getStatus() != null) {
            wrapper.eq(WmsStockOut::getStatus, req.getStatus());
        }
        if (req.getOutBy() != null) {
            wrapper.eq(WmsStockOut::getOutBy, req.getOutBy());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(WmsStockOut::getCreateTime, req.getDateRangeStart());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(WmsStockOut::getCreateTime, req.getDateRangeEnd());
        }
        return wrapper;
    }

    private void copyOrderFields(WmsStockOut order, StockOutSaveReq req) {
        order.setStockOutNo(req.getStockOutNo());
        order.setType(req.getType());
        order.setSourceBillNo(req.getSourceBillNo());
        order.setSourceItemId(req.getSourceItemId());
        order.setWarehouseId(req.getWarehouseId());
        order.setWarehouseName(req.getWarehouseName());
        order.setCustomerId(req.getCustomerId());
        order.setCustomerName(req.getCustomerName());
        order.setOutBy(req.getOutBy() != null ? req.getOutBy() : AuthContextHolder.getUserId());
        order.setAllocationRule(req.getAllocationRule() != null ? req.getAllocationRule() : 1);
        order.setTotalQty(req.getTotalQty() != null ? req.getTotalQty() : 0);
        order.setTotalCost(req.getTotalCost() != null ? req.getTotalCost() : BigDecimal.ZERO);
        order.setTotalSale(req.getTotalSale() != null ? req.getTotalSale() : BigDecimal.ZERO);
        order.setRemark(req.getRemark());
    }

    private void saveItems(WmsStockOut order, List<StockOutItemSaveReq> items) {
        int totalQty = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalSale = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(items)) {
            Long stockOutId = order.getStockOutId();
            String stockOutNo = order.getStockOutNo();
            int lineNo = 1;
            for (StockOutItemSaveReq itemReq : items) {
                WmsStockOutItem item = new WmsStockOutItem();
                item.setStockOutId(stockOutId);
                item.setStockOutNo(stockOutNo);
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
                int actualQty = itemReq.getActualQty() != null ? itemReq.getActualQty()
                        : (itemReq.getExpectedQty() != null ? itemReq.getExpectedQty() : 0);
                // 出库数量以实际为准，未填实际时取预期；小计/汇总一律后端重算，不信任前端传值
                int qty = actualQty > 0 ? actualQty : expectedQty;
                item.setExpectedQty(expectedQty);
                item.setActualQty(actualQty);
                item.setDiffQty(itemReq.getDiffQty() != null ? itemReq.getDiffQty() : 0);
                item.setPickedQty(itemReq.getPickedQty() != null ? itemReq.getPickedQty() : 0);
                BigDecimal costPrice = itemReq.getCostPrice() != null ? itemReq.getCostPrice() : BigDecimal.ZERO;
                BigDecimal salePrice = itemReq.getSalePrice() != null ? itemReq.getSalePrice() : BigDecimal.ZERO;
                BigDecimal subtotalCost = costPrice.multiply(BigDecimal.valueOf(qty));
                BigDecimal subtotalSale = salePrice.multiply(BigDecimal.valueOf(qty));
                item.setCostPrice(costPrice);
                item.setSubtotalCost(subtotalCost);
                item.setSalePrice(salePrice);
                item.setSubtotalSale(subtotalSale);
                item.setDiscountRate(itemReq.getDiscountRate() != null ? itemReq.getDiscountRate() : BigDecimal.ZERO);
                item.setAllocationRule(itemReq.getAllocationRule());
                item.setAllocationJson(itemReq.getAllocationJson());
                // 批次空串归一为 null，与库存维度保持一致
                item.setBatchNo(org.springframework.util.StringUtils.hasText(itemReq.getBatchNo())
                        ? itemReq.getBatchNo().trim() : null);
                item.setProduceDate(itemReq.getProduceDate());
                item.setExpireDate(itemReq.getExpireDate());
                item.setLocationId(itemReq.getLocationId());
                item.setLocationCode(itemReq.getLocationCode());
                item.setSnList(itemReq.getSnList());
                item.setRemark(itemReq.getRemark());
                itemMapper.insert(item);
                totalQty += qty;
                totalCost = totalCost.add(subtotalCost);
                totalSale = totalSale.add(subtotalSale);
                lineNo++;
            }
        }
        // 表头总数量/总成本/总销售金额由明细汇总得出
        order.setTotalQty(totalQty);
        order.setTotalCost(totalCost);
        order.setTotalSale(totalSale);
    }

    private void insertStatusLog(Long billId, String billNo, Integer fromStatus, Integer toStatus, String remark, String operateType) {
        WmsStockOutStatusLog logEntity = new WmsStockOutStatusLog();
        logEntity.setBillId(billId);
        logEntity.setBillNo(billNo);
        logEntity.setFromStatus(fromStatus);
        logEntity.setToStatus(toStatus);
        logEntity.setOperateType(operateType);
        logEntity.setOperateBy(AuthContextHolder.getUserId());
        var loginUser = AuthContextHolder.get();
        if (loginUser != null && StringUtils.hasText(loginUser.getNickname())) {
            logEntity.setOperateName(loginUser.getNickname());
        }
        logEntity.setOperateTime(LocalDateTime.now());
        logEntity.setRemark(remark);
        statusLogMapper.insert(logEntity);
    }
}
