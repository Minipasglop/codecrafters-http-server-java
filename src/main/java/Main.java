import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static final String CRLF = "\r\n";
    public static final String STATUS_OK = "200 OK";
    public static final String STATUS_NOT_FOUND = "404 Not Found";
    public static final String ECHO_PATH = "/echo/";

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        try {
            ServerSocket serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
            String line = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine();
            String[] httpRequest = line.split(" ", 0);
            // httpRequest[0] : HTTP Method
            // httpRequest[1] : Path
            // httpRequest[2] : HTTP Options
            String response;
            if (httpRequest[1].equals("/") || httpRequest[1].isBlank()) {
                response = buildResponseStatus(STATUS_OK) + buildResponseHeaders("") + buildResponseBody("");
            } else if (httpRequest[1].startsWith(ECHO_PATH)) {
                String textToEcho = httpRequest[1].substring(httpRequest[1].lastIndexOf(ECHO_PATH) + ECHO_PATH.length());
                response = buildResponseStatus(STATUS_OK) + buildResponseHeaders("Content-Type: text/plain\r\nContent-Length: " + textToEcho.length()+"\r\n") + buildResponseBody(textToEcho);
            }
            else {
                response = buildResponseStatus(STATUS_NOT_FOUND) + buildResponseHeaders("") + buildResponseBody("");
            }
            clientSocket.getOutputStream().write(response.getBytes());
            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static String buildResponseStatus(String status) {
        return "HTTP/1.1 " + status + CRLF;
    }

    private static String buildResponseHeaders(String headers) {
        return headers + CRLF;
    }

    private static String buildResponseBody(String body) {
        return body;
    }
}
