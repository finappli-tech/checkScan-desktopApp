module sn.finappli.cdcscanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.jetbrains.annotations;
    requires tess4j;
    requires commons.lang3;
    requires lombok;
    requires org.slf4j;
    requires java.net.http;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.module.paramnames;

    opens sn.finappli.cdcscanner to javafx.fxml;
    exports sn.finappli.cdcscanner;
    exports sn.finappli.cdcscanner.controller;
    exports sn.finappli.cdcscanner.service;
    exports sn.finappli.cdcscanner.model.input;
    exports sn.finappli.cdcscanner.model.output;
    opens sn.finappli.cdcscanner.controller to javafx.fxml;
}