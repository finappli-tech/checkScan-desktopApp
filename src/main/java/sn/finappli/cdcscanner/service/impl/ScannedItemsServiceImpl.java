package sn.finappli.cdcscanner.service.impl;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.input.ScanInputPaged;
import sn.finappli.cdcscanner.service.ItemsService;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.HttpUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static sn.finappli.cdcscanner.utility.Utils.jsonToClass;

public class ScannedItemsServiceImpl implements ItemsService {

    private static final Logger logger = LoggerFactory.getLogger(ScannedItemsServiceImpl.class);
    private static final String BASE_URL = STR."\{ConfigHolder.getContext().getBaseUrl()}/check-scan";

    @Override
    public ScanInputPaged listScannedItems(int page) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            val uri = STR."\{BASE_URL}?pageNumber=\{page}&pageSize=\{ConfigHolder.getContext().getPageItems()}";
            val body = STR."GET\{uri}\{date}";
            var request = HttpUtils.appendHeaderAndDigest(uri, body, date)
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                logger.error("FETCH LIST SCANNED ITEMS ERROR");
                logger.error("\tSTATUS: {}", response.statusCode());
                logger.error("\tCONTENT: {}", response.body());
                return new ScanInputPaged();
            } else {
                return jsonToClass(ScanInputPaged.class, response.body());
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error(e.getMessage(), e);
            return new ScanInputPaged();
        }
    }
}
