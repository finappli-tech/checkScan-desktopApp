package sn.finappli.cdcscanner.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import javafx.scene.control.Alert;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.input.APIError;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class Utils {

    public static final String CONTENT_TYPE_HEADER = "Content-type";
    public static final String CONTENT_TYPE = "application/json";
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Une erreur s'est produite. Veuillez réessayer SVP!";
    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static final DecimalFormat FORMATTER = (DecimalFormat) NumberFormat.getInstance(Locale.FRANCE);
    private static final DecimalFormatSymbols SYMBOLS = FORMATTER.getDecimalFormatSymbols();

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .addModule(new ParameterNamesModule())
            .addModule(new JavaTimeModule())
            .defaultTimeZone(TimeZone.getTimeZone("Africa/Dakar"))
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .build();

    static {
        SYMBOLS.setGroupingSeparator(' ');
        FORMATTER.setDecimalFormatSymbols(SYMBOLS);
    }

    private Utils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Converts an object of a specified class to its JSON representation.
     *
     * @param object The object to be converted to JSON.
     * @param <T>    The type of the object.
     * @return The JSON representation of the object.
     * @throws JsonProcessingException If an error occurs during JSON processing.
     */
    public static <T> String classToJson(T object) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsString(object);
    }

    /**
     * Converts a JSON string to an object of the specified class type.
     *
     * @param clazzType The class type to convert the JSON to.
     * @param json      The JSON string to be converted.
     * @param <T>       The type of the resulting object.
     * @return The object created from the JSON string.
     * @throws JsonProcessingException If an error occurs during JSON processing.
     */
    public static <T> T jsonToClass(Class<T> clazzType, String json) throws JsonProcessingException {
        return JSON_MAPPER.readValue(json, clazzType);
    }

    /**
     * Converts a JSON string to a list of objects of the specified element type.
     *
     * @param json The JSON string to be converted.
     * @param <T>  The type of elements in the resulting list.
     * @return The list of objects created from the JSON string.
     * @throws JsonProcessingException If an error occurs during JSON processing.
     */
    public static <T> List<T> jsonToList(Class<T> elementType, String json) throws JsonProcessingException {
        CollectionType listType = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
        return JSON_MAPPER.readValue(json, listType);
    }

    /**
     * Converts a List of objects to its corresponding JSON representation.
     *
     * @param list The List of objects to be converted to JSON.
     * @param <T>  The type of objects in the List.
     * @return A JSON representation of the input List.
     * @throws JsonProcessingException If an error occurs during JSON processing.
     */
    public static <T> String listToJson(List<T> list) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsString(list);
    }

    /**
     * Builds an {@link APIError} object from the given status code and JSON error response.
     *
     * @param status The HTTP status code of the error response.
     * @param json   The JSON error response string.
     * @return An {@link APIError} object representing the error.
     */
    public static @NotNull APIError buildError(int status, String json) {
        if (isBlank(json)) return new APIError(status, DEFAULT_ERROR_MESSAGE);
        try {
            var mapper = JSON_MAPPER.readValue(json, APIError.class);
            mapper.setStatus(status);
            return mapper;
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            return new APIError(status, DEFAULT_ERROR_MESSAGE);
        }
    }

    /**
     * Displays a simple informational alert dialog with the given title, header, and message.
     *
     * @param title   The title of the alert.
     * @param header  The header text of the alert (can be {@code null}).
     * @param message The main message of the alert.
     */
    public static void displaySimpleSuccessfulAlertDialog(String title, @Nullable String header, @NotNull String message) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        if (isNotBlank(header)) alert.setHeaderText(header);
        alert.setResizable(false);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays a simple error alert dialog with the given error message and optional header.
     *
     * @param message     The error message.
     * @param errorHeader The header text of the alert (can be {@code null}).
     */
    public static void displaySimpleErrorAlertDialog(@NotNull String message, @Nullable String errorHeader) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        if (isNotBlank(errorHeader)) alert.setHeaderText(errorHeader);
        alert.setResizable(false);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Catches a starter error, logs it, displays an error dialog, and exits the application.
     *
     * @param exception                    The exception to be caught.
     * @param shouldInterruptCurrentThread Whether to interrupt the current thread after catching the exception.
     */
    public static <T extends Exception> void catchStarterError(T exception, boolean shouldInterruptCurrentThread) {
        if (shouldInterruptCurrentThread) Thread.currentThread().interrupt();
        LOGGER.error("CANNOT LOAD APPLICATION");
        LOGGER.error(exception.getMessage(), exception);
        Utils.displaySimpleErrorAlertDialog("Une erreur s'est produite lors du chargement. Veuillez contacter votre administrateur", "INITIALISATION");
        System.exit(13876);
    }

    /**
     * Checks if a given string is a valid UUID.
     *
     * @param uuid The string to check.
     * @return {@code true} if the string is a valid UUID, {@code false} otherwise.
     */
    public static boolean isValidUUID(String uuid) {
        if (isBlank(uuid)) return false;
        Pattern pattern = Pattern.compile(UUID_REGEX);
        return pattern.matcher(uuid).matches();
    }

    /**
     * Gets the default CSS file path for styling the application.
     *
     * @return The default CSS file path.
     */
    public static String getDefaultCss() {
        return Objects.requireNonNull(Utils.class.getResource("/css/styles.css")).toExternalForm();
    }

    /**
     * Converts a {@link BigDecimal} value to a formatted string using the French locale.
     *
     * @param value The {@link BigDecimal} value to be formatted.
     * @return The formatted string.
     */
    @Contract("_ -> new")
    public static @NotNull String bigDecimalToFormattedFRString(@NotNull BigDecimal value) {
        return FORMATTER.format(value.doubleValue());
    }

    /**
     * Converts a hexadecimal string to its corresponding UTF-8 string representation.
     *
     * @param hex The hexadecimal string to be converted.
     * @return The UTF-8 string representation.
     * @throws NullPointerException If the input hex string is {@code null} or empty.
     */
    @Contract("_ -> new")
    public static @NotNull String hexToString(String hex) {
        if (isBlank(hex)) throw new NullPointerException("No value provided");
        byte[] bytes = new BigInteger(hex, 16).toByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
