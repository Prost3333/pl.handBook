package expensetracker.pl.trade.handbook.service;

import expensetracker.pl.trade.handbook.dto.InstrumentRequest;
import expensetracker.pl.trade.handbook.dto.InstrumentResponse;
import expensetracker.pl.trade.handbook.model.Category;
import expensetracker.pl.trade.handbook.model.Exchange;
import expensetracker.pl.trade.handbook.model.Instrument;
import expensetracker.pl.trade.handbook.repository.CategoryRepository;
import expensetracker.pl.trade.handbook.repository.ExchangeRepository;
import expensetracker.pl.trade.handbook.repository.InstrumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceTest {

    @Mock
    private InstrumentRepository repository;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private InstrumentService service;

    private Exchange nasdaq;

    @BeforeEach
    void setUp() {
        nasdaq = Exchange.builder()
                .id(UUID.randomUUID())
                .name("NASDAQ")
                .suffix("US")
                .country("USA")
                .build();
    }

    private Instrument instrument(UUID id, String ticker, String name, String currency, String isin) {
        return Instrument.builder()
                .id(id)
                .ticker(ticker)
                .name(name)
                .currency(currency)
                .isin(isin)
                .exchange(nasdaq)
                .categories(new HashSet<>())
                .build();
    }

    @Test
    void findAllWithExchangeReturnsMappedResponses() {
        UUID id = UUID.randomUUID();
        when(repository.findAllWithExchange())
                .thenReturn(List.of(instrument(id, "AAPL", "Apple", "USD","US0378331005")));

        List<InstrumentResponse> result = service.findAllWithExchange();

        assertThat(result)
                .containsExactly(new InstrumentResponse(id, "AAPL", "Apple", "USD"
                        , "NASDAQ","US0378331005"));
    }

    @Test
    void findByIdReturnsResponse() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdWithExchange(id))
                .thenReturn(Optional.of(instrument(id, "AAPL", "Apple", "USD","US0378331005")));

        InstrumentResponse result = service.findById(id);

        assertThat(result).isEqualTo(new InstrumentResponse(id, "AAPL", "Apple",
                "USD", "NASDAQ","US0378331005"));
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdWithExchange(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
    @Test
    void updateInstrumentKeepsIsinWhenRequestHasNone() {
        UUID id = UUID.randomUUID();
        Instrument existing = instrument(id, "ENR", "Siemens Energy", "EUR", "DE000ENER6Y0");
        InstrumentRequest request =
                new InstrumentRequest("ENR", "Siemens Energy AG", "EUR", "NASDAQ", null);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(exchangeRepository.findByName("NASDAQ")).thenReturn(Optional.of(nasdaq));
        when(repository.findByTickerAndExchange("ENR", nasdaq)).thenReturn(Optional.empty());
        when(repository.save(existing)).thenReturn(existing);

        InstrumentResponse result = service.updateInstrument(id, request);

        assertThat(result.isin()).isEqualTo("DE000ENER6Y0");
    }

    @Test
    void addInstrumentSavesAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        InstrumentRequest request = new InstrumentRequest("AAPL", "Apple", "USD", "NASDAQ","US0378331005");
        when(exchangeRepository.findByName("NASDAQ")).thenReturn(Optional.of(nasdaq));
        when(repository.findByTickerAndExchange("AAPL", nasdaq)).thenReturn(Optional.empty());
        when(repository.save(any(Instrument.class)))
                .thenAnswer(inv -> {
                    Instrument i = inv.getArgument(0);
                    i.setId(id);
                    return i;
                });

        InstrumentResponse result = service.addInstrument(request);

        assertThat(result).isEqualTo(new InstrumentResponse(id, "AAPL", "Apple", "USD", "NASDAQ","US0378331005"));
    }

    @Test
    void addInstrumentThrowsNotFoundWhenExchangeMissing() {
        InstrumentRequest request = new InstrumentRequest("AAPL", "Apple", "USD", "UNKNOWN","US0378331005");
        when(exchangeRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addInstrument(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository, never()).save(any());
    }

    @Test
    void addInstrumentThrowsConflictOnDuplicateTicker() {
        InstrumentRequest request = new InstrumentRequest("AAPL", "Apple", "USD", "NASDAQ","US0378331005");
        when(exchangeRepository.findByName("NASDAQ")).thenReturn(Optional.of(nasdaq));
        when(repository.findByTickerAndExchange("AAPL", nasdaq))
                .thenReturn(Optional.of(instrument(UUID.randomUUID(), "AAPL", "Apple", "USD","US0378331005")));

        assertThatThrownBy(() -> service.addInstrument(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    void updateInstrumentUpdatesFields() {
        UUID id = UUID.randomUUID();
        Instrument existing = instrument(id, "OLD", "Old name", "EUR","US0378331005");
        InstrumentRequest request = new InstrumentRequest("AAPL", "Apple", "USD", "NASDAQ","US0378331005");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(exchangeRepository.findByName("NASDAQ")).thenReturn(Optional.of(nasdaq));
        when(repository.findByTickerAndExchange("AAPL", nasdaq)).thenReturn(Optional.empty());
        when(repository.save(existing)).thenReturn(existing);

        InstrumentResponse result = service.updateInstrument(id, request);

        assertThat(result).isEqualTo(new InstrumentResponse(id, "AAPL", "Apple", "USD", "NASDAQ","US0378331005"));
    }

    @Test
    void updateInstrumentAllowsSameTickerForSameInstrument() {
        UUID id = UUID.randomUUID();
        Instrument existing = instrument(id, "AAPL", "Apple", "USD","US0378331005");
        InstrumentRequest request = new InstrumentRequest("AAPL", "Apple Inc.", "USD", "NASDAQ","US0378331005");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(exchangeRepository.findByName("NASDAQ")).thenReturn(Optional.of(nasdaq));
        when(repository.findByTickerAndExchange("AAPL", nasdaq)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        InstrumentResponse result = service.updateInstrument(id, request);

        assertThat(result.name()).isEqualTo("Apple Inc.");
    }

    @Test
    void updateInstrumentThrowsConflictWhenTickerTakenByAnother() {
        UUID id = UUID.randomUUID();
        Instrument existing = instrument(id, "OLD", "Old name", "EUR","US0378331005");
        Instrument other = instrument(UUID.randomUUID(), "AAPL", "Apple", "USD","US0378331005");
        InstrumentRequest request = new InstrumentRequest("AAPL", "Apple", "USD", "NASDAQ","US0378331005");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(exchangeRepository.findByName("NASDAQ")).thenReturn(Optional.of(nasdaq));
        when(repository.findByTickerAndExchange("AAPL", nasdaq)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updateInstrument(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    void updateInstrumentThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateInstrument(id,
                new InstrumentRequest("AAPL", "Apple", "USD", "NASDAQ","US0378331005")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteInstrumentByIdDeletes() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        service.deleteInstrument(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteInstrumentByIdThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteInstrument(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository, never()).deleteById(any(UUID.class));
    }

    @Test
    void deleteInstrumentByTickerDeletes() {
        Instrument existing = instrument(UUID.randomUUID(), "AAPL", "Apple", "USD", "US0378331005");
        when(exchangeRepository.findByName("NASDAQ")).thenReturn(Optional.of(nasdaq));
        when(repository.findByTickerAndExchange("AAPL", nasdaq)).thenReturn(Optional.of(existing));

        service.deleteInstrument("AAPL", "NASDAQ");

        verify(repository).delete(existing);
    }

    @Test
    void deleteInstrumentByTickerThrowsNotFoundWhenMissing() {
        when(exchangeRepository.findByName("NASDAQ")).thenReturn(Optional.of(nasdaq));
        when(repository.findByTickerAndExchange("AAPL", nasdaq)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteInstrument("AAPL", "NASDAQ"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository, never()).delete(any(Instrument.class));
    }

    @Test
    void checkExchangeThrowsNotFoundWhenMissing() {
        when(exchangeRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkExchange("UNKNOWN"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addCategoryToInstrumentLinksBothSides() {
        UUID id = UUID.randomUUID();
        Instrument existing = instrument(id, "AAPL", "Apple", "USD","US0378331005");
        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("Tech")
                .instruments(new HashSet<>())
                .build();
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByName("Tech")).thenReturn(Optional.of(category));

        InstrumentResponse result = service.addCategoryToInstrument(id, "Tech");

        assertThat(result.id()).isEqualTo(id);
        assertThat(existing.getCategories()).containsExactly(category);
        assertThat(category.getInstruments()).containsExactly(existing);
    }

    @Test
    void addCategoryToInstrumentThrowsNotFoundWhenInstrumentMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addCategoryToInstrument(id, "Tech"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addCategoryToInstrumentThrowsNotFoundWhenCategoryMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(instrument(id, "AAPL", "Apple", "USD","US0378331005")));
        when(categoryRepository.findByName("Tech")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addCategoryToInstrument(id, "Tech"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
