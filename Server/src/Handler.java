import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Handler implements Runnable {

    private Socket client;
    int serverID = 0;
    //int[] serverPorts = new int[] { 30500, 30501, 30502 };
	private String FILEPREFIX;
    private Message ClientMessage;
    private Message ServerMessage;
    ObjectInputStream ois;
    ObjectOutputStream oos;
    private Message socketRead(){
        //ObjectInputStream ois
        Message message = new Message(0,"",0,0,"");;
        try {
            message =(Message) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return  message;
    }
    private void socketWrite(Message message){
        //ObjectOutputStream oos
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String ReadLastLine(File file) {
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
    public void WriteLastLine(File file, String input) throws IOException {
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
    public String ListAllFile(){
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
    public Handler(Socket clientSocket,int ID) throws IOException {
        this.client = clientSocket;
        this.serverID = ID;
        this.ois = new ObjectInputStream(clientSocket.getInputStream());
        this.oos= new ObjectOutputStream(clientSocket.getOutputStream());


    }
    @Override
    public void run() {
        //read message from client and analyse it
        FILEPREFIX = ".//files"+serverID + "//";
        ClientMessage = (Message) socketRead();
        ServerMessage = getReturnMessage(ClientMessage);
        //System.out.println("client"+ClientMessage.getFrom()+"message : "+ClientMessage.getContent());
        File file = new File(FILEPREFIX + ClientMessage.getFileName());
        switch (ClientMessage.getType()){
            case "enquiry"://return the list of file
                ServerMessage.setContent(ListAllFile());
                break;
            case "read"://read the last line
                System.out.println(ClientMessage.getFrom() + ": Reading...");
                ServerMessage.setContent("Reading: "+ReadLastLine(file));
                System.out.println("Reading "+ClientMessage.getContent());
                break;
            case "write"://write to the end of file
                System.out.println(ClientMessage.getFrom() + ": writing...");
                try{
                    WriteLastLine(file,ClientMessage.getContent());
                }catch (IOException e) {
                    System.out.println("write error");
                }

                ServerMessage.setContent("writing: "+ReadLastLine(file));
                System.out.println("Writing "+ClientMessage.getContent());
                break;
            default://unknown command
                System.out.println(ClientMessage.getType());
                ServerMessage.setContent("Error");
                System.out.println(ClientMessage.getFrom() + ": Wrong type");
                break;
        }
        socketWrite(ServerMessage);

        try {//close the socket connection
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
