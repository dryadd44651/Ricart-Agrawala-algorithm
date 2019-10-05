
import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

//Tab/ Shift+Tab
public class Client{
    int[] clientPorts = new int[] { 30000, 30001, 30002, 30003, 30004 };
    //String[] clientIps = new String[] { "dc04.utdallas.edu", "dc05.utdallas.edu", "dc06.utdallas.edu","dc07.utdallas.edu", "dc08.utdallas.edu" };
    String[] clientIps = new String[] { "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1" };
    //static String[] clientIps = new String[] { "127.0.0.1","127.0.0.1","127.0.0.1"};
    //boolean[] token = new boolean[]{ false,false,false,false,false};
    Map<String,List<Boolean>> token = new HashMap<String,List<Boolean>>();
    int releaseCounter = 0;
    //static boolean[] token = new boolean[]{ false,false,false};
    //String[] serverIps = new String[] { "dc01.utdallas.edu", "dc02.utdallas.edu", "dc03.utdallas.edu" };
    String[] serverIps = new String[] { "127.0.0.1", "127.0.0.1", "127.0.0.1" };
    int[] serverPorts = new int[] { 30500, 30501, 30502 };
    ArrayList<Integer> request = new ArrayList<Integer>();

    private ArrayList<Handler> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(100);

    private int clientID = 0;
    private Message socketRead(Socket socket){
        BufferedReader br;
        Message message = new Message(0, "",0,0,"");;
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            message =(Message) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("socketRead error");
            message.setContent("fail");
        }
        return  message;
    }
    private boolean socketWrite(Socket socket, Message message){

        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            oos.writeObject(message);
            //oos.writeChars("test");
            oos.flush();
            return true;
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("socketWrite error");
            return false;
        }
    }
    private String getTimeStamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        String strTime = String.format("%s", timestamp);
        strTime = strTime.substring(0,19);
        return strTime;
    }
    private static int clock = 0;
    private boolean waiting = false;
    private boolean inCS = false;
    private String fileName = "0.txt";
    private Message getReturnMessage(Message src){
        Message dst = new Message(clock, src.getType(),src.getTo(),src.getFrom(),src.getFileName());
        return dst;
    }


    private void broadcastRepuest(){
        waiting = true;
        //while (fifo_flag);

        int tempClock = clock+1;
        //int tempClock = clock;
        //System.out.println("broadcast");
        for (int i = 0;i<clientIps.length;i++){
            if(i!=clientID&& !token.get(fileName).get(i)){
                int finalI = i;
                new Thread() {
                    public void run() {
                        boolean success = false;

                        while (!success) {
                            Socket socket = null;
                            success = true;
                            try {
                                socket = new Socket(clientIps[finalI], clientPorts[finalI]);
                                Message request = new Message(tempClock, "request", clientID, finalI, fileName);
                                request.setContent("request");
                                //System.out.println("request" + clientID + " to " + finalI);
                                success = socketWrite(socket, request);
                                Message ServerMessage = socketRead(socket);
                                if (ServerMessage.getContent().compareTo("wait") == 0){
                                    token.get(fileName).set(finalI,false);
                                    //System.out.println(finalI+"say wait!! :");

                                }
                                else if (ServerMessage.getContent().compareTo("ok") == 0) {
                                    //System.out.println("request sent to (ok) :" + request.getTo());
                                    token.get(fileName).set(finalI,true);
                                    //System.out.println(finalI+"say ok!! :");
                                    //printToken();
                                }
                                else {
                                    System.out.println(finalI+"connect error:!!!!!!!!!!!!!!!!!");
                                    success = false;
                                    Thread.sleep(200);
                                    //printToken();
                                }

                                //System.out.println("request sent to" + request.getTo() + success);
                            } catch (InterruptedException |IOException e) {
                                e.printStackTrace();
                            }finally{
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }.start();
            }
        }
        //fifo_flag = false;
    }
    private void broadcastRelease() throws InterruptedException {
        waiting = false;
        int tempClock = clock+1;

        //System.out.println("broadcast release");
        for (int i = 0;i<clientIps.length;i++){
        //for (int i:request) {

            if(i!=clientID){//&& token[i]==true
                int finalI = i;
                new Thread() {
                    public void run() {

                        boolean success = false;
                        while (!success) {
                            Socket socket = null;
                            try {
                                success = true;
                                socket = new Socket(clientIps[finalI], clientPorts[finalI]);
                                Message release = new Message(tempClock, "release", clientID, finalI, fileName);
                                release.setContent("release");
                                //System.out.println("release" + clientID + " to " + finalI);

                                success = socketWrite(socket, release);

                                Message ServerMessage = socketRead(socket);
                                if (ServerMessage.getContent().compareTo("ok") == 0) {
                                    token.get(fileName).set(finalI,false);
                                    //System.out.println("release to "+finalI);
                                    releaseCounter++;
                                } else {
                                    //System.out.println("Release error " + ServerMessage.getContent());
                                    success = false;

                                }
                                //System.out.println("request sent to " + release.getTo() + success);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }finally{
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }

                }.start();
            }

        }
        while (releaseCounter<clientIps.length-1) {
            Thread.sleep(30);
            //System.out.println("wait all released: "+releaseCounter);
        }
        releaseCounter = 0;
        request.clear();
    }
    private void clockUpdate(Message message){
        if(message.getClock()>clock){
            clock = message.getClock()+1;
        }
        else{
            clock++;
        }
    }
    private boolean checkToken() {
        for (boolean t:token.get(fileName)) {
            if(t==false) {//find one false, return false
                //System.out.print("checkToken: ");
                //System.out.print(t);
                return false;
            }
        }
        return true;
    }
    private void listening(){
        //receive request and reply
        System.out.println("Listening...");

        new Thread() {
            public void run() {
                Socket socket = null;
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(clientPorts[clientID]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while(true) {
                    try {
                        socket = ss.accept();
                        Handler clientThread = new Handler(socket);
                        clients.add(clientThread);
                        pool.execute((clientThread));
                    } catch (InterruptedException |IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

    }
    private  void CScheck() throws IOException, InterruptedException {
        broadcastRepuest();

        while (true){
            if(checkToken())
                break;
            for (boolean t:token.get(fileName)) {
                //System.out.print(" "+t);
            }
            //System.out.println(" Waiting..."+token.length) ;

            Thread.sleep(10);
        }

        inCS = true;
        //System.out.println("In CS");
    }
    private  void CSleave() throws IOException, InterruptedException {
        inCS = false;
        waiting = false;
        ArrayList<Integer> tmp = (ArrayList<Integer>)request.clone();
        broadcastRelease();
    }
    private  String run(int serverID,String action) throws IOException, InterruptedException {
        Message ServerMessage = new Message(0,"",0,0,"");

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Socket socket = new Socket(serverIps[serverID],serverPorts[serverID]);

        Message message;
        clock++;
        //System.out.println("clock: "+ clock);
        switch (action){
            case "enquiry":
                message = new Message(clock, action,clientID,serverID,fileName);
                break;
            case "read":
                message = new Message(clock, action,clientID,serverID,fileName);
                break;
            case "write":
                message = new Message(clock, action,clientID,serverID,fileName);
                break;
            default:
                System.out.println("Error");
                return "Error";
        }
        message.setContent(clientID+": "+getTimeStamp());
        //broadcast request

        //sent to server

        if(!socketWrite(socket,message))
            return "fail";





        //read message from server
        ServerMessage =socketRead(socket);
        if(ServerMessage.getContent().compareTo("fail")==0)
            return "fail";
        System.out.println("server: \n"+ServerMessage.getContent());


        return ServerMessage.getContent();
    }
    private void CallServerThread(Client client){
        new Thread() {
            public void run() {
                try {
                    String userCommand = "";
                    Scanner scanner = new Scanner(System.in).useDelimiter(";");
                    boolean isExit = true;
                    int times = 0;
                    String prompt = "Client: "+clientID;
                    String command = "";
                    System.out.println("Command: exit, enquiry, read, write command must end with ; ");
                    System.out.println("USAGE: [command] [filename] [times];");
                    //for (int i = 0;i<50;i++){
                    while (isExit){
                        System.out.println(prompt);
                        if(times==0)
                            userCommand = scanner.next().replace("\n", " ").replace("\r", "").trim().toLowerCase();
                        ArrayList<String> commandTokens = new ArrayList<String>(Arrays.asList(userCommand.split(" ")));
                        Random rand = new Random();
                        command = commandTokens.get(0);
                        if(commandTokens.size() == 3 && times ==0)
                            times = Integer.valueOf(commandTokens.get(2))%51;
                        if(command.compareTo("test") == 0 && times ==0)
                            times = 20;


                        switch (commandTokens.get(0)) {
                            case "exit":
                                isExit = false;
                                times = 0;
                                continue;
                            case "enquiry":
                                client.run(rand.nextInt(2),commandTokens.get(0));
                                //client.run(0,commandTokens.get(0));
                                times = 0;
                                continue;
                            case "read":
                                command = "read";
                                break;
                            case "write":
                                command = "write";
                                break;
                            case "test":
                                if(rand.nextInt(2)==0)
                                    command = "write";
                                else
                                    command = "read";
                                break;
                            default:
                                System.out.println("Command: exit, enquiry, read, write, test command must end with ; ");
                                System.out.println("USAGE: [command] [filename] [times];");
                                times = 0;
                                continue;
                        }


                        if(commandTokens.size()>=2)
                            client.fileName = commandTokens.get(1);
                        else{
                            System.out.println("Please enter the file name");
                            times = 0;
                            continue;
                        }
                        if(times>0) {
                            Thread.sleep(rand.nextInt(2) * 1000);
                            times--;
                            System.out.println(times);
                        }



                        CScheck();
                        clock++;
                        if(command.compareTo("write")==0 ){
                            client.run(0,command);
                            //client.run(1,command);
                            //client.run(2,command);
                        }
                        else{
                            client.run(rand.nextInt(2),command);
                            //client.run(0,commandTokens.get(0));
                        }
                        CSleave();

                    }
                    System.out.println("Exiting...");
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
    public class Handler implements Runnable {
        Socket socket;
        Message neighborRequest;
        Message reply;
        public Handler(Socket src) throws IOException, InterruptedException {
            this.socket = src;
            neighborRequest = (Message) socketRead(socket);
            reply = getReturnMessage(neighborRequest);
            //System.out.println(neighborRequest.getContent());
            switch (neighborRequest.getContent()){
                case "request":
                    //System.out.println(" get request: "+neighborRequest.getClock());
                    //reply.setContent("reply");
                    //System.out.println(waiting+" "+neighborRequest.getClock()+" > "+clock);

                    //if different file
                    if(neighborRequest.getFileName().compareTo(fileName) !=0){
                        reply.setContent("ok");
                        token.get(neighborRequest.getFileName()).set(neighborRequest.getFrom(),false);
                        //System.out.println("diff file ok");
                        break;
                    }
                    //if(inCS || waiting && neighborRequest.getFrom()>clientID){
                    if(inCS | (waiting && (neighborRequest.getClock()>=clock))){
                        //System.out.println("******************wait: "+neighborRequest.getClock()+">"+clock);
                        //Thread.sleep(1000);
                        reply.setContent("wait "+neighborRequest.getFrom());
                        request.add(neighborRequest.getFrom());
                        //System.out.println("defer: ");
                    }else{
                        token.get(fileName).set(neighborRequest.getFrom(),false);
                        //System.out.println("******************ok:  "+neighborRequest.getFrom());
                        //System.out.println("******************: "+neighborRequest.getClock()+">"+clock);
                        reply.setContent("ok");
                    }

                    break;
                case "release":
                    reply.setContent("ok");
                    //System.out.println("release from "+neighborRequest.getFrom());
                    token.get(fileName).set(neighborRequest.getFrom(),true);
//                    for (boolean t:token.get(fileName)) {
//                        System.out.print(" "+t);
//                    }
                    //System.out.println(" Release from..."+neighborRequest.getFrom()) ;
                    //socketWrite(socket, reply);
                    break;
                default:
                    //System.out.println("error");
                    reply.setContent("error"+neighborRequest.getContent());
                    //socketWrite(socket,reply);
                    break;
            }
            clockUpdate(neighborRequest);
            socketWrite(socket, reply);
        }
        @Override
        public void run() {

        }
    }


    private void ini(String id){
        clientID = Integer.valueOf(id);
        clientID = clientID%clientIps.length;
        clock =clientID;
    }
    public static void main(String[] args) throws InterruptedException, IOException {

        Client client = new Client();
        if(args.length!=0)
            client.ini(args[0]);
        else
            client.ini("0");
        client.listening();
        Thread.sleep(client.clientID*100);
        client.CallServerThread(client);


        String str;
        while (true) {
            //try {
            str = client.run(0, "enquiry");
            if(str.compareTo("fail")!=0)
                break;
            //} catch (IOException | InterruptedException e) {
             //System.out.println("Wait to connote");
            //}

        }
        ArrayList<String> strToken = new ArrayList<String>(Arrays.asList(str.split("\n")));
        List<Boolean> tmp = new ArrayList<>();
        for(int i = 0;i<client.clientIps.length;i++){
            if(i==client.clientID)
                tmp.add(true);
            else
                tmp.add(false);
        }

        for(int i = 0;i<strToken.size();i++){
            client.token.put(strToken.get(i),tmp);
        }
    }
}
