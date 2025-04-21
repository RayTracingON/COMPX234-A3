import java.util.ArrayList;
import java.util.List;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        TupleServer server = new TupleServer();
        Thread serverThread = new Thread(() -> server.start());
        serverThread.start();
        Thread.sleep(1000); // Wait for the server to start
        List<Thread> threads = new ArrayList<>();
        TupleClient [] clients = new TupleClient[10];
        for (int i = 0; i < clients.length; i++) {
            final int index = i; 
            clients[i] = new TupleClient("test-workload/client_" + (index+1) + ".txt");
            Thread thread = new Thread(() -> clients[index].start());
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        serverThread.join();
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("All clients have finished.");
    }
}
