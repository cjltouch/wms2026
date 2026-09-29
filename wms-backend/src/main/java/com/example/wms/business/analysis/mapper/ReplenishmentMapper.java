package com.example.wms.business.analysis.mapper;

import com.example.wms.business.analysis.dto.req.ReplenishmentReq;
import com.example.wms.business.analysis.dto.rsp.ReplenishmentRsp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReplenishmentMapper {

    /**
     * 按SKU维度统计补货分析数据
     */
    List<ReplenishmentRsp> selectBySku(@Param("req") ReplenishmentReq req);

    /**
     * 按内部编码维度统计补货分析数据
     */
    List<ReplenishmentRsp> selectByInnerCode(@Param("req") ReplenishmentReq req);
}
