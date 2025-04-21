import java.util.ArrayList;
import java.util.List;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        TupleClient [] clients = new TupleClient[10];
        for (int i = 0; i < clients.length; i++) {
            final int index = i; 
            clients[i] = new TupleClient("test-workload/client_" + (i+1) + ".txt");
            Thread thread = new Thread(() -> clients[i].startRead());
            threads.add(thread);
        }

    }
}
