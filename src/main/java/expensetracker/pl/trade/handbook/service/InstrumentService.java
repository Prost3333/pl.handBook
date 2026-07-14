package expensetracker.pl.trade.handbook.service;

import expensetracker.pl.trade.handbook.dto.InstrumentRequest;
import expensetracker.pl.trade.handbook.dto.InstrumentResponse;
import expensetracker.pl.trade.handbook.model.Exchange;
import expensetracker.pl.trade.handbook.model.Instrument;
import expensetracker.pl.trade.handbook.repository.ExchangeRepository;
import expensetracker.pl.trade.handbook.repository.InstrumentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InstrumentService {
    private final InstrumentRepository repository;
    private final ExchangeRepository exchangeRepository;

    public List<InstrumentResponse> findAllWithExchange() {
        return repository.findAllWithExchange()
                .stream()
                .map(i -> new InstrumentResponse(
                        i.getTicker(),
                        i.getName(),
                        i.getCurrency(),
                        i.getExchange().getName()
                ))
                .toList();
    }

    @Transactional
    public InstrumentResponse addInstrument(InstrumentRequest request) {
        Exchange exchange= checkExchange(request.exchangeName());

        if (repository.findByTickerAndExchange(request.ticker(), exchange).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrument already exists");
        }

        Instrument instrument = repository.save(
                Instrument.builder()
                        .ticker(request.ticker())
                        .name(request.name())
                        .currency(request.currency())
                        .exchange(exchange)
                        .build()
        );

        return new InstrumentResponse(
                instrument.getTicker(),
                instrument.getName(),
                instrument.getCurrency(),
                exchange.getName()
        );
    }

    public Exchange checkExchange(String exchangeName) {
        return exchangeRepository.findByName(exchangeName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND
                        , "Exchange not found: " + exchangeName));
    }

    @Transactional
    public void deleteInstrument(String ticker, String exchangeName) {
        Exchange exchange= checkExchange(exchangeName);
        Instrument instrument = repository.findByTickerAndExchange(ticker, exchange)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Instrument not found: " + ticker));
        repository.delete(instrument);
    }
}
