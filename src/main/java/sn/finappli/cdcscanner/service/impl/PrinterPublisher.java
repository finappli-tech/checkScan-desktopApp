package sn.finappli.cdcscanner.service.impl;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class PrinterPublisher implements Flow.Publisher<List<PrintService>> {
    private final SubmissionPublisher<List<PrintService>> publisher = new SubmissionPublisher<>();

    public void notifyPrintersChanged() {
        List<PrintService> printers = Arrays.asList(PrintServiceLookup.lookupPrintServices(null, null));
        publisher.submit(printers);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super List<PrintService>> subscriber) {
        publisher.subscribe(subscriber);
    }
}
