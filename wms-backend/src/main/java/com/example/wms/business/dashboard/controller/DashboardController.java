package com.example.wms.business.dashboard.controller;

import com.example.wms.business.dashboard.service.DashboardService;
import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "驾驶舱Dashboard")
@RestController
@RequestMapping("/api/report/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize(hasAuthority = "dashboard:view")
    @OperationLog(module = "驾驶舱", type = "GET", businessType = 4)
    @Operation(summary = "6张卡片汇总(今日入/出库、在途采购金额、库存SKU数、库存总金额、30天临期SKU)")
    @GetMapping("/summary")
    public R<?> summary() {
        return R.ok(dashboardService.getSummary());
    }

    @PreAuthorize(hasAuthority = "dashboard:view")
    @OperationLog(module = "驾驶舱", type = "POST", businessType = 4)
    @Operation(summary = "近30天出入库趋势")
    @PostMapping("/trend-30")
    public R<?> trend30() {
        return R.ok(dashboardService.getTrend30());
    }

    @PreAuthorize(hasAuthority = "dashboard:view")
    @OperationLog(module = "驾驶舱", type = "POST", businessType = 4)
    @Operation(summary = "出入库TOP10 SKU排行")
    @PostMapping("/top-in-out-sku")
    public R<?> topInOutSku() {
        return R.ok(dashboardService.getTopInOutSku());
    }

    @PreAuthorize(hasAuthority = "dashboard:view")
    @OperationLog(module = "驾驶舱", type = "POST", businessType = 4)
    @Operation(summary = "各仓库库存占比(饼图)")
    @PostMapping("/warehouse-qty-pie")
    public R<?> warehouseQtyPie() {
        return R.ok(dashboardService.getWarehouseQtyPie());
    }

    @PreAuthorize(hasAuthority = "dashboard:view")
    @OperationLog(module = "驾驶舱", type = "POST", businessType = 4)
    @Operation(summary = "异常汇总(临期/零库存/负库存/超期草稿)")
    @PostMapping("/abnormal-summary")
    public R<?> abnormalSummary() {
        return R.ok(dashboardService.getAbnormalSummary());
    }
}
