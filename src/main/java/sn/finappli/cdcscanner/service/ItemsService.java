package sn.finappli.cdcscanner.service;

import sn.finappli.cdcscanner.model.input.ScanInputPaged;

public interface ItemsService {

    ScanInputPaged listScannedItems(int page);
}
