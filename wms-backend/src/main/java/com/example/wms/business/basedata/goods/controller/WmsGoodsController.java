package com.example.wms.business.basedata.goods.controller;

import com.example.wms.business.basedata.goods.dto.req.GoodsSpuPageReq;
import com.example.wms.business.basedata.goods.dto.req.SpuBatchChangeStatusReq;
import com.example.wms.business.basedata.goods.dto.req.SpuBatchDeleteReq;
import com.example.wms.business.basedata.goods.dto.req.SpuSaveReq;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSpu;
import com.example.wms.business.basedata.goods.service.WmsGoodsSpuService;
import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/wms/goods")
@RequiredArgsConstructor
public class WmsGoodsController {

    private final WmsGoodsSpuService wmsGoodsSpuService;

    @Operation(summary = "SPU分页列表")
    @PreAuthorize(hasAuthority = "wms:goods:list")
    @GetMapping("/spu/page")
    public R<?> page(GoodsSpuPageReq req) {
        return R.ok(wmsGoodsSpuService.pageSpu(req));
    }

    @Operation(summary = "获取SPU详情（含SKU列表）")
    @PreAuthorize(hasAuthority = "wms:goods:list")
    @GetMapping("/spu/{spuId}")
    public R<WmsGoodsSpu> getSpuDetail(@PathVariable Long spuId) {
        return R.ok(wmsGoodsSpuService.getSpuDetail(spuId));
    }

    @PreAuthorize(hasAuthority = "wms:goods:add")
    @OperationLog(module = "商品管理", type = "POST", businessType = 1)
    @Operation(summary = "保存SPU（含批量SKU）")
    @PostMapping("/spu")
    public R<Long> saveSpu(@RequestBody SpuSaveReq req) {
        return R.ok(wmsGoodsSpuService.saveSpuWithSku(req));
    }

    @PreAuthorize(hasAuthority = "wms:goods:edit")
    @OperationLog(module = "商品管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改SPU（含批量SKU）")
    @PutMapping("/spu")
    public R<Long> updateSpu(@RequestBody SpuSaveReq req) {
        return R.ok(wmsGoodsSpuService.saveSpuWithSku(req));
    }

    @PreAuthorize(hasAuthority = "wms:goods:remove")
    @OperationLog(module = "商品管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除SPU")
    @DeleteMapping("/spu/{id}")
    public R<Void> deleteSpu(@PathVariable Long id) {
        wmsGoodsSpuService.deleteSpu(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:goods:remove")
    @OperationLog(module = "商品管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除SPU")
    @DeleteMapping("/spu/batch-delete")
    public R<Void> batchDeleteSpu(@RequestBody SpuBatchDeleteReq req) {
        wmsGoodsSpuService.batchDeleteSpu(req.getIds());
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:goods:edit")
    @OperationLog(module = "商品管理", type = "PUT", businessType = 2)
    @Operation(summary = "批量上下架SPU")
    @PutMapping("/spu/batch-change-status")
    public R<Void> batchChangeStatus(@RequestBody SpuBatchChangeStatusReq req) {
        wmsGoodsSpuService.batchChangeStatus(req);
        return R.ok();
    }

    @Operation(summary = "关键字搜索SKU")
    @GetMapping("/sku/search")
    public R<List<WmsGoodsSku>> searchSku(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId) {
        return R.ok(wmsGoodsSpuService.searchSku(keyword, warehouseId, supplierId));
    }

    @Operation(summary = "Excel批量导入SPU+SKU")
    @PostMapping("/sku/import")
    public R<Void> importSku() {
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:goods:export")
    @Operation(summary = "导出SPU")
    @GetMapping("/spu/export")
    public void exportSpu(GoodsSpuPageReq req, jakarta.servlet.http.HttpServletResponse response) {
        wmsGoodsSpuService.exportSpu(req, response);
    }
}
