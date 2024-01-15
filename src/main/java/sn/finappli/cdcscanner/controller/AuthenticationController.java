package sn.finappli.cdcscanner.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.CDCScannerApplication;
import sn.finappli.cdcscanner.service.AuthenticationService;
import sn.finappli.cdcscanner.service.impl.AuthenticationServiceImpl;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

@Getter
@Setter
@NoArgsConstructor
public class AuthenticationController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService = new AuthenticationServiceImpl();

    @FXML
    private Button loginButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @FXML
    void requestCode(@NotNull ActionEvent event) {

    }

    @FXML
    void authenticate(@NotNull ActionEvent event) {
        authenticationService.authenticate("", "vm8h-4en9-dqki-46qr-gdpj-7qh1-m2lb-m8da");
        goToHomePage((Node) event.getSource());
    }

    private void goToHomePage(@NotNull Node node) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(CDCScannerApplication.class.getResource("home.fxml")));
            Stage stage = (Stage) node.getScene().getWindow();
            var scene = new Scene(root, 900, 600);
            stage.setTitle("Scan Chèque: Accueil");
            stage.centerOnScreen();
            scene.getStylesheets().add(getDefaultCss());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(2);
        }
    }
}
