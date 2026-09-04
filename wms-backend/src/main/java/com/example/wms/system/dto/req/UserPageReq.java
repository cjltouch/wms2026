package com.example.wms.system.dto.req;

import com.example.wms.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userName;

    private String phonenumber;

    private String status;

    private Long deptId;
}
