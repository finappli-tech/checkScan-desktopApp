package sn.finappli.cdcscanner.controller;

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
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.CDCScannerApplication;
import sn.finappli.cdcscanner.model.input.ScanInputModel;
import sn.finappli.cdcscanner.model.input.ScanInputPaged;
import sn.finappli.cdcscanner.security.SecurityContextHolder;
import sn.finappli.cdcscanner.service.ItemsService;
import sn.finappli.cdcscanner.service.impl.ScannedItemsServiceImpl;
import sn.finappli.cdcscanner.utility.SystemUtils;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static sn.finappli.cdcscanner.utility.Utils.getDefaultCss;

@Getter
@Setter
@NoArgsConstructor
public class HomeController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);
    private final ItemsService itemsService = new ScannedItemsServiceImpl();

    @FXML
    private TableView<ScanInputModel> historyTable;
    @FXML
    private TableColumn<ScanInputModel, String> creationColumn;
    @FXML
    private TableColumn<ScanInputModel, String> dateColumn;
    @FXML
    private TableColumn<ScanInputModel, String> cmcColumn;
    @FXML
    private TableColumn<ScanInputModel, String> recipientColumn;
    @FXML
    private TableColumn<ScanInputModel, String> amountColumn;
    @FXML
    private TableColumn<ScanInputModel, String> statusColumn;

    @FXML
    private Pagination pagination;
    @FXML
    private Button paramButton;
    @FXML
    private Button newScanButton;
    @FXML
    private Button refreshButton;
    @FXML
    private ImageView imageView;
    @FXML
    private ProgressIndicator loader;
    @FXML
    private VBox container;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Utils.configureRefreshButton(refreshButton);
        refreshButton.setOnMouseClicked(_ -> refreshTable());

        pagination.currentPageIndexProperty().addListener((_, _, _) -> refreshTable());

        historyTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        historyTable.getSelectionModel().selectedItemProperty().addListener((_, _, newSelection) -> {
            if (Objects.isNull(newSelection)) return;
            System.out.println(newSelection);
        });
        refreshButton.fire();
    }

    @FXML
    void refreshTable() {
        var sec = SecurityContextHolder.getContext();
        var task = new Task<ScanInputPaged>() {
            @Override
            protected ScanInputPaged call() {
                SystemUtils.loadAppConfig();
                SecurityContextHolder.setContext(sec);
                return itemsService.listScannedItems(pagination.getCurrentPageIndex());
            }
        };
        task.setOnRunning(_ -> {
            container.setDisable(true);
            loader.setVisible(true);
        });
        task.setOnSucceeded(event -> {
            var page = (ScanInputPaged) event.getSource().getValue();
            historyTable.setItems(FXCollections.observableList(page.getOperations()));
            pagination.setPageCount(page.getTotalPages());
            container.setDisable(false);
            loader.setVisible(false);
        });
        task.setOnFailed(_ -> {
            historyTable.setItems(FXCollections.emptyObservableList());
            container.setDisable(false);
            loader.setVisible(false);
        });
        new Thread(task).start();
    }

    @FXML
    void toggleParameters(ActionEvent __) {

    }

    @FXML
    void launchNewScan(@NotNull ActionEvent event) {
        try {
            var stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(CDCScannerApplication.class.getResource("scanner.fxml")));
            var scene = new Scene(root, 900, 600);
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
