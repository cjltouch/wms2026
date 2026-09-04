package com.example.wms.business.basedata.location.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class LocationPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String locationCode;

    private String locationName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long areaId;

    private String locationType;

    private String status;
}
