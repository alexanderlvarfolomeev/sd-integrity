package ru.varfolomeev.cabinet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import ru.varfolomeev.Utils;
import ru.varfolomeev.db.ClientRepository;
import ru.varfolomeev.domain.Client;
import ru.varfolomeev.domain.Result;
import ru.varfolomeev.domain.Stock;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
public class Cabinet {
    private final Gson gson = new Gson();
    private final ClientRepository repository;
    private final int exchangePort;

    public Client createClient() {
        return repository.createClient();
    }

    public void addMoney(long clientId, double money) {
        Client client = repository.findByClientId(clientId);
        client.setMoney(client.getMoney() + money);
    }

    public List<Stock> getStocks(long clientId) {
        var clientStocks = repository.findByClientId(clientId).getStocks();
        List<Stock> stocks = getStocks(clientStocks.keySet().stream().toList());
        return stocks
                .stream()
                .map(s -> new Stock(s.getCompanyName(), s.getPrice(), clientStocks.get(s.getCompanyName())))
                .toList();
    }

    public List<Stock> getStocks(List<String> names) {
        Type stockListType = new TypeToken<List<Stock>>() {
        }.getType();
        return gson.fromJson(
                Utils.sendExchangeRequest(
                        String.format(
                                "get?names=%s",
                                URLEncoder.encode(gson.toJson(names), StandardCharsets.UTF_8)
                        ),
                        exchangePort
                ),
                stockListType
        );
    }

    public double getTotalMoney(long clientId) {
        Client client = repository.findByClientId(clientId);
        return client.getMoney() + getStocks(clientId).stream().mapToDouble(s -> s.getAmount() * s.getPrice()).sum();
    }

    public Result exchangeStock(long clientId, String companyName, long amount) {
        Client client = repository.findByClientId(clientId);
        List<Stock> stocks = getStocks(List.of(companyName));
        if (stocks.isEmpty()) {
            return Result.Failure;
        }
        Long currentClientAmount = client.getStocks().get(companyName);
        if (client.getMoney() < amount * stocks.get(0).getPrice() ||
                currentClientAmount != null && currentClientAmount + amount < 0) {
            return Result.Failure;
        }
        Result result = gson.fromJson(
                Utils.sendExchangeRequest(
                        String.format(
                                "exchange?name=%s&amount=%d",
                                companyName,
                                -amount
                        ),
                        exchangePort
                ),
                Result.class
        );
        if (result == Result.Success) {
            client.getStocks().compute(companyName, (n, a) -> a == null ? amount : a + amount);
            client.setMoney(client.getMoney() - amount * stocks.get(0).getPrice());
        }
        return result;
    }
}
