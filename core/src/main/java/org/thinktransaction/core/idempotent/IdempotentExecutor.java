package org.thinktransaction.core.idempotent;

import org.thinktransaction.core.TransactionDataProvider;
import org.thinktransaction.core.TransactionException;
import org.thinktransaction.core.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 幂等处理执行器
 *
 * @author darren.ouyang
 * @version 2018/10/26 11:22
 */
@Data
@Accessors(chain = true)
public class IdempotentExecutor {

    private TransactionDataProvider dataProvider;

    /**
     * 尝试写入幂等key (无返回参数)
     *
     * @param idempotentKey 幂等key
     * @return 结果
     */
    public IdempotentResult trySaveKey (String idempotentKey){
        return trySaveKey(idempotentKey, null);
    }

    /**
     * 尝试写入幂等key
     *
     * @param idempotentKey 幂等key
     * @param typeReference 返回参数类型
     * @return 结果
     */
    public <T> IdempotentResult<T> trySaveKey (String idempotentKey, TypeReference<T> typeReference){

        IdempotentModel model = new IdempotentModel().setKey(idempotentKey).setResultJson("");
        IdempotentResult<T> result = new IdempotentResult<>();

        try{
            // 尝试写入幂等key, 是写入成功流程继续执行, 是写入失败检测是否幂等返回 或 抛出异常
            dataProvider.idempotentTrySave(model);
            return result.setExistFlag(false);
        } catch (Exception e){
            model = dataProvider.idempotentGet(idempotentKey);
            if (model == null){
                throw new TransactionException("trySaveKey error, exist but not find data. idempotentKey:" + idempotentKey, e);
            }

            // 恢复之前现场对象并返回
            if (typeReference != null) {
                try {
                    T content = JsonUtils.jsonToType(model.getResultJson(), typeReference);
                    result.setContent(content);
                }catch (Exception toJsonException){
                    throw new TransactionException("trySaveKey error, string can't to json. idempotentKey:"
                        + idempotentKey + ", json:" + model.getResultJson(), toJsonException);
                }
            }

            return result.setExistFlag(true);
        }
    }

    /**
     * 保存场景数据
     *
     * @param idempotentKey 幂等key
     * @param contentObject 场景数据
     */
    public void saveScene (String idempotentKey, Object contentObject){
        if (contentObject == null){
            throw new TransactionException("saveScene error, contentObject can't be null, idempotentKey: " + idempotentKey);
        }

        IdempotentModel model = new IdempotentModel()
            .setKey(idempotentKey)
            .setResultJson(JsonUtils.toJson(contentObject));
        dataProvider.idempotentUpdateScene(model);
    }
}
