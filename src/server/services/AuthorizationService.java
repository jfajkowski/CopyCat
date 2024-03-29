package server.services;

import common.ClientCredentials;

public interface AuthorizationService {
    boolean login(ClientCredentials clientCredentials);
    boolean register(ClientCredentials clientCredentials);
}
