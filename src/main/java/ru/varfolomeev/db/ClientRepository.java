package ru.varfolomeev.db;

import ru.varfolomeev.domain.Client;

public interface ClientRepository {
    Client createClient();
    Client findByClientId(long clientId);
}
