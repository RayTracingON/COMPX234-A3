import java.util.ArrayList;
import java.util.List;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        TupleClient [] clients = new TupleClient[10];
        for (int i = 0; i < clients.length; i++) {
            final int index = i+1; 
            clients[i] = new TupleClient("test-workload/client_" + (index) + ".txt");
            Thread thread = new Thread(() -> clients[index].startRead());
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

    }
}
