import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static constants.HttpConstants.*;
import static constants.PathConstants.*;

public class HttpServer {

    private final String USER_AGENT_HEADER_PREFIX = "User-Agent: ";

    private final int port;

    private final ExecutorService executorService;

    public HttpServer(int port, final int socketAmount) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(socketAmount);
    }

    public void run(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> {
                    try {
                        String path = "";
                        if(args.length > 0 ){
                            path = args[0];
                        }
                        handleRequest(clientSocket, path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    private void handleRequest(Socket clientSocket, String basePath) throws IOException {
        String response = buildResponseStatus(STATUS_NOT_FOUND) + buildEmptyResponseHeaders() + buildEmptyResponseBody();
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
            if (httpRequest[1].equals("/") || httpRequest[1].isBlank()) {
                response = buildResponseStatus(STATUS_OK) + buildEmptyResponseHeaders() + buildEmptyResponseBody();
            } else if (httpRequest[1].startsWith(ECHO_PATH)) {
                String textToEcho = httpRequest[1].substring(httpRequest[1].lastIndexOf(ECHO_PATH) + ECHO_PATH.length());
                response = buildResponseStatus(STATUS_OK) + buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, textToEcho.length()) + buildResponseBody(textToEcho);
            } else if (httpRequest[1].equals(USER_AGENT_PATH)) {
                String headerToPrintInBody = headers.stream().anyMatch(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)) ? headers.stream().filter(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)).findAny().get().substring(USER_AGENT_HEADER_PREFIX.length()) : "";
                response = buildResponseStatus(STATUS_OK) + buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, headerToPrintInBody.length()) + buildResponseBody(headerToPrintInBody);
            } else if (httpRequest[1].startsWith(FILE_PATH)) {
                String filePath = httpRequest[1].substring(httpRequest[1].lastIndexOf(FILE_PATH) + FILE_PATH.length());
                String fileContent = new String(Files.readAllBytes(Paths.get(basePath + filePath)));
                if(!fileContent.isBlank() ) {
                    response = buildResponseStatus(STATUS_OK) + buildResponseHeaders(CONTENT_TYPE_OCTET_STREAM, fileContent.length()) + buildResponseBody(fileContent);
                }
            }
            System.out.println("accepted new connection");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            clientSocket.getOutputStream().write(response.getBytes());
            clientSocket.getOutputStream().flush();
            clientSocket.close();
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
