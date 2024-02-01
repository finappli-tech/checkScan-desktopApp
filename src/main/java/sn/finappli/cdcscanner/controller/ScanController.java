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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import sn.finappli.cdcscanner.service.FolderReaderService;
import sn.finappli.cdcscanner.service.ScanService;
import sn.finappli.cdcscanner.service.impl.FolderReaderServiceImpl;
import sn.finappli.cdcscanner.service.impl.ScannedScanServiceImpl;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

@Getter
@Setter
@NoArgsConstructor
public class ScanController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanController.class);
    private final ScanService scanService = new ScannedScanServiceImpl();
    private final FolderReaderService readerService = new FolderReaderServiceImpl();

    private boolean isDateValid = false;

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
    @FXML
    private ImageView recto;
    @FXML
    private ImageView verso;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setDateFormatter();

        dateField.valueProperty().addListener((a, b, value) -> isDateValid = value != null);
        amountField.textProperty().addListener((a, b, value) -> amountField.setText(value.replaceAll("\\D", "")));
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

    @FXML
    void print() {
        setFieldStatus(false);
        try {
            var items = readerService.readScanFolder();
            if (!items.isEmpty()) {
                setFieldStatus(true);
                var item = items.get(0);
                cmcField.setText(item.getCmc());
                recto.setImage(new Image(item.getFileR().toUri().toString()));
                verso.setImage(new Image(item.getFileV().toUri().toString()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setFieldStatus(boolean status) {
        dateField.setDisable(status);
        recipientField.setDisable(status);
        amountField.setDisable(status);
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

}
