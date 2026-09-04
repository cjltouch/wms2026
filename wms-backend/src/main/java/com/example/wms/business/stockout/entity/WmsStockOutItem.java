package com.example.wms.business.stockout.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.business.inventory.handler.ChangeItem;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("wms_stock_out_item")
public class WmsStockOutItem implements Serializable, ChangeItem {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细主键ID */
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    /** 出库单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stockOutId;

    /** 出库单号 */
    private String stockOutNo;

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

    /** 供应商名称（查询时按SKU主数据补齐，不落库） */
    @TableField(exist = false)
    private String supplierName;

    /** 应出数量 */
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

    /** 仓库ID(非数据库字段) */
    @TableField(exist = false)
    private Long warehouseId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Override
    public Integer getQty() {
        return actualQty;
    }
}
