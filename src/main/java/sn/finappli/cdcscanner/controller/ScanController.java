package sn.finappli.cdcscanner.controller;

import javafx.fxml.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.output.ScanRegistrationOutput;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class ScanController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ScanController.class);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void send(ScanRegistrationOutput output) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var body = Utils.classToJson(output);

            log.info(body);

            var cookie = new HttpCookie("access_token", "eyJhbGciOiJIUzI1NiJ9.eyJsLWxldiI6IkhtYWNTSEEyNTYiLCJpc3MiOiJtb2hhbWVkLmthIiwiZXhwIjoxNzA0OTMwOTU5LCJsb2dpbiI6Im1vaGFtZWQua2EiLCJqdGkiOiI2ZjZhMjczNS1iYjUzLTQ5OGItYjNkMC05M2E5ZGRiNTAxZDkifQ.27eClEtGLAMssCyqxBpOVnaPsyhiPu3qbhSoyB9AsCQ");

            cookie.setPath("/");
            cookie.setDomain("localhost");
            cookie.setMaxAge(3000);
            cookie.setVersion(1);

            // Build the request
            var request = HttpRequest.newBuilder(URI.create("http://localhost:8090/api/check-scan"))
                    .version(HttpClient.Version.HTTP_2)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-type", "application/json")
                    .header("Cookie", cookie.toString())
                    .build();

            // Send the request and get the response
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            log.info(STR."Status Code: \{response.statusCode()}");
            log.info(STR."Response Body: \{response.body()}");

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage(), e);
        }
    }
}
