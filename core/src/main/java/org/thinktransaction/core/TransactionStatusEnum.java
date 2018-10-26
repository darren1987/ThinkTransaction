package org.thinktransaction.core;

import lombok.Getter;

/**
 * 事务执行器状态
 *
 * @author darren.ouyang
 * @version 2018/9/26 15:19
 */
@Getter
public enum TransactionStatusEnum {

    /**
     * 错误的枚举值,不存在的枚举
     */
    NOT_EXIST ("NOT_EXIST", "错误的枚举值,不存在的枚举"),


    /**
     * 最大努力型事务模式-执行中
     */
    BEDT_EXECUTING ("BEDT_EXECUTING", "最大努力型事务模式-执行中"),
    /**
     * 最大努力型事务模式-执行完成
     */
    BEDT_FINISH ("BEDT_FINISH", "最大努力型事务模式-执行完成"),
    /**
     * 最大努力型事务模式-执行最终异常
     */
    BEDT_ERROR ("BEDT_ERROR", "最大努力型事务模式-执行最终异常"),


    /**
     * 事务补偿模式-初始化
     */
    CTP_INIT ("CTP_INIT", "事务补偿模式-初始化"),
    /**
     * 事务补偿模式-执行完成
     */
    CTP_FINISH ("CTP_FINISH", "事务补偿模式-执行完成"),
    /**
     * 事务补偿模式-执行异常
     */
    CTP_ERROR ("CTP_ERROR", "事务补偿模式-执行异常"),
    /**
     * 事务补偿模式-补偿中
     */
    CTP_COMPENSATING ("CTP_COMPENSATING", "事务补偿模式-补偿中"),
    /**
     * 事务补偿模式-补偿完成
     */
    CTP_COMPENSATE_FINISH ("CTP_COMPENSATE_FINISH", "事务补偿模式-补偿完成"),
    /**
     * 事务补偿模式-补偿最终异常
     */
    CTP_COMPENSATE_ERROR ("CTP_COMPENSATE_ERROR", "事务补偿模式-补偿最终异常"),


    /**
     * TCC事务模式-尝试中
     */
    TCC_TRYING ("TCC_TRYING", "TCC事务模式-尝试中"),
    /**
     * TCC事务模式-尝试异常
     */
    TCC_TRY_ERROR ("TCC_TRY_ERROR", " TCC事务模式-尝试异常"),
    /**
     * TCC事务模式-提交中
     */
    TCC_CONFIRMING ("TCC_CONFIRMING", "TCC事务模式-提交中"),
    /**
     * TCC事务模式-提交完成
     */
    TCC_CONFIRM_FINISH ("TCC_CONFIRM_FINISH", "TCC事务模式-提交完成"),
    /**
     * TCC事务模式-提交最终异常
     */
    TCC_CONFIRM_ERROR ("TCC_CONFIRM_ERROR", "TCC事务模式-提交最终异常"),
    /**
     * TCC事务模式-取消中
     */
    TCC_CANCELING ("TCC_CANCELING", "TCC事务模式-取消中"),
    /**
     * TCC事务模式-取消完成
     */
    TCC_CANCEL_FINISH ("TCC_INIT", "TCC事务模式-取消完成"),
    /**
     * TCC事务模式-取消最终异常
     */
    TCC_CANCEL_ERROR ("TCC_INIT", "TCC事务模式-取消最终异常"),

    ;

    private final String code;
    private final String comment;

    TransactionStatusEnum(String code, String comment){
        this.code = code;
        this.comment = comment;
    }


    /**
     * 检测code是否相等
     *
     * @param code code
     * @return 结果
     */
    public boolean equalsCode (String code){
        return this.code.equals(code);
    }

    /**
     * 通过code 获取枚举对象
     *
     * @param code code
     * @return 举对象
     */
    public static TransactionStatusEnum getEnum(String code){
        for (TransactionStatusEnum enumValue : TransactionStatusEnum.values()){
            if (enumValue.equalsCode(code)){
                return enumValue;
            }
        }

        return NOT_EXIST;
    }
}
