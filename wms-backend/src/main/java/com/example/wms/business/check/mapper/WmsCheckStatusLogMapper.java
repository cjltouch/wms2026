package com.example.wms.business.check.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.check.entity.WmsCheckStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsCheckStatusLogMapper extends BaseMapper<WmsCheckStatusLog> {

    List<WmsCheckStatusLog> selectByBillId(@Param("billId") Long billId);
}
