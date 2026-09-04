package com.example.wms.business.basedata.goods.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SpuBatchDeleteReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> ids;
}
