package sn.finappli.cdcscanner.service;

import java.io.File;

@FunctionalInterface
public interface OCRReader {
    String read(File file);
}
