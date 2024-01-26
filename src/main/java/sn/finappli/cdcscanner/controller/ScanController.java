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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.CDCScannerApplication;
import sn.finappli.cdcscanner.model.input.ServerResponse;
import sn.finappli.cdcscanner.model.output.ScanRegistrationOutput;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.OCRReader;
import sn.finappli.cdcscanner.service.PrinterService;
import sn.finappli.cdcscanner.service.ScanService;
import sn.finappli.cdcscanner.service.impl.InputStreamDoc;
import sn.finappli.cdcscanner.service.impl.PrinterServiceImpl;
import sn.finappli.cdcscanner.service.impl.ScannedScanServiceImpl;
import sn.finappli.cdcscanner.service.impl.TesseractOCRReaderImpl;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static sn.finappli.cdcscanner.service.impl.PrinterServiceImpl.attributeSet;
import static sn.finappli.cdcscanner.service.impl.PrinterServiceImpl.flavor;
import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

@Getter
@Setter
@NoArgsConstructor
public class ScanController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanController.class);
    private static final String EXTENSION = "jpeg";
    private final OCRReader reader = new TesseractOCRReaderImpl();
    private final PrinterService printerService = new PrinterServiceImpl();
    private final ScanService scanService = new ScannedScanServiceImpl();

    private PrintService ps = null;
    private boolean isDateValid = false;
    private String filename = null;

    @FXML
    private Button refreshButton;

    @FXML
    private ChoiceBox<PrintService> choiceBox;

    @FXML
    private DatePicker dateField;
    @FXML
    private TextField cmcField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField recipientField;
    @FXML
    private Button send;
    @FXML
    private ProgressIndicator loader;
    @FXML
    private Button launchScan;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Utils.configureRefreshButton(refreshButton);
        dateField.setValue(LocalDate.now());
        setDateFormatter();
        onRefresh();
        configureChoiceBox();

        dateField.valueProperty().addListener((a, b, value) -> isDateValid = value != null);
        cmcField.textProperty().addListener((a, b, value) -> cmcField.setText(value.replaceAll("[^0-9 ]", "")));
        amountField.textProperty().addListener((a, b, value) -> amountField.setText(value.replaceAll("\\D", "")));
    }

    @FXML
    void onRefresh() {
        loader.setVisible(true);
        choiceBox.getSelectionModel().clearSelection();
        var items = printerService.listPrinters();
        choiceBox.getItems().setAll(items);
        loader.setVisible(false);
    }

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

    private void configureChoiceBox() {
        choiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(PrintService object) {
                return object == null ? null : object.getName();
            }

            @Override
            public PrintService fromString(String string) {
                return null;
            }
        });
        choiceBox.getSelectionModel().selectedItemProperty().addListener((e, f, selected) -> {
            this.ps = selected;
            final boolean active = selected == null;
            launchScan.setDisable(active);
            dateField.setDisable(active);
            cmcField.setDisable(active);
            amountField.setDisable(active);
            recipientField.setDisable(active);
            send.setDisable(active);
        });
    }

    @FXML
    void print() {
        loader.setVisible(true);
        this.filename = "print_check_%d.%s".formatted(Instant.now().toEpochMilli(), EXTENSION);
        DocPrintJob printJob = ps.createPrintJob();
        var doc = new InputStreamDoc(filename, flavor);


        var listener = new PrintJobListener() {
            @Override
            public void printDataTransferCompleted(PrintJobEvent pje) {
                LOGGER.info(pje.toString());
                LOGGER.info("transfer completed");
            }

            @Override
            public void printJobCompleted(PrintJobEvent pje) {
                LOGGER.info(pje.toString());
                LOGGER.info("job completed");
                loader.setVisible(false);
            }

            @Override
            public void printJobFailed(PrintJobEvent pje) {
                LOGGER.info(pje.toString());
                LOGGER.info("failed");
                loader.setVisible(false);
            }

            @Override
            public void printJobCanceled(PrintJobEvent pje) {
                LOGGER.info(pje.toString());
                LOGGER.info("canceled");
                loader.setVisible(false);
            }

            @Override
            public void printJobNoMoreEvents(PrintJobEvent pje) {
                LOGGER.info(pje.toString());
                LOGGER.info("no more event");
            }

            @Override
            public void printJobRequiresAttention(PrintJobEvent pje) {
                LOGGER.info(pje.toString());
                LOGGER.info("attention");
            }
        };
        /* Print the doc as specified */
        try {
            printJob.addPrintJobListener(listener);
            printJob.print(doc, attributeSet);
        } catch (PrintException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @FXML
    void sendCheck(ActionEvent event) {
        if (dateField.getValue() == null || Stream.of(cmcField.getText(), recipientField.getText(), amountField.getText()).anyMatch(StringUtils::isBlank)) {
            Utils.displaySimpleErrorAlertDialog("Veuillez remplir tous les champs", "Nouveau scan");
            return;
        }
        var sec = SecurityContextHolder.getContext();
        var task = new Task<ServerResponse>() {
            @Override
            protected ServerResponse call() {
                SystemUtils.loadAppConfig();
                SecurityContextHolder.setContext(sec);
                return scanService.sendScan(getRequestValue());
            }
        };
        task.setOnRunning(e -> loader.setVisible(true));
        task.setOnSucceeded(evt -> {
            var response = (ServerResponse) evt.getSource().getValue();
            if (!response.hasError()) {
                Utils.displaySimpleSuccessfulAlertDialog("Nouveau Scan", null, "Votre nouveau scan a été enregistré avec succès.");
                goHome(event);
            } else {
                Utils.displaySimpleSuccessfulAlertDialog("Nouveau Scan", "Enregistrement", response.error());
                loader.setVisible(false);
            }
        });
        task.setOnFailed(e -> loader.setVisible(false));
        new Thread(task).start();
    }

    private @NotNull ScanRegistrationOutput getRequestValue() {
        return ScanRegistrationOutput.builder()
                .ip(SystemUtils.getIPAddress())
                .date(dateField.getValue().atTime(LocalTime.now()))
                .appId(SystemUtils.getAppIdentifier())
                .cmc(cmcField.getText())
                .amount(new BigDecimal(amountField.getText()))
                .recipient(StringUtils.trimToEmpty(recipientField.getText()))
                .build();
    }

    private String getCMCFromOcrReader(File file) {
        return reader.read(file);
    }
}
