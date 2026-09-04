package com.example.wms.business.report.manager;

import com.example.wms.business.inventory.mapper.WmsInventoryMapper;
import com.example.wms.business.purchase.mapper.WmsPurchaseOrderMapper;
import com.example.wms.business.report.mapper.ReportMapper;
import com.example.wms.business.stockin.mapper.WmsStockInMapper;
import com.example.wms.business.stockout.mapper.WmsStockOutMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReportManager {

    private final ReportMapper reportMapper;
    private final WmsStockInMapper stockInMapper;
    private final WmsStockOutMapper stockOutMapper;
    private final WmsPurchaseOrderMapper purchaseOrderMapper;
    private final WmsInventoryMapper inventoryMapper;

    public Map<String, Object> getTodaySummary() {
        Map<String, Object> result = reportMapper.selectTodaySummary();
        if (result == null) {
            result = new HashMap<>();
            result.put("todayInCount", 0L);
            result.put("todayOutCount", 0L);
            result.put("inTransitPurchaseAmount", 0);
            result.put("inventorySkuCount", 0L);
            result.put("inventoryTotalAmount", 0);
            result.put("expire30SkuCount", 0L);
        }
        return result;
    }

    public Map<String, Object> get30DayTrend() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> inTrend = reportMapper.select30DayTrend("in");
        List<Map<String, Object>> outTrend = reportMapper.select30DayTrend("out");
        result.put("inTrend", inTrend != null ? inTrend : new ArrayList<>());
        result.put("outTrend", outTrend != null ? outTrend : new ArrayList<>());
        return result;
    }

    public Map<String, Object> getTop10Sku() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> topIn = reportMapper.selectTop10Sku("in");
        List<Map<String, Object>> topOut = reportMapper.selectTop10Sku("out");
        result.put("topIn", topIn != null ? topIn : new ArrayList<>());
        result.put("topOut", topOut != null ? topOut : new ArrayList<>());
        return result;
    }

    public List<Map<String, Object>> getWarehousePie() {
        List<Map<String, Object>> list = reportMapper.selectWarehousePie();
        return list != null ? list : new ArrayList<>();
    }

    public List<Map<String, Object>> getAbnormalSummary() {
        List<Map<String, Object>> list = reportMapper.selectAbnormalSummary();
        return list != null ? list : new ArrayList<>();
    }

    public List<Map<String, Object>> getPurchaseReconcile(Map<String, Object> params) {
        List<Map<String, Object>> list = reportMapper.selectPurchaseReconcile(params);
        return list != null ? list : new ArrayList<>();
    }

    public List<Map<String, Object>> getStockInDetail(Map<String, Object> params) {
        List<Map<String, Object>> list = reportMapper.selectStockInDetail(params);
        return list != null ? list : new ArrayList<>();
    }

    public List<Map<String, Object>> getStockOutDetail(Map<String, Object> params) {
        List<Map<String, Object>> list = reportMapper.selectStockOutDetail(params);
        return list != null ? list : new ArrayList<>();
    }

    public List<Map<String, Object>> getInventoryBalance(Map<String, Object> params) {
        List<Map<String, Object>> list = reportMapper.selectInventoryBalance(params);
        return list != null ? list : new ArrayList<>();
    }

    public List<Map<String, Object>> getInventoryBatch(Map<String, Object> params) {
        List<Map<String, Object>> list = reportMapper.selectInventoryBatch(params);
        return list != null ? list : new ArrayList<>();
    }

    public List<Map<String, Object>> getExpireWarning(Map<String, Object> params) {
        List<Map<String, Object>> list = reportMapper.selectExpireWarning(params);
        return list != null ? list : new ArrayList<>();
    }
}
