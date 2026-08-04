import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ClientThread implements Runnable {
    public static final List<ClientThread> clientThread = Collections.synchronizedList(new ArrayList<>());
    private static final ReentrantLock clientsLock = new ReentrantLock();
    private Socket socket;                                      // Client socket
    private Server serverMain;                                  // Reference to main server
    private final List<String> clients = new ArrayList<>();     // list of all clients(their usernames)
    private BufferedReader in;
    private BufferedWriter out;
    private OnlineCounterSafe onlineCounter;
    public String clientUsername;
    private boolean closed = false;
    private static boolean ServerLog = false;                   // Just to change this to be constant amongst all iterations
    private String serverIn;
    private int serverNumber;
    // Just to change this to be constant amongst all iterations

    public ClientThread(Socket socket, Server serverMain, OnlineCounterSafe onlineCounter, Integer serverNumber) {
        try {
            this.socket = socket;
            this.serverMain = serverMain;
            //added a serverNumber to keep track of which server the thread is under
            this.serverNumber = serverNumber;
            this.onlineCounter =  onlineCounter;
            this.out            = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.in             = new BufferedReader(new InputStreamReader (socket.getInputStream ()));
            this.clientUsername = in.readLine();
            this.serverIn = "main";
            synchronized (clientThread) {
                clientThread.add(this);
            }

        } catch(IOException e) {
            this.closeClientThread(socket,in,out);
        }
    }

    public void broadCastMessage(String messageToSend) {                // broadcasts other clients messages to other clients
        List<ClientThread> clientThreads = serverMain.getServerClientList(serverIn);
        synchronized (clientThreads) {
            for (ClientThread c : clientThreads) {                      // For each client thread
                try {
                    if (messageToSend.equals("q")) {
                        closeClientThread(socket, in, out);
                        return;
                    }
                    if (!(c.clientUsername.equals((this.clientUsername)))&&!(messageToSend.isBlank())) {     // only print client message to other clients and dont print empty space messages
                        c.out.write(clientUsername + ": " + messageToSend);
                        c.out.newLine();
                        c.out.flush();
                    }
            } catch (IOException e) {
                    c.closeClientThread(socket, in, out);
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            System.out.println(clientUsername +
                    " connected at " + socket.getInetAddress());

            // adds client to client list and which server they are in
            // creates a new Serversocket for the client ideally used for group chat creation
            serverMain.addClientToServer(clientUsername, serverNumber);
            serverMain.addToChat("main",this);
            onlineCounter.incrementOnline(); // total users online rn
            
            // Test read a txt file:
            File test = new File("discord-system-313/src/ServerLogs/" + serverIn + ".txt");

            // Create file if it doesn't exist
            try {
                if (test.createNewFile()) {
                    System.out.println("File created: " + test.getName());
                }
            } catch (IOException e) {
                System.out.println("Error creating file.");
                e.printStackTrace();
            }   // This is my test file obviously

            // Read all texts
            try (Scanner myReader = new Scanner(test)) {
                synchronized (clientThread) {
//                    if (!ServerLog) {
//                        ServerLog = true;

                        // Read all texts
                        try (Scanner myServerReader = new Scanner(test)) {
                            while (myServerReader.hasNextLine()) {
                                String text = myServerReader.nextLine();
                                System.out.println(text);               // This is why it's read constantly
                            }
                        } catch (IOException e) {
                            System.out.println("Error reading messages, ensure the log actually exists at the specified location.");
                            e.printStackTrace();
                        }
//                    }
                }

                while (myReader.hasNextLine()) {
                    String text = myReader.nextLine();
                    out.write(text);
                    out.newLine();
                }
                out.flush();
            } catch (IOException e) {
                System.out.println("Error reading messages, ensure the log actually exists at the specified location.");
                e.printStackTrace();
            }

            out.write("Connected to server \"main\" as user: " + clientUsername);
            out.newLine();
            out.flush();

            String clientMessage;

            // This is the ONLY loop you need
            while ((clientMessage = in.readLine()) != null) {

                if (clientMessage.equalsIgnoreCase("q")) {      // use q for exiting as client
                    break; // exit loop
                }

                // client wants to send a file to someone
                // format: /sendfile <targetUser> <filesize> <filename>
                if (clientMessage.startsWith("/sendfile")) {
                    String[] parts = clientMessage.split(" ", 4);
                    if (parts.length == 4) {
                        String targetUser = parts[1];
                        long   fileSize   = Long.parseLong(parts[2]);
                        String filename   = parts[3];

                        // find the target and let them know
                        ClientThread target = serverMain.getClientThread(targetUser);
                        if (target != null) {
                            serverMain.addPendingTransfer(targetUser, this, filename, fileSize);
                            target.out.write("FILE_INCOMING:" + clientUsername + ":" + filename + ":" + fileSize);
                            target.out.newLine();
                            target.out.flush();
                        } else {
                            out.write("User '" + targetUser + "' not found.");
                            out.newLine();
                            out.flush();
                        }
                    }
                    continue;
                }

                // target accepted the file, tell the sender to start uploading
                if (clientMessage.startsWith("FILE_ACCEPT:")) {
                    String[] parts    = clientMessage.split(":", 4);
                    String senderName = parts[1];
                    String filename   = parts[2];

                    ClientThread sender = serverMain.getClientThread(senderName);
                    if (sender != null) {
                        sender.out.write("FILE_READY:" + filename);
                        sender.out.newLine();
                        sender.out.flush();
                    }

                    // register the pair so the relay server knows who to connect
                    serverMain.setFileReceiver(senderName, clientUsername);
                    continue;
                }

                // target rejected the file, tell the sender
                if (clientMessage.startsWith("FILE_REJECT:")) {
                    String senderName = clientMessage.split(":", 2)[1];
                    ClientThread sender = serverMain.getClientThread(senderName);
                    if (sender != null) {
                        sender.out.write("FILE_REJECTED");
                        sender.out.newLine();
                        sender.out.flush();
                    }
                    serverMain.removePendingTransfer(clientUsername);
                    continue;
                }

                //if that server exists
                if(clientMessage.startsWith("/join")) {

                    String[] parts = clientMessage.split(" ", 2);

                    if (parts.length < 2) {
                        out.write("Usage: /join <serverName>");
                        out.newLine();
                        out.flush();
                        continue;
                    }

                    String serverName = parts[1];

                    HashMap<String, List<ClientThread>> servers = serverMain.getServers();
                    if (servers.containsKey(serverName)) {
                        serverMain.removeFromChat(serverIn, this);
                        serverIn = serverName;
                        serverMain.addToChat(serverName, this);
                        try {
                            out.write("Server Joined: " + serverIn);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        // Test read a txt file:
                        File NewServertest = new File("discord-system-313/src/ServerLogs/" + serverIn + ".txt");

                        // Create file if it doesn't exist
                        try {
                            if (test.createNewFile()) {
                                System.out.println("File created: " + test.getName());
                            }
                        } catch (IOException e) {
                            System.out.println("Error creating file.");
                            e.printStackTrace();
                        }   // This is my test file obviously

                        try (Scanner myReader = new Scanner(NewServertest)) {
                            synchronized (clientThread) {
//                    if (!ServerLog) {
//                        ServerLog = true;

                                // Read all texts
                                try (Scanner myServerReader = new Scanner(NewServertest)) {
                                    while (myServerReader.hasNextLine()) {
                                        String text = myServerReader.nextLine();
                                        System.out.println(text);               // This is why it's read constantly
                                    }
                                } catch (IOException e) {
                                    System.out.println("Error reading messages, ensure the log actually exists at the specified location.");
                                    e.printStackTrace();
                                }
//                    }
                            }

                            while (myReader.hasNextLine()) {
                                String text = myReader.nextLine();
                                out.write(text);
                                out.newLine();
                            }
                            out.flush();
                        } catch (IOException e) {
                            System.out.println("Error reading messages, ensure the log actually exists at the specified location.");
                            e.printStackTrace();
                        }
                    }
                }

                if(clientMessage.startsWith("/create")) {
                    String[] parts = clientMessage.split(" ",2);

                    if (parts.length < 2) {
                        out.write("Usage: /create <serverName>");
                        out.newLine();
                        out.flush();
                        continue;
                    }

                    String newServer = parts[1];

                    serverMain.removeFromChat(serverIn, this);
                    serverIn = newServer;
                    serverMain.addToChat(newServer, this);

                    out.write("Server Created: " + serverIn);
                    out.newLine();
                    out.flush();
                    out.write("Server Joined: " + serverIn);
                    out.newLine();
                    out.flush();

                    // Test read a txt file:
                    File NewServertest = new File("discord-system-313/src/ServerLogs/" + serverIn + ".txt");

                    // Create file if it doesn't exist
                    try {
                        if (test.createNewFile()) {
                            System.out.println("File created: " + test.getName());
                        }
                    } catch (IOException e) {
                        System.out.println("Error creating file.");
                        e.printStackTrace();
                    }   // This is my test file obviously

                    // Read all texts
                    try (Scanner myReader = new Scanner(NewServertest)) {
                        synchronized (clientThread) {

                                // Read all texts
                                try (Scanner myServerReader = new Scanner(NewServertest)) {
                                    while (myServerReader.hasNextLine()) {
                                        String text = myServerReader.nextLine();
                                        System.out.println(text);               // This is why it's read constantly
                                    }
                                } catch (IOException e) {
                                    System.out.println("Error reading messages, ensure the log actually exists at the specified location.");
                                    e.printStackTrace();
                                }
                        }

                        while (myReader.hasNextLine()) {
                            String text = myReader.nextLine();
                            out.write(text);
                            out.newLine();
                        }
                        out.flush();
                    } catch (IOException e) {
                        System.out.println("Error reading messages, ensure the log actually exists at the specified location.");
                        e.printStackTrace();
                    }
                }

                // Chat Commands:
                if(clientMessage.equals("/status")) {
                    out.write("Current Chatroom: " + serverIn);
                    out.newLine();
                    out.flush();
                }
                if(clientMessage.equals("/online")) {
                    out.write("Number of users online: " + onlineCounter.getOnlineCount());
                    out.newLine();
                    out.flush();
                }
                if(clientMessage.equals("/chatrooms")) {
                    HashMap<String, List<ClientThread>> servers = serverMain.getServers();
                    out.write("Chat Rooms " + servers.keySet());
                    out.newLine();
                    out.flush();
                }
                if(clientMessage.equals("/clients")) {
                    clientsLock.lock();
                    try {
                        HashMap<String, Integer> clients = serverMain.getClients();
                        out.write("Clients: " + clients.keySet());
                        out.newLine();
                        out.flush();
                    } finally {
                        clientsLock.unlock();
                    }
                    continue;
                }
                if(clientMessage.startsWith("/changeUsername")) {
                    String[] parts = clientMessage.split(" ",2);

                    if (parts.length < 2) {
                        out.write("Usage: /changeUsername <newUsername>");
                        out.newLine();
                        out.flush();
                        continue;
                    }

                    String oldUsername = clientUsername;
                    String newUsername = parts[1];

                    clientsLock.lock();
                    try {
                        clientUsername = newUsername;

                        serverMain.removeClient(oldUsername);
                        serverMain.addClientToServer(newUsername, serverNumber);

                    } finally {
                        clientsLock.unlock();
                    }
                    out.write("Username successfully changed to " + newUsername);
                    out.newLine();
                    out.flush();

                    broadCastMessage(oldUsername + " changed name to " + newUsername);
                    continue;
                }

                // End of Chat Commands, Format message to send to Server
                    System.out.println(clientUsername + ": " + clientMessage);

                    // write all messages that any client sends to the server, and also to the logging system
                    if ((!clientMessage.contains("/create")) && (!clientMessage.contains("/join"))) {       // don't log create or join messages
                        FileWriter messagetoSend = new FileWriter("discord-system-313/src/ServerLogs/"+serverIn+".txt", true);
                        messagetoSend.write(clientUsername + ": " + clientMessage);
                        messagetoSend.write("\n");
                        messagetoSend.close();
                        //System.out.println(clientMessage);                        // print message so others can see
                    }
                    broadCastMessage(clientMessage);                                // This is for printing to Clients
                }

            } catch (IOException e) {
            // client disconnected
        } finally {
            closeClientThread(socket, in, out);
        }
    }

    // Close the Client Thread
    public void closeClientThread(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {

        if (closed) return;
        closed = true;

        synchronized (clientThread){
            clientThread.remove(this);
        }
        serverMain.removeClient(clientUsername);
        System.out.println(clientUsername + " has disconnected from server.");
        broadCastMessage(" has left the server");
        onlineCounter.decrementOnline();
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


