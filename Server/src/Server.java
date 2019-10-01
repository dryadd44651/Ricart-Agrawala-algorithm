
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.DirectoryStream;
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
    private void setID(int src){
        serverID = src;
    }
    private  void run(){
        //while (true){
        Socket socket = null;
        try (ServerSocket ss = new ServerSocket(serverPorts[serverID]);){

            System.out.println("Server"+serverID+" is runing...");
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
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    public static void main(String[] args) throws IOException {
        //File file = new File(FILEPREFIX + fileName);

        //Path path = Paths.get(FILEPREFIX);
        //if(!Files.exists(path))
        //DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.xml");
        deleteFolder(new File("./files"));//delete folder and content
        new File("./files").mkdir();//new a folder
        for (int i = 0;i<8;i++){//create new txt for client
            new File("./files/"+i+".txt").createNewFile();
        }


        Server server = new Server();
        if(args.length!=0)
            server.setID(Integer.valueOf(args[0]));
        else
            server.setID(0);

        while (true){
            server.run();
        }


    }
}