package com.example.wms.business.basedata.area.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AreaSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long areaId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    private String areaCode;

    private String areaName;

    private Integer areaType;

    private String manager;

    private String phone;

    private Integer sort;

    private String status;
}
