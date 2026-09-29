package com.example.wms.business.analysis.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ReplenishmentReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分析维度：sku按SKU / innerCode按内部编码 */
    private String dimension = "sku";

    /** 历史周期天数（7/15/30） */
    private Integer historyDays = 30;

    /** 预测周期天数（7/15/30） */
    private Integer forecastDays = 15;

    /** 仓库ID（可选，不传为全部仓库） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 商品分类ID（可选，不传为全部分类） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;
}
