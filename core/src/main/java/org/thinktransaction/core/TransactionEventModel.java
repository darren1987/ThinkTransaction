package org.thinktransaction.core;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Objects;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 事务事件模型
 *
 * @author darren.ouyang
 * @version 2018/9/26 16:39
 */
@Data
@Accessors(chain = true)
public class TransactionEventModel {

    /**
     * 事务uuid
     */
    private String uuid;

    /**
     * 事务执行步骤, 同一事务uuid下每个event步骤不同, 顺序递增, 从0开始
     */
    private Integer step;

    /**
     * 事务类型
     * @see TransactionTypeEnum
     */
    private String transactionType;

    /**
     * 事务行为动作
     *
     */
    private String action;

    /**
     * 事件类型(事件类名)
     * @see TransactionEventManager#EVENT_CLASS_MAP
     */
    private String eventType;

    /**
     * 事件状态
     * @see TransactionStatusEnum
     */
    private String status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 错误异常信息
     */
    private String errorInfo;

    /**
     * 事件json数据
     */
    private String eventJson;

    /**
     * 创建时间
     */
    private Date createTime;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof TransactionEventModel)) {
            return false;
        }
        TransactionEventModel object = (TransactionEventModel) o;
        return Objects.equals(object.uuid, uuid)
            && Objects.equals(object.step, step)
            && Objects.equals(object.eventType, eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, step, eventType);
    }

    /**
     * 事件幂等key
     * 规则: {事务类型}-{动作类型}-{事务uuid}-{执行编号}-{事件类型(名称)}
     *
     * @return 事件幂等key
     */
    public String idempotentKey (){
        return MessageFormat.format("{0}-{1}-{2}-{3}-{4}", transactionType, action, uuid, step, eventType);
    }
}
