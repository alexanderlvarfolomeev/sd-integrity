package ru.varfolomeev.db;

import ru.varfolomeev.domain.Stock;

import java.util.List;

public interface StockRepository {
    void save(Stock stock);
    Stock findByName(String companyName);
    List<Stock> findByNames(List<String> companyNames);
}
