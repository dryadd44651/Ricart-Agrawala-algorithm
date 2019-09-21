
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server {
    int serverID = 0;
    int[] serverPorts = new int[] { 30500, 30501, 30502 };
    //private static final int ID = 1;
    private ArrayList<Handler> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(10);


    private static final String FILEPREFIX = ".//files" + "//";
    private static String fileName = "123.txt";

    private  void run(){
        //while (true){
        Socket socket = null;
        try (ServerSocket ss = new ServerSocket(serverPorts[serverID]);){

            System.out.println("Server is runing...");
            socket = ss.accept();
            //System.out.println("Client:"+socket.getPort()+"connected");

            Handler clientThread = new Handler(socket);
            clients.add(clientThread);
            pool.execute((clientThread));



        }catch (SocketTimeoutException e) {
            System.out.println("time out");
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //}
    }

    public static void main(String[] args) throws IOException {
        File file = new File(FILEPREFIX + fileName);


        Server server = new Server();
        while (true){
            server.run();
        }


    }
}