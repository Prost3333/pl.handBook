package expensetracker.pl.trade.handbook.service;

import expensetracker.pl.trade.handbook.dto.ExchangeRequest;
import expensetracker.pl.trade.handbook.dto.ExchangeResponse;
import expensetracker.pl.trade.handbook.model.Exchange;
import expensetracker.pl.trade.handbook.repository.ExchangeRepository;
import expensetracker.pl.trade.handbook.repository.InstrumentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final ExchangeRepository repository;
    private final InstrumentRepository instrumentRepository;

    public List<ExchangeResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ExchangeResponse findById(UUID id) {
        return toResponse(getExchange(id));
    }

    @Transactional
    public ExchangeResponse addExchange(ExchangeRequest request) {
        if (repository.findByName(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exchange already exists: " + request.name());
        }
        if (repository.existsBySuffix(request.suffix())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exchange with suffix already exists: " + request.suffix());
        }

        Exchange exchange = repository.save(
                Exchange.builder()
                        .name(request.name())
                        .suffix(request.suffix())
                        .country(request.country())
                        .build()
        );

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse updateExchange(UUID id, ExchangeRequest request) {
        Exchange exchange = getExchange(id);

        if (repository.findByName(request.name())
                .filter(e -> !e.getId().equals(id))
                .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exchange already exists: " + request.name());
        }
        if (repository.existsBySuffixAndIdNot(request.suffix(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exchange with suffix already exists: " + request.suffix());
        }

        exchange.setName(request.name());
        exchange.setSuffix(request.suffix());
        exchange.setCountry(request.country());

        return toResponse(repository.save(exchange));
    }

    @Transactional
    public void deleteExchange(UUID id) {
        Exchange exchange = getExchange(id);
        if (instrumentRepository.existsByExchange(exchange)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exchange has instruments and cannot be deleted: " + exchange.getName());
        }
        repository.delete(exchange);
    }

    private Exchange getExchange(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Exchange not found: " + id));
    }

    private ExchangeResponse toResponse(Exchange exchange) {
        return new ExchangeResponse(
                exchange.getId(),
                exchange.getName(),
                exchange.getSuffix(),
                exchange.getCountry()
        );
    }
}
