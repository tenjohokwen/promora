package com.softropic.promora.utils;



import com.softropic.promora.email.api.Envelope;
import com.softropic.promora.email.api.MailManager;

import org.springframework.context.event.EventListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TestMailManager extends MailManager {
    private final Map<String, Envelope> sentMails = new ConcurrentHashMap<>();

    public TestMailManager() {
        super(null, null);
    }

    @EventListener
    public void sendEmailFromTemplate(final Envelope envelope) {
        sentMails.put(envelope.sendId(), envelope);
    }

    public Envelope getEnvelope(String referenceId) {
        return sentMails.get(referenceId);
    }
}
