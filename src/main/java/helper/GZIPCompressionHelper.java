package helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GZIPCompressionHelper {

    public static byte[] compress(final String str) throws IOException {
        if (str == null || str.isEmpty()) {
            return null;
        }
        byte[] dataToCompress = str.getBytes(UTF_8);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(dataToCompress.length);
        GZIPOutputStream gzip = new GZIPOutputStream(byteStream);
        gzip.write(dataToCompress);
        gzip.close();
        byteStream.close();
        return byteStream.toByteArray();
    }
}
