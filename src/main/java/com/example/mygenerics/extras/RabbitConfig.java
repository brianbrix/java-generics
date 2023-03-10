package com.example.mygenerics.extras;



import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitConfig {
    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.password}")
    private String password;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.virtual-host}")
    private String vHost;
     @Autowired
     private MyRabbitProperties myRabbitProperties;
    private static final String X_QUEUE_TYPE ="x-queue-type";
    private static final String DEAD_LTX = "x-dead-letter-exchange";
    private static final String DEAD_LTRK = "x-dead-letter-routing-key";

    @Bean("receiverQueue")
    Queue receiverQueue() {
        return QueueBuilder.durable(myRabbitProperties.getConsumerQueue())
                .withArgument(DEAD_LTX, myRabbitProperties.getDelayExchange())
                .withArgument(DEAD_LTRK, myRabbitProperties.getDelayRoutingKey())
                .build();
    }

    @Bean("producerQueue")
    Queue producerQueue() {
        return new Queue(myRabbitProperties.getProducerQueue(), true, false, false, Map.of(
                X_QUEUE_TYPE, myRabbitProperties.getQueueType()
        ));
    }

    @Bean("statusQueue")
    Queue statusQueue() {
        return new Queue(myRabbitProperties.getStatusQueue(), true, false, false, Map.of(
                X_QUEUE_TYPE, myRabbitProperties.getQueueType()
        ));
    }

    @Bean("delayQueue")
    Queue delayQueue() {
        return new Queue(myRabbitProperties.getDelayQueue(), true, false, false, Map.of(
                X_QUEUE_TYPE, myRabbitProperties.getQueueType()
        ));
    }

    @Bean("statusFailedQueue")
    Queue statusFailedQueue() {
        return new Queue(myRabbitProperties.getStatusFailedQueue(), true, false, false, Map.of(
                X_QUEUE_TYPE, myRabbitProperties.getQueueType()
        ));
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(myRabbitProperties.getExchange());
    }
    @Bean
    public DirectExchange delayExchange() {
        DirectExchange delayedExchange = new DirectExchange(myRabbitProperties.getDelayExchange());
        delayedExchange.setDelayed(true);
        return delayedExchange;
    }

    @Bean
    public Binding producerBinding() {
        return BindingBuilder.bind(producerQueue()).to(exchange()).with(myRabbitProperties.getProducerRoutingKey());
    }

    @Bean
    public Binding statusBinding() {
        return BindingBuilder.bind(statusQueue()).to(exchange()).with(myRabbitProperties.getStatusRoutingKey());
    }

    @Bean
    public Binding statusFailedBinding() {
        return BindingBuilder.bind(statusFailedQueue()).to(exchange()).with(myRabbitProperties.getStatusFailedRoutingKey());
    }

    @Bean
    public Binding delayBindingToOriginal() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(myRabbitProperties.getConsumerQueue());
    }


    @Bean("messageListenerAdapter")
    MessageListenerAdapter listenerAdapter(MessageConsumer receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }

    @Bean("messageListenerAdapter2")
    MessageListenerAdapter listenerAdapter2(MessageConsumer receiver) {
        return new MessageListenerAdapter(receiver, "delayMessage");
    }

    @Bean
//    @Primary
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
        connectionFactory.setVirtualHost(vHost);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        return connectionFactory;

    }

    @Bean
    @Primary
    public ConnectionFactory connectionFactoryClone() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        return connectionFactory;

    }
    @Bean(name = "pimAmqpAdmin")
    public AmqpAdmin pimAmqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setReturnsCallback(returnedMessage -> Logger.info("Message: {}", returnedMessage.getMessage().toString()));
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> Logger.info("ConfirmCallback correlation: "+Objects.requireNonNull(correlationData)+  "Acknowledged: "+ ack+ "Cause: "+cause));
        return rabbitTemplate;
    }
}
