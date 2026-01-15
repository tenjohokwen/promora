package com.softropic.promora.email.config;



import com.softropic.promora.email.api.MailManager;
import com.softropic.promora.email.persistence.repository.EnvelopeEntityRepository;
import com.softropic.promora.email.service.MailService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ComponentConfig {

    @Bean
    @ConditionalOnProperty(name = "enable.test.mail", havingValue = "false", matchIfMissing = true) //created this bean so that it can be replaced for some tests
    MailManager mailManager(final MailService mailService, final EnvelopeEntityRepository envelopeEntityRepo) {
        return new MailManager(mailService, envelopeEntityRepo);
    }
}
