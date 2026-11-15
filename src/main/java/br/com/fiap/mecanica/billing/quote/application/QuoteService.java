package br.com.fiap.mecanica.billing.quote.application;

import java.time.Clock;
import java.time.Instant;

import br.com.fiap.mecanica.billing.quote.domain.Quote;
import br.com.fiap.mecanica.billing.quote.domain.QuoteItem;
import br.com.fiap.mecanica.billing.quote.infrastructure.QuoteRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class QuoteService {

    private final QuoteRepository repository;
    private final Clock clock;

    public QuoteService(QuoteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Quote create(CreateQuoteCommand command) {
        if (repository.existsByOsId(command.osId())) {
            throw new QuoteAlreadyExistsException(command.osId());
        }

        var items = command.items().stream()
                .map(item -> new QuoteItem(
                        item.description(),
                        item.type(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        Quote quote = Quote.create(
                command.osId(),
                command.customerId(),
                items,
                command.currency(),
                command.validUntil(),
                Instant.now(clock)
        );

        try {
            return repository.save(quote);
        } catch (DuplicateKeyException exception) {
            throw new QuoteAlreadyExistsException(command.osId());
        }
    }

    public Quote findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new QuoteNotFoundException(id));
    }

    public Quote findByOsId(String osId) {
        return repository.findByOsId(osId)
                .orElseThrow(() -> new QuoteNotFoundException("OS " + osId));
    }

    public Quote submit(String id) {
        Quote quote = findById(id);
        quote.submit(Instant.now(clock));
        return repository.save(quote);
    }

    public Quote approve(String id) {
        Quote quote = findById(id);
        quote.approve(Instant.now(clock));
        return repository.save(quote);
    }

    public Quote reject(String id, String reason) {
        Quote quote = findById(id);
        quote.reject(reason, Instant.now(clock));
        return repository.save(quote);
    }
}
