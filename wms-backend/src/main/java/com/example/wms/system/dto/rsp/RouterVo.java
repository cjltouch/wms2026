package com.example.wms.system.dto.rsp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private String path;

    private String component;

    private String redirect;

    private Boolean alwaysShow;

    private Meta meta;

    private List<RouterVo> children;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;

        private String icon;

        private Boolean noCache;

        private Boolean link;

        private List<String> perms;
    }
}
