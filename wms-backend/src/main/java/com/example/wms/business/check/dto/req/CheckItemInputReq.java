package com.example.wms.business.check.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class CheckItemInputReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 盘点单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long checkId;

    /** 盘点明细实盘录入列表 */
    private List<ItemInput> items;

    @Data
    public static class ItemInput implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 明细ID */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long itemId;

        /** 实盘数量 */
        private Integer actualQty;
    }
}
