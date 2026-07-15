package expensetracker.pl.trade.handbook.controller;

import expensetracker.pl.trade.handbook.dto.InstrumentRequest;
import expensetracker.pl.trade.handbook.dto.InstrumentResponse;
import expensetracker.pl.trade.handbook.service.InstrumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/instrument")
@RequiredArgsConstructor
@Tag(name = "Instruments", description = "CRUD operations for trading instruments")
public class InstrumentController {
    private final InstrumentService service;

    @GetMapping
    @Operation(summary = "Get all instruments with their exchanges")
    public List<InstrumentResponse> getAllWithExchange() {
        return service.findAllWithExchange();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get instrument by id")
    public InstrumentResponse getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new instrument")
    public InstrumentResponse add(@RequestBody InstrumentRequest request) {
        return service.addInstrument(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing instrument")
    public InstrumentResponse update(@PathVariable UUID id, @RequestBody InstrumentRequest request) {
        return service.updateInstrument(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete instrument by id")
    public void deleteById(@PathVariable UUID id) {
        service.deleteInstrument(id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete instrument by ticker and exchange name")
    public void deleteInstrument(@RequestParam String ticker, @RequestParam String exchangeName) {
        service.deleteInstrument(ticker, exchangeName);
    }
}
