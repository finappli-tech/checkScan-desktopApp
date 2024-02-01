package sn.finappli.cdcscanner.service;

import sn.finappli.cdcscanner.model.input.FolderReaderResult;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
public interface FolderReaderService {

    List<FolderReaderResult> readScanFolder() throws IOException;
}
