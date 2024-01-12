package sn.finappli.cdcscanner.controller;

import javafx.fxml.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.output.ScanRegistrationOutput;
import sn.finappli.cdcscanner.service.OCRReader;
import sn.finappli.cdcscanner.service.impl.TesseractOCRReaderImpl;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

public class ScanController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ScanController.class);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    private void send(ScanRegistrationOutput output) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var body = Utils.classToJson(output);

            log.info(body);

            var cookie = new HttpCookie("access_token", "eyJhbGciOiJIUzI1NiJ9.eyJsLWxldiI6IkhtYWNTSEEyNTYiLCJpc3MiOiJtb2hhbWVkLmthIiwiZXhwIjoxNzA0OTkwODg5LCJsb2dpbiI6Im1vaGFtZWQua2EiLCJqdGkiOiI5MGU4Y2I4ZC0yNDgyLTQ1YjgtOThkYS0yZDk0ZWYyZDE2OTgifQ.MfcevTlramzuL-zkPFm7G91FNTOH3nEvQVxl-oLnZQI");

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

            client.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage(), e);
        }
    }

    private String getCMCFromOcrReader(File file) {
        OCRReader reader = new TesseractOCRReaderImpl();
        return reader.read(file);
    }
}
