package com.example.wms.business.stockout.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockOutItemSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细主键ID(新增时为空) */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    /** 来源单据明细ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceItemId;

    /** 行号 */
    private Integer lineNo;

    /** 商品SKU ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** 商品编码 */
    private String skuCode;

    /** 内部编码 */
    private String innerCode;

    /** 商品名称 */
    private String skuName;

    /** 规格描述 */
    private String specText;

    /** 计量单位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unitId;

    /** 计量单位名称 */
    private String unitName;

    /** 应出数量(兼容前端quantity字段) */
    @JsonAlias("quantity")
    private Integer expectedQty;

    /** 实出数量 */
    private Integer actualQty;

    /** 差异数量 */
    private Integer diffQty;

    /** 已拣货数量 */
    private Integer pickedQty;

    /** 成本单价 */
    private BigDecimal costPrice;

    /** 成本小计 */
    private BigDecimal subtotalCost;

    /** 销售单价 */
    private BigDecimal salePrice;

    /** 销售小计 */
    private BigDecimal subtotalSale;

    /** 折扣率 */
    private BigDecimal discountRate;

    /** 分配规则 */
    private Integer allocationRule;

    /** 分配明细JSON */
    private String allocationJson;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate produceDate;

    /** 过期日期 */
    private LocalDate expireDate;

    /** 库位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 序列号列表 */
    private String snList;

    /** 备注 */
    private String remark;
}
