package org.thinktransaction.core.idempotent;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 幂等返回结果
 *
 * @author darren.ouyang
 * @version 2018/10/26 15:51
 */
@Data
@Accessors(chain = true)
public class IdempotentResult<T> {

    /**
     * 是否存在数据
     */
    private boolean existFlag;

    /**
     * 返回内容
     */
    private T content;
}
