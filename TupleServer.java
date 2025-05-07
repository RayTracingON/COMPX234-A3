import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong; 
//server main class
public class TupleServer {
    private AtomicInteger clientCounter = new AtomicInteger(0); 
    private ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>(); 
    private AtomicInteger activeHandlers = new AtomicInteger(0); 
    private boolean started=false;
    private static AtomicInteger tuplenum=new AtomicInteger(0); 
    private static AtomicLong cumulativeKeySizeOfSuccessfullyRead = new AtomicLong(0);
    private static AtomicLong cumulativeValueSizeOfSuccessfullyRead = new AtomicLong(0); 
    private static AtomicInteger totalOperations = new AtomicInteger(0);    
    private static AtomicInteger totalReads = new AtomicInteger(0);   
    private static AtomicInteger totalGets = new AtomicInteger(0);    
    private static AtomicInteger totalPuts = new AtomicInteger(0);
    private static AtomicInteger totalErrors = new AtomicInteger(0);

    public void printSummary() {
        int numTuples = tuplenum.get();

        long totalSuccessfullyReadKeySize = cumulativeKeySizeOfSuccessfullyRead.get();
        long totalSuccessfullyReadValueSize = cumulativeValueSizeOfSuccessfullyRead.get();
        int successfulRetrievalsCount = numTuples;

        double avgHistTupleSize = successfulRetrievalsCount > 0 ? (double) (totalSuccessfullyReadKeySize + totalSuccessfullyReadValueSize) / successfulRetrievalsCount : 0;
        double avgHistKeySize = successfulRetrievalsCount > 0 ? (double) totalSuccessfullyReadKeySize / successfulRetrievalsCount : 0;
        double avgHistValueSize = successfulRetrievalsCount > 0 ? (double) totalSuccessfullyReadValueSize / successfulRetrievalsCount : 0;

        System.out.println("\n----- Tuple Space Summary -----");
        System.out.println("Total clients connected: " + clientCounter.get());
        System.out.println("Total operations attempted (R+G+P): " + totalOperations.get());
        System.out.println("Total READ operations attempted: " + totalReads.get());
        System.out.println("Total GET operations attempted: " + totalGets.get());
        System.out.println("Total PUT operations attempted: " + totalPuts.get());
        System.out.println("Total errors encountered: " + totalErrors.get());

        System.out.println("\n--- Statistics for Successfully Retrieved Tuples (Cumulative) ---");
        System.out.println("Total successful data retrievals (READs/GETs): " + successfulRetrievalsCount);
        System.out.printf("Average tuple size (of all retrieved tuples): %.2f chars\n", avgHistTupleSize);
        System.out.printf("Average key size (of all retrieved tuples): %.2f chars\n", avgHistKeySize);
        System.out.printf("Average value size (of all retrieved tuples): %.2f chars\n", avgHistValueSize);
        System.out.println("-----------------------------\n");
    }
    public void start() {
        int port = 51234;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(20000);
            System.out.println("Waiting...");
            new Thread(() -> {
                while (true) {
                    try {
                        if (activeHandlers.get() == 0 && started) {
                            break;
                        }
                        Thread.sleep(1000); 
                        printSummary(); 
                    } catch (InterruptedException e) {
                        System.err.println("Summary thread interrupted.");
                        break;
                    }
                }
            }).start();
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientCounter.incrementAndGet();
                    System.out.println("Connecting: " + clientSocket.getInetAddress().getHostAddress() + " (Client #" + clientId + ")");
                    started=true;
                    ClientHandler clientHandler = new ClientHandler(clientSocket, clientId, database, activeHandlers,
                            totalOperations, totalReads, totalGets, totalPuts, totalErrors, cumulativeKeySizeOfSuccessfullyRead, cumulativeValueSizeOfSuccessfullyRead,tuplenum);
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
//handle client class
class ClientHandler implements Runnable {
    private Socket clientSocket;
    private int clientId; 
    private ConcurrentHashMap<String, String> database; 
    private AtomicInteger activeHandlers;
    private AtomicInteger totalOperations;
    private AtomicInteger totalReads;
    private AtomicInteger totalGets;
    private AtomicInteger totalPuts;
    private AtomicInteger totalErrors;
    private AtomicInteger tuplenum; 
    private AtomicLong cumulativeKeySizeOfSuccessfullyRead;
    private AtomicLong cumulativeValueSizeOfSuccessfullyRead;
 
    public ClientHandler(Socket socket, int clientId, ConcurrentHashMap<String, String> database, AtomicInteger activeHandlers,
                         AtomicInteger totalOperations, AtomicInteger totalReads, AtomicInteger totalGets,
                         AtomicInteger totalPuts, AtomicInteger totalErrors,AtomicLong cumulativeKeySizeOfSuccessfullyRead,AtomicLong cumulativeValueSizeOfSuccessfullyRead,AtomicInteger tuplenum) { // 构造函数更新
        
        this.clientSocket = socket;
        this.clientId = clientId;
        this.database = database;
        this.activeHandlers = activeHandlers;
        // 初始化新增的成员变量
        this.totalOperations = totalOperations;
        this.totalReads = totalReads;
        this.totalGets = totalGets;
        this.totalPuts = totalPuts;
        this.totalErrors = totalErrors;
        this.tuplenum = tuplenum;
        this.cumulativeKeySizeOfSuccessfullyRead = cumulativeKeySizeOfSuccessfullyRead;
        this.cumulativeValueSizeOfSuccessfullyRead = cumulativeValueSizeOfSuccessfullyRead;
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
                tuplenum.incrementAndGet();
                //System.out.println("Client #" + clientId + " sent: " + clientMessage);
                String [] key;
                String returnstr="";
                String [] parts = clientMessage.split(" ",3);//num type key
                key= parts[2].split(" ",2);//key data
                String key1 = key[0];//key
                if (key.length==1) {
                    if (parts[1].equals("R")) {//read
                        totalReads.incrementAndGet();
                        if (database.containsKey(parts[2])) {
                            
                            String data=database.get(parts[2]);
                            cumulativeKeySizeOfSuccessfullyRead.addAndGet(parts[2].length());
                            cumulativeValueSizeOfSuccessfullyRead.addAndGet(data.length());
                            int num = data.length()+16+parts[2].length();
                            returnstr = String.format("%03d",num) + " OK ("+ parts[2] +", "+data+ ") read";
                            totalOperations.incrementAndGet();
                        } else {
                            int num= parts[2].length()+23;
                            returnstr = String.format("%03d",num) + " ERR "+ parts[2] +" does not exist";
                            totalErrors.incrementAndGet();
                        }
                    }
                    else{//get
                        totalGets.incrementAndGet();
                        if(database.containsKey(key1)) {
                            totalOperations.incrementAndGet();
                            String data=database.get(key1);
                            int num = data.length()+19+key1.length();
                            database.remove(key1);
                            returnstr = String.format("%03d",num) + " OK ("+ key1 +", "+data+ ") removed";
                        } else {
                            totalErrors.incrementAndGet();
                            int num= key1.length()+23;
                            returnstr = String.format("%03d",num) + " ERR "+ key1 +" does not exist";
                        }
                    }
                }
                else{//put
                    totalPuts.incrementAndGet();
                    if(database.containsKey(key1)) {
                        totalErrors.incrementAndGet();
                        int num= key1.length()+23;
                        returnstr = String.format("%03d",num) + " ERR "+ key1 +" already exists";
                    } else {
                        totalOperations.incrementAndGet();
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