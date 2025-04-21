import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class TupleClient {
    String filePath;
    
    public TupleClient(String filePaths) {
        this.filePath = filePaths;
        // Constructor
    }
    public void start(){
        TupleClient client = new TupleClient("test-workload/client_1.txt");
        String serverAddress="localhost";
        int serverPort=51234;
        try (Socket socket = new Socket(serverAddress, serverPort)) {
            System.out.println("Connected to server: " + serverAddress + ":" + serverPort);
            client.startRead(socket);      
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
    public synchronized void startRead(Socket socket) throws IOException {
        if (socket == null || socket.isClosed()) {
            System.err.println("Socket is not connected.");
            return;
        }
        else
        {
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream); 
            String filePaths = filePath;
            try (BufferedReader br = new BufferedReader(new FileReader(filePaths))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String [] key;
                    String returnString="";
                    String [] parts = line.split(" ",2);
                    key= parts[1].split(" ",2);
                    String key1 = key[0];
                    if (key.length==1) {
                        int retuint = key1.length()+6;
                        String formattedNumber = String.format("%03d",retuint);
                        if (parts[0].equals("READ")) {
                            returnString = formattedNumber+" R "+key1;
                            
                        }
                        else{
                            returnString = formattedNumber+" G "+key1;
                        }
                    }
                    else {
                        String key2 = key[1];
                        int retuint = key1.length()+key2.length()+7;
                        String formattedNumber = String.format("%03d",retuint);
                        returnString = formattedNumber+" P "+key1+" "+key2;
                    }
                    printWriter.println(returnString);
                    printWriter.flush();
                }
            } catch (IOException e) {
                System.err.println("Error file: " + e.getMessage());
            }
        }
        }   


}
