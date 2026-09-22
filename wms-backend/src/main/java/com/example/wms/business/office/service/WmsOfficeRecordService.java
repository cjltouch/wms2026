package com.example.wms.business.office.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.office.dto.req.OfficeRecordPageReq;
import com.example.wms.business.office.dto.req.OfficeRecordSaveReq;
import com.example.wms.business.office.entity.WmsOfficeRecord;
import com.example.wms.common.PageRsp;

import java.util.List;

/**
 * 办公用品/消耗品出入库登记服务
 */
public interface WmsOfficeRecordService extends IService<WmsOfficeRecord> {

    /**
     * 分页查询用品登记记录
     *
     * @param req 查询条件（名称模糊、类型、日期范围）
     * @return 分页结果
     */
    PageRsp<WmsOfficeRecord> pageOfficeRecord(OfficeRecordPageReq req);

    /**
     * 导出用品登记记录（按查询条件，不分页）
     *
     * @param req      查询条件（名称模糊、类型、日期范围）
     * @param response HTTP响应，输出Excel文件流
     */
    void exportOfficeRecord(OfficeRecordPageReq req, jakarta.servlet.http.HttpServletResponse response);

    /**
     * 用品名称联想（新增登记时模糊搜索历史名称，去重）
     *
     * @param keyword 模糊关键词
     * @return 联想项列表（itemName + 最近一次的 spec + unit），最多30条
     */
    java.util.List<java.util.Map<String, String>> suggestName(String keyword);

    /**
     * 新增用品登记记录
     *
     * @param req 保存请求
     */
    void saveOfficeRecord(OfficeRecordSaveReq req);

    /**
     * 修改用品登记记录
     *
     * @param req 保存请求（含记录ID）
     */
    void updateOfficeRecord(OfficeRecordSaveReq req);

    /**
     * 删除用品登记记录
     *
     * @param recordId 记录ID
     */
    void deleteOfficeRecord(Long recordId);

    /**
     * 批量删除用品登记记录
     *
     * @param recordIds 记录ID列表
     */
    void batchDeleteOfficeRecord(List<Long> recordIds);
}
