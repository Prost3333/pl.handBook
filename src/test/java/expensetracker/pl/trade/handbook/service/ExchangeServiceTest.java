package expensetracker.pl.trade.handbook.service;

import expensetracker.pl.trade.handbook.dto.ExchangeRequest;
import expensetracker.pl.trade.handbook.dto.ExchangeResponse;
import expensetracker.pl.trade.handbook.model.Exchange;
import expensetracker.pl.trade.handbook.repository.ExchangeRepository;
import expensetracker.pl.trade.handbook.repository.InstrumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
class ExchangeServiceTest {

    @Mock
    private ExchangeRepository repository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private ExchangeService service;

    private Exchange exchange(UUID id, String name, String suffix, String country) {
        return Exchange.builder().id(id).name(name).suffix(suffix).country(country).build();
    }

    @Test
    void findAllReturnsMappedResponses() {
        UUID id = UUID.randomUUID();
        when(repository.findAll()).thenReturn(List.of(exchange(id, "NASDAQ", "US", "USA")));

        List<ExchangeResponse> result = service.findAll();

        assertThat(result).containsExactly(new ExchangeResponse(id, "NASDAQ", "US", "USA"));
    }

    @Test
    void findByIdReturnsResponse() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(exchange(id, "NASDAQ", "US", "USA")));

        ExchangeResponse result = service.findById(id);

        assertThat(result).isEqualTo(new ExchangeResponse(id, "NASDAQ", "US", "USA"));
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addExchangeSavesAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        ExchangeRequest request = new ExchangeRequest("NASDAQ", "US", "USA");
        when(repository.findByName("NASDAQ")).thenReturn(Optional.empty());
        when(repository.existsBySuffix("US")).thenReturn(false);
        when(repository.save(any(Exchange.class)))
                .thenAnswer(inv -> {
                    Exchange e = inv.getArgument(0);
                    e.setId(id);
                    return e;
                });

        ExchangeResponse result = service.addExchange(request);

        assertThat(result).isEqualTo(new ExchangeResponse(id, "NASDAQ", "US", "USA"));
    }

    @Test
    void addExchangeThrowsConflictOnDuplicateName() {
        ExchangeRequest request = new ExchangeRequest("NASDAQ", "US", "USA");
        when(repository.findByName("NASDAQ"))
                .thenReturn(Optional.of(exchange(UUID.randomUUID(), "NASDAQ", "US", "USA")));

        assertThatThrownBy(() -> service.addExchange(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    void addExchangeThrowsConflictOnDuplicateSuffix() {
        ExchangeRequest request = new ExchangeRequest("NASDAQ", "US", "USA");
        when(repository.findByName("NASDAQ")).thenReturn(Optional.empty());
        when(repository.existsBySuffix("US")).thenReturn(true);

        assertThatThrownBy(() -> service.addExchange(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    void updateExchangeUpdatesFields() {
        UUID id = UUID.randomUUID();
        Exchange existing = exchange(id, "OLD", "OL", "Poland");
        ExchangeRequest request = new ExchangeRequest("NASDAQ", "US", "USA");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByName("NASDAQ")).thenReturn(Optional.empty());
        when(repository.existsBySuffixAndIdNot("US", id)).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);

        ExchangeResponse result = service.updateExchange(id, request);

        assertThat(result).isEqualTo(new ExchangeResponse(id, "NASDAQ", "US", "USA"));
    }

    @Test
    void updateExchangeAllowsSameNameForSameExchange() {
        UUID id = UUID.randomUUID();
        Exchange existing = exchange(id, "NASDAQ", "US", "USA");
        ExchangeRequest request = new ExchangeRequest("NASDAQ", "US", "Poland");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByName("NASDAQ")).thenReturn(Optional.of(existing));
        when(repository.existsBySuffixAndIdNot("US", id)).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);

        ExchangeResponse result = service.updateExchange(id, request);

        assertThat(result.country()).isEqualTo("Poland");
    }

    @Test
    void updateExchangeThrowsConflictWhenNameTakenByAnother() {
        UUID id = UUID.randomUUID();
        Exchange existing = exchange(id, "OLD", "OL", "Poland");
        Exchange other = exchange(UUID.randomUUID(), "NASDAQ", "US", "USA");
        ExchangeRequest request = new ExchangeRequest("NASDAQ", "XX", "USA");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByName("NASDAQ")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updateExchange(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    void updateExchangeThrowsConflictWhenSuffixTakenByAnother() {
        UUID id = UUID.randomUUID();
        Exchange existing = exchange(id, "OLD", "OL", "Poland");
        ExchangeRequest request = new ExchangeRequest("NASDAQ", "US", "USA");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByName("NASDAQ")).thenReturn(Optional.empty());
        when(repository.existsBySuffixAndIdNot("US", id)).thenReturn(true);

        assertThatThrownBy(() -> service.updateExchange(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    void updateExchangeThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateExchange(id, new ExchangeRequest("N", "S", "C")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteExchangeDeletesWhenNoInstruments() {
        UUID id = UUID.randomUUID();
        Exchange existing = exchange(id, "NASDAQ", "US", "USA");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(instrumentRepository.existsByExchange(existing)).thenReturn(false);

        service.deleteExchange(id);

        verify(repository).delete(existing);
    }

    @Test
    void deleteExchangeThrowsConflictWhenInstrumentsExist() {
        UUID id = UUID.randomUUID();
        Exchange existing = exchange(id, "NASDAQ", "US", "USA");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(instrumentRepository.existsByExchange(existing)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteExchange(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).delete(any(Exchange.class));
    }
}
