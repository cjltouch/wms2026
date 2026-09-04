package com.example.wms.business.sale.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wms_sale_order_item")
public class WmsSaleOrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细主键ID */
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    /** 销售单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long saleId;

    /** 销售单号 */
    private String saleNo;

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

    /** 数量 */
    private Integer quantity;

    /** 销售单价 */
    private BigDecimal salePrice;

    /** 成本单价 */
    private BigDecimal costPrice;

    /** 折扣率 */
    private BigDecimal discountRate;

    /** 小计金额 */
    private BigDecimal subtotal;

    /** 利润 */
    private BigDecimal profit;

    /** 已发货数量 */
    private Integer deliveredQty;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
