package com.example.wms.auth;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String realName;
    private List<String> warehouseIds;
    private Integer dataScope;
    private List<String> roleCodes;
    private Set<String> perms;
}
