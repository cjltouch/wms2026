package com.example.wms.business.inventory.controller;

import com.example.wms.business.inventory.dto.req.InventoryLogPageReq;
import com.example.wms.business.inventory.dto.req.InventoryPageReq;
import com.example.wms.business.inventory.entity.WmsInventory;
import com.example.wms.business.inventory.service.InventoryLogService;
import com.example.wms.business.inventory.service.InventoryService;
import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "库存管理")
@RestController
@RequestMapping("/api/wms/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryLogService inventoryLogService;

    @Operation(summary = "库存分页查询")
    @PreAuthorize(hasAuthority = "wms:inventory:list")
    @PostMapping("/page")
    public R<?> page(@RequestBody InventoryPageReq req) {
        return R.ok(inventoryService.pageInventory(req));
    }

    @Operation(summary = "按SKU分仓查询库存")
    @GetMapping("/by-sku/{skuId}")
    public R<List<WmsInventory>> bySkuId(@PathVariable Long skuId) {
        return R.ok(inventoryService.listBySkuId(skuId));
    }

    @Operation(summary = "按批次分页查询")
    @PreAuthorize(hasAuthority = "wms:inventory:list")
    @PostMapping("/page-by-batch")
    public R<?> pageByBatch(@RequestBody InventoryPageReq req) {
        return R.ok(inventoryService.pageByBatch(req));
    }

    @Operation(summary = "库存变动日志分页")
    @PreAuthorize(hasAuthority = "wms:inventory:log")
    @PostMapping("/log/page")
    public R<?> logPage(@RequestBody InventoryLogPageReq req) {
        return R.ok(inventoryLogService.pageLog(req));
    }

    @PreAuthorize(hasAuthority = "wms:inventory:warning")
    @Operation(summary = "临期预警")
    @PostMapping("/warning/expire")
    public R<?> warningExpire(@RequestBody InventoryPageReq req,
                              @RequestParam(required = false, defaultValue = "30") Integer thresholdDays) {
        return R.ok(inventoryService.warningExpire(req, thresholdDays));
    }

    @PreAuthorize(hasAuthority = "wms:inventory:warning")
    @Operation(summary = "低于安全库存预警")
    @PostMapping("/warning/below-min")
    public R<?> warningBelowMin(@RequestBody InventoryPageReq req) {
        return R.ok(inventoryService.warningBelowMin(req));
    }

    @PreAuthorize(hasAuthority = "wms:inventory:analytics")
    @Operation(summary = "库存周转分析")
    @PostMapping("/turnover/analytics")
    public R<?> turnoverAnalytics(@RequestBody InventoryPageReq req) {
        return R.ok(inventoryService.turnoverAnalytics(req));
    }

    @PreAuthorize(hasAuthority = "wms:inventory:list")
    @OperationLog(module = "库存管理", type = "EXPORT", businessType = 5)
    @Operation(summary = "导出库存")
    @GetMapping("/export")
    public void export(InventoryPageReq req, jakarta.servlet.http.HttpServletResponse response) {
        inventoryService.exportInventory(req, response);
    }

    @OperationLog(module = "库存管理", type = "EXPORT", businessType = 5)
    @Operation(summary = "导出库存日志")
    @GetMapping("/log/export")
    public R<Void> exportLog(InventoryLogPageReq req) {
        return R.ok();
    }
}
