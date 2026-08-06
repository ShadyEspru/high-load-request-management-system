package com.hlrms.requestservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
        return QueueBuilder
                .durable(RabbitMqConstants.REQUEST_QUEUE)
                .deadLetterExchange(
                    RabbitMqConstants.REQUEST_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                    RabbitMqConstants.REQUEST_DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Binding requestProcessingBinding(
        Queue requestProcessingQueue,
        DirectExchange requestExchange
    ) {
        return BindingBuilder
            .bind(requestProcessingQueue)
            .to(requestExchange)
            .with(
                RabbitMqConstants.REQUEST_ROUTING_KEY
            );
    }

    @Bean
    public JacksonJsonMessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}