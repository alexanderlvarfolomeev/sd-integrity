package ru.varfolomeev.db;

import ru.varfolomeev.domain.Stock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryStockRepository implements StockRepository {
    private final Map<String, Stock> db = new HashMap<>();


    public void save(Stock stock) {
        db.put(stock.getCompanyName(), stock);
    }

    public Stock findByName(String companyName) {
        return db.get(companyName);
    }

    public List<Stock> findByNames(List<String> companyNames) {
        return db.values().stream().filter(s -> companyNames.contains(s.getCompanyName())).toList();
    }
}
