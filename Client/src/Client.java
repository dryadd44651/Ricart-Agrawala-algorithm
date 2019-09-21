
import java.io.*;
import java.net.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;


public class Client{
    int[] clientPorts = new int[] { 30000, 30001, 30002, 30003, 30004 };
    String[] clientIps = new String[] { "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1" };
    //static String[] clientIps = new String[] { "127.0.0.1","127.0.0.1","127.0.0.1"};
    boolean[] token = new boolean[]{ false,false,false,false,false};
    //static boolean[] token = new boolean[]{ false,false,false};
    String[] serverIps = new String[] { "127.0.0.1", "127.0.0.1", "127.0.0.1" };
    int[] serverPorts = new int[] { 30500, 30501, 30502 };
    private ArrayList<Handler> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(10);

    private static int clientID = 0;
    private Message socketRead(Socket socket){
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
    private boolean socketWrite(Socket socket, Message message) throws InterruptedException {

        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            oos.writeObject(message);
            //oos.writeChars("test");
            oos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
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
    private String fileName = "";
    private Message getReturnMessage(Message src){
        Message dst = new Message(clock,src.getType(),src.getTo(),src.getFrom(),src.getFileName());
        return dst;
    }
    //private List<Message> pendingList = new ArrayList<Message>();
    private void printToken(){
        for (boolean t:token) {
                System.out.print(t);
            }
        System.out.println();
    }
    private void broadcastRepuest() throws IOException, InterruptedException {
        waiting = true;
        //while (fifo_flag);

        int tempClock = clock+1;
        //int tempClock = clock;
        System.out.println("broadcast");
        for (int i = 0;i<clientIps.length;i++){
            if(i!=clientID&&token[i]==false){
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
                                System.out.println("request" + clientID + " to " + finalI);
                                success = socketWrite(socket, request);
                                Message ServerMessage = socketRead(socket);
                                if (ServerMessage.getContent().compareTo("fail") == 0)
                                    success = false;
                                if (ServerMessage.getContent().compareTo("ok") == 0) {
                                    System.out.println("request sent to (ok) :" + request.getTo());
                                    token[finalI] = true;
                                    //printToken();
                                }
                                else {
                                    System.out.println("request sent to(wait) :" + request.getTo() );
                                    token[finalI] = false;
                                    //printToken();
                                }

                                //System.out.println("request sent to" + request.getTo() + success);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
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
        //fifo_flag = false;
    }
    private void broadcastRelease() throws IOException, InterruptedException {
        waiting = true;
        int tempClock = clock+1;
        System.out.println("broadcast releas");
        for (int i = 0;i<clientIps.length;i++){
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
                                System.out.println("release" + clientID + " to " + finalI);

                                success = socketWrite(socket, release);

                                Message ServerMessage = socketRead(socket);
                                if (ServerMessage.getContent().compareTo("ok") == 0) {
                                    token[finalI] = false;
                                    //printToken();
                                } else {
                                    System.out.println("Release error " + ServerMessage.getContent());
                                    success = false;

                                }
                                System.out.println("request sent to " + release.getTo() + success);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
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
            if(t==false) {//find one false, return false
                //System.out.print("checkToken: ");
                //System.out.print(t);
                return false;
            }
        }
        return true;
    }
    private void listening() throws IOException, InterruptedException {
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
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
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
            for (boolean t:token)
                System.out.print(" "+t);
            System.out.println(" Waiting..."+token.length) ;

            Thread.sleep(200);
        }

        inCS = true;
        System.out.println("In CS");
    }
    private  void CSleave() throws IOException, InterruptedException {
        inCS = false;
        waiting = false;
        for (boolean t:token)
            t = false;
        System.out.println("Out CS and Release");
        broadcastRelease();
    }
    private  void run(int serverID,int action) throws IOException, InterruptedException {
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


            socketWrite(socket,message);
            //read message from server
            //Message ServerMessage =socketRead(socket);
            //System.out.println("server: "+ServerMessage.getContent());



        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            System.out.println("time out");
            CSleave();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void CallServerThread(Client client){
        new Thread() {
            public void run() {
                try {
                    for (int i = 0;i<50;i++){
                    //while (true){
                        Random rand = new Random();
                        Thread.sleep(1000);
                        //Thread.sleep(rand.nextInt(5000));
                        client.fileName = "123.txt";

                        CScheck();
                        client.run(0,2);//2 for write (test)
                        CSleave();
                    }
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
            switch (neighborRequest.getContent()){
                case "request":
                    System.out.println(" get request: "+neighborRequest.getClock());
                    //reply.setContent("reply");
                    //System.out.println(waiting+" "+neighborRequest.getClock()+" > "+clock);
                    if(inCS || waiting && neighborRequest.getFrom()>clientID){
                        reply.setContent("wait");
                        socketWrite(socket, reply);
                        //pendingList.add(reply);
                        //System.out.println("defer listsize: "+pendingList.size());
                        break;
                    }else {
                        System.out.println("inCS "+inCS+ " waiting "+waiting+" nID "+neighborRequest.getFrom());
                        token[neighborRequest.getFrom()] = false;
                        reply.setContent("ok");
                    }
                    break;
                case "release":
                    reply.setContent("ok");
                    System.out.println("release from "+neighborRequest.getFrom());
                    token[neighborRequest.getFrom()] = true;
                    //socketWrite(socket, reply);
                    break;
                default:
                    System.out.println("error");
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
        System.out.println("clientID: "+clientID);
        token[clientID] = true;
    }
    public static void main(String[] args) throws InterruptedException, IOException {

        Client client = new Client();
        if(args.length!=0)
            client.ini(args[0]);
        else
            client.ini("0");
        client.listening();
        client.CallServerThread(client);

    }

}
