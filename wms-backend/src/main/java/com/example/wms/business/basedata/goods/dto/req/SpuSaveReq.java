package com.example.wms.business.basedata.goods.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SpuSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long spuId;

    private String spuCode;

    private String spuName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long brandId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long unitId;

    private String origin;

    private String description;

    private String picUrls;

    private String abcLevel;

    private String status;

    private List<SkuSaveReq> skuList;
}
