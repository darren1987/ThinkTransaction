package org.thinktransaction.core.schema.ctp;

import org.thinktransaction.core.EventResult;
import org.thinktransaction.core.TransactionEvent;
import org.thinktransaction.core.TransactionEventModel;

/**
 * 补偿型事务事件
 *
 * @author darren.ouyang
 * @version 2018/10/22 14:27
 */
public abstract class BaseCtpEvent implements TransactionEvent {

    @Override
    public String eventType() {
        return getClass().getSimpleName();
    }

    /**
     * 事件执行方法
     *
     * @param model 事件Model
     * @return 执行结果
     */
    protected abstract EventResult executeEvent(TransactionEventModel model);

    /**
     * 补偿事件方法
     *
     * @param model 事件Model
     * @return 执行结果
     */
    protected abstract EventResult<Boolean> compensateEvent(TransactionEventModel model);
}
