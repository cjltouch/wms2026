package com.example.wms.business.stockout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.stockout.entity.WmsStockOut;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WmsStockOutMapper extends BaseMapper<WmsStockOut> {
}
