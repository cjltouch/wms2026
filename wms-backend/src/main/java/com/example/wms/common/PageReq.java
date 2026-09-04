package com.example.wms.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PageReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderBy;
    private LocalDateTime dateRangeStart;
    private LocalDateTime dateRangeEnd;

    /**
     * 兼容前端部分页面传 page/size 的分页参数：
     * JSON 反序列化时映射到 pageNum/pageSize，避免落到默认值导致翻页失效。
     */
    public void setPage(Integer page) {
        if (page != null) {
            this.pageNum = page;
        }
    }

    public void setSize(Integer size) {
        if (size != null) {
            this.pageSize = size;
        }
    }
}
