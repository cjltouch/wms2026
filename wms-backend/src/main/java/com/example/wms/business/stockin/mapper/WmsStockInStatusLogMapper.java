package com.example.wms.business.stockin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.stockin.entity.WmsStockInStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsStockInStatusLogMapper extends BaseMapper<WmsStockInStatusLog> {

    List<WmsStockInStatusLog> selectByBillId(@Param("billId") Long billId);
}
