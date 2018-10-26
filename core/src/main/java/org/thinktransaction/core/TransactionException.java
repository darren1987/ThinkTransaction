package org.thinktransaction.core;

/**
 * 事务异常
 *
 * @author darren.ouyang
 * @version 2018/10/23 09:49
 */
public class TransactionException extends RuntimeException {

    private static final long serialVersionUID = -1472557644967847434L;

    public TransactionException() {
        super();
    }

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    protected TransactionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
