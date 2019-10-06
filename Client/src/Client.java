
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
    int releaseCounter = 0;

    Map<String,List<Boolean>> token = new HashMap<String,List<Boolean>>();

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
    synchronized private void counterPlus() {releaseCounter++;}
    private Message getReturnMessage(Message src){
        Message dst = new Message(clock, src.getType(),src.getTo(),src.getFrom(),src.getFileName());
        return dst;
    }
    private void broadcastRequest(){
        //turn on the waiting flag(sending request)
        waiting = true;

        int tempClock = clock+1;
        //broadcast the request to all client
        for (int i = 0;i<clientIps.length;i++){
            //beside the one which we already have
            if(i!=clientID&& !token.get(fileName).get(i)){
                int finalI = i;
                //start new thread
                new Thread() {
                    public void run() {
                        boolean success = false;
                        //sent until success
                        while (!success) {
                            Socket socket = null;
                            success = true;
                            try {
                                socket = new Socket(clientIps[finalI], clientPorts[finalI]);
                                Message request = new Message(tempClock, "request", clientID, finalI, fileName);
                                //set the message to request
                                request.setContent("request");
                                //sending (success is a flag of socketWrite result)
                                success = socketWrite(socket, request);
                                //read form client
                                Message ServerMessage = socketRead(socket);
                                //request has been deferred
                                if (ServerMessage.getContent().compareTo("wait") == 0){
                                    token.get(fileName).set(finalI,false);
                                    //System.out.println(finalI+"say wait!! :");

                                }//get token
                                else if (ServerMessage.getContent().compareTo("ok") == 0) {
                                    //System.out.println("request sent to (ok) :" + request.getTo());
                                    token.get(fileName).set(finalI,true);
                                    //System.out.println(finalI+"say ok!! :");

                                }//connection error(resent)
                                else {
                                    System.out.println(finalI+"connect error:!!!!!!!!!!!!!!!!!");
                                    success = false;
                                    Thread.sleep(200);

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

    }
    private void broadcastRelease() throws InterruptedException {
        //exit critical section, turn off flag
        waiting = false;
        int tempClock = clock+1;


        //for (int i = 0;i<clientIps.length;i++){
        //only release the token to the client who ask for it
        for (int i:request) {
            if(i!=clientID){
                int finalI = i;
                new Thread() {
                    public void run() {

                        boolean success = false;
                        //sending message util success
                        while (!success) {
                            Socket socket = null;
                            try {
                                success = true;
                                socket = new Socket(clientIps[finalI], clientPorts[finalI]);
                                Message release = new Message(tempClock, "release", clientID, finalI, fileName);
                                //set the message to release
                                release.setContent("release");
                                //sending
                                success = socketWrite(socket, release);
                                //read the result of sending message
                                Message ServerMessage = socketRead(socket);
                                //sent success (counterPlus(): counting the number of release)
                                if (ServerMessage.getContent().compareTo("ok") == 0) {
                                    token.get(fileName).set(finalI,false);
                                    //System.out.println("release to "+finalI);
                                    counterPlus();
                                } else {//release fail, resent the message
                                    System.out.println("Release error " + ServerMessage.getContent());
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
        //wait until all message has been released
        //while (releaseCounter<clientIps.length-1){
        while (releaseCounter<request.size()){
            //System.out.println(releaseCounter+"<"+(clientIps.length-1));
            Thread.sleep(30);
        }
        //clear the request list and reset the counter
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
        //a thread is always listening to other client
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
                        //once receive the message from other client put the connection to thread pool
                        //this is for multi-client, process message parallelly
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
        //broadcast the request for token
        broadcastRequest();

        //wait until receiving enough of token
        while (true){
            if(checkToken())
                break;
            //for (boolean t:token.get(fileName)) {
                //System.out.print(" "+t);
            //}
            //System.out.println(" Waiting..."+token.length) ;

            Thread.sleep(10);
        }
        //enter critical section, turn on the flag
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
        //create a new loop thread for connection of server
        new Thread() {
            public void run() {
                try {
                    //scan the user input
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
                        //split the input
                        ArrayList<String> commandTokens = new ArrayList<String>(Arrays.asList(userCommand.split(" ")));
                        Random rand = new Random();
                        command = commandTokens.get(0);
                        //the third parameter is run times
                        if(commandTokens.size() == 3 && times ==0)
                            times = Integer.valueOf(commandTokens.get(2))%51;
                        //command test: test randomly (read/write) 20 times
                        if(command.compareTo("test") == 0 && times ==0)
                            times = 20;

                        //analyse command
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
                                //randomly choose read and write
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

                        //second parameter is file name
                        if(commandTokens.size()>=2)
                            client.fileName = commandTokens.get(1);
                        else{
                            System.out.println("Please enter the file name");
                            times = 0;
                            continue;
                        }
                        //randomly pause
                        if(times>0) {
                            Thread.sleep(rand.nextInt(2) * 1000);
                            times--;
                            System.out.println(times);
                        }


                        //ask token from other clients
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
                        //release token to other clients
                        CSleave();

                    }
                    System.out.println("Exiting...");
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
    private class Handler implements Runnable {
        Socket socket;
        Message neighborRequest;
        Message reply;
        public Handler(Socket src) throws IOException, InterruptedException {
            this.socket = src;
        }
        @Override
        public void run() {
            //get get the message from other client
            neighborRequest = (Message) socketRead(socket);
            reply = getReturnMessage(neighborRequest);
            //System.out.println(neighborRequest.getContent());
            //determine which kind of message
            switch (neighborRequest.getContent()){
                //a request message: give token or defer request
                case "request":
                    //System.out.println(" get request: "+neighborRequest.getClock());
                    //reply.setContent("reply");
                    //System.out.println(waiting+" "+neighborRequest.getClock()+" > "+clock);

                    //different file: give token, and set our token to false
                    if(neighborRequest.getFileName().compareTo(fileName) !=0){
                        reply.setContent("ok");
                        token.get(neighborRequest.getFileName()).set(neighborRequest.getFrom(),false);
                        //System.out.println("diff file ok");
                        break;
                    }
                    //if client is in critical section or is waiting and clock is slower, defer message and set token to true
                    if(inCS | (waiting && (neighborRequest.getClock()>=clock))){
                        //System.out.println("******************wait: "+neighborRequest.getClock()+">"+clock);

                        token.get(fileName).set(neighborRequest.getFrom(),true);
                        reply.setContent("wait "+neighborRequest.getFrom());
                        //put deferred message in to the list
                        request.add(neighborRequest.getFrom());

                        //System.out.println("defer: ");
                        //otherwise, give token and set our token to false
                    }else{
                        token.get(fileName).set(neighborRequest.getFrom(),false);
                        //System.out.println("******************ok:  "+neighborRequest.getFrom());
                        //System.out.println("******************: "+neighborRequest.getClock()+">"+clock);
                        reply.setContent("ok");
                    }

                    break;
                //receive the release message set token to true and reply ok
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
                //wrong command
                default:
                    //System.out.println("error");
                    reply.setContent("error"+neighborRequest.getContent());
                    //socketWrite(socket,reply);
                    break;
            }
            //update clock and sent message
            clockUpdate(neighborRequest);
            socketWrite(socket, reply);
        }
    }
    private void ini(String id){
        clientID = Integer.valueOf(id);
        clientID = clientID%clientIps.length;
        clock =clientID;
    }
    public static void main(String[] args) throws InterruptedException, IOException {

        Client client = new Client();
        //read from input parameter and set the serial number of client
        if(args.length!=0)
            client.ini(args[0]);
        else
            client.ini("0");
        //listen to other client (decide which client should be executed)
        client.listening();

        //hold a little while to prevent to connect to the server at the same time
        Thread.sleep(client.clientID*100);
        //create a loop to connect server
        client.CallServerThread(client);

        //str: store the server's file list
        String str;
        //connect to server until success
        while (true) {
            str = client.run(0, "enquiry");
            if(str.compareTo("fail")!=0)
                break;
        }
        //get each file name and create the token for each file
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
