import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GZIPCompression {

    public static byte[] compress(final String str) throws IOException {
        if (str == null || str.isEmpty()) {
            return null;
        }
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(str.getBytes(UTF_8));
        return obj.toByteArray();
    }
}
