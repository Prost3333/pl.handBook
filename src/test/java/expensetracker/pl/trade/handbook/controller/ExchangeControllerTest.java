package expensetracker.pl.trade.handbook.controller;

import expensetracker.pl.trade.handbook.dto.ExchangeRequest;
import expensetracker.pl.trade.handbook.dto.ExchangeResponse;
import expensetracker.pl.trade.handbook.service.ExchangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeController.class)
class ExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeService service;

    private final UUID id = UUID.randomUUID();
    private final ExchangeResponse response = new ExchangeResponse(id, "NASDAQ", "US", "USA");

    @Test
    void getAllReturnsExchanges() throws Exception {
        when(service.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/exchange"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("NASDAQ"))
                .andExpect(jsonPath("$[0].suffix").value("US"))
                .andExpect(jsonPath("$[0].country").value("USA"));
    }

    @Test
    void getByIdReturnsExchange() throws Exception {
        when(service.findById(id)).thenReturn(response);

        mockMvc.perform(get("/exchange/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NASDAQ"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(service.findById(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Exchange not found"));

        mockMvc.perform(get("/exchange/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReturns201() throws Exception {
        when(service.addExchange(new ExchangeRequest("NASDAQ", "US", "USA"))).thenReturn(response);

        mockMvc.perform(post("/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NASDAQ\",\"suffix\":\"US\",\"country\":\"USA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void addReturns409OnDuplicate() throws Exception {
        when(service.addExchange(new ExchangeRequest("NASDAQ", "US", "USA")))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Exchange already exists"));

        mockMvc.perform(post("/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NASDAQ\",\"suffix\":\"US\",\"country\":\"USA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateReturnsUpdatedExchange() throws Exception {
        when(service.updateExchange(eq(id), eq(new ExchangeRequest("NASDAQ", "US", "USA"))))
                .thenReturn(response);

        mockMvc.perform(put("/exchange/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NASDAQ\",\"suffix\":\"US\",\"country\":\"USA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NASDAQ"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/exchange/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).deleteExchange(id);
    }

    @Test
    void deleteReturns409WhenExchangeHasInstruments() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Exchange has instruments"))
                .when(service).deleteExchange(id);

        mockMvc.perform(delete("/exchange/{id}", id))
                .andExpect(status().isConflict());
    }
}
