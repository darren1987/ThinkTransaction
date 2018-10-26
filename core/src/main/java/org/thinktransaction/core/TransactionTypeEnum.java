package org.thinktransaction.core;

import lombok.Getter;

/**
 * 事务类型枚举
 *
 * @author darren.ouyang
 * @version 2018/10/22 14:06
 */
@Getter
public enum TransactionTypeEnum {

    /**
     * 错误的枚举值,不存在的枚举
     */
    NOT_EXIST ("NOT_EXIST", "错误的枚举值,不存在的枚举"),

    /**
     * 最大努力通知柔性事务模式
     */
    BEDT("BEDT", "最大努力通知柔性事务(Best Effort Delivery Transaction)"),
    /**
     * 事务补偿模式
     */
    CTP("CTP", "事务补偿模式(Compensating Transaction pattern)"),
    /**
     * tcc事务模式
     */
    TCC("TCC", "tcc事务(Try-Confirm-Cancel transaction)"),
    ;

    private final String code;
    private final String comment;

    TransactionTypeEnum(String code, String comment){
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
    public static TransactionTypeEnum getEnum(String code){
        for (TransactionTypeEnum enumValue : TransactionTypeEnum.values()){
            if (enumValue.equalsCode(code)){
                return enumValue;
            }
        }

        return NOT_EXIST;
    }
}
