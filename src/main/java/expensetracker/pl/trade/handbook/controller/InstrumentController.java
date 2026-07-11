package expensetracker.pl.trade.handbook.controller;

import expensetracker.pl.trade.handbook.dto.InstrumentResponse;
import expensetracker.pl.trade.handbook.service.InstrumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/instrument")
@RequiredArgsConstructor
public class InstrumentController {
    private  final InstrumentService service;

    @GetMapping
    public List<InstrumentResponse> getAllWithExchange(){
        return service.findAllWithExchange();
    }
}
