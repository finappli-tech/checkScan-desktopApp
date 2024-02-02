package sn.finappli.cdcscanner.service;

import sn.finappli.cdcscanner.model.input.ScanInputPaged;
import sn.finappli.cdcscanner.model.input.ServerResponse;
import sn.finappli.cdcscanner.model.output.ChecksRegistrationOutput;

import java.util.List;

public interface ScanService {

    ScanInputPaged listScannedItems(int page);

    ServerResponse saveChecks(ChecksRegistrationOutput output);

    ServerResponse revertSave(List<String> list);
}
