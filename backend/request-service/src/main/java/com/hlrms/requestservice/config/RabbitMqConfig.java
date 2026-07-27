package com.hlrms.requestservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange requestExchange() {
        return new DirectExchange(
            RabbitMqConstants.REQUEST_EXCHANGE,
            true,
            false
        );
    }

    @Bean
    public Queue requestProcessingQueue() {
        return new Queue(
            RabbitMqConstants.REQUEST_QUEUE,
            true,
            false,
            false
        );
    }

    @Bean
    public Binding requestProcessingBinding(
        Queue requestProcessingQueue,
        DirectExchange requestExchange
    ) {
        return BindingBuilder
            .bind(requestProcessingQueue)
            .to(requestExchange)
            .with(RabbitMqConstants.REQUEST_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}