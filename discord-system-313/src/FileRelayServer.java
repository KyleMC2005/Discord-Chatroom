import java.io.*;
import java.net.*;
import java.util.concurrent.*;


class FileRelayServer implements Runnable {
    private final int port;
    private final Server server;

    // clients park here while waiting for the other side to connect
    private final ConcurrentHashMap<String, Socket> waitingReceivers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Socket> waitingSenders   = new ConcurrentHashMap<>();

    public FileRelayServer(int port, Server server) {
        this.port   = port;
        this.server = server;
    }

    @Override
    public void run() {
        try (ServerSocket fss = new ServerSocket(port)) {
            System.out.println("File relay server running on port " + port);
            // thread pool so multiple transfers can happen at the same time
            ExecutorService pool = Executors.newCachedThreadPool();

            while (!fss.isClosed()) {
                Socket conn = fss.accept();
                pool.submit(() -> handleConnection(conn));
            }
        } catch (IOException e) {
            System.out.println("File relay server error: " + e.getMessage());
        }
    }

    private void handleConnection(Socket conn) {
        try {
            DataInputStream dis = new DataInputStream(conn.getInputStream());

            String role     = dis.readUTF(); // either "SEND" or "RECEIVE"
            String username = dis.readUTF(); // who is connecting

            if (role.equals("RECEIVE")) {
                // find which sender this receiver is paired with
                String matchedSender = findSenderForReceiver(username);

                if (matchedSender != null && waitingSenders.containsKey(matchedSender)) {
                    // sender already here, start the transfer now
                    Socket senderSocket = waitingSenders.remove(matchedSender);
                    server.removeFileReceiver(matchedSender);
                    relayFile(senderSocket, conn);
                } else {
                    // sender not connected yet, wait for them
                    waitingReceivers.put(username, conn);
                }

            } else if (role.equals("SEND")) {
                String receiverName = server.getFileReceiver(username);

                if (receiverName != null && waitingReceivers.containsKey(receiverName)) {
                    // receiver already here, start the transfer now
                    Socket receiverSocket = waitingReceivers.remove(receiverName);
                    server.removeFileReceiver(username);
                    relayFile(conn, receiverSocket);
                } else {
                    // receiver not connected yet, wait for them
                    waitingSenders.put(username, conn);
                }
            }

        } catch (IOException e) {
            System.out.println("File relay connection error: " + e.getMessage());
        }
    }

    // checks which sender is mapped to this receiver
    private String findSenderForReceiver(String receiverUsername) {
        for (ClientThread ct : ClientThread.clientThread) {
            String mapped = server.getFileReceiver(ct.clientUsername);
            if (receiverUsername.equals(mapped)) {
                return ct.clientUsername;
            }
        }
        return null;
    }

    // reads bytes from sender and writes them straight to receiver
    private void relayFile(Socket senderSocket, Socket receiverSocket) {
        new Thread(() -> {
            try {
                DataInputStream  senderIn    = new DataInputStream(senderSocket.getInputStream());
                DataOutputStream receiverOut = new DataOutputStream(receiverSocket.getOutputStream());

                long fileSize  = senderIn.readLong();
                long remaining = fileSize;
                byte[] buffer  = new byte[8192];
                int bytesRead;

                while (remaining > 0 &&
                        (bytesRead = senderIn.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    receiverOut.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                receiverOut.flush();

                System.out.println("[Relay] Transfer complete (" + fileSize + " bytes).");

                senderSocket.close();
                receiverSocket.close();

            } catch (IOException e) {
                System.out.println("Relay error: " + e.getMessage());
            }
        }).start();
    }
}