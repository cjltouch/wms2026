package com.example.wms.business.transfer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.business.transfer.entity.WmsTransferStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WmsTransferStatusLogMapper extends BaseMapper<WmsTransferStatusLog> {

    List<WmsTransferStatusLog> selectByBillId(@Param("billId") Long billId);
}
