import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static constants.HttpConstants.*;
import static constants.PathConstants.*;

public class HttpServer {

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
                        if (args.length >= 2) {
                            if (Objects.equals(args[0], "--directory")) {
                                path = args[1];
                            }
                        }
                        handleRequest(clientSocket, path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }

    private void handleRequest(Socket clientSocket, String basePath) throws IOException {
        System.out.println("accepted new connection");
        String response = buildResponseStatus(STATUS_NOT_FOUND) + buildEmptyResponseHeaders() + buildEmptyResponseBody();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line = bufferedReader.readLine();
            System.out.println("Request : " + line);
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
            System.out.println("Headers : " + headers);
            StringBuilder payload = new StringBuilder();
            while (bufferedReader.ready()) {
                payload.append((char) bufferedReader.read());
            }
            System.out.println("Payload : " + payload.toString());
            String acceptedEncoding = headers.stream().anyMatch(header -> header.startsWith(ACCEPT_ENCODING_HEADER)) ? headers.stream().filter(header -> header.startsWith(ACCEPT_ENCODING_HEADER)).findAny().get().substring(ACCEPT_ENCODING_HEADER.length()) : "";
            boolean isEncodingValid = validateEncoding(acceptedEncoding);
            if (httpRequest[1].equals("/") || httpRequest[1].isBlank()) {
                response = buildResponseStatus(STATUS_OK) + buildEmptyResponseHeaders() + buildEmptyResponseBody();
            } else if (httpRequest[1].startsWith(ECHO_PATH)) {
                String textToEcho = httpRequest[1].substring(httpRequest[1].lastIndexOf(ECHO_PATH) + ECHO_PATH.length());
                response = buildResponseStatus(STATUS_OK) + buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, isEncodingValid, textToEcho.length()) + buildResponseBodyHandlingEncoding(textToEcho, isEncodingValid);
            } else if (httpRequest[1].equals(USER_AGENT_PATH)) {
                String headerToPrintInBody = headers.stream().anyMatch(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)) ? headers.stream().filter(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)).findAny().get().substring(USER_AGENT_HEADER_PREFIX.length()) : "";
                response = buildResponseStatus(STATUS_OK) + buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, isEncodingValid, headerToPrintInBody.length()) + buildResponseBody(headerToPrintInBody);
            } else if (httpRequest[1].startsWith(FILE_PATH)) {
                if (httpRequest[0].equals(GET_METHOD)) {
                    String filePath = httpRequest[1].substring(httpRequest[1].lastIndexOf(FILE_PATH) + FILE_PATH.length());
                    String fileContent = new String(Files.readAllBytes(Paths.get(basePath + filePath)));
                    if (!fileContent.isBlank()) {
                        response = buildResponseStatus(STATUS_OK) + buildResponseHeaders(CONTENT_TYPE_OCTET_STREAM, isEncodingValid, fileContent.length()) + buildResponseBody(fileContent);
                    }
                } else if (httpRequest[0].equals(POST_METHOD)) {
                    String fileName = httpRequest[1].substring(httpRequest[1].lastIndexOf(FILE_PATH) + FILE_PATH.length());
                    BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + fileName));
                    writer.write(payload.toString());
                    writer.close();
                    response = buildResponseStatus(STATUS_OK_CREATED) + buildEmptyResponseHeaders() + buildEmptyResponseBody();
                }
            }
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
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

    private boolean validateEncoding(String encoding) {
        List<String> encodingOptions = Arrays.stream(encoding.split(", ", 0)).toList();
        return encodingOptions.contains(SUPPORTED_ENCODING_OPTIONS);
    }

    private String buildResponseHeaders(String contentType, boolean handleEncoding, Integer contentLength) {
        if (contentType == null || contentLength == null) {
            return CRLF;
        }
        String contentTypeHeader = "Content-Type: " + contentType + CRLF;
        String contentEncodingHeader = handleEncoding ? "Content-Encoding: " + SUPPORTED_ENCODING_OPTIONS + CRLF : "";
        String contentLengthHeader = "Content-Length: " + contentLength + CRLF;
        return contentTypeHeader + contentEncodingHeader + contentLengthHeader + CRLF;
    }

    private String buildEmptyResponseBody() {
        return "";
    }

    private String buildResponseBody(String body) {
        return body;
    }

    private byte[] buildResponseBodyHandlingEncoding(String body, boolean encodedBody) {
        if(!encodedBody) {
            return body.getBytes();
        } else {
            try {
                return GZIPCompression.compress(body);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
