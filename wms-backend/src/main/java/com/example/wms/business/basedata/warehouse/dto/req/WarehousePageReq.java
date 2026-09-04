package com.example.wms.business.basedata.warehouse.dto.req;

import com.example.wms.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WarehousePageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String warehouseName;

    private String warehouseCode;

    private Integer warehouseType;

    private String status;
}
