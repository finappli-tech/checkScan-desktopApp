package sn.finappli.cdcscanner.service.impl;

import lombok.val;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.input.ScanInputPaged;
import sn.finappli.cdcscanner.model.input.ServerResponse;
import sn.finappli.cdcscanner.model.output.ScanImageOutput;
import sn.finappli.cdcscanner.model.output.ScanRegistrationOutput;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.ScanService;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.HttpUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static sn.finappli.cdcscanner.utility.HttpUtils.calculateHmacSha256;
import static sn.finappli.cdcscanner.utility.Utils.buildError;
import static sn.finappli.cdcscanner.utility.Utils.jsonToClass;

/**
 * Implementation of the {@link ScanService} interface for scanned items.
 */
public class ScannedScanServiceImpl implements ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScannedScanServiceImpl.class);

    /**
     * Retrieves a paged list of scanned items from the remote server.
     *
     * @param page The page number to retrieve.
     * @return A {@link ScanInputPaged} object representing the scanned items on the specified page.
     * If an error occurs during the retrieval process, an empty {@link ScanInputPaged} object is returned.
     */
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
            catchException(e);
            return new ScanInputPaged();
        }
    }

    /**
     * Sends the scan registration output to the remote server.
     *
     * @param output The {@link ScanRegistrationOutput} object representing the scan to be registered.
     * @return A {@link ServerResponse} object indicating the success or failure of the operation.
     */
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
            return catchException(e);
        }
    }

    /**
     * Sends the scanned images to the remote server.
     *
     * @param output The {@link ScanImageOutput} object representing the scanned images to be sent.
     * @return A {@link ServerResponse} object indicating the success or failure of the operation.
     */
    @Override
    public @NotNull ServerResponse sendScannedImages(@NotNull ScanImageOutput output) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            var uri = ConfigHolder.getContext().getScannedImageUrl().replace("#id", String.valueOf(output.scanId()));
            var sec = SecurityContextHolder.getContext();
            var date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            var digest = calculateHmacSha256("POST".concat(uri).concat(date), sec.secret(), sec.encoder());

            final HttpPost post = new HttpPost(uri);
            post.setHeader("cookie", "access_token=%s".formatted(sec.token()));
            post.setHeader("X-Once", date);
            post.setHeader("X-Digest", digest);


            val builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.LEGACY);
            for (var file : output.files())
                builder.addBinaryBody("files", file, ContentType.DEFAULT_BINARY, file.getName());

            post.setEntity(builder.build());

            return client.execute(post, response -> {
                var responseString = EntityUtils.toString(response.getEntity());

                if (response.getCode() <= 299)
                    return ServerResponse.builder().hasError(false).message(responseString).build();
                else {
                    var error = buildError(response.getCode(), responseString);
                    logger.error("SAVING SCAN IMAGE ERROR");
                    logger.error("\tERROR: {}", error);
                    return ServerResponse.builder().hasError(true).error(responseString).build();
                }
            });
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error(e.getMessage(), e);
            return new ServerResponse(true, e.getMessage(), null);
        }
    }

    private @NotNull <T extends Exception> ServerResponse catchException(T e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        logger.error(e.getMessage(), e);
        return ServerResponse.builder().hasError(true).error(e.getMessage()).build();
    }
}
