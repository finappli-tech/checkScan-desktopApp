package sn.finappli.cdcscanner.service.impl;

import com.dynarithmic.twain.DTwainConstants;
import com.dynarithmic.twain.DTwainGlobalOptions;
import com.dynarithmic.twain.exceptions.DTwainJavaAPIException;
import com.dynarithmic.twain.highlevel.TwainSession;
import com.dynarithmic.twain.highlevel.TwainSource;
import com.dynarithmic.twain.highlevel.TwainSourceDialog;
import com.dynarithmic.twain.highlevel.TwainSourceInfo;
import com.dynarithmic.twain.highlevel.acquirecharacteristics.AcquireCharacteristics;
import com.dynarithmic.twain.lowlevel.TwainConstants;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.finappli.cdcscanner.utility.ConfigHolder;
import sn.finappli.cdcscanner.utility.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DTwainServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(DTwainServiceImpl.class);
    private static final Map<String, String> ERROR_DESCRIPTOR = new HashMap<>();
    private static TwainSession session;

    static {
        try {
            initTwainSession();
            loadErrorDescriptions();
        } catch (DTwainJavaAPIException | IOException e) {
            if (e instanceof DTwainJavaAPIException) LOGGER.error("Error initializing DTwain");
            else LOGGER.error("Error while reading error file");
            LOGGER.error(e.getMessage(), e);
            System.exit(-1000);        }
    }

    private static void initTwainSession() throws DTwainJavaAPIException {
        if (System.getProperty("sun.arch.data.model").equals("32")) {
            DTwainGlobalOptions.setJNIVersion(DTwainConstants.JNIVersion.JNI_32U);
        } else {
            DTwainGlobalOptions.setJNIVersion(DTwainConstants.JNIVersion.JNI_64U);
        }
        session = new TwainSession(DTwainConstants.SessionStartupMode.NONE);
        session.setDSM(DTwainConstants.DSMType.LATESTVERSION);
        session.setLanguageResource("french");
    }

    private static void loadErrorDescriptions() throws IOException {
        var path = ConfigHolder.getContext().getDtwainErrorDescriptorPath();
        try (var inputStream = DTwainServiceImpl.class.getResourceAsStream(path)) {
            if (inputStream != null) {
                var lines = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)).lines().toList();

                for (var line : lines) {
                    var firstSpace = line.indexOf(' ');
                    if (firstSpace > 0 && firstSpace < line.length() - 1) {
                        var key = StringUtils.trimToEmpty(line.substring(0, firstSpace));
                        var value = StringUtils.trimToEmpty(line.substring(firstSpace + 1));
                        if (!StringUtils.isAnyBlank(key, value)) ERROR_DESCRIPTOR.put(key, value);
                    }
                }
                ERROR_DESCRIPTOR.put("NO_ERROR_MATCHER", "Une erreur s'est produite. Veuillez contacter votre fournisseur.");
            } else {
                LOGGER.error("Error file not found: {}", path);
            }
        }
    }

    public TwainSource.AcquireReturnInfo launch() {
        try {
            session.start();

            setAppInformations();
            logTwainInformation();

            var selectorOptions = new TwainSourceDialog().enableEnhancedDialog(true)
                    .center(true)
                    .sortNames(true)
                    .topmostWindow(true)
                    .topmostWindow(true)
                    .setTitle("Sélectionner votre scanner/imprimante");
            var twainSource = session.selectSource(selectorOptions);
            if (twainSource.isOpened()) {
                logTwainSourceDetails(twainSource);
                configureDuplexMode(twainSource);
                configureFileOptions(twainSource.getAcquireCharacteristics());
                var retInfo = twainSource.acquire();

                if (retInfo.getReturnCode() == DTwainConstants.ErrorCode.ERROR_NONE) {
                    LOGGER.error("Acquisition process started and ended successfully");
                    Utils.displaySimpleSuccessfulAlertDialog("Twain", "Scanner " + twainSource.getInfo().getProductName(), getErrorDescription(DTwainConstants.ErrorCode.ERROR_SOURCESELECTION_CANCELED));
                    return retInfo;
                } else {
                    var desc = getErrorDescription(retInfo.getReturnCode());
                    LOGGER.error("Acquisition process failed with error: {} {} {}", retInfo.getReturnCode().name(), retInfo.getReturnCode().value(), desc);
                    Utils.displaySimpleErrorAlertDialog(getErrorDescription(retInfo.getReturnCode()), "Twain Acquisition");
                }
            } else {
                var err = twainSource.getLastError();
                if (err == DTwainConstants.ErrorCode.ERROR_SOURCESELECTION_CANCELED) {
                    LOGGER.info("User closed the TWAIN dialog without selecting a data source");
                    Utils.displaySimpleSuccessfulAlertDialog("Twain", "Scanner " + twainSource.getInfo().getProductName(), getErrorDescription(DTwainConstants.ErrorCode.ERROR_SOURCESELECTION_CANCELED));
                    return null;
                } else {
                    var desc = getErrorDescription(err);
                    LOGGER.info("Source selection failed with error {} {} {}", err.name(), err.value(), desc);
                    Utils.displaySimpleErrorAlertDialog(getErrorDescription(err), "Twain Acquisition");
                }
            }
            twainSource.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                if (session != null && session.isStarted())
                    session.stop();
            } catch (DTwainJavaAPIException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private void configureDuplexMode(@NotNull TwainSource twainSource) {
        if (twainSource.getCapabilityInterface().isDuplexSupported()) {
            LOGGER.info("Duplex is supported for the device {}", twainSource.getInfo().getProductName());
            twainSource.getAcquireCharacteristics().getPaperHandlingOptions().enableDuplex(true);
        } else LOGGER.info("Duplex is not supported for the device {}", twainSource.getInfo().getProductName());
    }

    private void configureFileOptions(@NotNull AcquireCharacteristics characteristics) {
        characteristics.getCompressionSupportOptions().setCompression(TwainConstants.ICAP_COMPRESSION.TWCP_JPEG);
        characteristics.getGeneralOptions().setMaxPageCount(2);
        var fileOptions = characteristics.getFileTransferOptions();
        fileOptions.setType(DTwainConstants.FileType.JPEG);
        fileOptions.enableMultiPage(fileOptions.canMultiPage());
        fileOptions.setName(ConfigHolder.getContext().getScanStoragePath().concat("print_00001.jpeg"));
        fileOptions.getFilenameIncrementOptions().enable(true);
        fileOptions.getFilenameIncrementOptions().setIncrementValue(1);
        fileOptions.enableAutoCreateDirectory(true);
    }

    private void setAppInformations() {
        var appInfo = session.getAppInfo();
        appInfo.setManufacturer("My Java Manufacturer");
        appInfo.setProductName("My Java Product Name");
        appInfo.setVersionInfo("My Java Version Info");
        appInfo.setProductFamily("My Java Product Family");
    }

    private void logTwainInformation() {
        // Get info concerning this TWAIN session
        LOGGER.info("Short Version Info: {}", session.getShortVersionName());
        LOGGER.info("Long Version Info: {}", session.getLongVersionName());
        LOGGER.info("Library Path: {}", session.getDTwainPath());
        LOGGER.info("TWAIN DSM Path in use: {}", session.getDSMPath());
        LOGGER.info("TWAIN Version and Copyright: {}", session.getVersionCopyright());

        if (LOGGER.isDebugEnabled()) {
            // Get information on the installed TWAIN sources
            LOGGER.info("Available TWAIN Sources:");
            for (TwainSourceInfo oneInfo : session.getAllSourceInfo())
                LOGGER.info("   Product Name: {}", oneInfo.getProductName());
        }
    }

    private void logTwainSourceDetails(TwainSource twainSource) {
        String sourceName = twainSource.getInfo().getProductName();
        String sourceDetails = session.getSourceDetails(sourceName, 2, true);
        LOGGER.info("Here are the details of the selected TWAIN Source: {}", sourceName);
        LOGGER.info(sourceDetails);
    }

    private String getErrorDescription(DTwainConstants.@NotNull ErrorCode code) {
        final String value;
        if (code.value() < 0) value = String.valueOf(code.value() * -1);
        else value = String.valueOf(code.value());
        return ERROR_DESCRIPTOR.getOrDefault(value, ERROR_DESCRIPTOR.get("NO_ERROR_MATCHER"));
    }
}
