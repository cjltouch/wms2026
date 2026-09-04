package com.example.wms.business.dashboard.service;

import com.example.wms.business.report.manager.ReportManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ReportManager reportManager;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 返回 6 张统计卡片（字段名必须严格匹配前端 dashboard/index.vue loadSummary）：
     * - todayStockInQty 今日入库件数
     * - todayStockOutQty 今日出库件数
     * - inTransitAmount 在途采购金额（未入库）
     * - inventorySkuCount 库存 SKU 种数（去重）
     * - inventoryTotalAmount 库存总金额
     * - expireWarningCount 30天临期预警 SKU 数
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = reportManager.getTodaySummary();
        Map<String, Object> result = new HashMap<>();
        result.put("todayStockInQty", toLong(summary.getOrDefault("todayInCount", 0L)));
        result.put("todayStockOutQty", toLong(summary.getOrDefault("todayOutCount", 0L)));
        result.put("inTransitAmount", toBigDecimal(summary.getOrDefault("inTransitPurchaseAmount", BigDecimal.ZERO)));
        result.put("inventorySkuCount", toLong(summary.getOrDefault("inventorySkuCount", 0L)));
        result.put("inventoryTotalAmount", toBigDecimal(summary.getOrDefault("inventoryTotalAmount", BigDecimal.ZERO)));
        result.put("expireWarningCount", toLong(summary.getOrDefault("expire30SkuCount", 0L)));
        return result;
    }

    /**
     * 返回近 30 天出入库趋势（对齐前端 trend30：List<{date, stockInQty, stockOutQty}>）。
     * 把 DB 稀疏的 inTrend/outTrend 按日期合并，补全缺失日期为 0，保证 x 轴连续。
     */
    public List<Map<String, Object>> getTrend30() {
        Map<String, Object> raw = reportManager.get30DayTrend();
        List<Map<String, Object>> inTrend = (List<Map<String, Object>>) raw.getOrDefault("inTrend", new ArrayList<>());
        List<Map<String, Object>> outTrend = (List<Map<String, Object>>) raw.getOrDefault("outTrend", new ArrayList<>());

        // 构造 30 天日期表：按 LocalDate.now() 往前推 29 天 + 今天
        Map<String, Map<String, Object>> dayMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            String day = today.minusDays(i).format(DATE_FMT);
            Map<String, Object> row = new HashMap<>();
            row.put("date", day);
            row.put("stockInQty", 0L);
            row.put("stockOutQty", 0L);
            dayMap.put(day, row);
        }
        for (Map<String, Object> in : inTrend) {
            String d = in.get("date") != null ? String.valueOf(in.get("date")) : null;
            if (d != null && dayMap.containsKey(d)) {
                dayMap.get(d).put("stockInQty", toLong(in.getOrDefault("count", 0L)));
            }
        }
        for (Map<String, Object> out : outTrend) {
            String d = out.get("date") != null ? String.valueOf(out.get("date")) : null;
            if (d != null && dayMap.containsKey(d)) {
                dayMap.get(d).put("stockOutQty", toLong(out.getOrDefault("count", 0L)));
            }
        }
        return new ArrayList<>(dayMap.values());
    }

    /**
     * 出入库 TOP10 SKU（对齐前端 topSku：{ inList, outList }，每项 {skuCode,skuName,qty,amount}）。
     */
    public Map<String, Object> getTopInOutSku() {
        Map<String, Object> raw = reportManager.getTop10Sku();
        List<Map<String, Object>> topIn = (List<Map<String, Object>>) raw.getOrDefault("topIn", new ArrayList<>());
        List<Map<String, Object>> topOut = (List<Map<String, Object>>) raw.getOrDefault("topOut", new ArrayList<>());
        Map<String, Object> result = new HashMap<>();
        result.put("inList", adaptTopSku(topIn));
        result.put("outList", adaptTopSku(topOut));
        return result;
    }

    /**
     * 各仓库库存占比（对齐前端 warehousePie：List<{warehouseName, totalQty}>）。
     * qty 字段重命名为 totalQty，warehouseName 保留。
     */
    public List<Map<String, Object>> getWarehouseQtyPie() {
        List<Map<String, Object>> raw = reportManager.getWarehousePie();
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Map<String, Object> r : raw) {
            Map<String, Object> item = new HashMap<>();
            item.put("warehouseId", r.get("warehouseId"));
            item.put("warehouseName", r.getOrDefault("warehouseName", "未命名仓库"));
            item.put("totalQty", toLong(r.getOrDefault("qty", 0L)));
            item.put("amount", toBigDecimal(r.getOrDefault("amount", BigDecimal.ZERO)));
            out.add(item);
        }
        return out;
    }

    public List<Map<String, Object>> getAbnormalSummary() {
        return reportManager.getAbnormalSummary();
    }

    // --------- 私有工具 ---------

    /** 把 top sku 行字段映射为前端所需：skuCode / skuName / qty / amount */
    private List<Map<String, Object>> adaptTopSku(List<Map<String, Object>> raw) {
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Map<String, Object> r : raw) {
            Map<String, Object> o = new HashMap<>();
            o.put("skuId", r.get("skuId"));
            o.put("skuCode", r.getOrDefault("skuCode", ""));
            o.put("skuName", r.getOrDefault("skuName", ""));
            o.put("qty", toLong(r.getOrDefault("qty", 0L)));
            o.put("amount", toBigDecimal(r.getOrDefault("amount", BigDecimal.ZERO)));
            out.add(o);
        }
        return out;
    }

    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(((Number) o).doubleValue());
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
