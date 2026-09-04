package com.example.wms.business.sale.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.sale.dto.req.SaleAuditReq;
import com.example.wms.business.sale.dto.req.SalePageReq;
import com.example.wms.business.sale.dto.req.SaleSaveReq;
import com.example.wms.business.sale.dto.rsp.SaleDetailRsp;
import com.example.wms.business.sale.entity.WmsSaleOrder;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;

import java.time.LocalDateTime;
import java.util.Map;

public interface WmsSaleOrderService extends IService<WmsSaleOrder> {

    /**
     * 分页查询销售单
     *
     * @param req 分页查询条件
     * @return 分页结果
     */
    PageRsp<WmsSaleOrder> pageSale(SalePageReq req);

    /**
     * 根据ID查询销售单详情（含明细和状态日志）
     *
     * @param id 销售单ID
     * @return 销售单详情
     */
    SaleDetailRsp getDetailById(Long id);

    /**
     * 新增销售单
     *
     * @param req 销售单保存请求
     */
    void saveSale(SaleSaveReq req);

    /**
     * 更新销售单
     *
     * @param req 销售单保存请求
     */
    void updateSale(SaleSaveReq req);

    /**
     * 删除销售单
     *
     * @param id 销售单ID
     */
    void deleteSale(Long id);

    /**
     * 提交销售单审核
     *
     * @param id 销售单ID
     */
    void submitSale(Long id);

    /**
     * 审核销售单
     *
     * @param req 审核请求
     */
    void auditSale(SaleAuditReq req);

    /**
     * 确认出库
     *
     * @param id 销售单ID
     */
    void confirmOut(Long id);

    /**
     * 确认收款
     *
     * @param id             销售单ID
     * @param receivedAmount 实收金额
     */
    void confirmPay(Long id, java.math.BigDecimal receivedAmount);

    /**
     * 完成销售单
     *
     * @param id 销售单ID
     */
    void completeSale(Long id);

    /**
     * 作废销售单
     *
     * @param id     销售单ID
     * @param remark 作废备注
     */
    void voidSale(Long id, String remark);

    /**
     * 批量审核销售单
     *
     * @param req 批量审核请求
     */
    void batchAudit(BatchAuditReq req);

    /**
     * 导出销售单
     *
     * @param req 查询条件
     * @return 导出文件字节流
     */
    byte[] export(SalePageReq req);

    /**
     * 销售利润分析
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 利润分析结果
     */
    Map<String, Object> profitAnalysis(LocalDateTime startDate, LocalDateTime endDate);
}
