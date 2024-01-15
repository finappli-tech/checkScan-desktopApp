package sn.finappli.cdcscanner.service;

public interface AuthenticationService {

    void requestAuthentication(String telephone);

    boolean authenticate(String code, String uuid);
}
