package sn.finappli.cdcscanner.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.service.OCRReader;
import sn.finappli.cdcscanner.service.impl.TesseractOCRReaderImpl;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

@Getter
@Setter
@NoArgsConstructor
public class ScanController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanController.class);
    private final OCRReader reader = new TesseractOCRReaderImpl();

    @FXML
    private Button refreshButton;

    @FXML
    private ChoiceBox<Object> choiceBox;

    @FXML
    private DatePicker dateField;
    @FXML
    private TextField cmcField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField recipientField;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Utils.configureRefreshButton(refreshButton);
        refreshButton.setOnMouseClicked(System.out::println);

    }

    @FXML
    void fetchAvailableScanners(ActionEvent __) {
        // fetching scanner list
    }

    private String getCMCFromOcrReader(File file) {
        return reader.read(file);
    }
}
