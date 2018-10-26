package org.thinktransaction.core;

import org.thinktransaction.core.schema.bedt.BaseBedtEvent;
import org.thinktransaction.core.schema.ctp.BaseCtpEvent;
import org.thinktransaction.core.schema.tcc.BaseTccEvent;
import org.thinktransaction.core.utils.JsonUtils;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 事务管理器
 *
 * @author darren.ouyang
 * @version 2018/9/26 19:36
 */
public final class TransactionEventManager {

    /**
     * 事件class对象容器 map《事件类名, 事件class对象》
     */
    private final static Map<String, Class<? extends TransactionEvent>> EVENT_CLASS_MAP = new HashMap<>();

    /**
     * 注册事件
     *
     * @param clazz 事件class对象
     */
    public synchronized static void registerEvent (Class<? extends TransactionEvent> clazz){
        String simpleName = clazz.getSimpleName();
        if (EVENT_CLASS_MAP.containsKey(simpleName)){
            throw new RuntimeException(MessageFormat.format(
                "TransactionEventManager register error, event[{0}] already exist, all events：{1}",
                simpleName, EVENT_CLASS_MAP.keySet()));
        }

        EVENT_CLASS_MAP.put(simpleName, clazz);
    }

    /**
     * 创建最大努力型事件
     *
     * @param model 数据
     * @return 事件
     */
    public static BaseBedtEvent createBedtEvent (TransactionEventModel model){
        String simpleName = model.getEventType();
        Class<? extends TransactionEvent> clazz = EVENT_CLASS_MAP.get(simpleName);
        if (clazz == null){
            throw new RuntimeException(MessageFormat.format(
                "TransactionEventManager createBedtEvent error, event[{0}] not exist, all events：{1}",
                simpleName, EVENT_CLASS_MAP.keySet()));
        }

        return JsonUtils.jsonToType(model.getEventJson(), (Class<BaseBedtEvent>) clazz);
    }

    /**
     * 创建补偿型事件
     *
     * @param model 数据
     * @return 事件
     */
    public static BaseCtpEvent createCtpEvent (TransactionEventModel model){
        String simpleName = model.getEventType();
        Class<? extends TransactionEvent> clazz = EVENT_CLASS_MAP.get(simpleName);
        if (clazz == null){
            throw new RuntimeException(MessageFormat.format(
                "TransactionEventManager createCtpEvent error, event[{0}] not exist, all events：{1}",
                simpleName, EVENT_CLASS_MAP.keySet()));
        }

        return JsonUtils.jsonToType(model.getEventJson(), (Class<BaseCtpEvent>) clazz);
    }

    /**
     * 创建tcc事件
     *
     * @param model 数据
     * @return 事件
     */
    public static BaseTccEvent createTccEvent (TransactionEventModel model){
        String simpleName = model.getEventType();
        Class<? extends TransactionEvent> clazz = EVENT_CLASS_MAP.get(simpleName);
        if (clazz == null){
            throw new RuntimeException(MessageFormat.format(
                "TransactionEventManager createTccEvent error, event[{0}] not exist, all events：{1}",
                simpleName, EVENT_CLASS_MAP.keySet()));
        }

        return JsonUtils.jsonToType(model.getEventJson(), (Class<BaseTccEvent>) clazz);
    }

}
