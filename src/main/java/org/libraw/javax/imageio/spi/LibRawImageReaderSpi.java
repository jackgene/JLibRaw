package org.libraw.javax.imageio.spi;

import org.libraw.javax.imageio.LibRawImageReader;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;

public class LibRawImageReaderSpi extends ImageReaderSpi {
    private static final Field fileImageInputStreamRafField;
    private static final Field randomAccessFilePathField;
    static {
        System.loadLibrary("jlibraw");

        try {
            fileImageInputStreamRafField =
                FileImageInputStream.class.getDeclaredField("raf");
            fileImageInputStreamRafField.setAccessible(true);

            randomAccessFilePathField =
                RandomAccessFile.class.getDeclaredField("path");
            randomAccessFilePathField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String reflectionExtractPath(FileImageInputStream stream)
            throws IllegalAccessException {
        final RandomAccessFile raf =
            (RandomAccessFile)fileImageInputStreamRafField.get(stream);
        return (String)randomAccessFilePathField.get(raf);
    }

    public LibRawImageReaderSpi() {
        super(
            "LibRaw.org",
            "0.1",
            // TODO add everything here? https://stackoverflow.com/questions/43473056/which-mime-type-should-be-used-for-a-raw-image
            new String[] {
                "ARW", "arw",
                "CR2", "cr2",
                "CRW", "crw",
                "DNG", "dng",
                "NEF", "nef"
            },
            new String[] {"arw", "cr2", "crw", "dng", "nef"},
            new String[] {
                "image/x-sonyarw",
                "image/x-canon-cr2",
                "image/x-adobe-dng",
                "image/x-nikon-nef"
            },
            "org.libraw.javax.imageio.LibRawImageReader",
            new Class[] { ImageInputStream.class, File.class },
            null,
            false, // supportsStandardStreamMetadataFormat
            "WHATEVER", // TODO
            "WHATEVER", // TODO
            null, null,
            false, //  supportsStandardImageMetadataFormat
            "WHATEVER", // TODO
            "WHATEVER", // TODO
            null, null
        );
    }

    @Override
    public boolean canDecodeInput(Object source) /*throws IOException*/ {
        if (source instanceof FileImageInputStream) {
            try {
                final String path =
                    reflectionExtractPath((FileImageInputStream)source);
                final String suffix =
                    path.substring(path.lastIndexOf('.') + 1).toLowerCase();

                return Arrays.binarySearch(suffixes, suffix) >= 0;
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError();
            }
        } else {
            return false;
        }
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new LibRawImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "LibRaw-base RAW Image Reader ";
    }
}
