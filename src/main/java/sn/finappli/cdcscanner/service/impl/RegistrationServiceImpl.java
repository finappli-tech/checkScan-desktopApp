package sn.finappli.cdcscanner.service.impl;

import sn.finappli.cdcscanner.model.output.RegistrationConfirmationOutput;
import sn.finappli.cdcscanner.model.output.RegistrationOutput;
import sn.finappli.cdcscanner.service.RegistrationService;
import sn.finappli.cdcscanner.utility.SystemUtils;

import java.net.http.HttpClient;

public class RegistrationServiceImpl implements RegistrationService {
    @Override
    public boolean isRegistered() {
        var appId = SystemUtils.getAppIdentifier();

        try (var client = HttpClient.newHttpClient()) {
            Thread.sleep(2000);

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public boolean register(RegistrationOutput registrationOutput) {
        return false;
    }

    @Override
    public boolean confirmRegistration(RegistrationConfirmationOutput registrationConfirmationOutput) {
        return false;
    }
}
