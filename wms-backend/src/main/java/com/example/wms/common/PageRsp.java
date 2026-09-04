package com.example.wms.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageRsp<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long total;
    private List<T> rows;
    private Integer pageNum;
    private Integer pageSize;
    private Map<String, Object> summary;

    public PageRsp() {
    }

    public PageRsp(Long total, List<T> rows, Integer pageNum, Integer pageSize) {
        this.total = total;
        this.rows = rows;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public PageRsp(Long total, List<T> rows, Integer pageNum, Integer pageSize, Map<String, Object> summary) {
        this.total = total;
        this.rows = rows;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.summary = summary;
    }
}
