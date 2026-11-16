package br.com.fiap.mecanica.billing.messaging.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RabbitOutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitOutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final String exchange;
    private final int maxAttempts;
    private final long confirmTimeoutSeconds;

    public RabbitOutboxPublisher(
            OutboxEventRepository repository,
            RabbitTemplate rabbitTemplate,
            Clock clock,
            @Value("${billing.messaging.exchange}") String exchange,
            @Value("${billing.messaging.outbox.max-attempts:10}") int maxAttempts,
            @Value("${billing.messaging.outbox.confirm-timeout-seconds:5}")
            long confirmTimeoutSeconds
    ) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.exchange = exchange;
        this.maxAttempts = maxAttempts;
        this.confirmTimeoutSeconds = confirmTimeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${billing.messaging.outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {
        Instant now = Instant.now(clock);
        repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                now
        ).forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        try {
            Message message = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setMessageId(event.getId())
                    .setHeader("eventType", event.getEventType())
                    .build();
            CorrelationData correlationData = new CorrelationData(event.getId());
            rabbitTemplate.send(exchange, event.getEventType(), message, correlationData);

            var confirm = correlationData.getFuture()
                    .get(confirmTimeoutSeconds, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ rejected event: " + confirm.getReason()
                );
            }

            event.markPublished(Instant.now(clock));
            repository.save(event);
        } catch (Exception exception) {
            long retryDelay = Math.min(1L << Math.min(event.getAttempts(), 8), 300L);
            event.markFailedAttempt(
                    Instant.now(clock),
                    exception.getMessage(),
                    maxAttempts,
                    retryDelay
            );
            repository.save(event);
            LOGGER.warn(
                    "Could not publish outbox event {} on attempt {}",
                    event.getId(),
                    event.getAttempts(),
                    exception
            );
        }
    }
}
