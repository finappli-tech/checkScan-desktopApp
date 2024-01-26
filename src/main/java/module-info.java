module sn.finappli.cdcscanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
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
    requires org.yaml.snakeyaml;
    requires batik.transcoder;
    requires javafx.swing;
    requires bcrypt;
    requires com.sun.jna;

    opens sn.finappli.cdcscanner to javafx.fxml;
    exports sn.finappli.cdcscanner;
    exports sn.finappli.cdcscanner.controller;
    exports sn.finappli.cdcscanner.service;
    exports sn.finappli.cdcscanner.service.impl;
    exports sn.finappli.cdcscanner.model.input;
    exports sn.finappli.cdcscanner.model.output;
    exports sn.finappli.cdcscanner.utility;
    exports sn.finappli.cdcscanner.security;
    opens sn.finappli.cdcscanner.controller to javafx.fxml;
}