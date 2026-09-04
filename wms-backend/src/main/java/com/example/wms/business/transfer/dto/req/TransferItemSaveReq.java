package com.example.wms.business.transfer.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TransferItemSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 明细主键ID(新增时为空) */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

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
}
