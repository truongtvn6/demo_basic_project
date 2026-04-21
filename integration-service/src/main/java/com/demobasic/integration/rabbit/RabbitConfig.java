package com.demobasic.integration.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${demobasic.rabbit.exchange}")
    private String exchangeName;

    @Value("${demobasic.rabbit.queue}")
    private String queueName;

    @Bean
    public Declarables rabbitTopology() {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        Queue queue = QueueBuilder.durable(queueName).build();
        Binding mockBinding = BindingBuilder.bind(queue).to(exchange).with("demo.mock.#");
        Binding jiraBinding = BindingBuilder.bind(queue).to(exchange).with("jira.issue.#");
        Binding githubBinding = BindingBuilder.bind(queue).to(exchange).with("github.commit.#");
        return new Declarables(exchange, queue, mockBinding, jiraBinding, githubBinding);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setExchange(exchangeName);
        return template;
    }
}
