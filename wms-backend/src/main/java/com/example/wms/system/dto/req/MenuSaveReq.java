package com.example.wms.system.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MenuSaveReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    @NotNull(message = "显示顺序不能为空")
    private Integer orderNum;

    private String path;

    private String component;

    private String query;

    private String routeName;

    private Integer isFrame;

    private Integer isCache;

    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    private String visible;

    private String status;

    private String perms;

    private String icon;

    private String remark;
}
