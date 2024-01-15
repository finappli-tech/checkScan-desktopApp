package sn.finappli.cdcscanner.service.impl;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.output.AuthenticationOutput;
import sn.finappli.cdcscanner.security.SecurityContext;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.AuthenticationService;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static sn.finappli.cdcscanner.utility.Utils.buildError;

public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private static final String URL = "http://localhost:8090/api/tokens/signin/scanner";

    @Override
    public void requestAuthentication(String telephone) {
    }

    @Override
    public boolean authenticate(String code, String uuid) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var object = AuthenticationOutput.builder().appId(SystemUtils.getAppIdentifier()).code(code).build();
            var body = Utils.classToJson(object);
            var request = HttpRequest.newBuilder(URI.create(STR."\{URL}?uuid=\{uuid}")) // TODO: To be updated
                    .header("Content-type", Utils.CONTENT_TYPE)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                var error = buildError(response.statusCode(), response.body());
                logger.error("AUTHENTICATION ERROR");
                logger.error("\tAppId: {}", object.appId());
                logger.error("\tError: {}", error);
                return false;
            } else {
                storeToken(response);
                return true;
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private void storeToken(@NotNull HttpResponse<String> response) {
        var token = response.headers().firstValue("authorization").orElse("");
        var secret = response.headers().firstValue("x-secret").orElse("");
        if (isBlank(token) || isBlank(secret)) throw new IllegalArgumentException("MISSING_TOKEN");

        var encoder = response.headers().firstValue("www-authenticate").orElse(SystemUtils.DEFAULT_ENCODER);
        var expiry = LocalDateTime.now().plusSeconds(SystemUtils.TOKEN_EXPIRATION);
        SecurityContextHolder.setContext(new SecurityContext(token, secret, encoder, expiry));
    }

}
