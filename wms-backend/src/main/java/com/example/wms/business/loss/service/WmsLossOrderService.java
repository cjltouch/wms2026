package com.example.wms.business.loss.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.loss.dto.req.LossAuditReq;
import com.example.wms.business.loss.dto.req.LossHandleReq;
import com.example.wms.business.loss.dto.req.LossPageReq;
import com.example.wms.business.loss.dto.req.LossSaveReq;
import com.example.wms.business.loss.dto.rsp.LossDetailRsp;
import com.example.wms.business.loss.entity.WmsLossOrder;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;

public interface WmsLossOrderService extends IService<WmsLossOrder> {

    /**
     * 分页查询报损单
     *
     * @param req 分页查询条件
     * @return 分页结果
     */
    PageRsp<WmsLossOrder> pageLoss(LossPageReq req);

    /**
     * 根据ID查询报损单详情（含明细和状态日志）
     *
     * @param id 报损单ID
     * @return 报损单详情
     */
    LossDetailRsp getDetailById(Long id);

    /**
     * 根据报损单号查询报损单
     *
     * @param lossNo 报损单号
     * @return 报损单信息
     */
    WmsLossOrder getByLossNo(String lossNo);

    /**
     * 新增报损单
     *
     * @param req 报损单保存请求
     */
    void saveLoss(LossSaveReq req);

    /**
     * 更新报损单
     *
     * @param req 报损单保存请求
     */
    void updateLoss(LossSaveReq req);

    /**
     * 删除报损单
     *
     * @param id 报损单ID
     */
    void deleteLoss(Long id);

    /**
     * 提交报损单审核
     *
     * @param id 报损单ID
     */
    void submitLoss(Long id);

    /**
     * 审核报损单
     *
     * @param req 审核请求
     */
    void auditLoss(LossAuditReq req);

    /**
     * 处理报损单
     *
     * @param req 处理请求
     */
    void handleLoss(LossHandleReq req);

    /**
     * 作废报损单
     *
     * @param id     报损单ID
     * @param remark 作废备注
     */
    void voidLoss(Long id, String remark);

    /**
     * 批量审核报损单
     *
     * @param req 批量审核请求
     */
    void batchAudit(BatchAuditReq req);
}
