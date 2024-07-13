package helper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static constants.HttpConstants.*;
import static constants.HttpConstants.CRLF;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RequestHelper {

    public static String buildResponseStatus(String status) {
        return HTTP_PROTOCOL_VERSION + " " + status + CRLF;
    }

    public static String buildEmptyResponseHeaders() {
        return CRLF;
    }

    public static boolean validateEncoding(String encoding) {
        List<String> encodingOptions = Arrays.stream(encoding.split(", ", 0)).toList();
        return encodingOptions.contains(SUPPORTED_ENCODING_OPTIONS);
    }

    public static String buildResponseHeaders(String contentType, boolean handleEncoding, Integer contentLength) {
        if (contentType == null || contentLength == null) {
            return CRLF;
        }
        String contentTypeHeader = "Content-Type: " + contentType + CRLF;
        String contentEncodingHeader = handleEncoding ? "Content-Encoding: " + SUPPORTED_ENCODING_OPTIONS + CRLF : "";
        String contentLengthHeader = "Content-Length: " + contentLength + CRLF;
        return contentEncodingHeader +  contentTypeHeader + contentLengthHeader + CRLF;
    }

    public static String buildEmptyResponseBody() {
        return "";
    }

    public static String buildResponseBody(String body) {
        return body;
    }

    public static byte[] buildResponseBodyBytesHandlingEncoding(String body, boolean encodedBody) {
        if (!encodedBody) {
            return body.getBytes(UTF_8);
        } else {
            try {
                return GZIPCompressionHelper.compress(body);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
