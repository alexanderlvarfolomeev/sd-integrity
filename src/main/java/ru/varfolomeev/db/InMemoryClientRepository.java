package ru.varfolomeev.db;

import ru.varfolomeev.domain.Client;

import java.util.HashMap;
import java.util.Map;

public class InMemoryClientRepository implements ClientRepository {
    private final Map<Long, Client> db = new HashMap<>();

    private long id = 0;

    public Client createClient() {
        Client client = new Client(id);
        db.put(id, client);
        id++;
        return client;
    }

    public Client findByClientId(long clientId) {
        return db.get(clientId);
    }
}
