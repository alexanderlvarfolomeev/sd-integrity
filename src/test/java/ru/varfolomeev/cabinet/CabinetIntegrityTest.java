package ru.varfolomeev.cabinet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import ru.varfolomeev.Exchange;
import ru.varfolomeev.db.InMemoryClientRepository;
import ru.varfolomeev.domain.Client;
import ru.varfolomeev.domain.Result;
import ru.varfolomeev.domain.Stock;

import java.util.Comparator;
import java.util.List;

public class CabinetIntegrityTest {
    private final static String COMPANY_NAME = "AMAZON";

    private Exchange.ExchangeAdmin admin;

    private Cabinet cabinet;

    @Rule
    @SuppressWarnings("rawtypes")
    public GenericContainer exchangeContainer =
            new GenericContainer(DockerImageName.parse("se:1.0-SNAPSHOT")).withExposedPorts(8080);

    @Before
    public void before() {
        cabinet = new Cabinet(new InMemoryClientRepository(), exchangeContainer.getFirstMappedPort());
        admin = new Exchange.ExchangeAdmin(exchangeContainer.getFirstMappedPort());
    }

    @Test
    public void testCreation() {
        cabinet.createClient();
        Assert.assertEquals(Result.Success, admin.addCompany(COMPANY_NAME, 100));
    }

    @Test
    public void testOneCompany() {
        Client client = cabinet.createClient();
        Assert.assertEquals(Result.Success, admin.addCompany(COMPANY_NAME, 100));
        Assert.assertEquals(Result.Success, admin.addStock(COMPANY_NAME, 100));
        cabinet.addMoney(client.getId(), 20000);
        Assert.assertEquals(Result.Success, cabinet.exchangeStock(client.getId(), COMPANY_NAME, 100));
        Assert.assertEquals(Result.Failure, cabinet.exchangeStock(client.getId(), COMPANY_NAME, 1));
        Assert.assertEquals(Result.Success, admin.changePrice(COMPANY_NAME, 200));
        Assert.assertEquals(200 * 100 + 10_000, cabinet.getTotalMoney(client.getId()), 0);
        Assert.assertEquals(1, cabinet.getStocks(client.getId()).size());
    }

    @Test
    public void testSeveralCompanies() {
        Client client = cabinet.createClient();
        int companyCount = 10;

        for (int i = 0; i < companyCount; i++) {
            Assert.assertEquals(Result.Success, admin.addCompany(COMPANY_NAME + i, 1000));
            Assert.assertEquals(Result.Success, admin.addStock(COMPANY_NAME + i, 100));
        }

        cabinet.addMoney(client.getId(), 1_000_000);

        for (int i = 0; i < companyCount; i++) {
            Assert.assertEquals(
                    Result.Success,
                    cabinet.exchangeStock(client.getId(), COMPANY_NAME + i, 100)
            );
        }

        for (int i = 0; i < companyCount; i++) {
            Assert.assertEquals(Result.Success, admin.changePrice(COMPANY_NAME + i, i * 100));
        }

        Assert.assertEquals(450_000, cabinet.getTotalMoney(client.getId()), 0);

        List<Stock> stocks = cabinet
                .getStocks(client.getId())
                .stream()
                .sorted(Comparator.comparing(Stock::getCompanyName))
                .toList();
        for (int i = 0; i < companyCount; i++) {
            Assert.assertEquals(i * 100, stocks.get(i).getPrice(), 0);
            Assert.assertEquals(100, stocks.get(i).getAmount());
        }
    }
}
