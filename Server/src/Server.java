
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server {
    int serverID = 0;
    int[] serverPorts = new int[] { 30500, 30501, 30502 };
    //private static final int ID = 1;
    private static final String FILEPREFIX = ".//files" + "//";
    private static String fileName = "123.txt";
    private Message socketRead(Socket socket){
        BufferedReader br;
        Message message = new Message(0,"",0,0,"");;
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            message =(Message) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return  message;
    }
    private void socketWrite(Socket socket,Message message){
        BufferedWriter bw;

        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String ReadLastLine(File file) {
        RandomAccessFile fileHandler = null;
        try {
            fileHandler = new RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            //read from end
            for(long filePointer = fileLength; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();
                //0xA is the new line and 0xD is the carriage return \r \n
                if( readByte == 0xA||readByte == 0xD ) {//
                    break;
                }
                sb.append( ( char ) readByte );

            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch( java.io.FileNotFoundException e ) {
            e.printStackTrace();
            return null;
        } catch( java.io.IOException e ) {
            e.printStackTrace();
            return null;
        } finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } catch (IOException e) {
                    /* ignore */
                }
        }
    }
    public static void WriteLastLine(File file, String input) throws IOException {
        if(!file.exists()){
            file.createNewFile();
            System.out.println("New File Created Now");
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
        String end =  ReadLastLine(file);
        System.out.println(end);
        if(!end.isEmpty()){
            writer.append('\n');
        }
        writer.append(input);
        writer.close();

    }
    public static String ListAllFile(){
        String dst = "";
        try (Stream<Path> walk = Files.walk(Paths.get(FILEPREFIX))) {

            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            //result.forEach(System.out::println);
            for (String r:result) {
                r = r.substring(FILEPREFIX.length()-2);// size of "//" is 2
                dst+=r+"\n";
            }
            //System.out.println(dst);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dst;
    }
    private Message getReturnMessage(Message src){
        Message dst = new Message(1,src.getType(),src.getTo(),src.getFrom(),src.getFileName());
        return dst;
    }
    private  void run(){
        //while (true){
        try (ServerSocket ss = new ServerSocket(serverPorts[serverID]);){

            System.out.println("Server is runing...");
            Socket socket = ss.accept();
            System.out.println("Client:"+socket.getPort()+"connected");


            //read from client
            Message ClientMessage = (Message) socketRead(socket);
            Message ServerMessage = getReturnMessage(ClientMessage);
            System.out.println("client:"+ClientMessage.getContent());
            File file = new File(FILEPREFIX + ClientMessage.getFileName());
            switch (ClientMessage.getType()){
                case "ENQUIRY":
                    ServerMessage.setContent(ListAllFile());
                    break;
                case "READ":
                    System.out.println("Reading...");
                    ServerMessage.setContent(ReadLastLine(file));
                    break;
                case "WRITE":
                    System.out.println("writing...");
                    WriteLastLine(file,ClientMessage.getContent());
                    break;
                default:
                    System.out.println(ClientMessage.getType());
                    System.out.println("Wrong type");
                    break;
            }


            //write back to client
            System.out.println("Server: "+ServerMessage.getContent());
            socketWrite(socket,ServerMessage);

        } catch (IOException e) {
            e.printStackTrace();
        }
        //}
    }

    public static void main(String[] args) throws IOException {
        File file = new File(FILEPREFIX + fileName);
        //System.out.println(ReadLastLine(file));
        //System.out.println(content);
        //WriteLastLine(file,"124");
        //ListAllFile();
        //Message m = new Message(1,"111",1,1,"123.txt");

        Server server = new Server();
        while (true){
            server.run();
        }


    }
}