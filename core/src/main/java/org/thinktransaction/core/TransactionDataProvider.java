package org.thinktransaction.core;

import org.thinktransaction.core.idempotent.IdempotentModel;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 数据存储器
 *
 * @author darren.ouyang
 * @version 2018/9/26 16:10
 */
public interface TransactionDataProvider {

    void eventSave(TransactionEventModel model);
    void eventSaveBatch(Collection<TransactionEventModel> models);
    void eventUpdate(TransactionEventModel model);
    void eventUpdateStatusByUuid(String toStatus, String uuid);
    void eventUpdateStatusByTimeout(String toStatus, String fromStatus, Date gtDate);
    List<TransactionEventModel> eventSelectByStatus(String status);

    void idempotentTrySave(IdempotentModel model);
    IdempotentModel idempotentGet(String idempotentKey);
    IdempotentModel idempotentUpdateScene(IdempotentModel model);

}
