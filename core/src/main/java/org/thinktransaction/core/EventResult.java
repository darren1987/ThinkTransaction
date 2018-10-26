package org.thinktransaction.core;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 事件执行结果集合
 *
 * @author darren.ouyang
 * @version 2018/9/29 15:38
 */
@Data
@Accessors(chain = true)
public class EventResult<T> {

    /**
     * 执行成功标志, true成功, false失败
     */
    private boolean successFlag;

    /**
     * 错误信息
     */
    private String errorInfo;

    /**
     * 截获异常
     */
    private Throwable throwable;

    /**
     * 返回内容
     */
    private T content;

    /**
     * 构建异常信息
     *
     * @return 异常信息
     */
    public String buildErrorInfo (){

        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(errorInfo)){
            sb.append(errorInfo);
        }

        if (throwable != null){
            sb.append("\r\n").append(ExceptionUtils.getStackTrace(throwable));
        }

        return sb.toString();
    }

    /**
     *
     * 执行成功
     *
     * @param content 如存在需要返回的对象
     * @param <E> 对象类型
     * @return 结果
     */
    public static <E> EventResult<E> ok(E content){
        return new EventResult<E>()
            .setSuccessFlag(true)
            .setContent(content);
    }

    /**
     * 执行失败
     *
     * @param <E> 对象类型
     * @return 结果
     */
    public static <E> EventResult<E> error(){
        return new EventResult<E>().setSuccessFlag(false);
    }
}
