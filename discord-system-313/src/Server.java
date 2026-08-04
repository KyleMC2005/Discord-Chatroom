import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class Server {
    //HASHMAP JOOJOO AHHHH
    //there's absolutely a better way to do this but my smooth brain could only think of this the now.
    private final HashMap<String, Integer> clients = new HashMap<>();// list of all clients(their usernames)
    private final HashMap<String, List<ClientThread>> servers = new HashMap<>();
    private final HashMap<String, Integer> serverNameAndPort = new HashMap<>();
    private final HashMap<String, PendingTransfer>    pendingTransfers  = new HashMap<>(); // tracks file transfers waiting to happen
    private final HashMap<String, String>             fileReceiverMap   = new HashMap<>(); // maps sender -> receiver for the relay server

    private int port = 5001;
    private ServerSocket serverSocket;

    // holds info about a file transfer that hasn't started yet
    public static class PendingTransfer {
        public ClientThread sender;
        public String filename;
        public long fileSize;

        public PendingTransfer(ClientThread sender, String filename, long fileSize) {
            this.sender   = sender;
            this.filename = filename;
            this.fileSize = fileSize;
        }
    }

    // initialise server
    public Server (String name, int port) {
        // Starts server and waits for a connection
        OnlineCounterSafe onlineCounter = new OnlineCounterSafe();
        try {
            FileOutputStream file = new FileOutputStream("discord-system-313/src/namesInUse.txt");
            file.write("\n".getBytes());
            file.close();
        }  catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ServerSocket ss = new ServerSocket(port);
            this.serverSocket = ss;
            serverNameAndPort.put(name, ss.getLocalPort());
            // Creates a new server socket with the given port

            // start the file relay on port 5002 so clients can transfer files
            FileRelayServer fileRelay = new FileRelayServer(5002, this);
            new Thread(fileRelay).start();

            System.out.println("Server is running; waiting for a client...");

            // Loop to continuously listen for new client connections
            while (!ss.isClosed()) {
                Socket socket = ss.accept();                            // Accepts connection from a client when client tries to connect

                // Create a new thread to handle the connected client
                ClientThread Server = new ClientThread(socket, this,onlineCounter, serverNameAndPort.get(name));
                Thread thread = new Thread(Server);
                thread.start();
                // Create a new thread to handle client dms
                System.out.println(clients.size() + " clients connected.");
            }


        } catch (IOException e) {
            this.closeServerSocket();
            throw new RuntimeException(e);
        }
    }

    // file transfer methods

    public synchronized void addPendingTransfer(String receiverUsername, ClientThread sender, String filename, long fileSize) {
        pendingTransfers.put(receiverUsername, new PendingTransfer(sender, filename, fileSize));
    }

    public synchronized void removePendingTransfer(String receiverUsername) {
        pendingTransfers.remove(receiverUsername);
    }

    // called when receiver accepts, so the relay knows who to connect together
    public synchronized void setFileReceiver(String senderUsername, String receiverUsername) {
        fileReceiverMap.put(senderUsername, receiverUsername);
    }

    public synchronized String getFileReceiver(String senderUsername) {
        return fileReceiverMap.get(senderUsername);
    }

    public synchronized void removeFileReceiver(String senderUsername) {
        fileReceiverMap.remove(senderUsername);
    }

    // loops through all connected clients and finds one by username
    public synchronized ClientThread getClientThread(String username) {
        for (ClientThread ct : ClientThread.clientThread) {
            if (ct.clientUsername.equals(username)) return ct;
        }
        return null;
    }


    public synchronized List<ClientThread> getServerClientList(String server) {
        return(servers.get(server));
    }

    //fetches a new port number this will probably cause problems
    public synchronized int getPort() {
        this.port ++;
        System.out.println(this.port);
        return this.port;
    }

    //adds client name to a list and which server they are in
    public synchronized void addToChat(String server, ClientThread client) {
        if(!servers.containsKey(server)) {
            servers.put(server, new ArrayList<>());
        }
        servers.get(server).add(client);
    }

    public synchronized HashMap<String, Integer> getClients() {
        return clients;
    }

    public synchronized void addClientToServer(String UserName,Integer serverNumber) {
        clients.put(UserName, serverNumber);
    }

    public synchronized HashMap<String, List<ClientThread>> getServers() {
        return servers;
    }

    //function unchanged but pretty sure it didnt break
    public synchronized void removeFromChat(String server, ClientThread client) {
        if(servers.containsKey(server)) {
            servers.get(server).remove(client);
        }
    }

    public synchronized void removeClient(String out) {
        clients.remove(out);
    }

    public void closeServerSocket() {
        ServerSocket ss = this.serverSocket;
        try{
            if(ss != null) {
                this.serverSocket.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws IOException
    {
        try {
            // Create a server on port number 5001
            Server server = new Server("main",5001);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}