package expensetracker.pl.trade.handbook.service;

import expensetracker.pl.trade.handbook.dto.InstrumentRequest;
import expensetracker.pl.trade.handbook.dto.InstrumentResponse;
import expensetracker.pl.trade.handbook.dto.InstrumentWithCategoryResp;
import expensetracker.pl.trade.handbook.model.Category;
import expensetracker.pl.trade.handbook.model.Exchange;
import expensetracker.pl.trade.handbook.model.Instrument;
import expensetracker.pl.trade.handbook.repository.CategoryRepository;
import expensetracker.pl.trade.handbook.repository.ExchangeRepository;
import expensetracker.pl.trade.handbook.repository.InstrumentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstrumentService {
    private final InstrumentRepository repository;
    private final ExchangeRepository exchangeRepository;
    private final CategoryRepository categoryRepository;

    public List<InstrumentWithCategoryResp>findAllWithCategory(){
        List<Instrument> all=repository.findAllWithCategories();
        return all.stream().map(this::toResponseWithCategory).toList();

    }

    public List<InstrumentResponse> findAllWithExchange() {
        return repository.findAllWithExchange()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public InstrumentResponse findById(UUID id) {
        return repository.findByIdWithExchange(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Instrument not found: " + id));
    }

    @Transactional
    public InstrumentResponse addInstrument(InstrumentRequest request) {
        Exchange exchange = checkExchange(request.exchangeName());

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

        return toResponse(instrument);
    }

    @Transactional
    public InstrumentResponse updateInstrument(UUID id, InstrumentRequest request) {
        Instrument instrument = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Instrument not found: " + id));
        Exchange exchange = checkExchange(request.exchangeName());

        Optional<Instrument> duplicate = repository.findByTickerAndExchange(request.ticker(), exchange);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrument already exists");
        }

        instrument.setTicker(request.ticker());
        instrument.setName(request.name());
        instrument.setCurrency(request.currency());
        instrument.setExchange(exchange);

        return toResponse(repository.save(instrument));
    }

    public Exchange checkExchange(String exchangeName) {
        return exchangeRepository.findByName(exchangeName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND
                        , "Exchange not found: " + exchangeName));
    }

    @Transactional
    public void deleteInstrument(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public void deleteInstrument(String ticker, String exchangeName) {
        Exchange exchange = checkExchange(exchangeName);
        Instrument instrument = repository.findByTickerAndExchange(ticker, exchange)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Instrument not found: " + ticker));
        repository.delete(instrument);
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getId(),
                instrument.getTicker(),
                instrument.getName(),
                instrument.getCurrency(),
                instrument.getExchange().getName()
        );
    }
    private InstrumentWithCategoryResp toResponseWithCategory(Instrument instrument) {
        return new InstrumentWithCategoryResp(
                instrument.getId(),
                instrument.getTicker(),
                instrument.getName(),
                instrument.getCurrency(),
                instrument.getExchange().getName(),
                instrument.getCategories().stream().map(Category::getName).sorted().toList()
        );
    }

    @Transactional
    public InstrumentResponse addCategoryToInstrument(UUID instrumentId, String categoryName) {
        Instrument instrument=repository.findById(instrumentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));

        Category category=categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        instrument.addCategory(category);

        return toResponse(instrument);
    }


}
