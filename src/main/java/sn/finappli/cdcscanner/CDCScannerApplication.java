package sn.finappli.cdcscanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.controller.ScanController;
import sn.finappli.cdcscanner.model.input.RegistrationInput;
import sn.finappli.cdcscanner.model.output.ScanRegistrationOutput;
import sn.finappli.cdcscanner.service.impl.ScannerServiceImpl;
import sn.finappli.cdcscanner.service.impl.TesseractOCRReaderImpl;
import sn.finappli.cdcscanner.utility.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class CDCScannerApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(CDCScannerApplication.class);

    @Override
    public void start(Stage stage) throws IOException {
        try {
            new ScannerServiceImpl().listAllConnectedPrinters();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(e.getMessage(), e);
        }
        Parent root = FXMLLoader.load(Objects.requireNonNull(CDCScannerApplication.class.getResource("splash.fxml")));

        Scene scene = new Scene(root, 500, 300);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws InterruptedException {
//        launch();
        var ocr = new TesseractOCRReaderImpl();
        var file = new File("C:\\Users\\Seydou.Sow\\Downloads\\ze.pdf");
        var cmc = ocr.read(file);

        var body = new ScanRegistrationOutput(SystemUtils.getAppIdentifier(), SystemUtils.getIPAddress(), cmc, "Mbacke sy", LocalDateTime.now(), BigDecimal.valueOf(100000));

        var ctl = new ScanController();
        ctl.send(body);
//        System.exit(0);

    }
}