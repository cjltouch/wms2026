package com.example.wms.business.transfer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.transfer.dto.req.TransferAuditReq;
import com.example.wms.business.transfer.dto.req.TransferPageReq;
import com.example.wms.business.transfer.dto.req.TransferSaveReq;
import com.example.wms.business.transfer.dto.rsp.TransferDetailRsp;
import com.example.wms.business.transfer.entity.WmsTransferOrder;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;

public interface WmsTransferOrderService extends IService<WmsTransferOrder> {

    /**
     * 分页查询调拨单
     *
     * @param req 分页查询条件
     * @return 调拨单分页结果
     */
    PageRsp<WmsTransferOrder> pageTransfer(TransferPageReq req);

    /**
     * 根据ID查询调拨单详情
     *
     * @param id 调拨单ID
     * @return 调拨单详情(含明细及状态日志)
     */
    TransferDetailRsp getDetailById(Long id);

    /**
     * 新增调拨单
     *
     * @param req 调拨单保存请求
     */
    void saveTransfer(TransferSaveReq req);

    /**
     * 更新调拨单
     *
     * @param req 调拨单保存请求
     */
    void updateTransfer(TransferSaveReq req);

    /**
     * 删除调拨单
     *
     * @param id 调拨单ID
     */
    void deleteTransfer(Long id);

    /**
     * 提交调拨单
     *
     * @param id 调拨单ID
     */
    void submitTransfer(Long id);

    /**
     * 审核调拨单
     *
     * @param req 审核请求(含调拨单ID列表及备注)
     */
    void auditTransfer(TransferAuditReq req);

    /**
     * 反审核调拨单
     *
     * @param id    调拨单ID
     * @param remark 反审核备注
     */
    void unauditTransfer(Long id, String remark);

    /**
     * 确认调出
     *
     * @param id 调拨单ID
     */
    void confirmOut(Long id);

    /**
     * 确认调入
     *
     * @param id 调拨单ID
     */
    void confirmIn(Long id);

    /**
     * 作废调拨单
     *
     * @param id    调拨单ID
     * @param remark 作废备注
     */
    void voidTransfer(Long id, String remark);

    /**
     * 批量审核调拨单
     *
     * @param req 批量审核请求
     */
    void batchAudit(BatchAuditReq req);

    /**
     * 导出调拨单
     *
     * @param req 查询条件
     * @return 导出文件字节数组
     */
    byte[] export(TransferPageReq req);
}
