package com.softropic.promora.email.api;


import com.softropic.promora.email.persistence.entity.EmailDeliveryStatus;
import com.softropic.promora.email.persistence.entity.EnvelopeEntity;
import com.softropic.promora.email.persistence.repository.EnvelopeEntityRepository;
import com.softropic.promora.email.service.MailService;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MailManager {

    private static final Logger      logger = LoggerFactory.getLogger(MailManager.class);

    private final MailService mailService;

    private final EnvelopeEntityRepository envelopeEntityRepository;

    private static final List<Class<? extends Exception>> NON_REPAIRABLE_ERRORS = List.of(MailParseException.class,
                                                                                          MailPreparationException.class);

    public MailManager(final MailService mailService,
                       final EnvelopeEntityRepository envelopeEntityRepository) {
        this.mailService = mailService;
        this.envelopeEntityRepository = envelopeEntityRepository;
    }

    @Async("sendMailPool")
    @EventListener
    @Transactional
    public void sendEmailFromTemplate(final Envelope envelope) {
        logger.info("sendEmailFrom template called:  Envelope {}", envelope);
        final List<Recipient> recipients = envelope.recipients();
        if(recipients == null || recipients.isEmpty()) {
            //TODO add validation to model so that the following line of code is never executed
            throw new IllegalStateException("Recipient is missing. Cannot process email send request");
        }
        EnvelopeEntity envelopeEntity;
        try {
            for (Recipient recipient:recipients) {
                //make use of sendId
                final Map<String, Object> data = new HashMap<>(envelope.data());
                data.put("sendId", envelope.sendId());
                mailService.sendEmailFromTemplate(recipient, envelope.emailTemplate(), data);
            }
            envelopeEntity = toEnvelopeEntity(envelope, null);
        } catch (Exception exception) {
            //If failure occurs it is either an authentication issue or network issue
            //This means you would not have some sent and others fail. It would be an all or none
            envelopeEntity = toEnvelopeEntity(envelope, exception);
            logger.error("Could not send email. {}", envelopeEntity, exception);
        }
        final EnvelopeEntity entityBySendId = envelopeEntityRepository.findBySendId(envelopeEntity.getSendId());
        if(entityBySendId != null) {
            entityBySendId.setAttempts(entityBySendId.getAttempts() + 1);
            entityBySendId.setStatus(envelopeEntity.getStatus());
            entityBySendId.setError(envelopeEntity.getError());
            entityBySendId.setRetry(envelopeEntity.isRetry());
        } else {
            envelopeEntityRepository.save(envelopeEntity);
        }
    }

    private EnvelopeEntity toEnvelopeEntity(final Envelope envelope, final Exception exception) {
        final EnvelopeEntity envelopeEntity = EnvelopeMapper.toEntity(envelope);
        long attempts = envelopeEntity.getAttempts();
        envelopeEntity.setAttempts(++attempts);
        if(exception != null) {
            final String stacktrace = ExceptionUtils.getStackTrace(exception);
            envelopeEntity.setError(stacktrace);
            envelopeEntity.setStatus(EmailDeliveryStatus.FAILED);
            envelopeEntity.setRetry(isRetryable(exception));
        } else {
            envelopeEntity.setRetry(false);
            envelopeEntity.setStatus(EmailDeliveryStatus.SENT);
        }
        return envelopeEntity;
    }

    private boolean isRetryable(final Exception unknownException) {
        return NON_REPAIRABLE_ERRORS.stream().noneMatch(exceptionClass -> exceptionClass.isInstance(unknownException));
    }
}
