package org.thinktransaction.core.schema.tcc;

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
 * tcc事务执行器
 *
 * @author darren.ouyang
 * @version 2018/10/23 11:24
 */
@Data
@Accessors(chain = true)
public class TccExecutor {

    private LinkedHashMap<TransactionEventModel, BaseTccEvent> eventContainer;
    private TransactionDataProvider dataProvider;
    private ExecutorService executorService;


    private String uuid;
    private Date currentTime;

    public void start(){
        this.uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        this.currentTime = new Date();
        this.eventContainer = new LinkedHashMap<>();
    }

    public <E> EventResult<E> tryEvent (BaseTccEvent event){
        TransactionEventModel eventModel = new TransactionEventModel()
            .setUuid(this.uuid)
            .setStep(eventContainer.size())
            .setTransactionType(TransactionTypeEnum.TCC.getCode())
            .setAction(TransactionActionEnum.TRY.getCode())
            .setEventType(event.eventType())
            .setStatus(TransactionStatusEnum.TCC_TRYING.getCode())
            .setRetryCount(0)
            .setErrorInfo("")
            .setEventJson(JsonUtils.toJson(event))
            .setCreateTime(this.currentTime);
        eventContainer.put(eventModel, event);

        // 保存初始化后的事件. eventSave TCC event(mysql | memory)(status: TCC_TRYING)
        dataProvider.eventSave(eventModel);

        String errorInfo;
        try {
            // 执行业务
            EventResult<E> result = event.tryEvent(eventModel);
            if (result.isSuccessFlag()){
                return result;
            }
            errorInfo = result.buildErrorInfo();
        } catch (Exception e){
            errorInfo = ExceptionUtils.getStackTrace(e);
        }

        // 执行try异常, 更新event(status: TCC_TRY_ERROR, errorInfo)
        eventModel.setStatus(TransactionStatusEnum.TCC_TRY_ERROR.getCode());
        eventModel.setErrorInfo(errorInfo);
        dataProvider.eventUpdate(eventModel);

        // 异步执行task uuid取消操作(task-2), 对之前所有事件进行异步取消操作
        eventContainer.remove(eventModel);
        executorService.submit(()->
            eventContainer.forEach((containerModel, containerEvent)->
                TccExecutor.eventCancel0(dataProvider, containerModel, containerEvent)
            )
        );

        // 抛出异常, 终止业务当前流程
        throw new TransactionException(errorInfo);
    }

    /**
     * 所有事件提交确认
     */
    public void commitConfirm(){
        dataProvider.eventUpdateStatusByUuid(TransactionStatusEnum.TCC_CONFIRMING.getCode(), this.uuid);
        executorService.submit(()->
            eventContainer.forEach((containerModel, containerEvent)->
                TccExecutor.eventConfirm0(dataProvider, containerModel, containerEvent)
            )
        );
    }

    /**
     * 所有事件提交取消
     */
    public void commitCancel(){
        dataProvider.eventUpdateStatusByUuid(TransactionStatusEnum.TCC_CANCELING.getCode(), this.uuid);
        executorService.submit(()->
            eventContainer.forEach((containerModel, containerEvent)->
                TccExecutor.eventCancel0(dataProvider, containerModel, containerEvent)
            )
        );
    }

    public static boolean eventConfirm0 (TransactionDataProvider dataProvider, TransactionEventModel eventModel, BaseTccEvent event){
        try {
            // 执行业务
            eventModel.setAction(TransactionActionEnum.CONFIRM.getCode());
            EventResult<Boolean> result = event.confirmEvent(eventModel);
            if (result.isSuccessFlag()){
                // 执行成功, 更新(status: TCC_CONFIRM_FINISH)
                eventModel.setStatus(TransactionStatusEnum.TCC_CONFIRM_FINISH.getCode());
                dataProvider.eventUpdate(eventModel);
                return true;
            } else {
                // 执行异常, 更新retry += 1, errorInfo  status: TCC_CONFIRMING
                eventModel.setRetryCount(eventModel.getRetryCount() + 1);
                eventModel.setErrorInfo(result.buildErrorInfo());
                eventModel.setStatus(TransactionStatusEnum.TCC_CONFIRMING.getCode());
                dataProvider.eventUpdate(eventModel);
                return false;
            }
        } catch (Exception e){
            // 执行异常,更新retry += 1, errorInfo  status: TCC_CONFIRMING
            eventModel.setRetryCount(eventModel.getRetryCount() + 1);
            eventModel.setErrorInfo(ExceptionUtils.getStackTrace(e));
            eventModel.setStatus(TransactionStatusEnum.TCC_CONFIRMING.getCode());
            dataProvider.eventUpdate(eventModel);
            return false;
        }
    }


    public static boolean eventCancel0 (TransactionDataProvider dataProvider, TransactionEventModel eventModel, BaseTccEvent event){
        try {
            // 执行业务
            eventModel.setAction(TransactionActionEnum.CANCEL.getCode());
            EventResult<Boolean> result = event.cancelEvent(eventModel);
            if (result.isSuccessFlag()){
                // 执行成功, 更新(status: TCC_CANCEL_FINISH)
                eventModel.setStatus(TransactionStatusEnum.TCC_CANCEL_FINISH.getCode());
                dataProvider.eventUpdate(eventModel);
                return true;
            } else {
                // 执行异常, 更新retry += 1, errorInfo  status: TCC_CANCELING
                eventModel.setRetryCount(eventModel.getRetryCount() + 1);
                eventModel.setErrorInfo(result.buildErrorInfo());
                eventModel.setStatus(TransactionStatusEnum.TCC_CANCELING.getCode());
                dataProvider.eventUpdate(eventModel);
                return false;
            }
        } catch (Exception e){
            // 执行异常,更新retry += 1, errorInfo  status: TCC_CANCELING
            eventModel.setRetryCount(eventModel.getRetryCount() + 1);
            eventModel.setErrorInfo(ExceptionUtils.getStackTrace(e));
            eventModel.setStatus(TransactionStatusEnum.TCC_CANCELING.getCode());
            dataProvider.eventUpdate(eventModel);
            return false;
        }
    }
}
