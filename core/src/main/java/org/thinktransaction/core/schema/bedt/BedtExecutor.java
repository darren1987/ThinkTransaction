package org.thinktransaction.core.schema.bedt;

import org.thinktransaction.core.EventResult;
import org.thinktransaction.core.TransactionActionEnum;
import org.thinktransaction.core.TransactionDataProvider;
import org.thinktransaction.core.TransactionEventModel;
import org.thinktransaction.core.TransactionStatusEnum;
import org.thinktransaction.core.TransactionTypeEnum;
import org.thinktransaction.core.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 最大努力型分布式事务执行器
 * Best Effort Distributed Transaction
 *
 * @author darren.ouyang
 * @version 2018/9/26 15:10
 */
@Data
@Accessors(chain = true)
public class BedtExecutor {

    private List<BaseBedtEvent> eventContainer;
    private TransactionDataProvider dataProvider;
    private ExecutorService executorService;

    private String uuid;
    private Date currentTime;

    public void start (){
        this.uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        this.currentTime = new Date();
        this.eventContainer = new ArrayList<>();
    }

    public EventResult<Boolean> sendEvent(BaseBedtEvent event){
        this.eventContainer.add(event);
        return EventResult.ok(true);
    }

    public void commit(){
        Map<TransactionEventModel, BaseBedtEvent> eventMap = new HashMap<>(this.eventContainer.size());
        for (BaseBedtEvent event : this.eventContainer) {
            TransactionEventModel eventModel = new TransactionEventModel()
                .setUuid(this.uuid)
                .setStep(eventContainer.size())
                .setTransactionType(TransactionTypeEnum.BEDT.getCode())
                .setAction(TransactionActionEnum.EXECUTE.getCode())
                .setEventType(event.eventType())
                .setStatus(TransactionStatusEnum.BEDT_EXECUTING.getCode())
                .setRetryCount(0)
                .setErrorInfo("")
                .setEventJson(JsonUtils.toJson(event))
                .setCreateTime(this.currentTime);
            eventMap.put(eventModel, event);
        }

        // 保存所有事件, (status: BEDT_EXECUTING, retry:0)
        dataProvider.eventSaveBatch(eventMap.keySet());

        // 异步提交每个事件并执行
        executorService.submit(()->
            eventMap.forEach((model, event)->
                BedtExecutor.eventExecute0(dataProvider, model, event)
            )
        );
    }

    public static boolean eventExecute0(TransactionDataProvider dataProvider, TransactionEventModel eventModel, BaseBedtEvent event){
        try {
            // 执行业务
            EventResult<Boolean> result = event.executeEvent(eventModel);
            if (result.isSuccessFlag()){
                // 执行成功, 更新(status: BEDT_FINISH)
                eventModel.setStatus(TransactionStatusEnum.BEDT_FINISH.getCode());
                dataProvider.eventUpdate(eventModel);
                return true;
            } else {
                // 执行异常, 更新retry += 1, errorInfo
                eventModel.setRetryCount(eventModel.getRetryCount() + 1);
                eventModel.setErrorInfo(result.buildErrorInfo());
                dataProvider.eventUpdate(eventModel);
                return false;
            }
        } catch (Exception e){
            // 执行异常, 更新retry += 1, errorInfo
            eventModel.setRetryCount(eventModel.getRetryCount() + 1);
            eventModel.setErrorInfo(ExceptionUtils.getStackTrace(e));
            dataProvider.eventUpdate(eventModel);
            return false;
        }
    }


}
