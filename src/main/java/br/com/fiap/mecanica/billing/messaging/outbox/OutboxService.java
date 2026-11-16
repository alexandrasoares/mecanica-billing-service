package br.com.fiap.mecanica.billing.messaging.outbox;

import java.time.Clock;
import java.time.Instant;

import br.com.fiap.mecanica.billing.messaging.contract.IntegrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void enqueue(IntegrationEvent<?> event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(OutboxEvent.pending(
                    event.eventId().toString(),
                    event.eventType(),
                    payload,
                    Instant.now(clock)
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event", exception);
        }
    }
}
