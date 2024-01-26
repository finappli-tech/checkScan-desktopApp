package sn.finappli.cdcscanner.service.impl;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sn.finappli.cdcscanner.service.PrinterService;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.Sides;
import java.util.Arrays;
import java.util.List;

import static javax.print.attribute.ResolutionSyntax.DPI;

public class PrinterServiceImpl implements PrinterService {
    public static final DocFlavor flavor = DocFlavor.INPUT_STREAM.JPEG;
    public static final PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();

    static {
//        attributeSet.add(new Copies(1));
//        attributeSet.add(MediaSizeName.ISO_A4);
        attributeSet.add(MediaSize.findMedia(1392, 631, Size2DSyntax.MM));
//        attributeSet.add(Sides.ONE_SIDED);
//        attributeSet.add(PrintQuality.NORMAL);
//        attributeSet.add(new PrinterResolution(200, 200, DPI));
    }

    @Override
    public ObservableList<PrintService> listPrinters() {
        List<PrintService> printers = Arrays.asList(PrintServiceLookup.lookupPrintServices(null, attributeSet));
        return FXCollections.observableArrayList(printers);
    }

}

