package com.example.wms.business.basedata.customer.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CustomerSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long customerId;

    private String customerCode;

    private String customerName;

    private String shortName;

    private String contact;

    private String phone;

    private String tel;

    private String email;

    private String address;

    private Integer creditLevel;

    private Integer paymentDays;

    private String status;
}
