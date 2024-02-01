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

/**
 * Utility class for HTTP-related operations, including header manipulation and HMAC-SHA256 calculation.
 * This class provides methods for appending headers and digest to an HTTP request, handling cookies, and
 * calculating HMAC-SHA256 signatures for message integrity.
 *
 * <p>The class is designed as a utility and cannot be instantiated.</p>
 *
 * @implNote This class uses a {@link CookieManager} with {@link CookiePolicy#ACCEPT_ALL} as the default
 * {@link CookieHandler} to handle cookies in HTTP requests.
 * @see Logger
 * @see LoggerFactory
 * @see CookieHandler
 * @see CookieManager
 * @see CookiePolicy
 * @see HttpCookie
 * @see HttpRequest
 * @see StringUtils
 * @see SecurityContextHolder
 * @see Mac
 * @see SecretKeySpec
 * @see NoSuchAlgorithmException
 * @see InvalidKeyException
 */
public final class HttpUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    static {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    private HttpUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Appends headers and digest to an HTTP request based on the provided method, URI, body, and date.
     *
     * @param method The HTTP method (e.g., GET, POST).
     * @param uri    The URI of the HTTP request.
     * @param body   The body content of the HTTP request (can be null).
     * @param date   The date to be included in the request headers.
     * @return A {@link HttpRequest.Builder} with the appended headers, or {@code null} if an exception occurs.
     * @throws NullPointerException If {@code method}, {@code uri}, or {@code date} is {@code null}.
     */
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

    /**
     * Retrieves a pre-configured {@link HttpCookie} representing the access token.
     *
     * @return The access token {@link HttpCookie}.
     */
    private static @NotNull HttpCookie getTokenCookie() {
        var token = SecurityContextHolder.getContext().token();
        var cookie = new HttpCookie("access_token", token);
        cookie.setVersion(0);
        return cookie;
    }

    /**
     * Calculates the HMAC-SHA256 signature for a given message using a secret key and algorithm.
     *
     * @param message   The message for which the signature is calculated.
     * @param secretKey The secret key for HMAC-SHA256.
     * @param algorithm The HMAC algorithm (e.g., "HmacSHA256").
     * @return The calculated HMAC-SHA256 signature as a hexadecimal string.
     * @throws NoSuchAlgorithmException If the specified algorithm is not available.
     * @throws InvalidKeyException      If the provided secret key is invalid.
     * @throws NullPointerException     If {@code message}, {@code secretKey}, or {@code algorithm} is {@code null}.
     */
    @Contract("_, _, _ -> new")
    public static @NotNull String calculateHmacSha256(@NotNull String message, @NotNull String secretKey, String algorithm) throws NoSuchAlgorithmException, InvalidKeyException {
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
