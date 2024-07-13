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
import static helper.RequestHelper.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpServer {

    private final int port;

    private String basePath;

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
                                this.basePath = args[1];
                            }
                        }
                        handleRequest(clientSocket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        HttpResponse httpResponse = HttpResponse.builder()
                .status(buildResponseStatus(STATUS_NOT_FOUND).getBytes(UTF_8))
                .headers(buildEmptyResponseHeaders().getBytes(UTF_8))
                .body(buildEmptyResponseBody().getBytes(UTF_8))
                .build();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String bufferLine = bufferedReader.readLine();

            String[] httpRequest = bufferLine.split(" ", 0);
            String requestMethod = httpRequest[0];
            String requestPath = httpRequest[1];

            List<String> headers = new ArrayList<>();
            while (true) {
                bufferLine = bufferedReader.readLine();
                if (bufferLine == null || bufferLine.isBlank()) {
                    break;
                }
                headers.add(bufferLine);
            }

            StringBuilder payload = new StringBuilder();
            while (bufferedReader.ready()) {
                payload.append((char) bufferedReader.read());
            }

            boolean isEncodingEnabled = validateEncodingFromHeaders(headers);

            if (requestPath.equals(BASE_PATH) || requestPath.isBlank()) {
                httpResponse.setStatus(buildResponseStatus(STATUS_OK).getBytes(UTF_8));
            } else if (requestPath.startsWith(ECHO_PATH)) {
                httpResponse = handleEchoCommand(requestPath, isEncodingEnabled);
            } else if (requestPath.equals(USER_AGENT_PATH)) {
                httpResponse = handleUserAgentCommand(headers, isEncodingEnabled);
            } else if (requestPath.startsWith(FILE_PATH)) {
                httpResponse = handleFileCommand(requestPath, requestMethod, isEncodingEnabled, payload);
            }
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
            httpResponse = handleException(e);
        } finally {
            clientSocket.getOutputStream().write(httpResponse.getStatus());
            clientSocket.getOutputStream().write(httpResponse.getHeaders());
            clientSocket.getOutputStream().write(httpResponse.getBody());
        }
    }

    private HttpResponse handleException(Exception e) {
        return HttpResponse.builder()
                .status(buildResponseStatus(STATUS_INTERNAL_ERROR).getBytes(UTF_8))
                .headers(buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, false, e.getMessage().length()).getBytes(UTF_8))
                .body(e.getMessage().getBytes(UTF_8))
                .build();
    }

    private HttpResponse handleEchoCommand(String requestPath, boolean isEncodingEnabled) {
        String textToEcho = requestPath.substring(requestPath.lastIndexOf(ECHO_PATH) + ECHO_PATH.length());
        byte[] body = buildResponseBodyBytesHandlingEncoding(textToEcho, isEncodingEnabled);
        return HttpResponse.builder()
                .status(buildResponseStatus(STATUS_OK).getBytes(UTF_8))
                .headers(buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, isEncodingEnabled, body.length).getBytes(UTF_8))
                .body(body)
                .build();
    }

    private HttpResponse handleUserAgentCommand(List<String> headers, boolean isEncodingEnabled) {
        String headerToPrintInBody = headers.stream().anyMatch(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)) ? headers.stream().filter(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)).findAny().get().substring(USER_AGENT_HEADER_PREFIX.length()) : "";
        return HttpResponse.builder()
                .status(buildResponseStatus(STATUS_OK).getBytes(UTF_8))
                .headers(buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, isEncodingEnabled, headerToPrintInBody.length()).getBytes(UTF_8))
                .body(buildResponseBody(headerToPrintInBody).getBytes(UTF_8))
                .build();
    }

    private HttpResponse handleFileCommand(String requestPath, String requestMethod, boolean isEncodingEnabled, StringBuilder payload) throws IOException {
        HttpResponse httpResponse = null;
        String filePath = requestPath.substring(requestPath.lastIndexOf(FILE_PATH) + FILE_PATH.length());
        if (requestMethod.equals(GET_METHOD)) {
            httpResponse = handleGetFileCommand(isEncodingEnabled, filePath, httpResponse);
        } else if (requestMethod.equals(POST_METHOD)) {
            httpResponse = handlePostFileCommand(payload, filePath);
        }
        return httpResponse;
    }

    private HttpResponse handleGetFileCommand(boolean isEncodingEnabled, String filePath, HttpResponse httpResponse) throws IOException {
        String fileContent = new String(Files.readAllBytes(Paths.get(basePath + filePath)));
        if (!fileContent.isBlank()) {
            httpResponse = HttpResponse.builder()
                    .status(buildResponseStatus(STATUS_OK).getBytes(UTF_8))
                    .headers(buildResponseHeaders(CONTENT_TYPE_OCTET_STREAM, isEncodingEnabled, fileContent.length()).getBytes(UTF_8))
                    .body(buildResponseBody(fileContent).getBytes(UTF_8))
                    .build();
        }
        return httpResponse;
    }

    private HttpResponse handlePostFileCommand(StringBuilder payload, String filePath) throws IOException {
        HttpResponse httpResponse;
        BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + filePath));
        writer.write(payload.toString());
        writer.close();
        httpResponse = HttpResponse.builder()
                .status(buildResponseStatus(STATUS_OK_CREATED).getBytes(UTF_8))
                .headers(buildEmptyResponseHeaders().getBytes(UTF_8))
                .body(buildEmptyResponseBody().getBytes(UTF_8))
                .build();
        return httpResponse;
    }
}
