package com.example.wms.business.stockin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.stockin.dto.req.StockInAuditReq;
import com.example.wms.business.stockin.dto.req.StockInPageReq;
import com.example.wms.business.stockin.dto.req.StockInSaveReq;
import com.example.wms.business.stockin.dto.rsp.StockInDetailRsp;
import com.example.wms.business.stockin.entity.WmsStockIn;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;

import jakarta.servlet.http.HttpServletResponse;

public interface WmsStockInService extends IService<WmsStockIn> {

    /**
     * 分页查询入库单
     *
     * @param req 分页查询请求参数
     * @return 入库单分页结果
     */
    PageRsp<WmsStockIn> pageStockIn(StockInPageReq req);

    /**
     * 根据ID获取入库单详情（包含明细和状态日志）
     *
     * @param id 入库单ID
     * @return 入库单详情
     */
    StockInDetailRsp getDetailById(Long id);

    /**
     * 根据入库单号查询入库单
     *
     * @param stockInNo 入库单号
     * @return 入库单信息
     */
    WmsStockIn getByStockInNo(String stockInNo);

    /**
     * 根据来源单号生成入库单预填数据
     *
     * @param sourceBillNo 来源单号
     * @return 入库单详情（含预填明细）
     */
    StockInDetailRsp fromSource(String sourceBillNo);

    /**
     * 新增入库单
     *
     * @param req 入库单保存请求参数
     */
    void saveStockIn(StockInSaveReq req);

    /**
     * 更新入库单
     *
     * @param req 入库单保存请求参数
     */
    void updateStockIn(StockInSaveReq req);

    /**
     * 删除入库单（逻辑删除）
     *
     * @param id 入库单ID
     */
    void deleteStockIn(Long id);

    /**
     * 提交入库单（提交审核）
     *
     * @param id 入库单ID
     */
    void submitStockIn(Long id);

    /**
     * 审核入库单
     *
     * @param req 审核请求参数
     */
    void auditStockIn(StockInAuditReq req);

    /**
     * 反审核入库单
     *
     * @param id    入库单ID
     * @param remark 反审核备注
     */
    void unauditStockIn(Long id, String remark);

    /**
     * 作废入库单
     *
     * @param id    入库单ID
     * @param remark 作废备注
     */
    void voidStockIn(Long id, String remark);

    /**
     * 批量审核入库单
     *
     * @param req 批量审核请求参数
     */
    void batchAudit(BatchAuditReq req);

    /**
     * 分配库位
     *
     * @param id 入库单ID
     */
    void allocateLocation(Long id);

    /**
     * 导出入库明细（只导出已上架 status=3 的入库单）
     *
     * @param req      查询条件
     * @param response HTTP响应
     */
    void exportStockInItems(StockInPageReq req, HttpServletResponse response);
}
