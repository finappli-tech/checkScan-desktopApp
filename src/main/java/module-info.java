module sn.finappli.cdcscanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.apache.logging.log4j;
    requires org.jetbrains.annotations;
    requires java.net.http;

    opens sn.finappli.cdcscanner to javafx.fxml;
    exports sn.finappli.cdcscanner;
    exports sn.finappli.cdcscanner.controller;
    opens sn.finappli.cdcscanner.controller to javafx.fxml;
}