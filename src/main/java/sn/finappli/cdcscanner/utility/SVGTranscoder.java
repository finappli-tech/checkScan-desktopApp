package sn.finappli.cdcscanner.utility;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import lombok.Getter;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Utility class for transcoding SVG images to JavaFX ImageViews.
 * This class uses the Apache Batik library for SVG transcoding.
 * It provides a method to transcode an SVG file and return the result as a JavaFX ImageView.
 *
 * <p>The class is designed as a utility and cannot be instantiated.</p>
 *
 * @implNote This utility class uses the Apache Batik library for SVG transcoding.
 * @see Logger
 * @see LoggerFactory
 * @see ImageView
 * @see TranscoderInput
 * @see TranscoderOutput
 * @see TranscoderException
 * @see ImageTranscoder
 * @see IOException
 * @see BufferedImage
 * @see SwingFXUtils
 */
public final class SVGTranscoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SVGTranscoder.class);

    private SVGTranscoder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Transcodes an SVG file and returns the result as a JavaFX ImageView.
     *
     * @param path The path to the SVG file.
     * @return A JavaFX ImageView containing the transcoded image.
     * @throws IOException          If an I/O error occurs during transcoding.
     * @throws NullPointerException If the specified path is null.
     */
    public static ImageView transcodeSVG(String path) throws IOException, NullPointerException {
        var image = new ImageView();
        var transcoder = new BufferedImageTranscoder();
        try (var file = BufferedImageTranscoder.class.getResourceAsStream(path)) {
            var transIn = new TranscoderInput(file);
            try {
                transcoder.transcode(transIn, null);
                var img = SwingFXUtils.toFXImage(transcoder.getImg(), null);
                image.setImage(img);
            } catch (TranscoderException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            return image;
        }
    }

    /**
     * Inner class for extending the Batik ImageTranscoder to retrieve the BufferedImage result.
     */
    private static final class BufferedImageTranscoder extends ImageTranscoder {

        private @Getter BufferedImage img = null;

        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput to) {
            this.img = img;
        }

    }
}
