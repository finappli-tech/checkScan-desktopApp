package sn.finappli.cdcscanner.service.impl;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.input.LoginRequestInput;
import sn.finappli.cdcscanner.model.input.LoginRequestPhoneInput;
import sn.finappli.cdcscanner.model.output.AuthenticationRequestOutput;
import sn.finappli.cdcscanner.security.SecurityContext;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.AuthenticationService;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static sn.finappli.cdcscanner.utility.Utils.CONTENT_TYPE;
import static sn.finappli.cdcscanner.utility.Utils.CONTENT_TYPE_HEADER;
import static sn.finappli.cdcscanner.utility.Utils.buildError;
import static sn.finappli.cdcscanner.utility.Utils.classToJson;
import static sn.finappli.cdcscanner.utility.Utils.hexToString;
import static sn.finappli.cdcscanner.utility.Utils.jsonToList;

public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    @Override
    public List<LoginRequestPhoneInput> findUserForApp() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            var appId = SystemUtils.getAppIdentifier();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigHolder.getContext().getRequestLoginUsersUrl()))
                    .GET()
                    .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
                    .headers("appId", appId.toString())
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                var error = buildError(response.statusCode(), response.body());
                LOGGER.error("FETCHING LOGIN REQUEST USERS ERROR");
                LOGGER.error("\tAppId: {}", appId);
                LOGGER.error("\tError: {}", error);
                return null;
            } else return jsonToList(LoginRequestPhoneInput.class, response.body());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public @NotNull LoginRequestInput requestAuthentication(String uuid) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            var body = classToJson(new AuthenticationRequestOutput(uuid));
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigHolder.getContext().getRequestLoginUrl()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-type", CONTENT_TYPE)
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                var error = buildError(response.statusCode(), response.body());
                LOGGER.error("AUTHENTICATION ERROR");
                LOGGER.error("\tError: {}", error);
                return LoginRequestInput.builder().error(error.getMessage()).build();
            } else {
                return storeUsersLoginInformation(response.headers());
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage(), e);
            return LoginRequestInput.builder().error(e.getMessage()).build();
        }
    }

    @Override
    public boolean authenticate(String uuid) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder(URI.create(ConfigHolder.getContext().getLoginUrl()))
                    .header("Content-type", Utils.CONTENT_TYPE)
                    .header("X-Digest", uuid)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                var error = buildError(response.statusCode(), response.body());
                LOGGER.error("AUTHENTICATION ERROR");
                LOGGER.error("\tAppId: {}", hexToString(uuid));
                LOGGER.error("\tError: {}", error);
                return false;
            } else {
                storeToken(response);
                return true;
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    private @NotNull LoginRequestInput storeUsersLoginInformation(@NotNull HttpHeaders headers) {
        var userId = headers.firstValue("Proxy-Authenticate").orElse(null);
        var hashedCode = headers.firstValue("Proxy-Authorization").orElse(null);
        try {
            return LoginRequestInput.builder().hashedCode(hashedCode).uuid(userId).expiry(LocalDateTime.now()).build();
        } catch (NullPointerException | IllegalArgumentException e) {
            LOGGER.error("AUTHENTICATION REQUEST");
            LOGGER.error("\tERROR catching login credentials from headers proxy with values");
            LOGGER.error("\tUSER_ID: %s".formatted(userId));
            LOGGER.error("\tHASH: %s".formatted(hashedCode));
            return LoginRequestInput.builder().error(e.getMessage()).build();
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
