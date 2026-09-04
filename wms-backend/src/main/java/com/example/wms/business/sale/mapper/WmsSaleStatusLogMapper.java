package com.example.wms.business.sale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.sale.entity.WmsSaleStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsSaleStatusLogMapper extends BaseMapper<WmsSaleStatusLog> {

    List<WmsSaleStatusLog> selectByBillId(@Param("billId") Long billId);
}
