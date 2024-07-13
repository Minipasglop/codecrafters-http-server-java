import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
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
            if (httpRequest[1].equals("/") || httpRequest[1].isBlank()) {
                clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
            } else {
                clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }
            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
