package com.example.wms.business.stockout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.stockout.dto.req.StockOutAuditReq;
import com.example.wms.business.stockout.dto.req.StockOutPageReq;
import com.example.wms.business.stockout.dto.req.StockOutSaveReq;
import com.example.wms.business.stockout.dto.rsp.StockOutDetailRsp;
import com.example.wms.business.stockout.entity.WmsStockOut;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

public interface WmsStockOutService extends IService<WmsStockOut> {

    /**
     * 分页查询出库单
     *
     * @param req 分页查询条件
     * @return 出库单分页结果
     */
    PageRsp<WmsStockOut> pageStockOut(StockOutPageReq req);

    /**
     * 根据ID查询出库单详情
     *
     * @param id 出库单ID
     * @return 出库单详情(含明细及状态日志)
     */
    StockOutDetailRsp getDetailById(Long id);

    /**
     * 根据出库单号查询出库单
     *
     * @param stockOutNo 出库单号
     * @return 出库单对象
     */
    WmsStockOut getByStockOutNo(String stockOutNo);

    /**
     * 新增出库单
     *
     * @param req 出库单保存请求
     */
    void saveStockOut(StockOutSaveReq req);

    /**
     * 更新出库单
     *
     * @param req 出库单保存请求
     */
    void updateStockOut(StockOutSaveReq req);

    /**
     * 删除出库单
     *
     * @param id 出库单ID
     */
    void deleteStockOut(Long id);

    /**
     * 提交出库单
     *
     * @param id 出库单ID
     */
    void submitStockOut(Long id);

    /**
     * 预览库存分配结果
     *
     * @param id 出库单ID
     * @return 分配预览结果列表
     */
    List<Map<String, Object>> previewAllocation(Long id);

    /**
     * 锁定库存
     *
     * @param id 出库单ID
     */
    void lockInventory(Long id);

    /**
     * 拣货确认
     *
     * @param id 出库单ID
     */
    void pickConfirm(Long id);

    /**
     * 审核出库单
     *
     * @param req 审核请求(含出库单ID列表及备注)
     */
    void auditStockOut(StockOutAuditReq req);

    /**
     * 反审核出库单
     *
     * @param id    出库单ID
     * @param remark 反审核备注
     */
    void unauditStockOut(Long id, String remark);

    /**
     * 作废出库单
     *
     * @param id    出库单ID
     * @param remark 作废备注
     */
    void voidStockOut(Long id, String remark);

    /**
     * 批量审核出库单
     *
     * @param req 批量审核请求
     */
    void batchAudit(BatchAuditReq req);

    /**
     * 导出出库明细（只导出已审核 status=4 的出库单）
     *
     * @param req      查询条件
     * @param response HTTP响应
     */
    void exportStockOutItems(StockOutPageReq req, HttpServletResponse response);

    /**
     * 生成下一个出库单号：CK + yyyyMMdd + 3位序号（001 起步）
     *
     * @return 出库单号，如 CK20260922001
     */
    String generateStockOutNo();
}
