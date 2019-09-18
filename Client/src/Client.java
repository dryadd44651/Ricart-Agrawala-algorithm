
import java.io.*;
import java.net.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;


public class Client{
    static int[] clientPorts = new int[] { 30000, 30001, 30002, 30003, 30004 };
    //static String[] clientIps = new String[] { "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1" };
    static String[] clientIps = new String[] { "127.0.0.1","127.0.0.1","127.0.0.1"};
    static boolean[] token = new boolean[]{ false,false,false};
    String[] serverIps = new String[] { "127.0.0.1", "127.0.0.1", "127.0.0.1" };
    int[] serverPorts = new int[] { 30500, 30501, 30502 };
    private static int clientID = 0;
    private static Message socketRead(Socket socket){
        BufferedReader br;
        Message message = new Message(0,"",0,0,"");;
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            message =(Message) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            message.setContent("fail");
        }
        return  message;
    }
    private static boolean socketWrite(Socket socket, Message message) throws InterruptedException {

        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);
            oos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static String getTimeStamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        String strTime = String.format("%s", timestamp);
        strTime = strTime.substring(0,19);
        return strTime;
    }
    private static int clock = 0;
    private static int permission = 0;
    private static boolean waiting = false;
    private boolean inCS = false;
    private static boolean fifo_flag = false;
    private static String fileName = "";
    private Message getReturnMessage(Message src){
        Message dst = new Message(clock,src.getType(),src.getTo(),src.getFrom(),src.getFileName());
        return dst;
    }
    private List<Message> pendingList = new ArrayList<Message>();;

    private static void broadcastRepuest1(){

    }
    private static void broadcastRepuest() throws IOException, InterruptedException {
        waiting = true;
        //while (fifo_flag);
        //fifo_flag = true;
        int tempClock = clock+1;
        System.out.println("broadcast");
        for (int i = 0;i<clientIps.length;i++){
            if(i!=clientID){
                int finalI = i;
                new Thread() {
                    public void run() {
                        boolean success = false;
                        while (!success) {
                            success = true;
                            Socket socket = null;
                            try {
                                socket = new Socket(clientIps[finalI], clientPorts[finalI]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Message request = new Message(tempClock, "request", clientID, finalI, fileName);
                            request.setContent("request");
                            System.out.println("request" + clientID + " to " + finalI);
                            try {
                                success = socketWrite(socket, request);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message ServerMessage = socketRead(socket);
                            if(ServerMessage.getContent()=="fail")
                                success = false;
                            if(ServerMessage.getContent()=="ok")
                                token[finalI] = true;
                            System.out.println("request sent to"+request.getTo()+success);
                        }
                    }
                }.start();
            }
        }
        //fifo_flag = false;
    }

    private static void broadcastRelease() throws IOException, InterruptedException {
        waiting = true;
        //while (fifo_flag);
        //fifo_flag = true;
        int tempClock = clock+1;
        System.out.println("broadcast");
        for (int i = 0;i<clientIps.length;i++){
            if(i!=clientID){
                int finalI = i;
                new Thread() {
                    public void run() {
                        boolean success = false;
                        while (!success) {
                            success = true;
                            Socket socket = null;
                            try {
                                socket = new Socket(clientIps[finalI], clientPorts[finalI]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Message release = new Message(tempClock, "release", clientID, finalI, fileName);
                            release.setContent("release");
                            System.out.println("release" + clientID + " to " + finalI);
                            try {
                                success = socketWrite(socket, release);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message ServerMessage = socketRead(socket);
                            if(ServerMessage.getContent()=="fail")
                                success = false;
                            if(ServerMessage.getContent()=="ok")
                                token[finalI] = false;
                            System.out.println("request sent to"+release.getTo()+success);
                        }
                    }
                }.start();
            }
        }
        //fifo_flag = false;
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
        for (boolean t:token) {
            if(!t)
                return false;
        }
        return true;
    }

    private void listening() throws IOException, InterruptedException {
        //receive request and reply
        try (ServerSocket ss = new ServerSocket(clientPorts[clientID]);){
            System.out.println("Listening...");
            Socket socket = ss.accept();
            System.out.println("P:"+socket.getInetAddress().getLocalHost()+"connected");


            Message neighborRequest = (Message) socketRead(socket);
            //while (fifo_flag);
            //fifo_flag = true;
            Message reply = getReturnMessage(neighborRequest);
            System.out.println("*********************"+neighborRequest.getContent());
            switch (neighborRequest.getContent()){
                case "request":
                    System.out.println(neighborRequest.getClock()+" get request: ");
                    //reply.setContent("reply");
                    //System.out.println(waiting+" "+neighborRequest.getClock()+" > "+clock);
                    boolean defer =(waiting && neighborRequest.getFileName().compareTo(fileName)==0);
                    //i am waiting and we waiting for same file
                    defer = defer && (neighborRequest.getClock()>clock);
                    if(neighborRequest.getClock()==clock &&!defer)
                        defer = (neighborRequest.getFrom()>clientID);
                    clockUpdate(neighborRequest);

                    //and your clock is higher than me, then you should wait
                    if((!defer&&!inCS)||(pendingList.size()>=clientIps.length-1)) {
                        //replyFun(reply);
                        reply.setContent("ok");
                        socketWrite(socket, reply);
                    }
                    else{
                        reply.setContent("wait");
                        socketWrite(socket, reply);
                        pendingList.add(reply);
                        System.out.println("defer listsize: "+pendingList.size());
                    }
                    break;
                case "reply":
                    //clockUpdate(neighborRequest);
                    //permission++;
                    //neighborRequest.setContent("received");
                    //System.out.println("get reply permission: "+permission+" from "+neighborRequest.getFrom());
                    System.out.println("wwwwww");
                    break;
                case "release":
                    reply.setContent("ok");
                    token[neighborRequest.getFrom()] = true;
                    socketWrite(socket, reply);
                    break;
                default:
                    System.out.println("error");
                    reply.setContent("error"+neighborRequest.getContent());
                    //socketWrite(socket,reply);
                    socketWrite(socket, reply);
                    break;
            }


            //fifo_flag = false;

            //System.out.println("client:"+neighborRequest.getContent()+"end");
        }
    }
    private  void run(int serverID,int action){
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Socket socket = new Socket(serverIps[serverID],serverPorts[serverID]);
            Message message;
            clock++;
            System.out.println("clock: "+ clock);
            switch (action){
                case 0:
                    message = new Message(clock,"ENQUIRY",clientID,serverID,fileName);
                    break;
                case 1:
                    message = new Message(clock,"READ",clientID,serverID,fileName);
                    break;
                default:
                    message = new Message(clock,"WRITE",clientID,serverID,fileName);
                    break;
            }
            message.setContent(clientID+": "+getTimeStamp());
            //broadcast request

            //sent to server
            broadcastRepuest();
            System.out.println("permission: "+permission);
            while (checkToken()){
                Thread.sleep(800);
                System.out.println("permission: "+permission);
            }
            permission = 0;
            inCS = true;
            System.out.println("In CS");
            socketWrite(socket,message);
            //read message from server
            socket.setSoTimeout(3000);
            Message ServerMessage =socketRead(socket);
            System.out.println("server: "+ServerMessage.getContent());
            inCS = false;
            for (boolean t:token)
                t = false;
            System.out.println("Out CS and Release");
            broadcastRelease();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        //Message m = new Message(1,"111",1,1,"123.txt");
        //System.out.println(m.getType());
        //String str = getTimeStamp();
        //System.out.println(str);
        //System.out.println(args[0]);
        if(args.length!=0){
            clientID = Integer.valueOf(args[0]);
            clientID = clientID%clientIps.length;
            clock =clientID;
            System.out.println("clientID: "+clientID);
        }
        for(int i =0;i<token.length;i++){
            if(i<=clientID)
                token[i] = true;
        }


        Client client = new Client();
        new Thread() {
            public void run() {
                try {
                    while (true){
                        client.listening();
                        //Thread.sleep(1000);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        new Thread() {
            public void run() {
                try {
                    //for (int i = 0;i<5;i++){
                    while (true){
                        Random rand = new Random();
                        //Thread.sleep(rand.nextInt(5000));
                        Thread.sleep(rand.nextInt(5000));
                        client.fileName = "123.txt";
                        client.run(0,rand.nextInt(3));



                    }
                } catch (InterruptedException  e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

}
