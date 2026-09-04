package com.example.wms.business.basedata.location.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LocationBatchGenerateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long areaId;

    private String locationTemplate;

    private String locationType;

    private Integer maxQty;

    private String status;
}
