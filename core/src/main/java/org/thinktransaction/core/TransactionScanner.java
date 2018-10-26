package org.thinktransaction.core;

import org.thinktransaction.core.schema.bedt.BaseBedtEvent;
import org.thinktransaction.core.schema.bedt.BedtExecutor;
import org.thinktransaction.core.schema.ctp.BaseCtpEvent;
import org.thinktransaction.core.schema.ctp.CtpExecutor;
import org.thinktransaction.core.schema.tcc.BaseTccEvent;
import org.thinktransaction.core.schema.tcc.TccExecutor;
import java.util.Date;
import java.util.List;

/**
 * 事务扫描器
 *
 * @author darren.ouyang
 * @version 2018/9/26 16:28
 */
public class TransactionScanner implements Runnable {

    /**
     * 数据存储供应对象
     */
    private final TransactionDataProvider transactionDataProvider;

    TransactionScanner (TransactionDataProvider transactionDataProvider){
        this.transactionDataProvider = transactionDataProvider;
    }

    @Override
    public void run (){
        bedtScanner();
        ctpScanner();
        tccScanner();
    }

    /**
     * 最大努力型分布式事务扫描
     */
    private void bedtScanner (){

        // 查找BEDT events (status: BEDT_EXECUTING)
        List<TransactionEventModel> modelList = transactionDataProvider
            .eventSelectByStatus(TransactionStatusEnum.BEDT_EXECUTING.getCode());
        for (TransactionEventModel model : modelList){

            BaseBedtEvent event = TransactionEventManager.createBedtEvent(model);
            boolean result = BedtExecutor.eventExecute0(transactionDataProvider, model, event);

            // event retry超过最大次数: (status: BEDT_ERROR)
            if (!result && model.getRetryCount() > 5){
                model.setStatus(TransactionStatusEnum.BEDT_ERROR.getCode());
                transactionDataProvider.eventUpdate(model);
            }
        }
    }

    /**
     * 补偿型事务扫描
     */
    private void ctpScanner (){
        // 1. 查找events (status: CTP_INIT) & 时间超出有效范围(3分账) => 标记status: CTP_COMPENSATING
        transactionDataProvider.eventUpdateStatusByTimeout(
            TransactionStatusEnum.CTP_COMPENSATING.getCode(),
            TransactionStatusEnum.CTP_INIT.getCode(),
            new Date(System.currentTimeMillis()-5L*60L*1000L)
        );

        // 2. 查找events (status: CTP_COMPENSATING)
        List<TransactionEventModel> modelList = transactionDataProvider
            .eventSelectByStatus(TransactionStatusEnum.CTP_COMPENSATING.getCode());
        for (TransactionEventModel model : modelList){

            BaseCtpEvent event = TransactionEventManager.createCtpEvent(model);
            boolean result = CtpExecutor.eventCompensate0(transactionDataProvider, model, event);

            // 超过最大重试次数 : 更新(status: CTP_COMPENSATE_ERROR)
            if (!result && model.getRetryCount() > 10){
                model.setStatus(TransactionStatusEnum.CTP_COMPENSATE_ERROR.getCode());
                transactionDataProvider.eventUpdate(model);
            }
        }
    }

    /**
     * tcc事务扫描
     */
    private void tccScanner (){
        // 1. 查找events(status: TCC_TRYING) & 时间超出有效范围(5分钟) => 标记status: TCC_CANCELING
        transactionDataProvider.eventUpdateStatusByTimeout(
            TransactionStatusEnum.TCC_CANCELING.getCode(),
            TransactionStatusEnum.TCC_TRYING.getCode(),
            new Date(System.currentTimeMillis()-5L*60L*1000L)
        );

        // 2. 查找events(status: TCC_CANCELING)
        List<TransactionEventModel> cancelModelList = transactionDataProvider
            .eventSelectByStatus(TransactionStatusEnum.TCC_CANCELING.getCode());
        for (TransactionEventModel model : cancelModelList){

            BaseTccEvent event = TransactionEventManager.createTccEvent(model);
            boolean result = TccExecutor.eventCancel0(transactionDataProvider, model, event);

            // 超过最大重试次数 : 更新(status: CTP_COMPENSATE_ERROR)
            if (!result && model.getRetryCount() > 15){
                model.setStatus(TransactionStatusEnum.TCC_CANCEL_ERROR.getCode());
                transactionDataProvider.eventUpdate(model);
            }
        }

        // 3. 查找status = TCC_CONFIRMING 提交中 events
        List<TransactionEventModel> confirmModelList = transactionDataProvider
            .eventSelectByStatus(TransactionStatusEnum.TCC_CANCELING.getCode());
        for (TransactionEventModel model : confirmModelList){

            BaseTccEvent event = TransactionEventManager.createTccEvent(model);
            boolean result = TccExecutor.eventConfirm0(transactionDataProvider, model, event);

            // 超过最大重试次数 : 更新(status: TCC_CONFIRM_ERROR)
            if (!result && model.getRetryCount() > 15){
                model.setStatus(TransactionStatusEnum.TCC_CONFIRM_ERROR.getCode());
                transactionDataProvider.eventUpdate(model);
            }
        }
    }
}
