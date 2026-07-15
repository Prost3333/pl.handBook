package expensetracker.pl.trade.handbook.controller;

import expensetracker.pl.trade.handbook.dto.ExchangeRequest;
import expensetracker.pl.trade.handbook.dto.ExchangeResponse;
import expensetracker.pl.trade.handbook.service.ExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Tag(name = "Exchanges", description = "CRUD operations for exchanges")
public class ExchangeController {
    private final ExchangeService service;

    @GetMapping
    @Operation(summary = "Get all exchanges")
    public List<ExchangeResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exchange by id")
    public ExchangeResponse getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new exchange")
    public ExchangeResponse add(@RequestBody ExchangeRequest request) {
        return service.addExchange(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing exchange")
    public ExchangeResponse update(@PathVariable UUID id, @RequestBody ExchangeRequest request) {
        return service.updateExchange(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete exchange by id")
    public void delete(@PathVariable UUID id) {
        service.deleteExchange(id);
    }
}
