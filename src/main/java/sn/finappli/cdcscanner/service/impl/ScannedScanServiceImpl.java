package sn.finappli.cdcscanner.service.impl;

import lombok.SneakyThrows;
import lombok.val;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.input.ScanInputPaged;
import sn.finappli.cdcscanner.model.input.ServerResponse;
import sn.finappli.cdcscanner.model.output.ChecksRegistrationOutput;
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
import java.util.List;

import static sn.finappli.cdcscanner.utility.HttpUtils.calculateHmacSha256;
import static sn.finappli.cdcscanner.utility.Utils.buildError;
import static sn.finappli.cdcscanner.utility.Utils.jsonToClass;

/**
 * Implementation of the {@link ScanService} interface for scanned items.
 */
public class ScannedScanServiceImpl implements ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScannedScanServiceImpl.class);
    private final HttpClientResponseHandler<ServerResponse> handleResponse = response -> {
        var result = ServerResponse.builder();

        var responseString = EntityUtils.toString(response.getEntity());
        if (List.of(200, 201, 202).contains(response.getCode()))
            return result.hasError(false).build();

        var error = buildError(response.getCode(), responseString);
        logger.error("SAVING SCANNED ITEMS ERROR");
        logger.error("\tERROR: {}", error);
        return result.hasError(true).error(responseString).build();
    };

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
            var client = HttpClient.newHttpClient();
            val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            val uri = "%s?pageNumber=%d&pageSize=%d".formatted(ConfigHolder.getContext().getScannedItemsUrl(), page, ConfigHolder.getContext().getPageItems());
            var request = HttpUtils.appendHeaderAndDigest("GET", uri, null, date)
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) return jsonToClass(ScanInputPaged.class, response.body());
            logger.error("FETCH LIST SCANNED ITEMS ERROR");
            logger.error("\tSTATUS: {}", response.statusCode());
            logger.error("\tCONTENT: {}", response.body());
            return new ScanInputPaged();
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error(e.getMessage(), e);
            return new ScanInputPaged();
        }
    }

    @Override
    public ServerResponse revertSave(List<String> values) {
        var result = ServerResponse.builder();
        try {
            var client = HttpClient.newHttpClient();
            val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            val uri = ConfigHolder.getContext().getRevertScannedItemsUrl();
            val body = Utils.listToJson(values);
            var request = HttpUtils.appendHeaderAndDigest("PUT", uri, body, date)
                    .uri(URI.create(uri))
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 204) return result.hasError(false).message(response.body()).build();

            var error = buildError(response.statusCode(), response.body());
            logger.error("REVERTING SAVED SCANNED ITEMS ERROR");
            logger.error("\tERROR: {}", error);
            return result.hasError(true).error(response.body()).build();
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error(e.getMessage(), e);
            return result.error(e.getMessage()).hasError(true).build();
        }
    }

    /**
     * Sends the scanned items to the remote server.
     *
     * @param output The {@link ChecksRegistrationOutput} object representing the scanned items to be sent.
     * @return A {@link ServerResponse} object indicating the success or failure of the operation.
     */
    @Override
    public @NotNull ServerResponse saveChecks(@NotNull ChecksRegistrationOutput output) {
        var result = ServerResponse.builder();
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            var uri = ConfigHolder.getContext().getScannedItemsUrl();
            var sec = SecurityContextHolder.getContext();
            var date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            var digest = calculateHmacSha256("POST".concat(uri).concat(date), sec.secret(), sec.encoder());

            final HttpPost post = new HttpPost(uri);
            post.setHeader("cookie", "access_token=%s".formatted(sec.token()));
            post.setHeader("X-Once", date);
            post.setHeader("X-Digest", digest);
            post.setEntity(constructEntity(output));

            return client.execute(post, handleResponse);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error(e.getMessage(), e);
            return result.error(e.getMessage()).hasError(true).build();
        }
    }

    @SneakyThrows
    @Contract(pure = true, value = "_ -> new")
    private @NotNull HttpEntity constructEntity(@NotNull ChecksRegistrationOutput output) {
        val builder = MultipartEntityBuilder
                .create()
                .setMode(HttpMultipartMode.LEGACY)
                .addTextBody("check", Utils.classToJson(output.getBody()));

        List.of(output.getFileD(), output.getFileR(), output.getFileV())
                .forEach(file -> builder.addBinaryBody("files", file, ContentType.DEFAULT_BINARY, file.getName()));
        return builder.build();
    }

}
