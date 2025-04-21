import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger; 

public class TupleServer {
    private AtomicInteger clientCounter = new AtomicInteger(0); 
    public void start() {
        int port = 51234;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(10000);
            System.out.println("Waiting...");
    
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientCounter.incrementAndGet();
                    System.out.println("Connecting: " + clientSocket.getInetAddress().getHostAddress() + " (Client #" + clientId + ")");
    
                    ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
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
 
    public ClientHandler(Socket socket, int clientId) {
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
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client #" + clientId + " disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}