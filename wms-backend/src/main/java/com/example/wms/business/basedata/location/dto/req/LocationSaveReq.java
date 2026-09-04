package com.example.wms.business.basedata.location.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LocationSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long areaId;

    private String locationCode;

    private String locationName;

    private String locationType;

    private Integer rowNo;

    private Integer columnNo;

    private Integer levelNo;

    private Integer maxQty;

    private Integer sort;

    private String status;
}
