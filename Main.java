import java.io.*;

public class Main {
    public static void main(String[] args) {
        String filePath = "test-workload/client_1.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String [] parts = line.split(" ");
                if (parts[0]=="READ"){
                    
                }
            }
        } catch (IOException e) {
            System.err.println("Error file: " + e.getMessage());
        }
    }
}
