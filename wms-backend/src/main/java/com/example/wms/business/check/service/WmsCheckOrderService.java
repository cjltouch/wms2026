package com.example.wms.business.check.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.check.dto.req.CheckAuditReq;
import com.example.wms.business.check.dto.req.CheckItemInputReq;
import com.example.wms.business.check.dto.req.CheckPageReq;
import com.example.wms.business.check.dto.req.CheckSaveReq;
import com.example.wms.business.check.dto.rsp.CheckDetailRsp;
import com.example.wms.business.check.entity.WmsCheckOrder;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.PageRsp;

import java.util.List;
import java.util.Map;

public interface WmsCheckOrderService extends IService<WmsCheckOrder> {

    /**
     * 分页查询盘点单
     *
     * @param req 分页查询条件
     * @return 盘点单分页结果
     */
    PageRsp<WmsCheckOrder> pageCheck(CheckPageReq req);

    /**
     * 根据ID获取盘点单详情
     *
     * @param id 盘点单ID
     * @return 盘点单详情（含明细与状态日志）
     */
    CheckDetailRsp getDetailById(Long id);

    /**
     * 保存盘点单（新增或编辑）
     *
     * @param req 盘点单保存请求
     */
    void saveCheck(CheckSaveReq req);

    /**
     * 根据仓库/库区加载库存生成盘点明细
     *
     * @param checkId    盘点单ID
     * @param warehouseId 仓库ID
     * @param areaId     库区ID
     * @return 盘点单详情
     */
    CheckDetailRsp loadInventory(Long checkId, Long warehouseId, Long areaId);

    /**
     * 开始盘点
     *
     * @param id 盘点单ID
     */
    void startCheck(Long id);

    /**
     * 录入实盘数量
     *
     * @param req 实盘录入请求
     */
    void inputActual(CheckItemInputReq req);

    /**
     * 完成盘点
     *
     * @param id 盘点单ID
     */
    void finishCheck(Long id);

    /**
     * 审核盘点单
     *
     * @param req 审核请求
     */
    void auditCheck(CheckAuditReq req);

    /**
     * 处理盘点单（差异处理）
     *
     * @param id     盘点单ID
     * @param remark 处理备注
     */
    void handleCheck(Long id, String remark);

    /**
     * 作废盘点单
     *
     * @param id     盘点单ID
     * @param remark 作废备注
     */
    void voidCheck(Long id, String remark);

    /**
     * 批量审核盘点单
     *
     * @param req 批量审核请求
     */
    void batchAudit(BatchAuditReq req);

    /**
     * 导出盘点单
     *
     * @param req 查询条件
     * @return 导出文件字节数组
     */
    byte[] export(CheckPageReq req);

    /**
     * 差异汇总查询
     *
     * @param warehouseId 仓库ID
     * @return 差异汇总列表
     */
    List<Map<String, Object>> diffSummary(Long warehouseId);
}
