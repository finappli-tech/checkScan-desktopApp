package sn.finappli.cdcscanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

public interface AuthenticationService {

    void requestAuthentication(String telephone);

    void authenticate(String code) throws IllegalArgumentException, SecurityException, IOException, InterruptedException;
}
