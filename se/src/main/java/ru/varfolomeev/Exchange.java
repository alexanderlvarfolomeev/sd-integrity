package ru.varfolomeev;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.RequiredArgsConstructor;
import ru.varfolomeev.db.InMemoryStockRepository;
import ru.varfolomeev.db.StockRepository;
import ru.varfolomeev.domain.Result;
import ru.varfolomeev.domain.Stock;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Exchange {
    private final StockRepository repository;

    private Result addCompany(String companyName, double price) {
        if (repository.findByName(companyName) == null) {
            repository.save(new Stock(companyName, price, 0));
            return Result.Success;
        } else {
            return Result.Failure;
        }
    }

    private List<Stock> getStocks(List<String> companyNames) {
        return repository.findByNames(companyNames);
    }

    private Result exchangeStock(String companyName, long amount) {
        Stock old = repository.findByName(companyName);
        if (old.getAmount() + amount < 0) {
            return Result.Failure;
        } else {
            repository.save(new Stock(old.getCompanyName(), old.getPrice(), old.getAmount() + amount));
            return Result.Success;
        }
    }

    private Result changePrice(String companyName, double price) {
        Stock old = repository.findByName(companyName);
        repository.save(new Stock(old.getCompanyName(), price, old.getAmount()));
        return Result.Success;
    }

    public void run() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new ExchangeHandler());
        server.setExecutor(null);
        server.start();
    }

    public static void main(String... args) throws IOException {
        new Exchange(new InMemoryStockRepository()).run();
    }

    private class ExchangeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String uriPath = uri.getPath();
            Map<String, String> parameters = getParameters(uri.getQuery());
            OutputStream os = exchange.getResponseBody();
            Gson gson = new Gson();
            String response = switch (uriPath) {
                case "/add" -> gson.toJson(addCompany(
                        parameters.get("name"),
                        Double.parseDouble(parameters.get("price"))
                ));
                case "/get" -> gson.toJson(getStocks(
                        gson.fromJson(parameters.get("names"),
                        new TypeToken<List<String>>() {}.getType())
                ));
                case "/exchange" -> gson.toJson(exchangeStock(
                        parameters.get("name"),
                        Long.parseLong(parameters.get("amount"))
                ));
                case "/change" -> gson.toJson(changePrice(
                        parameters.get("name"),
                        Double.parseDouble(parameters.get("price"))
                ));
                default -> "Not found";
            };
            exchange.sendResponseHeaders(200, response.length());
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        private static Map<String, String> getParameters(String query) {
            String decodedQuery = URLDecoder.decode(query, StandardCharsets.UTF_8);
            return Arrays.stream(decodedQuery.split("&"))
                    .filter(Predicate.not(String::isEmpty))
                    .collect(Collectors.toMap(p -> p.substring(0, p.indexOf("=")), p -> p.substring(p.indexOf("=") + 1)));
        }
    }

    @RequiredArgsConstructor
    public static class ExchangeAdmin {
        private final Gson gson = new Gson();
        private final int exchangePost;

        public Result addCompany(String companyName, double price) {
            return getQueryResult(String.format("add?name=%s&price=%s", companyName, price));
        }

        public Result addStock(String companyName, long amount) {
            if (amount < 0) {
                return Result.Failure;
            } else {
                return getQueryResult(String.format("exchange?name=%s&amount=%d", companyName, amount));
            }
        }

        public Result changePrice(String companyName, double price) {
            return getQueryResult(String.format("change?name=%s&price=%s", companyName, price));
        }

        private Result getQueryResult(String query) {
            return gson.fromJson(Utils.sendExchangeRequest(query, exchangePost), Result.class);
        }
    }
}
