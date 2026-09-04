package com.example.wms.system.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class RoleDataScopeReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "角色ID不能为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;

    private Integer dataScope;

    private List<Long> deptIds;
}
