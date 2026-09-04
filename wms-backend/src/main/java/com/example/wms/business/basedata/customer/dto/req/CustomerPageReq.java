package com.example.wms.business.basedata.customer.dto.req;

import com.example.wms.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String customerName;

    private String customerCode;

    private String contact;

    private String phone;

    private String status;
}
