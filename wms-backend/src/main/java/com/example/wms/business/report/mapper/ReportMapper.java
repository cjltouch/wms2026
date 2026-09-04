package com.example.wms.business.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    Map<String, Object> selectTodaySummary();

    List<Map<String, Object>> select30DayTrend(@Param("type") String type);

    List<Map<String, Object>> selectTop10Sku(@Param("type") String type);

    List<Map<String, Object>> selectWarehousePie();

    List<Map<String, Object>> selectAbnormalSummary();

    List<Map<String, Object>> selectPurchaseReconcile(@Param("params") Map<String, Object> params);

    List<Map<String, Object>> selectStockInDetail(@Param("params") Map<String, Object> params);

    List<Map<String, Object>> selectStockOutDetail(@Param("params") Map<String, Object> params);

    List<Map<String, Object>> selectInventoryBalance(@Param("params") Map<String, Object> params);

    List<Map<String, Object>> selectInventoryBatch(@Param("params") Map<String, Object> params);

    List<Map<String, Object>> selectExpireWarning(@Param("params") Map<String, Object> params);
}
