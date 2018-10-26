package org.thinktransaction.core;

/**
 * 事务事件接口
 *
 * @author darren.ouyang
 * @version 2018/10/23 16:57
 */
public interface TransactionEvent {

    /**
     * 返回该事件的类型
     *
     * @return 事件类型, event类名
     */
    String eventType();
}
