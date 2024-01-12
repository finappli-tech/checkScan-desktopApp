package sn.finappli.cdcscanner.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.PrintService;
import java.util.List;
import java.util.concurrent.Flow;

public class PrinterSubscriber implements Flow.Subscriber<List<PrintService>> {
    
    private static final Logger log = LoggerFactory.getLogger(PrinterSubscriber.class);

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE); // Request all available items
    }

    @Override
    public void onNext(List<PrintService> printers) {
        log.info("Printers changed. New list:");
        for (PrintService printer : printers) {
            log.info(printer.getName());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
    }

    @Override
    public void onComplete() {
        log.info("Subscription completed.");
    }
}
