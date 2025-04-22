import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger; 

public class TupleServer {
    private AtomicInteger clientCounter = new AtomicInteger(0); 
    private ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>(); 
    public void start() {
        int port = 51234;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(20000);
            System.out.println("Waiting...");
    
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientCounter.incrementAndGet();
                    System.out.println("Connecting: " + clientSocket.getInetAddress().getHostAddress() + " (Client #" + clientId + ")");
    
                    ClientHandler clientHandler = new ClientHandler(clientSocket, clientId,database);
                    new Thread(clientHandler).start();
                } catch (SocketTimeoutException e) {
                    System.out.println("No new connections. Server is shutting down...");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private int clientId; 
    private ConcurrentHashMap<String, String> database; 
 
    public ClientHandler(Socket socket, int clientId, ConcurrentHashMap<String, String> database) {
        this.database = database;
        this.clientSocket = socket;
        this.clientId = clientId;
    }
 
    @Override
    public void run() {
        try (
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            PrintWriter writer = new PrintWriter(output, true)
        ) {
            String clientMessage;
            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Client #" + clientId + " sent: " + clientMessage);

                writer.println("Response to Client #" + clientId + ": " + clientMessage); 
            }
        } catch (IOException e) {
            System.err.println("Error handling client #" + clientId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client #" + clientId + " disconnected.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}