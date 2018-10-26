package org.thinktransaction.core.schema.tcc;

import org.thinktransaction.core.EventResult;
import org.thinktransaction.core.TransactionEvent;
import org.thinktransaction.core.TransactionEventModel;

/**
 * tcc事务事件
 *
 * @author darren.ouyang
 * @version 2018/10/22 15:26
 */
public abstract class BaseTccEvent implements TransactionEvent {

    @Override
    public String eventType() {
        return getClass().getSimpleName();
    }

    /**
     * 事件尝试方法
     *
     * @param model 事件Model
     * @return 执行结果
     */
    protected abstract EventResult tryEvent (TransactionEventModel model);

    /**
     * 事件提交方法
     *
     * @param model 事件Model
     * @return 执行结果
     */
    protected abstract EventResult<Boolean> confirmEvent (TransactionEventModel model);

    /**
     * 事件取消方法
     *
     * @param model 事件Model
     * @return 执行结果
     */
    protected abstract EventResult<Boolean> cancelEvent (TransactionEventModel model);
}
