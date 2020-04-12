package org.libraw.javax.imageio.spi;

import org.libraw.javax.imageio.LibRawImageReader;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;

public class LibRawImageReaderSpi extends ImageReaderSpi {
    // For accessing the path from a FileImageInputStream
    private static final Field fileImageInputStreamRafField;
    private static final Field randomAccessFilePathField;
    // For accessing the path from a FileCacheImageInputStream/MemoryCacheImageInputStream
    private static final Field fileCachedImageInputStreamStreamField;
    private static final Field memoryCachedImageInputStreamStreamField;
    private static final Field fileInputStreamPathField;
    static {
        System.loadLibrary("jlibraw");

        try {
            fileImageInputStreamRafField =
                FileImageInputStream.class.getDeclaredField("raf");
            fileImageInputStreamRafField.setAccessible(true);

            randomAccessFilePathField =
                RandomAccessFile.class.getDeclaredField("path");
            randomAccessFilePathField.setAccessible(true);

            fileCachedImageInputStreamStreamField =
                FileCacheImageInputStream.class.getDeclaredField("stream");
            fileCachedImageInputStreamStreamField.setAccessible(true);

            memoryCachedImageInputStreamStreamField =
                MemoryCacheImageInputStream.class.getDeclaredField("stream");
            memoryCachedImageInputStreamStreamField.setAccessible(true);

            fileInputStreamPathField =
                FileInputStream.class.getDeclaredField("path");
            fileInputStreamPathField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String reflectionExtractPath(ImageInputStream iis)
            throws IllegalAccessException {
        if (iis instanceof FileImageInputStream) {
            final RandomAccessFile raf =
                (RandomAccessFile)fileImageInputStreamRafField.get(iis);
            return (String)randomAccessFilePathField.get(raf);
        } else if (iis instanceof FileCacheImageInputStream ||
                iis instanceof MemoryCacheImageInputStream) {
            final InputStream is;
            if (iis instanceof FileCacheImageInputStream) {
                is = (InputStream)
                    fileCachedImageInputStreamStreamField.get(iis);
            } else {
                is = (InputStream)
                    memoryCachedImageInputStreamStreamField.get(iis);
            }

            if (is instanceof FileInputStream) {
                return (String)fileInputStreamPathField.get(is);
            } else {
                throw new IllegalArgumentException(
                    "underlying stream type (" + is.getClass() +
                    ") of InputStreamImageInputStream is not supported"
                );
            }
        } else {
            throw new IllegalArgumentException(
                "input type (" + iis.getClass() + ") is not supported"
            );
        }
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
        if (source instanceof FileImageInputStream ||
                source instanceof FileCacheImageInputStream ||
                source instanceof MemoryCacheImageInputStream) {
            try {
                final String path =
                    reflectionExtractPath((ImageInputStream)source);
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
