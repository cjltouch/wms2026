package com.example.wms.business.sale.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaleSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 销售单ID（新增时为空，更新时必填） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long saleId;

    /** 销售单号 */
    private String saleNo;

    /** 客户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 销售类型 */
    private Integer saleType;

    /** 总数量 */
    private Integer totalQty;

    /** 商品金额 */
    private BigDecimal goodsAmount;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 销售金额 */
    private BigDecimal saleAmount;

    /** 已收金额 */
    private BigDecimal receivedAmount;

    /** 付款状态 */
    private Integer payStatus;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货人地址 */
    private String receiverAddress;

    /** 快递公司 */
    private String expressCompany;

    /** 快递单号 */
    private String expressNo;

    /** 快递费用 */
    private BigDecimal expressFee;

    /** 销售日期 */
    private LocalDate saleDate;

    /** 备注 */
    private String remark;

    /** 销售明细列表 */
    private List<SaleItemSaveReq> items;
}
