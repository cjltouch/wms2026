package com.example.wms.business.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.purchase.entity.WmsPurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsPurchaseOrderItemMapper extends BaseMapper<WmsPurchaseOrderItem> {

    void deleteByPurchaseId(@Param("purchaseId") Long purchaseId);

    List<WmsPurchaseOrderItem> selectByPurchaseId(@Param("purchaseId") Long purchaseId);
}
