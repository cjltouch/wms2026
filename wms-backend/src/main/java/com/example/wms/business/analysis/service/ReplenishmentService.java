package com.example.wms.business.analysis.service;

import com.example.wms.business.analysis.dto.req.ReplenishmentReq;
import com.example.wms.business.analysis.dto.rsp.ReplenishmentRsp;

import java.util.List;

public interface ReplenishmentService {

    /**
     * 智能补货分析列表
     *
     * @param req 查询参数
     * @return 补货建议列表
     */
    List<ReplenishmentRsp> listReplenishment(ReplenishmentReq req);
}
