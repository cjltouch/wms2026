package com.example.wms.business.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.purchase.dto.req.PurchaseAuditReq;
import com.example.wms.business.purchase.dto.req.PurchasePageReq;
import com.example.wms.business.purchase.dto.req.PurchaseSaveReq;
import com.example.wms.business.purchase.dto.rsp.PurchaseDetailRsp;
import com.example.wms.business.purchase.entity.WmsPurchaseOrder;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;

import java.util.Map;

public interface WmsPurchaseOrderService extends IService<WmsPurchaseOrder> {

    /**
     * 分页查询采购单
     *
     * @param req 分页查询请求参数
     * @return 采购单分页结果
     */
    PageRsp<WmsPurchaseOrder> pagePurchase(PurchasePageReq req);

    /**
     * 根据ID获取采购单详情（包含明细和状态日志）
     *
     * @param id 采购单ID
     * @return 采购单详情
     */
    PurchaseDetailRsp getDetailById(Long id);

    /**
     * 根据采购单号查询采购单
     *
     * @param purchaseNo 采购单号
     * @return 采购单信息
     */
    WmsPurchaseOrder getByPurchaseNo(String purchaseNo);

    /**
     * 新增采购单
     *
     * @param req 采购单保存请求参数
     */
    void savePurchase(PurchaseSaveReq req);

    /**
     * 更新采购单
     *
     * @param req 采购单保存请求参数
     */
    void updatePurchase(PurchaseSaveReq req);

    /**
     * 删除采购单（逻辑删除）
     *
     * @param id 采购单ID
     */
    void deletePurchase(Long id);

    /**
     * 提交采购单（提交审核）
     *
     * @param id 采购单ID
     */
    void submitPurchase(Long id);

    /**
     * 审核采购单
     *
     * @param req 审核请求参数
     */
    void auditPurchase(PurchaseAuditReq req);

    /**
     * 反审核采购单
     *
     * @param id    采购单ID
     * @param remark 反审核备注
     */
    void unauditPurchase(Long id, String remark);

    /**
     * 作废采购单
     *
     * @param id    采购单ID
     * @param remark 作废备注
     */
    void voidPurchase(Long id, String remark);

    /**
     * 批量审核采购单
     *
     * @param req 批量审核请求参数
     */
    void batchAudit(BatchAuditReq req);

    /**
     * 批量作废采购单
     *
     * @param req 批量作废请求参数
     */
    void batchVoid(BatchAuditReq req);

    /**
     * 分页查询采购对账数据
     *
     * @param req 分页查询请求参数
     * @return 对账数据分页结果
     */
    PageRsp<Map<String, Object>> pageReconcile(PurchasePageReq req);
}
