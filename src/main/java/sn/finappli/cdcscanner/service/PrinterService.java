package sn.finappli.cdcscanner.service;

import javafx.collections.ObservableList;

import javax.print.PrintService;

public interface PrinterService {

    ObservableList<PrintService> listPrinters();
}
