package sn.finappli.cdcscanner;

/**
 * Main class for executing the CDC Scanner application.
 * <p>
 * This class is created to allow the creation of an executable JAR since Java does not support the extension
 * of the abstract class {@link javafx.application.Application}. Its sole purpose is to call the main method
 * in the {@link sn.finappli.cdcscanner.CDCScannerApplication}.
 * </p>
 */
public class CDCScannerApplicationMain {

    /**
     * Entry point for executing the CDC Scanner application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        CDCScannerApplication.main(args);
    }
}
