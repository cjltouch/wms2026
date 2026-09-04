package com.example.wms.system.dto.req;

import com.example.wms.system.entity.SysUser;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class UserSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private SysUser user;

    private List<Long> roleIds;

    private String password;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String status;
}
