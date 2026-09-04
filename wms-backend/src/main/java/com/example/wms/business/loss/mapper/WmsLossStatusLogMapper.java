package com.example.wms.business.loss.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.loss.entity.WmsLossStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsLossStatusLogMapper extends BaseMapper<WmsLossStatusLog> {

    List<WmsLossStatusLog> selectByBillId(@Param("billId") Long billId);
}
