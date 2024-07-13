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
        HttpResponse httpResponse = HttpResponse.builder()
                .status(buildResponseStatus(STATUS_NOT_FOUND).getBytes(UTF_8))
                .headers(buildEmptyResponseHeaders().getBytes(UTF_8))
                .body(buildEmptyResponseBody().getBytes(UTF_8))
                .build();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String bufferLine = bufferedReader.readLine();

            System.out.println("Request : " + bufferLine);
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

            String acceptedEncoding = headers.stream().anyMatch(header -> header.startsWith(ACCEPT_ENCODING_HEADER)) ? headers.stream().filter(header -> header.startsWith(ACCEPT_ENCODING_HEADER)).findAny().get().substring(ACCEPT_ENCODING_HEADER.length()) : "";
            boolean isEncodingValid = validateEncoding(acceptedEncoding);

            if (requestPath.equals(BASE_PATH) || requestPath.isBlank()) {
                httpResponse.setStatus(buildResponseStatus(STATUS_OK).getBytes(UTF_8));
            } else if (requestPath.startsWith(ECHO_PATH)) {
                httpResponse = handleEchoCommand(requestPath, isEncodingValid);
            } else if (requestPath.equals(USER_AGENT_PATH)) {
                httpResponse = handleUserAgentCommand(headers, isEncodingValid);
            } else if (requestPath.startsWith(FILE_PATH)) {
                httpResponse = handleFileCommand(basePath, requestPath, requestMethod,  isEncodingValid, payload);
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

    private HttpResponse handleEchoCommand(String requestPath, boolean isEncodingValid) {
        String textToEcho = requestPath.substring(requestPath.lastIndexOf(ECHO_PATH) + ECHO_PATH.length());
        byte[] body = buildResponseBodyBytesHandlingEncoding(textToEcho, isEncodingValid);
        return HttpResponse.builder()
                .status(buildResponseStatus(STATUS_OK).getBytes(UTF_8))
                .headers(buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, isEncodingValid, body.length).getBytes(UTF_8))
                .body(body)
                .build();
    }

    private HttpResponse handleUserAgentCommand(List<String> headers, boolean isEncodingValid) {
        String headerToPrintInBody = headers.stream().anyMatch(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)) ? headers.stream().filter(header -> header.startsWith(USER_AGENT_HEADER_PREFIX)).findAny().get().substring(USER_AGENT_HEADER_PREFIX.length()) : "";
        return HttpResponse.builder()
                .status(buildResponseStatus(STATUS_OK).getBytes(UTF_8))
                .headers(buildResponseHeaders(CONTENT_TYPE_TEXT_PLAIN, isEncodingValid, headerToPrintInBody.length()).getBytes(UTF_8))
                .body(buildResponseBody(headerToPrintInBody).getBytes(UTF_8))
                .build();
    }

    private HttpResponse handleFileCommand(String basePath, String requestPath, String requestMethod, boolean isEncodingValid, StringBuilder payload) throws IOException {
        HttpResponse httpResponse = null;
        String filePath = requestPath.substring(requestPath.lastIndexOf(FILE_PATH) + FILE_PATH.length());
        if (requestMethod.equals(GET_METHOD)) {
            String fileContent = new String(Files.readAllBytes(Paths.get(basePath + filePath)));
            if (!fileContent.isBlank()) {
                httpResponse = HttpResponse.builder()
                        .status(buildResponseStatus(STATUS_OK).getBytes(UTF_8))
                        .headers(buildResponseHeaders(CONTENT_TYPE_OCTET_STREAM, isEncodingValid, fileContent.length()).getBytes(UTF_8))
                        .body(buildResponseBody(fileContent).getBytes(UTF_8))
                        .build();
            }
        } else if (requestMethod.equals(POST_METHOD)) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + filePath));
            writer.write(payload.toString());
            writer.close();
            httpResponse = HttpResponse.builder()
                    .status(buildResponseStatus(STATUS_OK_CREATED).getBytes(UTF_8))
                    .headers(buildEmptyResponseHeaders().getBytes(UTF_8))
                    .body(buildEmptyResponseBody().getBytes(UTF_8))
                    .build();
        }
        return httpResponse;
    }
}
