package org.libraw.javax.imageio;

import org.libraw.javax.imageio.spi.LibRawImageReaderSpi;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

public class LibRawImageReader extends ImageReader {
    // Most of these are assigned from JNI
    private int width, height, colors, bits,
            pixelStride, scanlineStride, bandOffsets[];
    private byte[] data;
    private Object initializedInput;

    /**
     * Constructs an <code>ImageReader</code> and sets its
     * <code>originatingProvider</code> field to the supplied value.
     * <p>
     * <p> Subclasses that make use of extensions should provide a
     * constructor with signature <code>(ImageReaderSpi,
     * Object)</code> in order to retrieve the extension object.  If
     * the extension object is unsuitable, an
     * <code>IllegalArgumentException</code> should be thrown.
     *
     * @param originatingProvider the <code>ImageReaderSpi</code> that is
     *                            invoking this constructor, or <code>null</code>.
     */
    public LibRawImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    /**
     * Populates the type, width, height, colors, bits and data fields
     * from the file at the given path.
     *
     * TODO Document exit codes
     *
     * @param filePath the path of the file to load from.
     * @return 0 if successful; the documented error code otherwise.
     */
    private native int loadImageFromFilePath(String filePath);


    /**
     * Populates the type, width, height, colors, bits and data fields
     * from the given {@link ImageInputStream}.
     *
     * TODO Document exit codes
     *
     * @param stream the stream to load from.
     * @return 0 if successful; the documented error code otherwise.
     */
    private native int loadImageFromImageInputStream(ImageInputStream stream);

    private void initializeIfNecessary() throws IOException {
        if (!input.equals(initializedInput)) {
            int nativeErrorCode;

            if (input instanceof File) {
                final File inputFile = (File)input;
                if (!inputFile.canRead()) throw new FileNotFoundException();
                nativeErrorCode =
                    loadImageFromFilePath(inputFile.getCanonicalPath());
            } else if (input instanceof FileImageInputStream) {
                try {
                    nativeErrorCode =
                        loadImageFromFilePath(
                            LibRawImageReaderSpi.reflectionExtractPath(
                                (FileImageInputStream)input
                            )
                        );
                } catch (IllegalAccessException e) {
                    nativeErrorCode =
                        loadImageFromImageInputStream((ImageInputStream)input);
                }
            } else if (input instanceof ImageInputStream) {
                nativeErrorCode =
                    loadImageFromImageInputStream((ImageInputStream)input);
            } else {
                throw new IllegalStateException("Unsupported input type");
            }
            switch (nativeErrorCode) {
                case 0:
                    break;
                case -100009:
                    throw new IOException("LibRaw native I/O error");
                default:
                    throw new IIOException(
                        "Unexpected error: " + nativeErrorCode
                    );
            }
            pixelStride = colors * bits / 8;
            scanlineStride = pixelStride * width;
            bandOffsets = new int[colors];
            for (int i = 0; i < colors; i ++) {
                bandOffsets[i] = i * bits / 8;
            }
            initializedInput = input;
        }
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        if (input == null) {
            throw new IllegalStateException(
                "the input source has not been set"
            );
        }
        if (seekForwardOnly) {
            throw new IllegalStateException("seekForwardOnly");
        }
        initializeIfNecessary();
        return 1;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        if (input == null) {
            throw new IllegalStateException(
                "the input source has not been set"
            );
        }
        initializeIfNecessary();
        if (imageIndex != 0) throw new IndexOutOfBoundsException();

        return width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        if (input == null) {
            throw new IllegalStateException(
                "the input source has not been set"
            );
        }
        initializeIfNecessary();
        if (imageIndex != 0) throw new IndexOutOfBoundsException();

        return height;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
        throw new UnsupportedOperationException("pending");
    }

    @Override
    public IIOMetadata getStreamMetadata() /*throws IOException*/ {
        throw new UnsupportedOperationException("pending");
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) {
        throw new UnsupportedOperationException("pending");
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param)
            throws IOException {
        if (input == null) {
            throw new IllegalStateException(
                "the input source has not been set"
            );
        }
        initializeIfNecessary();
        if (imageIndex != 0) throw new IndexOutOfBoundsException();

        return new BufferedImage(
            new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB),
                false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
            ),
            Raster.createWritableRaster(
                new PixelInterleavedSampleModel(
                    DataBuffer.TYPE_BYTE,
                    width, height, pixelStride, scanlineStride, bandOffsets
                ),
                new DataBufferByte(data, data.length),
                new Point()
            ),
            /* isRasterPremultiplied = */ false,
            /* properties = */ null
        );
    }
}
