//import javafx.application.Application;
//import javafx.event.ActionEvent;
//import javafx.event.EventHandler;
//import javafx.geometry.Pos;
//import javafx.scene.Scene;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.control.TextField;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.BorderPane;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.StackPane;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//import javafx.scene.image.Image;
//
//import java.awt.*;
//import java.io.IOException;
//
//public class Main extends Application {
//    Stage window;
//    Scene loginScene, mainScene;
//
//    public static void main(String[] args)  {
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage primaryStage) throws Exception{
//        window = primaryStage;
//
//
//        //objects
//        Label welcomeLabel = new Label("Welcome");
//
//        Image userImage = new Image("Images/user.png");
//        ImageView userImageView = new ImageView(userImage);
//
//        Label loginLabel = new Label("Username: ");
//
//        TextField loginField = new TextField();
//
//        Button loginButton = new Button("Login");
//        loginButton.setOnAction(e -> {
//            window.setScene(mainScene);
//            window.setTitle("Chat");
//            window.setResizable(true);
//            window.setMaximized(true);});
//
//        //Login Page
//        //Building
//        VBox loginFieldHolder = new VBox(5);
//        loginFieldHolder.getChildren().addAll(loginLabel,loginField);
//        VBox loginVBox = new VBox(20);
//        loginVBox.getChildren().addAll(welcomeLabel,userImageView,loginFieldHolder, loginButton);
//        BorderPane loginBorderPane = new BorderPane();
//        loginBorderPane.setCenter(loginVBox);
//        BorderPane backgroundPane = new BorderPane();
//        backgroundPane.setCenter(loginBorderPane);
//        loginScene = new Scene(backgroundPane, 800, 600);
//
//        //styling
//        loginScene.getStylesheets().add("styles.css");
//        loginBorderPane.getStyleClass().add("loginBorderPane");
//        loginLabel.getStyleClass().add("loginLabels");
//        backgroundPane.getStyleClass().add("backgroundPane");
//        loginButton.getStyleClass().add("loginButton");
//        loginField.getStyleClass().add("loginField");
//        welcomeLabel.getStyleClass().add("welcomeLabel");
//        userImageView.setFitWidth(150);
//        userImageView.setFitHeight(150);
//        userImageView.setPreserveRatio(true);
//        userImageView.setSmooth(true);
//        userImageView.setCache(true);
//
//        loginField.setMaxWidth(350);
//
//        loginFieldHolder.setAlignment(Pos.CENTER);
//        loginVBox.setAlignment(Pos.CENTER);
//
//        loginBorderPane.setMaxWidth(600);
//        loginBorderPane.setMaxHeight(450);
//
//        loginButton.setMaxWidth(110);
//
//        //Main View
//        VBox gcButtonVBox = new VBox(10);
//        HBox titleBox = new HBox(10);
//        BorderPane mainViewBackgroundPane = new BorderPane();
//        mainViewBackgroundPane.setLeft(gcButtonVBox);
//        mainViewBackgroundPane.setTop(titleBox);
//        mainScene = new Scene(mainViewBackgroundPane, 600, 600);
//
//        //Styling
//        mainScene.getStylesheets().add("styles.css");
//        mainViewBackgroundPane.getStyleClass().add("mainViewBackgroundPane");
//        gcButtonVBox.getStyleClass().add("gcButtonVBox");
//        titleBox.getStyleClass().add("titleBox");
//
//        gcButtonVBox.setAlignment(Pos.CENTER);
//        gcButtonVBox.setPrefWidth(400);
//        titleBox.setPrefHeight(100);
//
//
//
//        //set window
//        window.setScene(loginScene);
//        window.setTitle("Login");
//        window.setResizable(false);
//        window.show();
//
//    }
//
//}
