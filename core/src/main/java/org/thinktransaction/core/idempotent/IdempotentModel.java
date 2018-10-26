package org.thinktransaction.core.idempotent;

import java.util.Objects;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 幂等model
 *
 * @author darren.ouyang
 * @version 2018/10/26 11:32
 */
@Data
@Accessors(chain = true)
public class IdempotentModel {

    /**
     * 幂等key, 唯一值
     */
    private String key;

    /**
     * 结果json数据
     */
    private String resultJson;


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof IdempotentModel)) {
            return false;
        }
        IdempotentModel object = (IdempotentModel) o;
        return Objects.equals(object.key, key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
