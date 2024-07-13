import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String CRLF = "\r\n";
    private static final String STATUS_OK = "200 OK";
    private static final String STATUS_NOT_FOUND = "404 Not Found";
    private static final String ECHO_PATH = "/echo/";
    private static final String USER_AGENT_PATH = "/user-agent";
    private static final String HTTP_PROTOCOL_VERSION = "HTTP/1.1";
    private static final String USER_AGENT_HEADER_PREFIX = "User-Agent: ";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    private static final int socketNumber = 10;

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        try {
            ServerSocket serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            for(int i = 0; i < socketNumber; i++) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String line = bufferedReader.readLine();
                String[] httpRequest = line.split(" ", 0);
                // httpRequest[0] : HTTP Method
                // httpRequest[1] : Path
                // httpRequest[2] : HTTP Options
                List<String> headers = new ArrayList<>();
                while (true) {
                    line = bufferedReader.readLine();
                    if (line == null || line.isBlank()) {
                        break;
                    }
                    headers.add(line);
                }
                String response;
                if (httpRequest[1].equals("/") || httpRequest[1].isBlank()) {
                    response = buildResponseStatus(STATUS_OK) + buildEmptyResponseHeaders() + buildEmptyResponseBody();
                } else if (httpRequest[1].startsWith(ECHO_PATH)) {
                    String textToEcho = httpRequest[1].substring(httpRequest[1].lastIndexOf(ECHO_PATH) + ECHO_PATH.length());
                    response = buildResponseStatus(STATUS_OK) + buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, textToEcho.length()) + buildResponseBody(textToEcho);
                } else if (httpRequest[1].equals(USER_AGENT_PATH)) {
                    String headerToPrintInBody = headers.stream().anyMatch(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)) ? headers.stream().filter(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)).findAny().get().substring(USER_AGENT_HEADER_PREFIX.length()) : "";
                    response = buildResponseStatus(STATUS_OK) + buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, headerToPrintInBody.length()) + buildResponseBody(headerToPrintInBody);
                } else {
                    response = buildResponseStatus(STATUS_NOT_FOUND) + buildEmptyResponseHeaders() + buildEmptyResponseBody();
                }
                System.out.println("accepted new connection");
                clientSocket.getOutputStream().write(response.getBytes());
                System.out.println("wrote response : " + response);
                clientSocket.close();
                System.out.println("closed new connection");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static String buildResponseStatus(String status) {
        return HTTP_PROTOCOL_VERSION + " " + status + CRLF;
    }

    private static String buildEmptyResponseHeaders() {
        return CRLF;
    }

    private static String buildResponseHeaders(String contentType, Integer contentLength) {
        if (contentType == null || contentLength == null) {
            return CRLF;
        }
        String contentTypeHeader = "Content-Type: " + contentType + CRLF;
        String contentLengthHeader = "Content-Length: " + contentLength + CRLF;
        return contentTypeHeader + contentLengthHeader + CRLF;
    }

    private static String buildEmptyResponseBody() {
        return "";
    }

    private static String buildResponseBody(String body) {
        return body;
    }
}
