
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    int serverID = 0;
    int[] serverPorts = new int[] { 30500, 30501, 30502 };
    private ArrayList<Handler> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(500);
	private String pathName;


    private void setID(int src){
        serverID = src;
    }
	private int getID(){
        return	serverID;
    }
	private void setPathName(){
        pathName = "./files"+serverID;
    }
	private String getPathName(){
        return	pathName;
    }
    private  void run() throws IOException {
		System.out.println("Server"+serverID+" is runing...");
        Socket socket = null;
        try (ServerSocket ss = new ServerSocket(serverPorts[serverID]);){

            socket = ss.accept();
            Handler clientThread = new Handler(socket,serverID);
            clients.add(clientThread);
            pool.execute((clientThread));



        }catch (SocketTimeoutException e) {
            System.out.println("time out");
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            //socket close in handler
        }
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

        Server server = new Server();
        if(args.length!=0)
            server.setID(Integer.valueOf(args[0]));
        else
            server.setID(0);
		
		//Remove all file and create the folder and file (make sure file is empty)
        server.setPathName();
		deleteFolder(new File(server.getPathName()));//delete folder and content
        new File(server.getPathName()).mkdir();//new a folder
        for (int i = 0;i<8;i++){//create new txt for client
            new File(server.getPathName()+"/"+i+".txt").createNewFile();
		}
        while (true){
            server.run();
        }


    }
}