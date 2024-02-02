package sn.finappli.cdcscanner.service.impl;

import lombok.val;
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static sn.finappli.cdcscanner.utility.Utils.CONTENT_TYPE;
import static sn.finappli.cdcscanner.utility.Utils.CONTENT_TYPE_HEADER;
import static sn.finappli.cdcscanner.utility.Utils.buildError;
import static sn.finappli.cdcscanner.utility.Utils.classToJson;
import static sn.finappli.cdcscanner.utility.Utils.hexToString;
import static sn.finappli.cdcscanner.utility.Utils.jsonToList;

/**
 * Implementation of the {@link AuthenticationService} interface for handling user authentication operations.
 */
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    /**
     * Retrieves a list of phone inputs for login requests from the remote server. The method sends a request
     * to the server to fetch login users' information and processes the server's response. If the response
     * status is successful (HTTP 200), the method returns a list of {@link LoginRequestPhoneInput} objects
     * containing user information. Otherwise, an error is logged, and an empty list is returned.
     *
     * @return A list of {@link LoginRequestPhoneInput} objects or an empty list if an error occurs.
     */
    @Override
    public List<LoginRequestPhoneInput> findUserForApp() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            var appId = SystemUtils.getAppIdentifier();
            var request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .uri(URI.create(ConfigHolder.getContext().getRequestLoginUsersUrl()))
                    .GET()
                    .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
                    .headers("appId", appId.toString())
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                var error = buildError(response.statusCode(), response.body());
                LOGGER.error("FETCHING LOGIN REQUEST USERS ERROR");
                LOGGER.error("\tAppId: {}, error: {}", appId, error);
                return Collections.emptyList();
            } else return jsonToList(LoginRequestPhoneInput.class, response.body());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Requests authentication for the given UUID from the remote server. The method constructs an
     * authentication request using the provided UUID, sends the request to the server, and processes
     * the server's response. If the response status is successful (HTTP 200), the method stores the
     * obtained authentication information and returns a {@link LoginRequestInput} object. Otherwise,
     * an error is logged, and a {@link LoginRequestInput} object with an error message is returned.
     *
     * @param uuid The UUID for which authentication is requested.
     * @return A {@link LoginRequestInput} object containing authentication information or an error message.
     */
    @Override
    public @NotNull LoginRequestInput requestAuthentication(String uuid) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            var body = classToJson(new AuthenticationRequestOutput(uuid));
            var request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
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
            } else return storeUsersLoginInformation(response.headers());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage(), e);
            return LoginRequestInput.builder().error(e.getMessage()).build();
        }
    }

    /**
     * Authenticates the user with the remote server using the provided UUID. The method sends an authentication
     * request to the server and verifies the response status. If the response status is successful (HTTP 200),
     * the method stores the authentication token and secret obtained from the response headers, and returns
     * {@code true}. Otherwise, an error is logged, and the method returns {@code false}.
     *
     * @param uuid The UUID used for authentication.
     * @return {@code true} if authentication is successful, {@code false} otherwise.
     */
    @Override
    public boolean authenticate(String uuid) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder(URI.create(ConfigHolder.getContext().getLoginUrl()))
                    .version(HttpClient.Version.HTTP_2)
                    .header("Content-type", Utils.CONTENT_TYPE)
                    .header("X-Digest", uuid)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                var error = buildError(response.statusCode(), response.body());
                LOGGER.error("AUTHENTICATION ERROR");
                val id = hexToString(uuid);
                LOGGER.error("\tAppId: {} -> Error: {}", id, error);
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

    /**
     * Extracts and stores user login information from the provided HTTP headers. The method retrieves
     * the user ID and hashed code from the headers, and constructs a {@link LoginRequestInput} object
     * with the extracted information. In case of missing or invalid data, an error message is logged,
     * and a {@link LoginRequestInput} object with an error message is returned.
     *
     * @param headers The HTTP headers containing user login information.
     * @return A {@link LoginRequestInput} object with stored login information or an error message.
     */
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

    /**
     * Stores the authentication token and secret obtained from the server response. The method retrieves
     * the authentication token and secret from the provided HTTP response headers and stores them in the
     * {@link SecurityContextHolder} for future use in the application.
     *
     * @param response The HTTP response containing authentication token and secret.
     * @throws IllegalArgumentException If the authentication token or secret is missing from the response headers.
     */
    private void storeToken(@NotNull HttpResponse<String> response) {
        var token = response.headers().firstValue("authorization").orElse("");
        var secret = response.headers().firstValue("x-secret").orElse("");
        if (isBlank(token) || isBlank(secret)) throw new IllegalArgumentException("MISSING_TOKEN");

        var encoder = response.headers().firstValue("www-authenticate").orElse(SystemUtils.DEFAULT_ENCODER);
        var expiry = LocalDateTime.now().plusSeconds(SystemUtils.TOKEN_EXPIRATION);
        SecurityContextHolder.setContext(new SecurityContext(token, secret, encoder, expiry));
    }

}
