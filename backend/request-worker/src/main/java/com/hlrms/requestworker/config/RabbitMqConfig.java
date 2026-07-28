package com.hlrms.requestworker.config;

import com.hlrms.requestworker.event.RequestCreatedEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
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
    public DefaultClassMapper rabbitClassMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();

        Map<String, Class<?>> typeMappings = new HashMap<>();

        typeMappings.put(
            "com.hlrms.requestservice.event.RequestCreatedEvent",
            RequestCreatedEvent.class
        );

        classMapper.setIdClassMapping(typeMappings);

        return classMapper;
    }

    @Bean
    public JacksonJsonMessageConverter rabbitMessageConverter(
        DefaultClassMapper rabbitClassMapper
    ) {
        JacksonJsonMessageConverter converter =
            new JacksonJsonMessageConverter();

        converter.setClassMapper(rabbitClassMapper);

        return converter;
    }
}