import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger; 

public class TupleServer {
    private AtomicInteger clientCounter = new AtomicInteger(0); 
    private ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>(); 
    private AtomicInteger activeHandlers = new AtomicInteger(0); 
    private boolean started=false;
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
                    started=true;
                    ClientHandler clientHandler = new ClientHandler(clientSocket, clientId,database, activeHandlers);
                    activeHandlers.incrementAndGet();
                    new Thread(clientHandler).start();
                } catch (SocketTimeoutException e) {
                    System.out.println("No new connections. Server is shutting down...");
                    break;
                }finally {
                    if (activeHandlers.get() == 0 && started) {
                        System.out.println("All clients have finished. Server is shutting down...");
                        break;
                    }
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
    private AtomicInteger activeHandlers;
 
    public ClientHandler(Socket socket, int clientId, ConcurrentHashMap<String, String> database, AtomicInteger activeHandlers) {
        this.activeHandlers = activeHandlers;
        this.database = database;
        this.clientSocket = socket;
        this.clientId = clientId;
    }
 
    @Override
    public synchronized void run() {
        try (
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            PrintWriter writer = new PrintWriter(output, true)
        ) {
            String clientMessage;
            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Client #" + clientId + " sent: " + clientMessage);
                String [] key;
                String returnstr="";
                String [] parts = clientMessage.split(" ",3);//num type key
                key= parts[2].split(" ",2);//key data
                String key1 = key[0];//key
                if (key.length==1) {
                    if (parts[1].equals("R")) {//read
                        if (database.containsKey(parts[2])) {
                            String data=database.get(parts[2]);
                            int num = data.length()+16+parts[2].length();
                            returnstr = String.format("%03d",num) + " OK ("+ parts[2] +", "+data+ ") read";
                        } else {
                            int num= parts[2].length()+23;
                            returnstr = String.format("%03d",num) + " ERR "+ parts[2] +" does not exist";
                        }
                    }
                    else{//get
                        if(database.containsKey(key1)) {
                            String data=database.get(key1);
                            int num = data.length()+19+key1.length();
                            database.remove(key1);
                            returnstr = String.format("%03d",num) + " OK ("+ key1 +", "+data+ ") removed";
                        } else {
                            int num= key1.length()+23;
                            returnstr = String.format("%03d",num) + " ERR "+ key1 +" does not exist";
                        }
                    }
                }
                else{//put
                    if(database.containsKey(key1)) {
                        int num= key1.length()+23;
                        returnstr = String.format("%03d",num) + " ERR "+ key1 +" already exists";
                    } else {
                        database.put(key1, key[1]); 
                        int num= key1.length()+17+key[1].length();
                        returnstr = String.format("%03d",num) + " OK ("+ key1+", "+key[1] +" ) added";
                    }
                }
                writer.println(returnstr); 
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error handling client #" + clientId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                activeHandlers.decrementAndGet();
                clientSocket.close();
                System.out.println("Client #" + clientId + " disconnected.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}