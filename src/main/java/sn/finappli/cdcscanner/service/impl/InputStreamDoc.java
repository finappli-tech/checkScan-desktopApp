package sn.finappli.cdcscanner.service.impl;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.attribute.DocAttributeSet;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class InputStreamDoc implements Doc, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InputStreamDoc.class);
    private final String filename;
    @Getter
    private final DocFlavor docFlavor;
    private InputStream stream;

    public InputStreamDoc(String name, DocFlavor flavor) {
        filename = name;
        docFlavor = flavor;
    }

    /* No attributes attached to this Doc - mainly useful for MultiDoc */
    public DocAttributeSet getAttributes() {
        return null;
    }

    /* Since the data have been  supplied as an InputStream delegate to
     * getStreamForBytes().
     */
    public Object getPrintData() throws IOException {
        return getStreamForBytes();
    }

    /* Not possible to return a GIF as text */
    public Reader getReaderForText() throws IOException {
        return null;
    }

    /* Return the print data as an InputStream.
     * Always return the same instance.
     */
    public InputStream getStreamForBytes() throws IOException {
        synchronized (this) {
            if (stream == null) {
                stream = new FileInputStream(filename);
            }
            return stream;
        }
    }

    @Override
    public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
