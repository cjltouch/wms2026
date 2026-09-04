package com.example.wms.business.basedata.brand.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class BrandSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long brandId;

    private String brandName;

    private String brandCode;

    private String firstLetter;

    private String logo;

    private String banner;

    private String description;

    private Integer sort;

    private String status;
}
