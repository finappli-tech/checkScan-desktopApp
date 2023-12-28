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

public final class SystemUtils {

    private static final String IP_URL = "https://httpbin.org/ip";

    private SystemUtils() {
    }

    public static @Nullable String getMacAddress() throws UnknownHostException, SocketException {
        var localhost = InetAddress.getLocalHost();

        var networkInterface = NetworkInterface.getByInetAddress(localhost);

        byte[] macAddressBytes = networkInterface.getHardwareAddress();

        StringBuilder macAddressStringBuilder = new StringBuilder();
        for (int i = 0; i < macAddressBytes.length; i++) {
            macAddressStringBuilder.append("%02X%s".formatted(macAddressBytes[i], (i < macAddressBytes.length - 1) ? "-" : ""));
        }

        return macAddressStringBuilder.toString();
    }

    public static @Nullable String getIPAddress() throws IOException, InterruptedException {
        try (var httpClient = HttpClient.newHttpClient()) {
            var uri = URI.create(IP_URL);

            var request = HttpRequest.newBuilder(uri).GET().build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().split("\"")[3];
            } else {
                return null;
            }
        }
    }
}
