package com.example.wms.business.analysis.dto.rsp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ReplenishmentRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** SKU ID（内部编码维度时为组内第一个SKU） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称（内部编码维度时取SPU名或组内第一个SKU名） */
    private String skuName;

    /** 规格描述 */
    private String specText;

    /** 颜色 */
    private String color;

    /** 内部编码 */
    private String innerCode;

    /** 历史出库总量（实出数量合计） */
    private Integer historyQty;

    /** 日均出库量 */
    private BigDecimal dailyAvgQty;

    /** 当前可用库存 */
    private Integer availableQty;

    /** 预测需求量（向上取整） */
    private Integer forecastQty;

    /** 建议采购量 */
    private Integer suggestQty;

    /** 库存支撑天数（日均为0时为null，前端显示"充足"） */
    private BigDecimal supportDays;

    /** 状态：1充足 2紧张 3缺货 */
    private Integer status;

    /** 采购成本单价 */
    private BigDecimal defaultCost;

    /** 建议采购金额 = 建议采购量 × 采购成本 */
    private BigDecimal suggestAmount;
}
