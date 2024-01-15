package sn.finappli.cdcscanner.service.impl;

import sn.finappli.cdcscanner.service.PrinterService;

import javax.print.PrintService;
import java.util.concurrent.TimeUnit;

public class PrinterServiceImpl implements PrinterService {

    @Override
    public void listAllConnectedPrinters() throws InterruptedException {
        PrinterPublisher printerPublisher = new PrinterPublisher();
        PrinterSubscriber printerSubscriber = new PrinterSubscriber();

        // Subscribe the subscriber to the publisher
        printerPublisher.subscribe(printerSubscriber);

        // Simulate a new printer being added while the app is running
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5); // Wait for 5 seconds
                // Add a new printer
                PrintService newPrinter = new SimulatedPrinter("New Printer");
                printerPublisher.notifyPrintersChanged();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // Sleep to keep the main thread alive for a while
        TimeUnit.SECONDS.sleep(10);
    }
}

