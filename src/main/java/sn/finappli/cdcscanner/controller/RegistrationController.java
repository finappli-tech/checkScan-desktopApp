package sn.finappli.cdcscanner.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.CDCScannerApplication;
import sn.finappli.cdcscanner.model.output.RegistrationOutput;
import sn.finappli.cdcscanner.service.RegistrationService;
import sn.finappli.cdcscanner.service.impl.RegistrationServiceImpl;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

@NoArgsConstructor
@Getter
@Setter
public class RegistrationController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);
    private final RegistrationService registrationService = new RegistrationServiceImpl();

    @FXML
    private Button saveButton;
    @FXML
    private TextField appId;
    @FXML
    private TextField token;
    @FXML
    private ProgressIndicator loader;

    @FXML
    private boolean isTokenValid;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.loader.setVisible(false);
        appId.setText(SystemUtils.getAppIdentifier().toString());
        token.textProperty().addListener((_, _, value) -> this.saveButton.setDisable(!Utils.isValidUUID(value)));
    }

    @FXML
    void executeAppRegistration(ActionEvent event) {
        this.loader.setVisible(true);
        var body = new RegistrationOutput(this.appId.getText(), this.token.getText());
        var status = registrationService.register(body);
        if (status) goToAuthenticationPage((Node) event.getSource());
        else this.loader.setVisible(false);
    }

    private void goToAuthenticationPage(@NotNull Node node) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(CDCScannerApplication.class.getResource("authentication.fxml")));
            Stage stage = (Stage) node.getScene().getWindow();
            var scene = new Scene(root, 800, 600);
            stage.setTitle("Connexion");
            scene.getStylesheets().add(getDefaultCss());
            stage.setScene(scene);
            this.loader.setVisible(false);
            stage.show();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            System.exit(4);
        }
    }

}
