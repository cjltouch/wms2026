package com.example.wms.business.loss.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
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
@TableName("wms_loss_order_item")
public class WmsLossOrderItem implements Serializable, ChangeItem {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细主键ID */
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    /** 报损单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long lossId;

    /** 报损单号 */
    private String lossNo;

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

    /** 报损数量 */
    private Integer lossQty;

    /** 成本单价 */
    private BigDecimal costPrice;

    /** 小计金额 */
    private BigDecimal subtotal;

    /** 批次号 */
    private String batchNo;

    /** 库位ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 报损原因 */
    private String lossReason;

    /** 备注 */
    private String remark;

    /** 仓库ID（非数据库字段，用于库存变更处理） */
    @TableField(exist = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @Override
    public Integer getQty() {
        return lossQty;
    }
}
