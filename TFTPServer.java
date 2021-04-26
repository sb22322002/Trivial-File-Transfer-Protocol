import java.io.*;
import java.net.*;
import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.stage.DirectoryChooser;

/**
 * TFTPServer - A Trivial File Transfer Protocol server
 * class to upload and download files
 * @author H Rose
 * @author Guiseppe Giambanco
 * @author Seth Button-Mosher
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
   private ServerThread serverThread = null;
   
   // Client-Server Components
   private DatagramSocket serverSocket = null;
   
   // main
   public static void main(String[] args) {
      launch(args);
   }
   
   // start
   public void start(Stage _stage) {
      stage = _stage;
      stage.setTitle("Instructor's TFTP Server");
      
      // Handle WindowEvent
      stage.setOnCloseRequest(
         new EventHandler<WindowEvent>() {
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
      btnStartStop.setOnAction(
         new EventHandler<ActionEvent>() {
            public void handle(ActionEvent aevt) {
               doStartStop();
            }
         });
      
      // Handle ChooseFolder Button
      btnChooseFolder.setOnAction(
         new EventHandler<ActionEvent>() {
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
      Platform.runLater(
         new Runnable() {
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
      DirectoryChooser chooser = new DirectoryChooser();
      chooser.setTitle("Choose new server Directory");
      chooser.setInitialDirectory(new File(tfFolder.getText()));
      File selectedDirectory = chooser.showDialog(stage);
      tfFolder.setText(selectedDirectory.getAbsolutePath());
   }
   
   /**
    * ListenThread - An inner class to keep server running
    */
   class ServerThread extends Thread {
      public void run(){
         try{
            serverSocket = new DatagramSocket(TFTP_PORT);
         }catch(IOException ioe){
            log("IOException: " + ioe + "\n");
         }
         
         while(true){
         
            byte[] bytePKT = new byte[MAX_PACKET];
            DatagramPacket pkt = new DatagramPacket(bytePKT, TFTP_PORT);
            
            try{
               serverSocket.receive(pkt);
            }catch(IOException ioe){
               return;
            }
            
            ClientThread ct = new ClientThread(pkt);
            ct.start();
         }
      }
   }
   
   /**
    * CleintThread - An inner class to communicate with client
    */
   class ClientThread extends Thread {
      
      private DatagramSocket cSocket = null;
      private DatagramPacket firstPkt = null;
   
      public ClientThread(DatagramPacket _pkt){
         firstPkt = _pkt;
      }
      
      public void run() {
         try{
            cSocket = new DatagramSocket();
            cSocket.setSoTimeout(1000);
         }catch(IOException ioe){
            log("IOException creating reply socket \n" + ioe);
            return;
         }
         Packet pktFirst = new Packet();
         pktFirst.dissectPacket(firstPkt);
         
         switch(pktFirst.getOpcode()){
            case RRQ:
               doRRQ(pktFirst, cSocket);
               return;
            case WRQ:
               doWRQ(pktFirst,cSocket);
               return;
            default:
               sendError(pktFirst,cSocket);
               return;
         }
      }
   }
   
   // Instantiate other classes or something?? //
   
   private void doRRQ(Packet pkt, DatagramSocket cSocket) {
      log("RRQ request from Client(FileName:" + pkt.getS1() + " Mode:" + pkt.getS2() + ")\n");
      return;
   }
   
   private void doWRQ(Packet pkt, DatagramSocket cSocket) {
      log("WRQ request from Client(FileName:" + pkt.getS1() + " Mode:" + pkt.getS2() + ")\n");
      return;  
   }
   
   private void sendError(Packet _pkt, DatagramSocket _cSocket){
      Packet pkt = _pkt;
      DatagramSocket cSocket = _cSocket;
      
      log("Unexpected/ERROR Opcode from Client: " + pkt.getOpcode());
      
      if(pkt.getOpcode() == 5){
         log("Error message from Packet: " + pkt.getS1()); 
      }
      
      try{
         Packet errorPacket = new Packet(ERROR, 4, "Unexpected opcode " + 2, null, null, 0, pkt.getInaPeer(), pkt.getPort());
         cSocket.send(errorPacket.buildPacket());
         return;
      }catch(IOException ioe){
         log("IOException creating reply socket \n" + ioe);
         return;
      }
   }
}