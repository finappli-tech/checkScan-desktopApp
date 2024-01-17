package sn.finappli.cdcscanner.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.output.RegistrationOutput;
import sn.finappli.cdcscanner.service.RegistrationService;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static sn.finappli.cdcscanner.utility.Utils.CONTENT_TYPE;

public class RegistrationServiceImpl implements RegistrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    @Override
    public boolean isRegistered() throws IOException, InterruptedException {
        var appId = SystemUtils.getAppIdentifier();

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create(ConfigHolder.getContext().getCheckAppRegistrationUrl()))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-type", CONTENT_TYPE)
                .header("appId", appId.toString())
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200)
            return true;
        var error = Utils.buildError(response.statusCode(), response.body());
        LOGGER.error("REGISTRATION VERIFICATION: {}", error);
        return false;
    }

    @Override
    public boolean register(RegistrationOutput registrationOutput) {
        try {
            var client = HttpClient.newHttpClient();
            var body = Utils.classToJson(registrationOutput);

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigHolder.getContext().getRegistrationUrl()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-type", CONTENT_TYPE)
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Utils.displaySimpleSuccessfulAlertDialog("Enregistrement", "Réussi!", "Enregistrement effectué avec succès");
                return true;
            }
            var error = Utils.buildError(response.statusCode(), response.body());
            LOGGER.error("REGISTRATION ERROR");
            LOGGER.error("\tAPP_ID: {}", registrationOutput.appId());
            LOGGER.error("\tCLIENT_ID: {}", registrationOutput.digest());
            LOGGER.error("\t{}", error);
            Utils.displaySimpleErrorAlertDialog(error.getMessage(), null);
            return false;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

}
