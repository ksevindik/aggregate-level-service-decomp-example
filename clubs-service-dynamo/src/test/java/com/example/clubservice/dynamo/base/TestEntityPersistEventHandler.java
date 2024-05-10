package com.example.clubservice.dynamo.base;

import com.example.clubservice.dynamo.migration.EntityPersistedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@TestComponent
public class TestEntityPersistEventHandler {
    private CountDownLatch latchForEntityPersistedEvents = new CountDownLatch(1);

    @Autowired
    protected ObjectMapper objectMapper;

    public void reset() {
        latchForEntityPersistedEvents = new CountDownLatch(1);
    }

    @TransactionalEventListener
    public void handle(EntityPersistedEvent event) {
        latchForEntityPersistedEvents.countDown();
    }

    public void waitForEntityPersistedEvent() {
        try {
            latchForEntityPersistedEvents.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
