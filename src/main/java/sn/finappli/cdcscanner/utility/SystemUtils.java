package sn.finappli.cdcscanner.utility;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import sn.finappli.cdcscanner.model.input.YamlConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public final class SystemUtils {

    public static final String DEFAULT_ENCODER = "HmacSHA256";
    public static final int TOKEN_EXPIRATION = 6 * 60 * 60; // seconds
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemUtils.class);
    private static final String IP_URL = "https://httpbin.org/ip";

    private SystemUtils() {
    }

    /**
     * Loads the application configuration from the "/config.yaml" resource.
     * Resolves placeholders in the configuration and sets it as the current context.
     * Exit the application with an error code if the config file is not found or an error occurs during loading.
     */
    public static void loadAppConfig() {
        try (InputStream is = SystemUtils.class.getResourceAsStream("/config.yaml")) {
            if (is == null) {
                LOGGER.error("Config file not found");
                System.exit(10);
            }
            var constructor = new Constructor(YamlConfig.class, new LoaderOptions());
            var yaml = new Yaml(constructor);
            var config = yaml.loadAs(is, YamlConfig.class);
            resolverYamlPlaceholders(config);
            ConfigHolder.setContext(config);
        } catch (Exception e) {
            LOGGER.error("Error while loading config file: %s".formatted(e.getMessage()), e);
            System.exit(11);
        }
    }

    /**
     * Retrieves an application identifier based on the machine's MAC address.
     *
     * @return A {@link UUID} representing the application identifier.
     * @throws IllegalArgumentException If the app identifier cannot be retrieved.
     */
    public static @NotNull UUID getAppIdentifier() {
        try {
            var macAddressBytes = fetchMachineAddress();
            return UUID.nameUUIDFromBytes(macAddressBytes);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new IllegalArgumentException("CANNOT RETRIEVE APPID");
        }
    }

    /**
     * Retrieves an application identifier based on the machine's MAC address.
     *
     * @return A {@link UUID} representing the application identifier.
     * @throws IllegalArgumentException If the app identifier cannot be retrieved.
     */
    public static @Nullable String getIPAddress() {
        try {
            var httpClient = HttpClient.newHttpClient();
            var uri = URI.create(IP_URL);
            var request = HttpRequest.newBuilder(uri).GET().build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body().split("\"")[3] : null;
        } catch (InterruptedException | IOException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetches the machine's hardware address (MAC address) using the local network interface.
     *
     * @return A byte array representing the machine's hardware address.
     * @throws UnknownHostException If the local host is unknown.
     * @throws SocketException      If a socket error occurs while fetching the hardware address.
     */
    private static byte[] fetchMachineAddress() throws UnknownHostException, SocketException {
        var localhost = InetAddress.getLocalHost();
        var networkInterface = NetworkInterface.getByInetAddress(localhost);
        return networkInterface.getHardwareAddress();
    }

    /**
     * Resolves placeholders in the fields of the provided {@link YamlConfig} object.
     * It iterates over the String fields, extracts placeholders, and replaces them
     * with corresponding values from other fields within the same object.
     *
     * @param config The {@link YamlConfig} object for which placeholders need to be resolved.
     * @throws RuntimeException If any reflection-related exception occurs during the process.
     */
    private static void resolverYamlPlaceholders(@NotNull YamlConfig config) throws IllegalAccessException, NoSuchFieldException {
        Class<?> clazz = config.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.getType().equals(String.class)) continue;

            try {
                field.setAccessible(true);
                var value = (String) field.get(config);

                for (var placeholder : extractPlaceholders(value)) {
                    var placeholderField = clazz.getDeclaredField(placeholder);
                    placeholderField.setAccessible(true);
                    var replacement = String.valueOf(placeholderField.get(config));
                    value = value.replace("${%s}".formatted(placeholder), replacement);
                    placeholderField.setAccessible(false);
                }
                field.set(config, value);
            } finally {
                field.setAccessible(false);
            }
        }
    }

    /**
     * Extracts placeholders from the given input string. Placeholders are expected
     * to follow the pattern "${placeholder}".
     *
     * @param input The input string from which to extract placeholders.
     * @return An {@code java.util.List<String>} containing all the extracted placeholders.
     */
    private static @NotNull List<String> extractPlaceholders(String input) {
        var pattern = Pattern.compile("\\$\\{([^}]+)}");
        var matcher = pattern.matcher(input);

        var placeholders = new ArrayList<String>();
        while (matcher.find()) placeholders.add(matcher.group(1));
        return placeholders;
    }
}
