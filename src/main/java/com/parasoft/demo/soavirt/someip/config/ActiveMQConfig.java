package com.parasoft.demo.soavirt.someip.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
@EnableJms
@Slf4j
public class ActiveMQConfig {
    private BrokerService brokerService;
    public static final String SOMEIP_MESSAGE_QUEUE = "someip_message_queue";

    @Value("${spring.activemq.broker-url}")
    private String embeddedBrokerUrl;

    @Value("${spring.activemq.embedded-broker-name}")
    private String embeddedBrokerName;

    @PostConstruct
    public void startEmbeddedBroker() throws Exception {
        brokerService = new BrokerService();
        brokerService.setBrokerName(embeddedBrokerName);
        brokerService.setPersistent(false);
        brokerService.addConnector(embeddedBrokerUrl);
        brokerService.start();
    }

    @PreDestroy
    public void stopEmbeddedBroker() throws Exception {
        if (brokerService != null) {
            brokerService.stop();
        }
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(embeddedBrokerUrl);
        return factory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(DefaultJmsListenerContainerFactoryConfigurer configurer,
                                                                          ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setErrorHandler((Throwable throwable) -> log.error("JMS error", throwable));
        factory.setPubSubDomain(false);
        return factory;
    }
}