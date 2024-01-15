package sn.finappli.cdcscanner.utility;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class SVGTranscoder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SVGTranscoder.class);

    private SVGTranscoder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ImageView transcodeSVG(String path) throws IOException {
        var image = new ImageView();
        var transcoder = new BufferedImageTranscoder();
        try (var file = BufferedImageTranscoder.class.getResourceAsStream(path)) {
            var transIn = new TranscoderInput(file);
            try {
                transcoder.transcode(transIn, null);
                var img = SwingFXUtils.toFXImage(transcoder.getBufferedImage(), null);
                image.setImage(img);
            } catch (TranscoderException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            return image;
        }
    }
}
