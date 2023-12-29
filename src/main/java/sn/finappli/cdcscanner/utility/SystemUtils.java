package sn.finappli.cdcscanner.utility;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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

    private static final String IP_URL = "https://httpbin.org/ip";

    private SystemUtils() {
    }

    public static @Nullable String getAppIdentifier() {
        try {
            var macAddressBytes = fetchMachineAddress();
            UUID uuid = UUID.nameUUIDFromBytes(macAddressBytes);
            return uuid.toString();
        } catch (UnknownHostException | SocketException _) {
            return null;
        }
    }

    public static @Nullable String getMacAddress() {
        try {
            var macAddressBytes = fetchMachineAddress();

            StringBuilder macAddressStringBuilder = new StringBuilder();
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
