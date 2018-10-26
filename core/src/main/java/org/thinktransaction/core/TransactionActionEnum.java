package org.thinktransaction.core;

import lombok.Getter;

/**
 * 事务动作类型
 *
 * @author darren.ouyang
 * @version 2018/10/26 10:51
 */
@Getter
public enum TransactionActionEnum {

    /**
     * 错误的枚举值,不存在的枚举
     */
    NOT_EXIST ("NOT_EXIST", "错误的枚举值,不存在的枚举"),

    /**
     * bedt/ctp-执行业务动作
     */
    EXECUTE("EXECUTE", "bedt/ctp-执行业务动作"),
    /**
     * ctp-补偿动作
     */
    COMPENSATE("COMPENSATE", "ctp-补偿动作"),
    /**
     * tcc-尝试动作
     */
    TRY("TRY", "tcc-尝试动作"),
    /**
     * tcc-提交动作
     */
    CONFIRM("CONFIRM", "tcc-提交动作"),
    /**
     * tcc-取消动作
     */
    CANCEL("CANCEL", "tcc-取消动作"),

    ;

    private final String code;
    private final String comment;

    TransactionActionEnum(String code, String comment){
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
    public static TransactionActionEnum getEnum(String code){
        for (TransactionActionEnum enumValue : TransactionActionEnum.values()){
            if (enumValue.equalsCode(code)){
                return enumValue;
            }
        }

        return NOT_EXIST;
    }
}
