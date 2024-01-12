package sn.finappli.cdcscanner.service;

import sn.finappli.cdcscanner.model.output.RegistrationCheckerOutput;
import sn.finappli.cdcscanner.model.output.RegistrationConfirmationOutput;
import sn.finappli.cdcscanner.model.output.RegistrationOutput;

public interface RegistrationService {

    boolean isRegistered() throws InterruptedException;

    boolean register(RegistrationOutput registrationOutput);

    boolean confirmRegistration(RegistrationConfirmationOutput registrationConfirmationOutput);
}
