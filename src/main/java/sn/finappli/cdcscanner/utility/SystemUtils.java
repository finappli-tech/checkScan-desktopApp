package sn.finappli.cdcscanner.utility;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import sn.finappli.cdcscanner.model.input.YamlConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public final class SystemUtils {

    public static final String DEFAULT_ENCODER = "HmacSHA256";
    public static final int TOKEN_EXPIRATION = 6 * 60 * 60; // seconds
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemUtils.class);
    private static final String IP_URL = "https://httpbin.org/ip";

    private SystemUtils() {
    }

    public static void loadAppConfig() {
        try (InputStream is = SystemUtils.class.getResourceAsStream("/config.yaml")) {
            if (is == null) {
                LOGGER.error("Config file not found");
                System.exit(10);
            }
            var constructor = new Constructor(YamlConfig.class, new LoaderOptions());
            var yaml = new Yaml( constructor );
            var config = yaml.loadAs(is, YamlConfig.class);
            ConfigHolder.setContext(config);
        } catch (Exception e) {
            LOGGER.error(STR."Error while loading config file: \{e.getMessage()}", e);
            System.exit(11);
        }
    }

    public static UUID getAppIdentifier() {
        try {
            var macAddressBytes = fetchMachineAddress();
            return UUID.nameUUIDFromBytes(macAddressBytes);
        } catch (Exception _) {
            throw new IllegalArgumentException("CANNOT RETRIEVE APPID");
        }
    }

    public static @Nullable String getMacAddress() {
        try {
            var macAddressBytes = fetchMachineAddress();
            var macAddressStringBuilder = new StringBuilder();

            for (int i = 0; i < macAddressBytes.length; i++) {
                macAddressStringBuilder.append("%02X%s".formatted(macAddressBytes[i], (i < macAddressBytes.length - 1) ? "-" : ""));
            }
            return macAddressStringBuilder.toString();
        } catch (UnknownHostException | SocketException _) {
            return null;
        }
    }

    public static @Nullable String getIPAddress() {
        try (var httpClient = HttpClient.newHttpClient()) {
            var uri = URI.create(IP_URL);

            var request = HttpRequest.newBuilder(uri).GET().build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().split("\"")[3];
            } else {
                return null;
            }
        } catch (InterruptedException | IOException _) {
            return null;
        }
    }

    private static byte[] fetchMachineAddress() throws UnknownHostException, SocketException {
        var localhost = InetAddress.getLocalHost();
        var networkInterface = NetworkInterface.getByInetAddress(localhost);
        return networkInterface.getHardwareAddress();
    }
}
