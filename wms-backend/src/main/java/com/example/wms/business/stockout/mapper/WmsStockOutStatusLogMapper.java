package com.example.wms.business.stockout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.stockout.entity.WmsStockOutStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsStockOutStatusLogMapper extends BaseMapper<WmsStockOutStatusLog> {

    List<WmsStockOutStatusLog> selectByBillId(@Param("billId") Long billId);
}
