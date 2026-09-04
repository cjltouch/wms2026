package com.example.wms.business.basedata.warehouse.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class WarehouseSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private Integer warehouseType;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String manager;

    private String phone;

    private BigDecimal area;

    private Integer sort;

    private String status;
}
