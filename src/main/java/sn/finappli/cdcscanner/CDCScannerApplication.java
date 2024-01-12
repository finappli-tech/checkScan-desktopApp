package sn.finappli.cdcscanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.RegistrationService;
import sn.finappli.cdcscanner.service.impl.RegistrationServiceImpl;
import sn.finappli.cdcscanner.service.impl.ScannerServiceImpl;

import java.io.IOException;
import java.util.Objects;

public class CDCScannerApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(CDCScannerApplication.class);

    private final RegistrationService registrationService;

    CDCScannerApplication(RegistrationServiceImpl registrationService) {
        this.registrationService = registrationService;
    }

    public static void main(String[] args) {
        launch();
    }

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

        try {
            SecurityContextHolder.clearContext();
            var isAppRegistered = registrationService.isRegistered();

            if (isAppRegistered) {
                // TODO redirect to login page
            }
            else {
                // TODO redirect to registration page
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            // TODO throw error to user
            System.exit(1);
        }

    }
}