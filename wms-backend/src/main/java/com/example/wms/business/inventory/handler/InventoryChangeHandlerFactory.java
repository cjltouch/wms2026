package com.example.wms.business.inventory.handler;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 库存变更处理器工厂
 * <p>
 * 根据业务类型(ChangeType)分发到对应的库存变更处理器(InventoryChangeHandler)。
 * Spring 启动时自动注入所有 handler 实现，并按 type() 方法注册到 EnumMap。
 * <p>
 * 业务模块调用方式：handlerFactory.getHandler(ChangeType.LOSS).handle(changeItem)
 */
@Component
public class InventoryChangeHandlerFactory {

    private final Map<ChangeType, InventoryChangeHandler> handlerMap;

    public InventoryChangeHandlerFactory(List<InventoryChangeHandler> handlers) {
        this.handlerMap = new EnumMap<>(ChangeType.class);
        for (InventoryChangeHandler handler : handlers) {
            handlerMap.put(handler.type(), handler);
        }
    }

    /**
     * 根据变更类型获取对应的库存处理器
     * @param type 库存变更类型（出库/入库/调拨出/调拨入/报损/盘点等）
     * @return 对应的处理器实现
     */
    public InventoryChangeHandler getHandler(ChangeType type) {
        InventoryChangeHandler handler = handlerMap.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("未找到对应库存变更处理器: " + type);
        }
        return handler;
    }
}
