package com.softropic.promora.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionExceptionSimulator {

    @Transactional
    public void call(Runnable runner) {
        runner.run();
        throw new RuntimeException();
    }
}
