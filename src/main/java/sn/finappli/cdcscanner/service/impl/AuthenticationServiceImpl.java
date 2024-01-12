package sn.finappli.cdcscanner.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import sn.finappli.cdcscanner.security.SecurityContext;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.AuthenticationService;
import sn.finappli.cdcscanner.utility.SystemUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class AuthenticationServiceImpl implements AuthenticationService {

    // this is a toll url for testing purpose
    private static final String URL = "http://localhost:8090/api/tokens/signin/scanner?uuid=vm8h-4en9-dqki-46qr-gdpj-7qh1-m2lb-m8da";


    @Override
    public void requestAuthentication(String telephone) {

    }

    @Override
    public void authenticate(String code) throws IllegalArgumentException, SecurityException, IOException, InterruptedException {
        var mapper = new ObjectMapper();
        var rootNode = mapper.createObjectNode();
        var node = mapper.createObjectNode();
        node.put("appId", SystemUtils.getAppIdentifier().toString());
        node.put("code", code);
        rootNode.set("obj1", node);

        var body =  mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .version(HttpClient.Version.HTTP_2)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-type", "application/json")
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new SecurityException("AUTHENTICATION_FAILED");

            var token = response.headers().firstValue("authorization").orElse("");
            var secret = response.headers().firstValue("x-secret").orElse("");
            if (isBlank(token) || isBlank(secret)) throw new IllegalArgumentException("MISSING_TOKEN");

            var enc = response.headers().firstValue("www-authenticate").orElse(SystemUtils.DEFAULT_ENCODER);
            var expiry = LocalDateTime.now().plusSeconds(SystemUtils.TOKEN_EXPIRATION);
            SecurityContextHolder.setContext(new SecurityContext(token, secret, enc, expiry));
        }
    }

}
