package org.thinktransaction.core.schema.ctp;

import org.thinktransaction.core.EventResult;
import org.thinktransaction.core.TransactionActionEnum;
import org.thinktransaction.core.TransactionDataProvider;
import org.thinktransaction.core.TransactionEventModel;
import org.thinktransaction.core.TransactionException;
import org.thinktransaction.core.TransactionStatusEnum;
import org.thinktransaction.core.TransactionTypeEnum;
import org.thinktransaction.core.utils.JsonUtils;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 补偿型事务执行器
 * Compensating Transaction pattern
 *
 * @author darren.ouyang
 * @version 2018/10/22 17:59
 */
@Data
@Accessors(chain = true)
public class CtpExecutor {

    private LinkedHashMap<TransactionEventModel, BaseCtpEvent> eventContainer;
    private TransactionDataProvider dataProvider;
    private ExecutorService executorService;

    private String uuid;
    private Date currentTime;

    public void start(){
        this.uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        this.currentTime = new Date();
        this.eventContainer = new LinkedHashMap<>();
    }

    public <E> EventResult<E> executeEvent(BaseCtpEvent event){
        TransactionEventModel eventModel = new TransactionEventModel()
            .setUuid(this.uuid)
            .setStep(eventContainer.size())
            .setTransactionType(TransactionTypeEnum.CTP.getCode())
            .setAction(TransactionActionEnum.EXECUTE.getCode())
            .setEventType(event.eventType())
            .setStatus(TransactionStatusEnum.CTP_INIT.getCode())
            .setRetryCount(0)
            .setErrorInfo("")
            .setEventJson(JsonUtils.toJson(event))
            .setCreateTime(this.currentTime);
        eventContainer.put(eventModel, event);

        // 保存初始化后的事件. eventSave CTP event(mysql | memory)(status: CTP_INIT)
        dataProvider.eventSave(eventModel);

        String errorInfo;
        try {
            // 执行业务
            EventResult<E> result = event.executeEvent(eventModel);
            if (result.isSuccessFlag()){
                return result;
            }
            errorInfo = result.buildErrorInfo();
        } catch (Exception e){
            errorInfo = ExceptionUtils.getStackTrace(e);
        }

        // 执行异常, 更新event(status: CTP_ERROR, errorInfo)
        eventModel.setStatus(TransactionStatusEnum.CTP_ERROR.getCode());
        eventModel.setErrorInfo(errorInfo);
        dataProvider.eventUpdate(eventModel);

        // 异步执行task uuid补偿操作(task-2), 对之前所有事件进行异步补偿操作
        eventContainer.remove(eventModel);
        executorService.submit(()->
            eventContainer.forEach((containerModel, containerEvent)->
                CtpExecutor.eventCompensate0(dataProvider, containerModel, containerEvent)
            )
        );

        // 抛出异常, 终止业务当前流程
        throw new TransactionException(errorInfo);
    }

    /**
     * 提交确认成功
     */
    public void commitSuccess(){
        dataProvider.eventUpdateStatusByUuid(TransactionStatusEnum.CTP_FINISH.getCode(), this.uuid);
    }

    /**
     * 提交补偿
     */
    public void commitCompensate(){
        dataProvider.eventUpdateStatusByUuid(TransactionStatusEnum.CTP_COMPENSATING.getCode(), this.uuid);
    }

    public static boolean eventCompensate0(TransactionDataProvider dataProvider, TransactionEventModel eventModel, BaseCtpEvent event){
        try {
            // 执行业务
            eventModel.setAction(TransactionActionEnum.COMPENSATE.getCode());
            EventResult<Boolean> result = event.compensateEvent(eventModel);
            if (result.isSuccessFlag()){
                // 执行成功, 更新(status: CTP_COMPENSATE_FINISH)
                eventModel.setStatus(TransactionStatusEnum.CTP_COMPENSATE_FINISH.getCode());
                dataProvider.eventUpdate(eventModel);
                return true;
            } else {
                // 执行异常, 更新retry += 1, errorInfo, status: CTP_COMPENSATING
                eventModel.setRetryCount(eventModel.getRetryCount() + 1);
                eventModel.setErrorInfo(result.buildErrorInfo());
                eventModel.setStatus(TransactionStatusEnum.CTP_COMPENSATING.getCode());
                dataProvider.eventUpdate(eventModel);
                return false;
            }
        } catch (Exception e){
            // 执行异常, 更新retry += 1, errorInfo, status: CTP_COMPENSATING
            eventModel.setRetryCount(eventModel.getRetryCount() + 1);
            eventModel.setErrorInfo(ExceptionUtils.getStackTrace(e));
            eventModel.setStatus(TransactionStatusEnum.CTP_COMPENSATING.getCode());
            dataProvider.eventUpdate(eventModel);
            return false;
        }
    }
}
