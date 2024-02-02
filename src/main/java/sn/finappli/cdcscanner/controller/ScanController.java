package sn.finappli.cdcscanner.controller;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.CDCScannerApplication;
import sn.finappli.cdcscanner.model.input.FolderReaderResult;
import sn.finappli.cdcscanner.model.input.ServerResponse;
import sn.finappli.cdcscanner.model.output.ChecksRegistrationOutput;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.FolderReaderService;
import sn.finappli.cdcscanner.service.ScanService;
import sn.finappli.cdcscanner.service.impl.FolderReaderServiceImpl;
import sn.finappli.cdcscanner.service.impl.ScannedScanServiceImpl;
import sn.finappli.cdcscanner.utility.SVGTranscoder;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

@Getter
@Setter
@NoArgsConstructor
public class ScanController implements Initializable {

    public static final String PAGINATION_COUNT_TEMPLATE = "%d/%d";
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanController.class);
    private final ScanService scanService = new ScannedScanServiceImpl();
    private final FolderReaderService readerService = new FolderReaderServiceImpl();

    private boolean isDateValid = false;
    private int numberOfValidCheck = 0;
    private int currentIndex = 0;
    private List<FolderReaderResult> checks = new ArrayList<>();

    @FXML
    private Pane container;
    @FXML
    private Pane loaderContainer;
    @FXML
    private DatePicker dateField;
    @FXML
    private TextField cmcField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField recipientField;
    @FXML
    private Button sendButton;
    @FXML
    private Button homeButton;
    @FXML
    private ProgressIndicator loader;
    @FXML
    private Button launchScan;
    @FXML
    private ImageView recto;
    @FXML
    private ImageView verso;
    @FXML
    private Label numberOfCheck;
    @FXML
    private Label numberOfCompletedCheck;
    @FXML
    private Label numberOfRejectedCheck;

    @FXML
    private Label paginationCount;
    @FXML
    private Button paginationPrevious;
    @FXML
    private Button paginationNext;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setDateFormatter();
        setChecksCountInfo();

        configurePagination(paginationPrevious, "/svg/caret-left.svg");
        configurePagination(paginationNext, "/svg/caret-right.svg");

        configureFields();
    }

    /**
     * Handles the "Go Home" button click event.
     *
     * @param event The ActionEvent triggered by the button click.
     */
    @FXML
    void goHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(CDCScannerApplication.class.getResource("home.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            var scene = new Scene(root, 900, 600);
            stage.setTitle("Scan Chèque: Accueil");
            stage.centerOnScreen();
            scene.getStylesheets().add(getDefaultCss());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(2);
        }
    }

    /**
     * Fetches scanned items from the designated folder.
     */
    @FXML
    void fetchScannedItems() {
        showLoader();
        checks.clear();
        numberOfValidCheck = 0;
        setChecksCountInfo();
        setFieldStatus(true);
        try {
            checks.addAll(readerService.readScanFolder());
            setChecksCountInfo();
            currentIndex = 1;
            paginationPrevious.setDisable(true);
            paginationNext.setDisable(checks.size() <= 1);
            paginationCount.setText(PAGINATION_COUNT_TEMPLATE.formatted(currentIndex, checks.size()));
            if (!checks.isEmpty()) {
                setFieldStatus(false);
                selectCheck();
            } else {
                hideLoader();
            }
        } catch (Exception e) {
            hideLoader();
            LOGGER.error(e.getMessage(), e);
            Utils.displaySimpleErrorAlertDialog(e.getMessage(), "Récupération de scan");
        }
    }

    /**
     * Shows the next scanned item in the list.
     */
    @FXML
    void showNextCheck() {
        if (currentIndex == checks.size()) return;
        showLoader();
        paginationCount.setText(PAGINATION_COUNT_TEMPLATE.formatted(++currentIndex, checks.size()));
        paginationNext.setDisable(currentIndex == checks.size());
        paginationPrevious.setDisable(currentIndex <= 1);
        selectCheck();
    }

    /**
     * Shows the previous scanned item in the list.
     */
    @FXML
    void showPreviousCheck() {
        if (currentIndex <= 1) return;
        showLoader();
        paginationCount.setText(PAGINATION_COUNT_TEMPLATE.formatted(--currentIndex, checks.size()));
        paginationPrevious.setDisable(currentIndex <= 1);
        paginationNext.setDisable(currentIndex == checks.size());
        selectCheck();
    }

    /**
     * Sends the details of the scanned items to the backend for registration.
     *
     * @param event The ActionEvent triggered by the button click.
     */
    @FXML
    void sendCheck(ActionEvent event) {
        if (dateField.getValue() == null || Stream.of(cmcField.getText(), recipientField.getText(), amountField.getText()).anyMatch(StringUtils::isBlank)) {
            Utils.displaySimpleErrorAlertDialog("Veuillez remplir tous les champs de tous les scans", "Nouveau scan");
            return;
        }
        var sec = SecurityContextHolder.getContext();
        var task = new Task<String>() {
            @Override
            protected String call() {
                SystemUtils.loadAppConfig();
                SecurityContextHolder.setContext(sec);
                StringBuilder builder = new StringBuilder();
                getRequestValue().map(scanService::saveChecks).filter(ServerResponse::hasError)
                        .forEach(s -> builder.append(s.error()).append(System.lineSeparator()));
                return builder.toString();
            }
        };
        task.setOnRunning(e -> showLoader());
        task.setOnSucceeded(evt -> {
            var results = (String) evt.getSource().getValue();
            if (!results.isEmpty()) {
                scanService.revertSave(checks.stream().map(FolderReaderResult::getCmc).toList());
                Utils.displaySimpleErrorAlertDialog("Enregistrement", results);
                hideLoader();
            } else {
                Utils.displaySimpleSuccessfulAlertDialog("Nouveau Scan", null, "Votre nouveau scan a été enregistré avec succès.");
                goHome(event);
            }
        });
        task.setOnFailed(e -> hideLoader());
        new Thread(task).start();
    }

    /**
     * Configures the listener for the recipient field, date field, and amount field.
     */
    private void configureFields() {
        recipientField.textProperty().addListener((a, b, value) -> {
            checks.get(currentIndex - 1).setRecipient(value);
            manageChecksCompletion();
        });

        dateField.valueProperty().addListener((a, b, value) -> {
            isDateValid = value != null;
            checks.get(currentIndex - 1).setDate(isDateValid ? value : null);
            manageChecksCompletion();
        });
        amountField.textProperty().addListener((a, b, value) -> {
            var amount = isNotBlank(trimToEmpty(value)) ? value.replaceAll("\\D", "") : "";
            checks.get(currentIndex - 1).setAmount(amount);
            amountField.setText(amount);
            manageChecksCompletion();
        });
    }

    /**
     * Selects and displays the scanned item at the current index.
     */
    private void selectCheck() {
        FolderReaderResult item;
        if (currentIndex <= 1) item = checks.get(0);
        else item = checks.get(Math.min(currentIndex, checks.size()) - 1);
        cmcField.setText(item.getCmc());
        recto.setImage(new Image(item.getFileR().toUri().toString()));
        verso.setImage(new Image(item.getFileV().toUri().toString()));
        dateField.setValue(item.getDate());
        recipientField.setText(item.getRecipient());
        amountField.setText(item.getAmount());
        hideLoader();
    }

    /**
     * Configures the date formatter for the date field.
     */
    private void setDateFormatter() {
        dateField.converterProperty().setValue(new StringConverter<>() {
            @Override
            public String toString(LocalDate localDate) {
                try {
                    return localDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE));
                } catch (Exception ignored) {
                    return null;
                }
            }

            @Override
            public LocalDate fromString(String s) {
                try {
                    return LocalDate.parse(s);
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
        });
    }

    /**
     * Builds and returns the ScanRegistrationOutput object with the current field values.
     *
     * @return A stream of ScanRegistrationOutput object.
     */
    private Stream<ChecksRegistrationOutput> getRequestValue() {
        var appId = SystemUtils.getAppIdentifier().toString();
        var ip = SystemUtils.getIPAddress();
        return checks.stream().map(check -> ChecksRegistrationOutput.builder()
                .name(check.getName())
                .cmc(check.getCmc())
                .date(check.getDate())
                .scanDate(check.getScanDate())
                .recipient(check.getRecipient())
                .amount(check.getAmount())
                .fileD(check.getFileD().toFile())
                .fileR(check.getFileR().toFile())
                .fileV(check.getFileV().toFile())
                .appId(appId)
                .ip(ip)
                .build());
    }

    /**
     * Sets the checks count information labels.
     */
    private void setChecksCountInfo() {
        numberOfCheck.setText("Nombre de chèque: %d".formatted(checks.size()));
        numberOfCompletedCheck.setText("Chèque complété: %d".formatted(numberOfValidCheck));
    }

    /**
     * Sets the status of the date, recipient, and amount fields.
     *
     * @param status The status to set (true for disabled, false for enabled).
     */
    private void setFieldStatus(boolean status) {
        dateField.setDisable(status);
        recipientField.setDisable(status);
        amountField.setDisable(status);
    }

    /**
     * Displays the loader and disables UI elements.
     */
    private void showLoader() {
        homeButton.setDisable(true);
        sendButton.setDisable(true);
        container.setDisable(true);
        loader.setVisible(true);
        loaderContainer.setVisible(true);
    }

    /**
     * Hides the loader and enables UI elements.
     */
    private void hideLoader() {
        homeButton.setDisable(false);
        sendButton.setDisable(numberOfValidCheck != checks.size());
        container.setDisable(false);
        loader.setVisible(false);
        loaderContainer.setVisible(false);
    }

    /**
     * Manages the completion of scanned checks and updates relevant information.
     */
    private void manageChecksCompletion() {
        numberOfValidCheck = (int) checks.stream().filter(FolderReaderResult::isValid).count();
        sendButton.setDisable(numberOfValidCheck < checks.size());
        numberOfCompletedCheck.setText("Chèque complété: %d".formatted(numberOfValidCheck));
    }

    /**
     * Configures the pagination buttons with SVG icons.
     *
     * @param button The Button to configure.
     * @param path   The path to the SVG icon.
     */
    @SneakyThrows
    private void configurePagination(@NotNull Button button, String path) {
        var image = SVGTranscoder.transcodeSVG(path);
        image.setPreserveRatio(true);
        button.setGraphic(image);
    }
}
