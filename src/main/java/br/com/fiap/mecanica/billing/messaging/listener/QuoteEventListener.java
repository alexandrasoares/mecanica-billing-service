package br.com.fiap.mecanica.billing.messaging.listener;

import java.io.IOException;

import br.com.fiap.mecanica.billing.messaging.contract.EventTypes;
import br.com.fiap.mecanica.billing.messaging.contract.IntegrationEvent;
import br.com.fiap.mecanica.billing.messaging.contract.OsEventPayloads;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class QuoteEventListener {

    private static final TypeReference<IntegrationEvent<OsEventPayloads.QuoteRequested>>
            QUOTE_REQUESTED_TYPE = new TypeReference<>() {
            };
    private static final TypeReference<IntegrationEvent<OsEventPayloads.Cancelled>>
            OS_CANCELLED_TYPE = new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final QuoteSagaHandler handler;

    public QuoteEventListener(ObjectMapper objectMapper, QuoteSagaHandler handler) {
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    @RabbitListener(queues = "${billing.messaging.queues.quote-requested}")
    public void onQuoteRequested(Message message) {
        IntegrationEvent<OsEventPayloads.QuoteRequested> event =
                read(message, QUOTE_REQUESTED_TYPE);
        validateSourceAndType(event, EventTypes.OS_QUOTE_REQUESTED);
        handler.handleQuoteRequested(event);
    }

    @RabbitListener(queues = "${billing.messaging.queues.os-cancelled}")
    public void onOsCancelled(Message message) {
        IntegrationEvent<OsEventPayloads.Cancelled> event =
                read(message, OS_CANCELLED_TYPE);
        validateSourceAndType(event, EventTypes.OS_CANCELLED);
        handler.handleOsCancelled(event);
    }

    private <T> IntegrationEvent<T> read(
            Message message,
            TypeReference<IntegrationEvent<T>> type
    ) {
        try {
            return objectMapper.readValue(message.getBody(), type);
        } catch (IOException | IllegalArgumentException exception) {
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid integration event",
                    exception
            );
        }
    }

    private void validateSourceAndType(IntegrationEvent<?> event, String eventType) {
        if (!eventType.equals(event.eventType()) || !"os-service".equals(event.source())) {
            throw new AmqpRejectAndDontRequeueException(
                    "Unexpected event type or source: " + event.eventType()
            );
        }
    }
}
