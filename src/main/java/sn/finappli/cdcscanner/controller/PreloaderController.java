package sn.finappli.cdcscanner.controller;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class PreloaderController extends Preloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreloaderController.class);

    Stage stage;
    private ImageView image;

    private Scene createPreloaderScene() {
        getLogoImage();

        var progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-progress-color: black; -fx-padding: 0,3em,0,0");
        progressIndicator.setPrefSize(35d, 35d);

        var footer = new Label("Finappli SAS");
        footer.setStyle("-fx-font-size: 1.5em; -fx-text-fill: #000066; -fx-font-weight: 700; -fx-font-style: oblique; -fx-min-width: 100%; -fx-text-alignment: right; -fx-padding: 0 0 0.4em 0.5em");

        BorderPane pane = new BorderPane();
        pane.setCenter(image);
        pane.setRight(progressIndicator);
        pane.setBottom(footer);
        pane.setStyle("-fx-background-color: linear-gradient(from 0.0% 0.0% to 100.0% 0.0%, #ce93c1ff 0.0%, #dcd2b6ff 50.0%, #7ba2d3ff 100.0%)");

        return new Scene(pane, 500, 300, Color.TRANSPARENT);
    }

    private void getLogoImage() {
        try {
            var imageUrl = Objects.requireNonNull(PreloaderController.class.getResource("/images/logo-cdc-202.png")).toExternalForm();
            image = new ImageView(imageUrl);
            image.setCache(true);
            image.setFitHeight(300.0);
            image.setFitWidth(350.0);
            image.setLayoutX(130.0);
            image.setLayoutY(82.0);
            image.setPickOnBounds(true);
            image.setPreserveRatio(true);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(4);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.initStyle(StageStyle.UNDECORATED);
        stage.centerOnScreen();
        stage.setAlwaysOnTop(true);
        stage.setScene(createPreloaderScene());
        stage.setResizable(false);
        stage.setOnCloseRequest(_ -> System.exit(0));
        stage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification scn) {
        if (scn.getType() == StateChangeNotification.Type.BEFORE_START) {
            stage.hide();
        }
    }

}
