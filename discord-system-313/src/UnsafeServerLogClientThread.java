//  Commented out as it doesn't match format of our client thread anymore, so it causes errors
//
// import java.io.*;
//import java.net.Socket;
//import java.util.*;
//
//public class UnsafeServerLogClientThread implements Runnable {
//    public static final List<UnsafeServerLogClientThread> clientThread = Collections.synchronizedList(new ArrayList<>());
//    private Socket socket; // Client socket
//    private Server serverMain; // Reference to the main server
//    private BufferedReader in;
//    private BufferedWriter out;
//    private String clientUsername;
//    private Boolean ServerLog = false;
//
//    public UnsafeServerLogClientThread(Socket socket, Server serverMain) {
//        try {
//            this.socket = socket;
//            this.serverMain = serverMain;
//            this.out            = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//            this.in             = new BufferedReader(new InputStreamReader (socket.getInputStream ()));
//            this.clientUsername = in.readLine();
//
//            synchronized (clientThread) {
//                clientThread.add(this);
//            }
//
//        } catch(IOException e) {
//            this.closeClientThread(socket,in,out);
//        }
//    }
//
//    public void broadCastMessage(String messageToSend) {       // broadcasts other clients messages to other clients
//        synchronized (clientThread) {
//            for (UnsafeServerLogClientThread c : clientThread) {           // For each client thread
//                try {
//                    if (messageToSend.equals("q")) {
//                        closeClientThread(socket, in, out);
//                        return;
//                    }
//                    if (!(c.clientUsername.equals((this.clientUsername))) && !(messageToSend.isEmpty())) {     // only print client message to other clients and dont print empty space messages
//                        c.out.write(clientUsername + ": " + messageToSend);
//                        c.out.newLine();
//                        c.out.flush();
//                    }
//                } catch (IOException e) {
//                    closeClientThread(socket, in, out);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void run() {
//        try {
//
//            System.out.println(clientUsername +
//                    " connected at " + socket.getInetAddress());
//
//            serverMain.addClient(clientUsername);
//            // Test read a txt file:
//            File test = new File("discord-system-313/src/test.txt");   // This is my test file obviously
//
//            // This is for any troubleshooting:
//
//            // Read all texts
//            try (Scanner myReader = new Scanner(test)) {
//                // No sync therefore if two threads join at the same time, this can cause logs to print twice
//                if (ServerLog == false) {
//                    ServerLog = true;
//                    // Read all texts
//                    try (Scanner myServerReader = new Scanner(test)) {
//                        while (myServerReader.hasNextLine()) {
//                            String text = myServerReader.nextLine();
//                            System.out.println(text);               // This is why it's read constantly
//                        }
//                    } catch (IOException e) {
//                        System.out.println("Error reading messages, ensure the log actually exists at the specified location.");
//                        e.printStackTrace();
//                    }
//                }
//
//                while (myReader.hasNextLine()) {
//                    String text = myReader.nextLine();
//                    out.write(text);
//                    out.newLine();
//                }
//                out.flush();
//            } catch (IOException e) {
//                System.out.println("Error reading messages, ensure the log actually exists at the specified location.");
//                e.printStackTrace();
//            }
//
//            out.write("Connected to server as user: " + clientUsername);
//            out.newLine();
//            out.flush();
//
//            String clientMessage;
//
//            // This is the ONLY loop you need
//            while ((clientMessage = in.readLine()) != null) {
//
//                if (clientMessage.equalsIgnoreCase("q")) {      // use q for exiting as client
//                    break; // exit loop
//                }
//
//                System.out.println(clientUsername + ": " + clientMessage);      // Prints to Server
//                // The writer, it will write all messages any client sends to the server, and also to the logging system
//                FileWriter messagetoSend = new FileWriter("discord-system-313/src/test.txt",true);
//                messagetoSend.write(clientUsername + ": " + clientMessage);
//                messagetoSend.write("\n");
//                messagetoSend.close();
//                System.out.println(clientMessage);  // print message so others can see
//
//                broadCastMessage(clientMessage);                                // This is for printing to Clients
//            }
//
//        } catch (IOException e) {
//            // client disconnected
//        } finally {
//            closeClientThread(socket, in, out);
//        }
//    }
//
//    public void closeClientThread(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
//        synchronized (clientThread){
//            clientThread.remove(this);
//        }
//        serverMain.removeClient(clientUsername);
//        System.out.println(clientUsername + " has disconnected from server.");
//        broadCastMessage(" has left the server");
//        try {
//
//            if (socket != null && !socket.isClosed()) {
//                socket.close();
//            }
//            if (bufferedReader != null) {
//                bufferedReader.close();
//            }
//            if (bufferedWriter != null) {
//                bufferedWriter.close();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//}
