package net.allape.exception;

import java.io.IOException;

/**
 * 传输取消后抛出的异常
 */
public class TransferCancelledException extends IOException {

    public TransferCancelledException(String message) {
        super(message);
    }
}
