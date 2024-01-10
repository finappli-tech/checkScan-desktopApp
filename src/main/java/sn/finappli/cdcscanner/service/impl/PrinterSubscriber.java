package sn.finappli.cdcscanner.service.impl;

import javax.print.PrintService;
import java.util.List;
import java.util.concurrent.Flow;

public class PrinterSubscriber implements Flow.Subscriber<List<PrintService>> {
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE); // Request all available items
    }

    @Override
    public void onNext(List<PrintService> printers) {
        System.out.println("Printers changed. New list:");
        for (PrintService printer : printers) {
            System.out.println(printer.getName());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        System.out.println("Subscription completed.");
    }
}
