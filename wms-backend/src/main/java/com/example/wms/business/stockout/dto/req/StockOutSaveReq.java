package com.example.wms.business.stockout.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class StockOutSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 出库单ID(新增时为空,更新时必填) */
    @JsonAlias("stockOutId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stockId;

    /** 出库单号 */
    private String stockOutNo;

    /** 出库类型 */
    private Integer type;

    /** 来源单号 */
    private String sourceBillNo;

    /** 来源单据明细ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceItemId;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 客户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long customerId;

    /** 客户名称（支持手工录入） */
    private String customerName;

    /** 出库操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long outBy;

    /** 分配规则 */
    private Integer allocationRule;

    /** 总数量 */
    private Integer totalQty;

    /** 货品成本总额 */
    private BigDecimal totalCost;

    /** 销售总额 */
    private BigDecimal totalSale;

    /** 备注 */
    private String remark;

    /** 出库明细列表 */
    private List<StockOutItemSaveReq> items;
}
