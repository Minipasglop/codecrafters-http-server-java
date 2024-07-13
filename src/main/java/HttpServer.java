import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {

    private final String CRLF = "\r\n";
    private final String STATUS_OK = "200 OK";
    private final String STATUS_NOT_FOUND = "404 Not Found";
    private final String ECHO_PATH = "/echo/";
    private final String USER_AGENT_PATH = "/user-agent";
    private final String HTTP_PROTOCOL_VERSION = "HTTP/1.1";
    private final String USER_AGENT_HEADER_PREFIX = "User-Agent: ";
    private final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    private final int port;

    private final ExecutorService executorService;

    public HttpServer(int port, final int socketAmount) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(socketAmount);
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleRequest(clientSocket));
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    private void handleRequest(Socket clientSocket) {
        try {
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
            clientSocket.getOutputStream().flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private String buildResponseStatus(String status) {
        return HTTP_PROTOCOL_VERSION + " " + status + CRLF;
    }

    private String buildEmptyResponseHeaders() {
        return CRLF;
    }

    private String buildResponseHeaders(String contentType, Integer contentLength) {
        if (contentType == null || contentLength == null) {
            return CRLF;
        }
        String contentTypeHeader = "Content-Type: " + contentType + CRLF;
        String contentLengthHeader = "Content-Length: " + contentLength + CRLF;
        return contentTypeHeader + contentLengthHeader + CRLF;
    }

    private String buildEmptyResponseBody() {
        return "";
    }

    private String buildResponseBody(String body) {
        return body;
    }
}
