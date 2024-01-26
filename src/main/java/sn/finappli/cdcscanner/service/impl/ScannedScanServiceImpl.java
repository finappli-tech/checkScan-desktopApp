package sn.finappli.cdcscanner.service.impl;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.input.ScanInputPaged;
import sn.finappli.cdcscanner.model.input.ServerResponse;
import sn.finappli.cdcscanner.model.output.ScanRegistrationOutput;
import sn.finappli.cdcscanner.service.ScanService;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.HttpUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static sn.finappli.cdcscanner.utility.Utils.buildError;
import static sn.finappli.cdcscanner.utility.Utils.jsonToClass;

public class ScannedScanServiceImpl implements ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScannedScanServiceImpl.class);

    @Override
    public ScanInputPaged listScannedItems(int page) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            val uri = "%s?pageNumber=%d&pageSize=%d".formatted(ConfigHolder.getContext().getScannedItemsUrl(), page, ConfigHolder.getContext().getPageItems());
            var request = HttpUtils.appendHeaderAndDigest("GET", uri, null, date)
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                logger.error("FETCH LIST SCANNED ITEMS ERROR");
                logger.error("\tSTATUS: {}", response.statusCode());
                logger.error("\tCONTENT: {}", response.body());
                return new ScanInputPaged();
            } else return jsonToClass(ScanInputPaged.class, response.body());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error(e.getMessage(), e);
            return new ScanInputPaged();
        }
    }

    @Override
    public ServerResponse sendScan(ScanRegistrationOutput output) {
        var result = ServerResponse.builder();
        try {
            var client = HttpClient.newHttpClient();
            val content = Utils.classToJson(output);
            val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            val uri = ConfigHolder.getContext().getScannedItemsUrl();

            // Build the request
            var request = HttpUtils.appendHeaderAndDigest("POST", uri, content, date)
                    .uri(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofString(content))
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            result.hasError(response.statusCode() > 299);
            if (response.statusCode() != 200) {
                var error = buildError(response.statusCode(), response.body());
                logger.error("SAVING SCAN ERROR");
                logger.error("\tERROR: {}", error);
                return result.error(error.getMessage()).build();
            } else return result.build();
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error(e.getMessage(), e);
            return ServerResponse.builder().hasError(true).error(e.getMessage()).build();
        }
    }
}
