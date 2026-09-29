package com.example.wms.business.analysis.controller;

import com.example.wms.business.analysis.dto.req.ReplenishmentReq;
import com.example.wms.business.analysis.dto.rsp.ReplenishmentRsp;
import com.example.wms.business.analysis.service.ReplenishmentService;
import com.example.wms.common.R;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "智能分析")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final ReplenishmentService replenishmentService;

    @Operation(summary = "智能补货分析")
    @PreAuthorize(hasAuthority = "analysis:replenishment:list")
    @PostMapping("/replenishment")
    public R<List<ReplenishmentRsp>> replenishment(@RequestBody ReplenishmentReq req) {
        return R.ok(replenishmentService.listReplenishment(req));
    }
}
