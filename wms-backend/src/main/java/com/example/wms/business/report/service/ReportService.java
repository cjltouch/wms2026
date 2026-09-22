package com.example.wms.business.report.service;

import com.alibaba.excel.EasyExcel;
import com.example.wms.business.report.manager.ReportManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportManager reportManager;

    public void exportPurchaseReconcile(Map<String, Object> params, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = reportManager.getPurchaseReconcile(params);
        writeExcel(response, "采购对账表", buildPurchaseReconcileHead(), list);
    }

    public void exportStockInDetail(Map<String, Object> params, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = reportManager.getStockInDetail(params);
        writeExcel(response, "入库明细表", buildStockInHead(), list);
    }

    public void exportStockOutDetail(Map<String, Object> params, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = reportManager.getStockOutDetail(params);
        writeExcel(response, "出库明细表", buildStockOutHead(), list);
    }

    public void exportInventoryBalance(Map<String, Object> params, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = reportManager.getInventoryBalance(params);
        writeExcel(response, "库存余额表", buildInventoryBalanceHead(), list);
    }

    public void exportInventoryBatch(Map<String, Object> params, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = reportManager.getInventoryBatch(params);
        writeExcel(response, "批次库存表", buildInventoryBatchHead(), list);
    }

    public void exportExpireWarning(Map<String, Object> params, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = reportManager.getExpireWarning(params);
        writeExcel(response, "临期预警表", buildExpireWarningHead(), list);
    }

    private List<List<String>> buildPurchaseReconcileHead() {
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("采购单号"));
        head.add(List.of("采购时间"));
        head.add(List.of("供应商"));
        head.add(List.of("仓库"));
        head.add(List.of("行号"));
        head.add(List.of("SKU编码"));
        head.add(List.of("SKU名称"));
        head.add(List.of("规格"));
        head.add(List.of("下单数量"));
        head.add(List.of("已到货"));
        head.add(List.of("未到货"));
        head.add(List.of("采购价"));
        head.add(List.of("小计"));
        return head;
    }

    private List<List<String>> buildStockInHead() {
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("入库单号"));
        head.add(List.of("入库时间"));
        head.add(List.of("类型"));
        head.add(List.of("源单号"));
        head.add(List.of("仓库"));
        head.add(List.of("供应商"));
        head.add(List.of("行号"));
        head.add(List.of("SKU编码"));
        head.add(List.of("SKU名称"));
        head.add(List.of("规格"));
        head.add(List.of("单位"));
        head.add(List.of("应收数量"));
        head.add(List.of("实收数量"));
        head.add(List.of("成本价"));
        head.add(List.of("小计金额"));
        head.add(List.of("批次号"));
        head.add(List.of("库位"));
        head.add(List.of("备注"));
        return head;
    }

    private List<List<String>> buildStockOutHead() {
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("出库单号"));
        head.add(List.of("出库时间"));
        head.add(List.of("类型"));
        head.add(List.of("源单号"));
        head.add(List.of("仓库"));
        head.add(List.of("行号"));
        head.add(List.of("SKU编码"));
        head.add(List.of("SKU名称"));
        head.add(List.of("规格"));
        head.add(List.of("单位"));
        head.add(List.of("应发数量"));
        head.add(List.of("实发数量"));
        head.add(List.of("成本价"));
        head.add(List.of("成本小计"));
        head.add(List.of("销售价"));
        head.add(List.of("销售小计"));
        return head;
    }

    private List<List<String>> buildInventoryBalanceHead() {
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("仓库"));
        head.add(List.of("区域"));
        head.add(List.of("库位"));
        head.add(List.of("SKU编码"));
        head.add(List.of("SKU名称"));
        head.add(List.of("规格"));
        head.add(List.of("单位"));
        head.add(List.of("批次号"));
        head.add(List.of("库存数量"));
        head.add(List.of("锁定数量"));
        head.add(List.of("可用数量"));
        head.add(List.of("成本价"));
        head.add(List.of("总金额"));
        head.add(List.of("有效期"));
        return head;
    }

    private List<List<String>> buildInventoryBatchHead() {
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("仓库"));
        head.add(List.of("SKU编码"));
        head.add(List.of("SKU名称"));
        head.add(List.of("批次号"));
        head.add(List.of("生产日期"));
        head.add(List.of("有效期"));
        head.add(List.of("数量"));
        head.add(List.of("成本价"));
        head.add(List.of("总金额"));
        return head;
    }

    private List<List<String>> buildExpireWarningHead() {
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("仓库"));
        head.add(List.of("SKU编码"));
        head.add(List.of("SKU名称"));
        head.add(List.of("规格"));
        head.add(List.of("批次号"));
        head.add(List.of("有效期"));
        head.add(List.of("剩余天数"));
        head.add(List.of("数量"));
        head.add(List.of("成本价"));
        head.add(List.of("总金额"));
        return head;
    }

    private void writeExcel(HttpServletResponse response, String fileName, List<List<String>> head, List<Map<String, Object>> dataList) throws IOException {
        String encoded = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + encoded);
        response.setCharacterEncoding("utf-8");

        List<List<Object>> rows = new ArrayList<>();
        if (dataList.isEmpty()) {
            rows.add(new ArrayList<>());
        } else {
            for (Map<String, Object> row : dataList) {
                List<Object> rowData = new ArrayList<>();
                for (int i = 0; i < head.size(); i++) {
                    String key = mapIndexToKey(i, fileName);
                    rowData.add(row.getOrDefault(key, ""));
                }
                rows.add(rowData);
            }
        }

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet(fileName)
                .doWrite(rows);
    }

    private String mapIndexToKey(int index, String fileName) {
        return switch (fileName) {
            case "采购对账表" -> switch (index) {
                case 0 -> "purchase_no";
                case 1 -> "purchase_time";
                case 2 -> "supplier_name";
                case 3 -> "warehouse_name";
                case 4 -> "line_no";
                case 5 -> "sku_code";
                case 6 -> "sku_name";
                case 7 -> "spec_text";
                case 8 -> "order_qty";
                case 9 -> "delivered_qty";
                case 10 -> "unreceived_qty";
                case 11 -> "purchase_price";
                case 12 -> "subtotal";
                default -> "";
            };
            case "入库明细表" -> switch (index) {
                case 0 -> "stock_in_no";
                case 1 -> "in_time";
                case 2 -> "type_name";
                case 3 -> "source_bill_no";
                case 4 -> "warehouse_name";
                case 5 -> "supplier_name";
                case 6 -> "line_no";
                case 7 -> "sku_code";
                case 8 -> "sku_name";
                case 9 -> "spec_text";
                case 10 -> "unit_name";
                case 11 -> "expected_qty";
                case 12 -> "actual_qty";
                case 13 -> "cost_price";
                case 14 -> "subtotal";
                case 15 -> "batch_no";
                case 16 -> "location_code";
                case 17 -> "remark";
                default -> "";
            };
            case "出库明细表" -> switch (index) {
                case 0 -> "stock_out_no";
                case 1 -> "out_time";
                case 2 -> "type_name";
                case 3 -> "source_bill_no";
                case 4 -> "warehouse_name";
                case 5 -> "line_no";
                case 6 -> "sku_code";
                case 7 -> "sku_name";
                case 8 -> "spec_text";
                case 9 -> "unit_name";
                case 10 -> "expected_qty";
                case 11 -> "actual_qty";
                case 12 -> "cost_price";
                case 13 -> "subtotal_cost";
                case 14 -> "sale_price";
                case 15 -> "subtotal_sale";
                default -> "";
            };
            case "库存余额表" -> switch (index) {
                case 0 -> "warehouse_name";
                case 1 -> "area_name";
                case 2 -> "location_code";
                case 3 -> "sku_code";
                case 4 -> "sku_name";
                case 5 -> "spec_text";
                case 6 -> "unit_name";
                case 7 -> "batch_no";
                case 8 -> "quantity";
                case 9 -> "locked_qty";
                case 10 -> "available_qty";
                case 11 -> "cost_price";
                case 12 -> "total_amount";
                case 13 -> "expire_date";
                default -> "";
            };
            case "批次库存表" -> switch (index) {
                case 0 -> "warehouse_name";
                case 1 -> "sku_code";
                case 2 -> "sku_name";
                case 3 -> "batch_no";
                case 4 -> "produce_date";
                case 5 -> "expire_date";
                case 6 -> "quantity";
                case 7 -> "cost_price";
                case 8 -> "total_amount";
                default -> "";
            };
            case "临期预警表" -> switch (index) {
                case 0 -> "warehouse_name";
                case 1 -> "sku_code";
                case 2 -> "sku_name";
                case 3 -> "spec_text";
                case 4 -> "batch_no";
                case 5 -> "expire_date";
                case 6 -> "remain_days";
                case 7 -> "quantity";
                case 8 -> "cost_price";
                case 9 -> "total_amount";
                default -> "";
            };
            default -> "";
        };
    }
}
