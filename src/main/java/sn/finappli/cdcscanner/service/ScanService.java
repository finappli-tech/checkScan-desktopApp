package sn.finappli.cdcscanner.service;

import sn.finappli.cdcscanner.model.input.ScanInputPaged;
import sn.finappli.cdcscanner.model.input.ServerResponse;
import sn.finappli.cdcscanner.model.output.ScanRegistrationOutput;

public interface ScanService {

    ScanInputPaged listScannedItems(int page);

    ServerResponse sendScan(ScanRegistrationOutput output);
}
