package com.example.wms.business.basedata.goods.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsSpuPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String spuName;

    private String spuCode;

    private String keyword;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long brandId;

    private String status;
}
