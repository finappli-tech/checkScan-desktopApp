package sn.finappli.cdcscanner.service;

import sn.finappli.cdcscanner.model.output.RegistrationOutput;

import java.io.IOException;

public interface RegistrationService {

    boolean isRegistered() throws IOException, InterruptedException;

    boolean register(RegistrationOutput registrationOutput);

}
