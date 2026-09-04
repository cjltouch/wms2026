package com.example.wms.business.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.business.purchase.dto.req.PurchasePageReq;
import com.example.wms.business.purchase.entity.WmsPurchaseStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WmsPurchaseStatusLogMapper extends BaseMapper<WmsPurchaseStatusLog> {

    List<WmsPurchaseStatusLog> selectByBillId(@Param("billId") Long billId);

    IPage<Map<String, Object>> pageReconcile(Page<?> page, @Param("req") PurchasePageReq req);
}
