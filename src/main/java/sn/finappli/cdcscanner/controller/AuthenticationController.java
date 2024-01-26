package sn.finappli.cdcscanner.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.CDCScannerApplication;
import sn.finappli.cdcscanner.model.input.LoginRequestInput;
import sn.finappli.cdcscanner.model.input.LoginRequestPhoneInput;
import sn.finappli.cdcscanner.service.AuthenticationService;
import sn.finappli.cdcscanner.service.impl.AuthenticationServiceImpl;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

@Getter
@Setter
@NoArgsConstructor
public class AuthenticationController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);
    private static final int MAX_ATTEMPT = 3;
    private static final String SUCCESS_LOGIN_ATTEMPT_MESSAGE = """
            Un sms contenant votre code de vérification vous a été envoyé.
            Il expire dans 5 minutes.
            """;
    private final AuthenticationService authenticationService = new AuthenticationServiceImpl();

    private final AtomicInteger step = new AtomicInteger(0);
    private int attempt = 0;
    private LoginRequestInput request;
    private List<LoginRequestPhoneInput> items;
    private int remaining;
    private Timeline timeline;

    @FXML
    private ChoiceBox<LoginRequestPhoneInput> choiceBox;
    @FXML
    private Button button;
    @FXML
    private TextField token;
    @FXML
    private ProgressIndicator loader;
    @FXML
    private Label countdown;
    @FXML
    private Label tokenAttempt;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configureChoiceBox();
        token.textProperty().addListener((e, f, newValue) -> {
            var value = newValue.replaceAll("\\D", "");
            token.setText(value.length() <= 6 ? value : value.substring(0, 6));
            button.setDisable(token.getText().length() < 6);
        });
    }

    @FXML
    void performAuthentication(ActionEvent event) {
        if (step.get() == 0) {
            choiceBox.setDisable(true);
            button.setDisable(true);
            requestAuthentication();
        } else doAuthenticate(event);
    }

    private void requestAuthentication() {
        var task = new Task<LoginRequestInput>() {
            @Override
            protected LoginRequestInput call() {
                SystemUtils.loadAppConfig();
                return authenticationService.requestAuthentication(choiceBox.getValue().getUuid());
            }
        };
        task.setOnRunning(e -> {
            choiceBox.setDisable(true);
            loader.setVisible(true);
        });
        task.setOnSucceeded(event -> {
            request = (LoginRequestInput) event.getSource().getValue();
            Platform.runLater(this::onRequestAuthSuccess);
        });
        new Thread(task).start();
    }

    private void onRequestAuthSuccess() {
        var hasError = isBlank(request.uuid()) || isBlank(request.hashedCode());
        if (hasError) Utils.displaySimpleErrorAlertDialog(request.error(), "Tentative de connexion");
        else {
            Utils.displaySimpleSuccessfulAlertDialog("Connexion", null, SUCCESS_LOGIN_ATTEMPT_MESSAGE);
            launchTimeLine();
            token.requestFocus();
        }
        token.setDisable(hasError);
        choiceBox.setDisable(false);
        loader.setVisible(false);
        button.setDisable(false);
    }

    private void launchTimeLine() {
        remaining = Math.max(ConfigHolder.getContext().getLoginTokenCountdown(), 5 * 60);
        attempt = 0;
        step.set(1);
        tokenAttempt.setText("Tentative %d/3".formatted(attempt));
        tokenAttempt.setVisible(true);
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), this::updateCountdown));
        timeline.setCycleCount(-1);
        countdown.setVisible(true);
        countdown.setText("Ce token expire dans %d:%d".formatted(remaining / 60, remaining % 60));
        timeline.play();
    }

    private void updateCountdown(ActionEvent event) {
        remaining--;
        countdown.setText("Ce token expire dans %d:%d".formatted(remaining / 60, remaining % 60));
        if (remaining <= 0) stopCountdown(true);
    }

    private void stopCountdown(boolean expiry) {
        countdown.setVisible(false);
        countdown.setText("");
        tokenAttempt.setVisible(false);
        tokenAttempt.setText("");
        remaining = Math.max(ConfigHolder.getContext().getLoginTokenCountdown(), 5 * 60);
        attempt = 0;
        step.set(0);
        button.setDisable(true);
        token.setText("");
        token.setDisable(true);
        choiceBox.getSelectionModel().clearSelection();
        timeline.stop();
        if (expiry) Utils.displaySimpleSuccessfulAlertDialog("Timer", null, "Votre token a expiré");
        else Utils.displaySimpleSuccessfulAlertDialog("Timer", null, "Ce token n'est plus valide.");
    }

    private void doAuthenticate(@NotNull ActionEvent event) {
        BCrypt.Result result = BCrypt.verifyer().verify(token.getText().toCharArray(), request.hashedCode());
        if (result.verified) {
            if (authenticationService.authenticate(request.uuid())) goToHomePage((Node) event.getSource());
        } else {
            attempt++;
            tokenAttempt.setText("Tentative %d/3".formatted(attempt));
            if (attempt >= 3) stopCountdown(false);
            else Utils.displaySimpleErrorAlertDialog("Ce token est invalide.", "Vérification");
        }
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
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(2);
        }
    }

    private void configureChoiceBox() {
        if (items == null || items.isEmpty())
            items = authenticationService.findUserForApp();
        choiceBox.setItems(FXCollections.observableArrayList(items));
        choiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(LoginRequestPhoneInput object) {
                if (object == null) return null;
                return object.getTelephone();
            }

            @Override
            public LoginRequestPhoneInput fromString(String string) {
                return null;
            }
        });
        choiceBox.getSelectionModel().selectedItemProperty().addListener((e, f, selected) -> {
            button.setDisable(selected == null);
            if (selected != null && step.get() == 1) {
                token.clear();
                token.setDisable(true);
                step.set(0);
                attempt = 0;
                remaining = Math.max(ConfigHolder.getContext().getLoginTokenCountdown(), 5 * 60);
                timeline.stop();
                countdown.setVisible(false);
                countdown.setText("");
                tokenAttempt.setVisible(false);
                tokenAttempt.setText("");
            }
        });
    }

}
