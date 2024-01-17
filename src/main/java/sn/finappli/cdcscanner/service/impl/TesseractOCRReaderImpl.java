package sn.finappli.cdcscanner.service.impl;

import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.service.OCRReader;

import java.io.File;
import java.util.Arrays;

public class TesseractOCRReaderImpl implements OCRReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TesseractOCRReaderImpl.class);

    private static final String PATTERN = "\\d{7}";

    @Contract("_ -> !null")
    private static String extractCheckNumber(@NotNull String text) {
        if (StringUtils.isBlank(text)) return "";
        return Arrays.stream(text.split("\\s")).filter(r -> r.matches(PATTERN)).findFirst().orElse("");
    }

    @Override
    public String read(File file) {
        var tesseract = new Tesseract();
        tesseract.setDatapath(System.getenv("tessdata"));
        tesseract.setLanguage("fra");

        try {
            var result = tesseract.doOCR(file);
            return extractCheckNumber(result);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return "";
        }
    }
}
