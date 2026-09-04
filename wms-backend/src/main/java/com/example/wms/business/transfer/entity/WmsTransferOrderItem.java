package com.example.wms.business.transfer.entity;

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
import java.time.LocalDateTime;

@Data
@TableName("wms_transfer_order_item")
public class WmsTransferOrderItem implements Serializable, ChangeItem {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细主键ID */
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    /** 调拨单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long transferId;

    /** 调拨单号 */
    private String transferNo;

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

    /** 调拨数量 */
    private Integer transferQty;

    /** 已收数量 */
    private Integer receivedQty;

    /** 差异数量 */
    private Integer diffQty;

    /** 成本单价 */
    private BigDecimal costPrice;

    /** 成本小计 */
    private BigDecimal subtotal;

    /** 批次号 */
    private String batchNo;

    /** 调出库位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long outLocationId;

    /** 调入库位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inLocationId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 仓库ID(非数据库字段) */
    @TableField(exist = false)
    private Long warehouseId;

    /** 库位ID(非数据库字段) */
    @TableField(exist = false)
    private Long locationId;

    @Override
    public Integer getQty() {
        return transferQty != null ? transferQty : 0;
    }
}
