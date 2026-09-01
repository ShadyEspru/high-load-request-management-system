package com.hlrms.requestworker.config;

import com.hlrms.requestworker.event.RequestCreatedEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

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
    public DirectExchange requestDeadLetterExchange() {
        return new DirectExchange(
            RabbitMqConstants.REQUEST_DEAD_LETTER_EXCHANGE,
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
    public Queue requestDeadLetterQueue() {
        return QueueBuilder
            .durable(
                RabbitMqConstants.REQUEST_DEAD_LETTER_QUEUE
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
            .with(RabbitMqConstants.REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding requestDeadLetterBinding(
        Queue requestDeadLetterQueue,
        DirectExchange requestDeadLetterExchange
    ) {
        return BindingBuilder
            .bind(requestDeadLetterQueue)
            .to(requestDeadLetterExchange)
            .with(
                RabbitMqConstants
                    .REQUEST_DEAD_LETTER_ROUTING_KEY
            );
    }

    @Bean
    public DefaultClassMapper rabbitClassMapper() {
        DefaultClassMapper classMapper =
            new DefaultClassMapper();

        Map<String, Class<?>> typeMappings =
            new HashMap<>();

        typeMappings.put(
            "com.hlrms.requestservice.event.RequestCreatedEvent",
            RequestCreatedEvent.class
        );

        classMapper.setIdClassMapping(typeMappings);

        return classMapper;
    }

    @Bean
    public JacksonJsonMessageConverter
    rabbitMessageConverter(
        DefaultClassMapper rabbitClassMapper
    ) {
        JacksonJsonMessageConverter converter =
            new JacksonJsonMessageConverter();

        converter.setClassMapper(rabbitClassMapper);

        return converter;
    }
}