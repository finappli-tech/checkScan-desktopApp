package sn.finappli.cdcscanner.utility;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.security.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public final class HttpUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    static {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    private HttpUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @Contract("_, _, _, _ -> new")
    public static HttpRequest.@Nullable Builder appendHeaderAndDigest(String method, String uri, @Nullable String body, String date) {
        try {
            var content = method.concat(StringUtils.trimToEmpty(body)).concat(uri).concat(date);

            var sec = SecurityContextHolder.getContext();
            var digest = calculateHmacSha256(content, sec.secret(), sec.encoder());
            var cookieManager = (CookieManager) CookieHandler.getDefault();

            cookieManager.getCookieStore().add(URI.create(uri), getTokenCookie());

            return HttpRequest.newBuilder()
                    .header("cookie", "access_token=%s".formatted(sec.token()))
                    .header("Content-type", Utils.CONTENT_TYPE)
                    .header("X-Once", date)
                    .header("X-Digest", digest);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    private static @NotNull HttpCookie getTokenCookie() {
        var token = SecurityContextHolder.getContext().token();
        var cookie = new HttpCookie("access_token", token);
        cookie.setVersion(0);
        return cookie;
    }

    @Contract("_, _, _ -> new")
    private static @NotNull String calculateHmacSha256(@NotNull String message, @NotNull String secretKey, String algorithm) throws NoSuchAlgorithmException, InvalidKeyException {
        var sha256Hmac = Mac.getInstance(algorithm);
        var keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm);
        sha256Hmac.init(keySpec);

        var bytes = sha256Hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        var hash = new StringBuilder();
        for (byte b : bytes) {
            var hex = Integer.toHexString(255 & b);
            if (hex.length() == 1) hash.append('0');
            hash.append(hex);
        }
        return hash.toString();
    }
}
