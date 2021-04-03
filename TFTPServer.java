import java.io.*;
import java.net.*;
import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

/**
 * TFTPClient - A Trivial File Transfer Protocol server
 * class to upload and download files
 * @author GHS Squad
 * version 2205
 */
public class TFTPServer extends Application implements TFTPConstants {
   private Stage stage;
   private Scene scene;
   private VBox root = new VBox(10);
   
   // GUI Components
   private TextField tfFolder = new TextField();
   private Button btnChooseFolder = new Button("Choose Folder");
   private Label lblStartStop = new Label("Start the server: ");
   private Button btnStartStop = new Button("Start");
   private TextArea taLog = new TextArea();
   private ListenThread listenThread = null;
   
   // Client-Server Components
   private DatagramSocket mainSocket = null;
   
   // main
   public static void main(String[] args) {
     launch(args);
   }
   
   // start
   public void start(Stage _stage) {
      stage = _stage;
      stage.setTitle("Instructor's TFTP Server");
      
      // Handle WindowEvent
      stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
         public void handle(WindowEvent wevt) {
            System.exit(0);
         }
      });
      
      tfFolder.setFont(Font.font("MONOSPACED",
      FontWeight.NORMAL, tfFolder.getFont().getSize()));
      
      // Create new file
      File initial = new File(".");
      // Set text to current directory
      tfFolder.setText(initial.getAbsolutePath());
      tfFolder.setPrefColumnCount(tfFolder.getText().length());
      
      // ScrollPane for Choose Folder Button and TextField
      ScrollPane sp = new ScrollPane();
      sp.setContent(tfFolder);
      root.getChildren().addAll(btnChooseFolder, sp);
      
      // HBox for Start Button and Label
      HBox hbButton = new HBox(10.0);
      hbButton.getChildren().addAll(new Node[] { lblStartStop, btnStartStop });
      btnStartStop.setStyle("-fx-background-color: #00ff00;");
      root.getChildren().add(hbButton);
      
      // TextArea Log
      taLog.setPrefRowCount(50);
      root.getChildren().add(taLog);
      
      // Handle Start/Stop Button
      btnStartStop.setOnAction(new EventHandler<ActionEvent>() {
         public void handle(ActionEvent aevt) {
            doStartStop();
         }
      });
      
      // Handle ChooseFolder Button
      btnChooseFolder.setOnAction(new EventHandler<ActionEvent>() {
         public void handle(ActionEvent aevt) {
            doChooseFolder();
         }
      });
      
      scene = new Scene(root, 300, 400);
      stage.setScene(scene);
      stage.setX(525);
      stage.setY(75);
      stage.show();
   }
   
   /**
    * doStartStop()
    * method to start and stop server
    */
   public void doStartStop() {
      // if Button is "Start" switch to "Stop"
      if (btnStartStop.getText().equals("Start")) {
         btnStartStop.setText("Stop");
         btnStartStop.setStyle("-fx-background-color: #ff0000;");
         lblStartStop.setText("Stop the server: ");
         
         //Thread listenThread = new ListenThread();
         //listenThread.start();
         
         tfFolder.setDisable(true);
         btnChooseFolder.setDisable(true);
      }
      // if Button is "Stop" switch to "Start"
      else {
         btnStartStop.setText("Start");
         btnStartStop.setStyle("-fx-background-color: #00ff00;");
         lblStartStop.setText("Start the server: ");
         
         tfFolder.setDisable(false);
         btnChooseFolder.setDisable(false);
      } 
   }
   
   /**
    * log()
    * method to append server taLog
    * @param   String      message
    */
   private void log(final String message) {
      System.out.print(message + "\n");
      Platform.runLater(new Runnable() {
         public void run() {
            taLog.appendText(message + "\n");
         }
      });
   }
   
   /**
    * doChooseFolder()
    * method to choose a directory
    */
   public void doChooseFolder() {
      return;
   }
   
   /**
    * ListenThread - An inner class to keep server running
    */
   class ListenThread extends Thread {
      /*public void run() {
         try {
            log("ListenThread.run -- Listen thread started");
            mainSocket = new DatagramSocket(69);
            while (true) {
               DatagramPacket dgmPacket = new DatagramPacket(new byte[1500], 1500);
               mainSocket.receive(dgmPacket);
               log("ListenThread.run -- Received a packet!");
               Thread cThread = new ClientThread(dgmPacket);
               cThread.start();
            } 
         }
         catch (Exception e) {
            log("ListenThread.run -- Unexpected Exception:\n     " + e);
            return;
         } 
      }*/
   }
   
   /**
    * CleintThread - An inner class to communicate with client
    */
   class ClientThread extends Thread {
      public void run() {
         return;
      }
   }
   
   // Instantiate other classes or something?? //
   
   private void doRRQ() {
      return;
   }
   
   private void doWRQ() {
      return;  
   }
   
   private void doDATA() {
      return;  
   }
   
   private void doACK() {
      return;  
   }
   
   private void doERROR() {
      return;  
   }
}