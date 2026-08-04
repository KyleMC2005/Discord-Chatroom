import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;

import java.awt.*;
import java.io.IOException;

import static java.lang.System.exit;

public class Client extends Application {
    Stage window;
    Scene loginScene, mainScene;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private String username;
    private String pendingFilePath; // the file we want to send
    private volatile boolean waitingForFileAccept = false; // true when we are waiting on a y/n answer
    private boolean connected;
    private VBox msgBox;
    private ScrollPane msgDisplay;
    private TextField txtInput;

    public static void main(String[] args) {
        launch(args);
    }

    public void connectClient (Socket socket, String username) {
        try {
            this.socket = socket;
            System.out.println("Connected to server.");
            this.connected = true;

            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Reads messages from other clients, and reads current client's input
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); //
            this.username = username;

            out.write(username);
            out.newLine();
            out.flush();

        } catch (IOException e) {
            closeClient(socket,in,out);
        }

    }

    private void addMessage(String message) {
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(Double.MAX_VALUE);

        msgBox.getChildren().add(msgLabel);

        msgDisplay.layout();
    }

    public void listenForMessages() {       // Look for any incoming messages from other clients, and display on client's side
        new Thread(() -> {              // runs as a thread, client is always listening for messages
            String messageFromServer;
            try {
                while ((messageFromServer = in.readLine()) != null) {

                    // someone wants to send us a file
                    if (messageFromServer.startsWith("FILE_INCOMING:")) {
                        String[] parts = messageFromServer.split(":", 4);
                        String from     = parts[1];
                        String filename = parts[2];
                        long   size     = Long.parseLong(parts[3]);

                        System.out.println("[FILE] " + from + " wants to send you '" + filename + "' (" + size + " bytes). Accept? (y/n)");

                        // stop sendMessage() from grabbing the input
                        waitingForFileAccept = true;
                        Scanner sc = new Scanner(System.in);
                        String answer = sc.nextLine().trim();
                        waitingForFileAccept = false;

                        if (answer.equalsIgnoreCase("y")) {
                            out.write("FILE_ACCEPT:" + from + ":" + filename + ":" + size);
                            out.newLine();
                            out.flush();
                            receiveFile(filename, size);
                        } else {
                            out.write("FILE_REJECT:" + from);
                            out.newLine();
                            out.flush();
                        }
                        continue;
                    }

                    // other client accepted, start sending
                    if (messageFromServer.startsWith("FILE_READY:")) {
                        String filename = messageFromServer.split(":", 2)[1];
                        System.out.println("[FILE] Transfer accepted! Sending " + filename + "...");
                        sendFileData(filename);
                        continue;
                    }

                    // other client said no
                    if (messageFromServer.equals("FILE_REJECTED")) {
                        System.out.println("[FILE] Transfer was rejected.");
                        continue;
                    }

                    if (messageFromServer.equalsIgnoreCase("q")) {
                        closeClient(socket, in, out);
                        break;
                    }

                    System.out.println("FROM SERVER: " + messageFromServer);

                    String finalMessageFromServer = messageFromServer;
                    Platform.runLater(() -> addMessage(finalMessageFromServer));
                }

                System.out.println("Disconnected from server.");
                closeClient(socket, in, out);

            } catch (IOException e) {
                closeClient(socket, in, out);
            }
        }).start();
    }

//    public void sendMessage() {
//        try {
//            out.write(username);
//            out.newLine();
//            out.flush();
//
//            Scanner scanner = new Scanner(System.in);
//
//            while (socket.isConnected()) {
//                String messageToSend = scanner.nextLine();
//
//                // ignore input while we are waiting on a file accept prompt
//                if (waitingForFileAccept) {
//                    continue;
//                }
//
//                // send a file to another user, usage: /sendfile <username> <filepath>
//                if (messageToSend.startsWith("/sendfile")) {
//                    String[] parts = messageToSend.split(" ", 3);
//                    if (parts.length == 3) {
//                        String targetUser = parts[1];
//                        String filePath   = parts[2];
//                        File   file       = new File(filePath);
//
//                        if (!file.exists()) {
//                            System.out.println("[FILE] File not found: " + filePath);
//                            continue;
//                        }
//
//                        this.pendingFilePath = filePath;
//
//                        out.write("/sendfile " + targetUser + " " + file.length() + " " + file.getName());
//                        out.newLine();
//                        out.flush();
//                        System.out.println("[FILE] Waiting for " + targetUser + " to accept...");
//                    } else {
//                        System.out.println("Usage: /sendfile <username> <filepath>");
//                    }
//                    continue;
//                }
//
//                out.write(messageToSend);
//                out.newLine();
//                out.flush();
//            }
//
//        } catch (IOException e) {
//            closeClient(socket, in, out);
//        }
//    }

    // connects to port 5002 and sends the file as bytes
    private void sendFileData(String filename) {
        new Thread(() -> {
            try (Socket fileSocket = new Socket("localhost", 5002)) {
                DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());

                dos.writeUTF("SEND");
                dos.writeUTF(username);

                File file = new File(pendingFilePath);
                dos.writeLong(file.length());

                // send in chunks so big files dont break
                byte[] buffer = new byte[8192];
                int bytesRead;
                try (FileInputStream fis = new FileInputStream(file)) {
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                    }
                }
                dos.flush();
                System.out.println("[FILE] Upload complete: " + filename);

            } catch (IOException e) {
                System.out.println("File send error: " + e.getMessage());
            }
        }).start();
    }


    // connects to port 5002 and saves the incoming file
    private void receiveFile(String filename, long size) {
        new Thread(() -> {
            try (Socket fileSocket = new Socket("localhost", 5002)) {
                DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());
                DataInputStream  dis = new DataInputStream(fileSocket.getInputStream());

                dos.writeUTF("RECEIVE");
                dos.writeUTF(username);
                dos.flush();

                // save the file with received_ prefix so we know where it came from
                File saveFile = new File("received_" + filename);
                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    byte[] buffer    = new byte[8192];
                    long   remaining = size;
                    int    bytesRead;
                    while (remaining > 0 &&
                            (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }
                }
                System.out.println("[FILE] Saved: " + saveFile.getAbsolutePath());

            } catch (IOException e) {
                System.out.println("File receive error: " + e.getMessage());
            }
        }).start();
    }

    public void closeClient(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {     // closes current socket, reader and writer
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }

            // removes username from names in use file
            File file = new File("discord-system-313/src/namesInUse.txt");
            String content = new String(Files.readAllBytes(file.toPath()));
            content = content.replace(username + "\n", "");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        exit(0);    // terminates client after all related elements closed
    }
//    public static void main(String[] args) throws IOException {
//        launch(args);
//        boolean valid = false;
//        Scanner scanner = new Scanner(System.in);
//        String inputName = "user";      // placeholder username in case something messes up
//
//        // simple input validation gotta love it
//        while(!valid){
//            System.out.println("Enter your username: ");
//            inputName = scanner.nextLine();
//
//            // check name isn't already in use because that causes issues if there are multiple users with same username
//            BufferedReader reader = new BufferedReader(new FileReader("discord-system-313/src/namesInUse.txt"));
//            String line = reader.readLine();
//            ArrayList<String> names = new ArrayList<>();
//
//            while (line != null) {
//                names.add(line);
//                line = reader.readLine();
//            }
//
//            if (inputName.isBlank()){
//                System.out.println("Invalid username, must include characters");
//            } else if (names.contains(inputName)) {
//                System.out.println("This username is already in use, choose a different name");
//            } else {
//                valid = true;
//            }
//
//        }
//
//        FileWriter newOnlineUser = new FileWriter("discord-system-313/src/namesInUse.txt", true);
//        newOnlineUser.write(inputName);
//        newOnlineUser.write("\n");
//        newOnlineUser.close();
//
//        Socket socket = new Socket("localhost", 5001);   // Creates a new socket with the given host and port
//        Client client = new Client(socket, inputName);
//        client.listenForMessages();
//        client.sendMessage();
//
//
//    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        window = primaryStage;


        //objects
        Label welcomeLabel = new Label("Welcome");

        Image userImage = new Image("Images/user.png");
        ImageView userImageView = new ImageView(userImage);

        Label loginLabel = new Label("Username: ");

        TextField loginField = new TextField();

        Label errorLabel = new Label();

        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> {
            String inputName = loginField.getText().trim();
            BufferedReader reader = null;
            ArrayList<String> names;
            try {
                reader = new BufferedReader(new FileReader("discord-system-313/src/namesInUse.txt"));
                String line = reader.readLine();
                names = new ArrayList<>();
                while (line != null) {
                    names.add(line);
                    line = reader.readLine();
                }
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            if (inputName.isBlank() || inputName.startsWith("/")){
				System.out.println("Invalid username, must include characters");
                errorLabel.setText("Invalid username, try again");
            }
            else if (names.contains(inputName)) {
                System.out.println("Invalid username");
                errorLabel.setText("Username already in use, try again");
            } else {

            try {
                FileWriter newOnlineUser = new FileWriter("discord-system-313/src/namesInUse.txt", true);
                newOnlineUser.write(inputName);
                newOnlineUser.write("\n");
                newOnlineUser.close();

                Socket socket = new Socket("localhost", 5001);
                connectClient(socket, inputName);
                listenForMessages();

                window.setScene(mainScene);
                window.setTitle("Chat");
                window.setResizable(true);
                window.setMaximized(true);

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
                });

        msgBox = new VBox(10);
        msgDisplay = new ScrollPane(msgBox);
        msgDisplay.setFitToWidth(true);
        msgDisplay.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        msgDisplay.vvalueProperty().bind(msgBox.heightProperty());


        txtInput = new TextField();

        txtInput.setOnAction(e -> {
            String message = txtInput.getText().trim();

            try{
                    out.write(message);
                    out.newLine();
                    out.flush();

                    if(message.length()>0){
                        Platform.runLater(() -> addMessage(username + ": " + message));
                    }

                    if ((message.startsWith("/create")) || (message.startsWith("/join"))) {
                        msgBox.getChildren().clear();       // This wipes the box every time to clear it of previous texts
                                                            // Create and join bring text back
                    }

                    if (message.startsWith("/sendfile")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length == 3) {
                        String targetUser = parts[1];
                        String filePath   = parts[2];
                        File   file       = new File(filePath);

                        if (!file.exists()) {
                            System.out.println("[FILE] File not found: " + filePath);
                        }

                        this.pendingFilePath = filePath;

                        out.write("/sendfile " + targetUser + " " + file.length() + " " + file.getName());
                        out.newLine();
                        out.flush();
                        System.out.println("[FILE] Waiting for " + targetUser + " to accept...");
                    } else {
                        System.out.println("Usage: /sendfile <username> <filepath>");
                    }
                }


                    // ignore input while we are waiting on a file accept prompt

//                    addMessage(username + ": " + message);
                    txtInput.clear();

            }catch (IOException ex){
                ex.printStackTrace();
            }



        });

        //Login Page
        //Building
        VBox loginFieldHolder = new VBox(5);
        loginFieldHolder.getChildren().addAll(loginLabel,loginField, errorLabel);
        VBox loginVBox = new VBox(20);
        loginVBox.getChildren().addAll(welcomeLabel,userImageView,loginFieldHolder, loginButton);
        BorderPane loginBorderPane = new BorderPane();
        loginBorderPane.setCenter(loginVBox);
        BorderPane backgroundPane = new BorderPane();
        backgroundPane.setCenter(loginBorderPane);
        loginScene = new Scene(backgroundPane, 800, 600);

        //styling
        loginScene.getStylesheets().add("styles.css");
        loginBorderPane.getStyleClass().add("loginBorderPane");
        loginLabel.getStyleClass().add("loginLabels");
        errorLabel.getStyleClass().add("loginLabels");
        backgroundPane.getStyleClass().add("backgroundPane");
        loginButton.getStyleClass().add("loginButton");
        loginField.getStyleClass().add("loginField");
        welcomeLabel.getStyleClass().add("welcomeLabel");
        userImageView.setFitWidth(150);
        userImageView.setFitHeight(150);
        userImageView.setPreserveRatio(true);
        userImageView.setSmooth(true);
        userImageView.setCache(true);

        loginField.setMaxWidth(350);

        loginFieldHolder.setAlignment(Pos.CENTER);
        loginVBox.setAlignment(Pos.CENTER);

        loginBorderPane.setMaxWidth(600);
        loginBorderPane.setMaxHeight(450);

        loginButton.setMaxWidth(110);

        //Main View
        VBox gcButtonVBox = new VBox(10);
        HBox titleBox = new HBox(10);
        BorderPane chatContainer = new BorderPane();
        BorderPane mainViewBackgroundPane = new BorderPane();

        mainViewBackgroundPane.setCenter(chatContainer);
        mainViewBackgroundPane.setLeft(gcButtonVBox);
        mainViewBackgroundPane.setTop(titleBox);



        chatContainer.setBottom(txtInput);
        chatContainer.setCenter(msgDisplay);
        mainScene = new Scene(mainViewBackgroundPane, 600, 600);

        //Styling
        mainScene.getStylesheets().add("styles.css");
        mainViewBackgroundPane.getStyleClass().add("mainViewBackgroundPane");
        gcButtonVBox.getStyleClass().add("gcButtonVBox");
        titleBox.getStyleClass().add("titleBox");
        chatContainer.getStyleClass().add("chatContainer");
        txtInput.getStyleClass().add("txtInput");
        msgDisplay.getStyleClass().add("msgDisplay");

        mainViewBackgroundPane.setPadding(new Insets(10));
        gcButtonVBox.setPadding(new Insets(10));
        titleBox.setPadding(new Insets(10));
        chatContainer.setPadding(new Insets(10));

        gcButtonVBox.setAlignment(Pos.CENTER);
        gcButtonVBox.setPrefWidth(180);
        titleBox.setPrefHeight(100);

        msgDisplay.setPrefHeight(Region.USE_COMPUTED_SIZE);
        msgDisplay.setPrefWidth(Region.USE_COMPUTED_SIZE);
        msgBox.setPrefWidth(Region.USE_COMPUTED_SIZE);

        //set window
        window.setScene(loginScene);
        window.setTitle("Login");
        window.setResizable(false);
        window.show();

    }
}