package com.example.wms.business.stockin.entity;

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
@TableName("wms_stock_in_item")
public class WmsStockInItem implements Serializable, ChangeItem {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细主键ID */
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    /** 入库单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stockInId;

    /** 入库单号 */
    private String stockInNo;

    /** 来源单明细ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceItemId;

    /** 行号 */
    private Integer lineNo;

    /** SKU ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** SKU 编码 */
    private String skuCode;

    /** 内部编码 */
    private String innerCode;

    /** SKU 名称 */
    private String skuName;

    /** 规格描述 */
    private String specText;

    /** 单位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unitId;

    /** 单位名称 */
    private String unitName;

    /** 供应商名称（查询时按SKU主数据补齐，不落库） */
    @TableField(exist = false)
    private String supplierName;

    /** 应收数量 */
    private Integer expectedQty;

    /** 实收数量 */
    private Integer actualQty;

    /** 差异数量 */
    private Integer diffQty;

    /** 成本单价 */
    private BigDecimal costPrice;

    /** 金额小计 */
    private BigDecimal subtotal;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate produceDate;

    /** 过期日期 */
    private LocalDate expireDate;

    /** 供应商批次 */
    private String supplierBatch;

    /** 库位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** SN序列号列表 */
    private String snList;

    /** 备注 */
    private String remark;

    /** 仓库ID（非数据库字段，用于业务传递） */
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
