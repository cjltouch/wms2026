package com.example.wms.business.basedata.goods.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SkuSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** 主供应商ID（采购下单时按供应商过滤可选商品） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long supplierId;

    private String skuCode;

    /** 内部编码 */
    private String innerCode;

    private String barcode;

    private String specText;

    private Integer weightG;

    private Integer volumeMl;

    private String color;

    private Integer batchFlag;

    private Integer expireFlag;

    private Integer snFlag;

    private Integer shelfLifeDays;

    private BigDecimal defaultCost;

    private BigDecimal defaultSale;

    private String status;
}
