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
import sn.finappli.cdcscanner.security.SecurityContext;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.RegistrationService;
import sn.finappli.cdcscanner.service.impl.RegistrationServiceImpl;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static sn.finappli.cdcscanner.utility.Utils.catchStarterError;
import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

/**
 * Main class for the CDC Scanner application.
 */
public class CDCScannerApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(CDCScannerApplication.class);
    private final RegistrationService registrationService = new RegistrationServiceImpl();

    private Stage stage;
    private YamlConfig config;
    private boolean isAppRegistered = false;
    private Exception initException = null;

    /**
     * The entry point for the application.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        System.setProperty("javafx.preloader", PreloaderController.class.getCanonicalName());
        launch(args);
    }

    /**
     * Starts the JavaFX application.
     *
     * @param primaryStage The primary stage for the application.
     */
    @Override
    public void start(Stage primaryStage) {
        if (Objects.nonNull(initException)) catchStarterError(initException, false);
        SecurityContextHolder.setContext(new SecurityContext(
                "eyJhbGciOiJIUzI1NiJ9.eyJsLWxldiI6IkhtYWNTSEEyNTYiLCJpc3MiOiJhZG1pbl9TQ1AgS0EgRVQgS0EiLCJleHAiOjE3MDY2NDIxMTMsImxvZ2luIjoiYWRtaW5fU0NQIEtBIEVUIEtBIiwianRpIjoiYzI3MTZhOWQtOTVjYS00MTU2LThjODAtMjNkNjk0NWViYzUxIn0.aJV_HiWstuQtNAyp5ClotUc2jtqJTo4Bhb2QxBuEyJg",
                "9z1pzqakdeazb8fpnqq4cg44ryu9209l",
                "HmacSHA256",
                LocalDateTime.now().plusDays(1)));

        this.stage = primaryStage;
        var icon = Objects.requireNonNull(CDCScannerApplication.class.getResource("/images/logo.png")).toExternalForm();
        this.stage.getIcons().add(new Image(icon));
        this.stage.centerOnScreen();
        this.stage.setResizable(false);

        ConfigHolder.setContext(config);
        if (isAppRegistered) replaceSceneContent("scanner.fxml", "Connexion");
        else replaceSceneContent("registration.fxml", "Enregistrement");
        stage.show();
        logger.info("Application started successfully");
    }

    /**
     * Initializes the application by loading configurations and checking the registration status.
     */
    @Override
    public void init() {
        try {
            logger.info("Initializing application");
            SystemUtils.loadAppConfig();
            this.config = ConfigHolder.getContext();
            checkScanStorePath(ConfigHolder.getContext().getScanStoragePath(), "de stockage");
            checkScanStorePath(ConfigHolder.getContext().getScanArchivePath(), "d'archive");

            isAppRegistered = registrationService.isRegistered();
            logger.info("Application init run successfully");
            Thread.sleep(1000);
        } catch (InterruptedException | IOException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            initException = e;
        }
    }

    /**
     * Checks the validity of the scan store path.
     *
     * @param root       The root path of the scan store.
     * @param folderType The type of the folder (storage or archive).
     */
    private void checkScanStorePath(String root, String folderType) {
        var path = Paths.get(root);
        String message;
        if (isBlank(root))
            message = "Le dossier %s des fichiers scannés n'est pas configuré".formatted(folderType);
        else if (!Files.exists(path))
            message = "Le chemin `%s` du dossier %s des fichiers scannés n'existe pas".formatted(root, folderType);
        else if (!Files.isReadable(path))
            message = "Veuillez autoriser l'accès au dossier %s des fichiers scannés".formatted(folderType);
        else if (!Files.isDirectory(path))
            message = "Le chemin du dossier %s doit être un dossier".formatted(folderType);
        else return;
        logger.error("No access to folder");
        logger.error(root);
        logger.error(message);
        Utils.displaySimpleErrorAlertDialog(message, "INITIALISATION");
        System.exit(-10000);
    }

    /**
     * Stops the application, clearing security and configuration contexts.
     */
    @Override
    public void stop() {
        logger.info("Start application exit process");
        SecurityContextHolder.clearContext();
        ConfigHolder.clearContext();
        logger.info("Application exited gracefully");
    }

    /**
     * Replaces the current scene content with a new one.
     *
     * @param fxml  The FXML file to load.
     * @param title The title of the new scene.
     */
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
