package sn.finappli.cdcscanner.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.model.input.APIError;

import java.io.IOException;
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

    public static <T> String classToJson(T object) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsString(object);
    }

    public static <T> T jsonToClass(Class<T> clazzType, String json) throws JsonProcessingException {
        return JSON_MAPPER.readValue(json, clazzType);
    }

    public static <T> List<T> jsonToList(Class<T> elementType, String json) throws JsonProcessingException {
        CollectionType listType = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
        return JSON_MAPPER.readValue(json, listType);
    }

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

    public static void displaySimpleSuccessfulAlertDialog(String title, @Nullable String header, @NotNull String message) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        if (isNotBlank(header)) alert.setHeaderText(header);
        alert.setResizable(false);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void displaySimpleErrorAlertDialog(@NotNull String message, @Nullable String errorHeader) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        if (isNotBlank(errorHeader)) alert.setHeaderText(errorHeader);
        alert.setResizable(false);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static <T extends Exception> void catchStarterError(T exception, boolean shouldInterruptCurrentThread) {
        if (shouldInterruptCurrentThread) Thread.currentThread().interrupt();
        LOGGER.error("CANNOT LOAD APPLICATION");
        LOGGER.error(exception.getMessage(), exception);
        Utils.displaySimpleErrorAlertDialog("Une erreur s'est produite lors du chargement. Veuillez contacter votre administrateur", "INITIALISATION");
        System.exit(13876);
    }

    public static boolean isValidUUID(String uuid) {
        if (isBlank(uuid)) return false;
        Pattern pattern = Pattern.compile(UUID_REGEX);
        return pattern.matcher(uuid).matches();
    }

    public static String getDefaultCss() {
        return Objects.requireNonNull(Utils.class.getResource("/css/styles.css")).toExternalForm();
    }

    @Contract("_ -> new")
    public static @NotNull String bigDecimalToFormattedFRString(@NotNull BigDecimal value) {
        return FORMATTER.format(value.doubleValue());
    }

    public static void configureRefreshButton(@NotNull Button button) {
        var tooltip = new Tooltip("Rafraîchir");
        tooltip.setShowDelay(Duration.ZERO);
        button.setTooltip(tooltip);
        try {
            var image = SVGTranscoder.transcodeSVG("/images/refresh.svg");
            image.setFitHeight(30);
            image.setPreserveRatio(true);
            button.setGraphic(image);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            var region = new Region();
            region.getStyleClass().add("refresh-icon");
            button.setGraphic(region);
        }
    }

    @Contract("_ -> new")
    public static @NotNull String hexToString(String hex) {
        if (isBlank(hex)) throw new NullPointerException("No value provided");
        byte[] bytes = new BigInteger(hex, 16).toByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
