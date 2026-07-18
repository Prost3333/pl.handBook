package expensetracker.pl.trade.handbook.controller;

import expensetracker.pl.trade.handbook.dto.InstrumentRequest;
import expensetracker.pl.trade.handbook.dto.InstrumentResponse;
import expensetracker.pl.trade.handbook.service.InstrumentService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InstrumentController.class)
class InstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstrumentService service;

    private final UUID id = UUID.randomUUID();
    private final InstrumentResponse response =
            new InstrumentResponse(id, "AAPL", "Apple", "USD", "NASDAQ");

    @Test
    void getAllReturnsInstruments() throws Exception {
        when(service.findAllWithExchange()).thenReturn(List.of(response));

        mockMvc.perform(get("/instrument"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].exchangeName").value("NASDAQ"));
    }

    @Test
    void getByIdReturnsInstrument() throws Exception {
        when(service.findById(id)).thenReturn(response);

        mockMvc.perform(get("/instrument/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(service.findById(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));

        mockMvc.perform(get("/instrument/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReturns201() throws Exception {
        when(service.addInstrument(new InstrumentRequest("AAPL", "Apple", "USD", "NASDAQ")))
                .thenReturn(response);

        mockMvc.perform(post("/instrument")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"AAPL\",\"name\":\"Apple\",\"currency\":\"USD\",\"exchangeName\":\"NASDAQ\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void addReturns409OnDuplicate() throws Exception {
        when(service.addInstrument(new InstrumentRequest("AAPL", "Apple", "USD", "NASDAQ")))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Instrument already exists"));

        mockMvc.perform(post("/instrument")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"AAPL\",\"name\":\"Apple\",\"currency\":\"USD\",\"exchangeName\":\"NASDAQ\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateReturnsUpdatedInstrument() throws Exception {
        when(service.updateInstrument(eq(id), eq(new InstrumentRequest("AAPL", "Apple", "USD", "NASDAQ"))))
                .thenReturn(response);

        mockMvc.perform(put("/instrument/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"AAPL\",\"name\":\"Apple\",\"currency\":\"USD\",\"exchangeName\":\"NASDAQ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"));
    }

    @Test
    void deleteByIdReturns204() throws Exception {
        mockMvc.perform(delete("/instrument/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).deleteInstrument(id);
    }

    @Test
    void deleteByTickerAndExchangeReturns204() throws Exception {
        mockMvc.perform(delete("/instrument")
                        .param("ticker", "AAPL")
                        .param("exchangeName", "NASDAQ"))
                .andExpect(status().isNoContent());

        verify(service).deleteInstrument("AAPL", "NASDAQ");
    }

    @Test
    void addCategoryReturns204() throws Exception {
        when(service.addCategoryToInstrument(id, "Tech")).thenReturn(response);

        mockMvc.perform(post("/instrument/instruments/{id}/categories", id)
                        .param("category", "Tech"))
                .andExpect(status().isNoContent());

        verify(service).addCategoryToInstrument(id, "Tech");
    }
}
