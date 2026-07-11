package expensetracker.pl.trade.handbook.service;

import expensetracker.pl.trade.handbook.dto.InstrumentResponse;
import expensetracker.pl.trade.handbook.model.Instrument;
import expensetracker.pl.trade.handbook.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstrumentService {
    private  final InstrumentRepository repository;

    public List<InstrumentResponse> findAllWithExchange(){
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
}
