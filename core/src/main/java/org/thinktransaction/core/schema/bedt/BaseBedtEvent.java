package org.thinktransaction.core.schema.bedt;

import org.thinktransaction.core.EventResult;
import org.thinktransaction.core.TransactionEvent;
import org.thinktransaction.core.TransactionEventModel;

/**
 * 最大努力型事件
 *
 * @author darren.ouyang
 * @version 2018/10/22 15:32
 */
public abstract class BaseBedtEvent implements TransactionEvent {

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
    protected abstract EventResult<Boolean> executeEvent(TransactionEventModel model);

}
