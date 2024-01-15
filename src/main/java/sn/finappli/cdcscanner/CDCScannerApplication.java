package sn.finappli.cdcscanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.controller.PreloaderController;
import sn.finappli.cdcscanner.model.input.YamlConfig;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.RegistrationService;
import sn.finappli.cdcscanner.service.impl.RegistrationServiceImpl;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.SystemUtils;

import java.io.IOException;
import java.util.Objects;

import static sn.finappli.cdcscanner.utility.Utils.catchStarterError;
import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

public class CDCScannerApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(CDCScannerApplication.class);
    private final RegistrationService registrationService = new RegistrationServiceImpl();
    private Stage stage;
    private YamlConfig config;
    private boolean isAppRegistered;

    public static void main(String[] args) {
        System.setProperty("javafx.preloader", PreloaderController.class.getCanonicalName());
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        var icon = Objects.requireNonNull(CDCScannerApplication.class.getResource("/images/logo.png")).toExternalForm();
        this.stage.getIcons().add(new Image(icon));
        this.stage.centerOnScreen();
        this.stage.setResizable(false);

        ConfigHolder.setContext(config);
        if (isAppRegistered) replaceSceneContent("authentication.fxml", "Connexion");
        else replaceSceneContent("registration.fxml", "Enregistrement");
        stage.show();
        logger.info("Application started successfully");
    }

    @Override
    public void init() {
        try {
            logger.info("Initializing application");
            SystemUtils.loadAppConfig();
            this.config = ConfigHolder.getContext();
            isAppRegistered = registrationService.isRegistered();
            logger.info("Application init run successfully");
        } catch (InterruptedException | IOException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            catchStarterError(e, false);
        }
    }

    @Override
    public void stop() {
        logger.info("Start application exit process");
        SecurityContextHolder.clearContext();
        ConfigHolder.clearContext();
        logger.info("Application exited gracefully");
    }

    private void replaceSceneContent(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(CDCScannerApplication.class.getResource(fxml)));
            Scene scene = stage.getScene();
            if (scene == null) {
                var sc = new Scene(root);
                sc.getStylesheets().add(getDefaultCss());
                stage.setTitle(title);
                stage.setScene(sc);
            } else {
                stage.getScene().setRoot(root);
            }
            stage.sizeToScene();
        } catch (Exception e) {
            catchStarterError(e, e instanceof InterruptedException);
        }
    }
}