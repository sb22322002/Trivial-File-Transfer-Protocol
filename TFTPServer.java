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
      stage.setTitle("GHS-Squad's TFTP Server");
      
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
      taLog.setEditable(false);
      
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
         
         Thread serverThread = new ServerThread();
         serverThread.start();
         
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
   
   private void doRRQ(Packet packet, DatagramSocket csocket) {
      log("RRQ request from Client(FileName:" + packet.getS1() + " Mode:" + packet.getS2() + ")");
      //which block this is
      int blockNum = 1;
      //the size of the read in bytes
      int size = 512;
      //dis
      DataInputStream dis = null;
      //hope you know what this is (hint: filename)
      String filename = tfFolder.getText() + File.separator + packet.getS1();
      try {
         dis = new DataInputStream(new FileInputStream(new File(filename)));
      } 
      catch (FileNotFoundException fnfe) {
         log("File Not Found");
         Packet error = new Packet(5, 2, "File not found " + packet.getS1(), null, null, 0, packet.getInaPeer(), packet.getPort());
         DatagramPacket errordgmp = error.buildPacket();
         try {
            csocket.send(errordgmp);
         } catch (IOException ioe) {}
         return;
      }
      while (size == 512) {
         byte[] block = new byte[512];
         size = 0;
         try {
            size = dis.read(block);
         }
         catch (IOException ioe) {
            log("IOException from reading " + ioe.toString());
            return;
         }
         Packet outPack = new Packet(3, blockNum, null, null, block, size, packet.getInaPeer(), packet.getPort());
         DatagramPacket outPacket = outPack.buildPacket();
         try {
            csocket.send(outPacket);
         }
         catch (IOException ioe) {
            log("Error sending RRQ");
            return;
         }
         log("sending " + PacketChecker.decipher(outPacket));
         DatagramPacket datagram = new DatagramPacket(new byte[1500], 1500);
         try {
            csocket.receive(datagram);
         }
         catch (IOException ioe) {
            log(ioe.toString());
            return;
         }
         log("Received -- " + PacketChecker.decipher(datagram));
         Packet inPacket = new Packet();
         inPacket.dissectPacket(datagram);
         if (inPacket.getOpcode() != 4 || blockNum != inPacket.getNumber()) {
            log("bad opcode or block num");
            Packet error = new Packet(5, 0, String.format("Bad opcode (%d != 4) or block num (%d != %d)", inPacket.getOpcode(), inPacket.getNumber(), blockNum), null, null, 0, packet.getInaPeer(), packet.getPort());
            DatagramPacket errordgmp = error.buildPacket();
            try {
               csocket.send(errordgmp);
            } catch (IOException ioe) {}
            return;
         }
         blockNum++;
      }
      try {
            dis.close();
            csocket.close();
      } catch (IOException ioe) {}  
   }
   
   private void doWRQ(Packet packet, DatagramSocket csocket) {
      log("WRQ request from Client(FileName:" + packet.getS1() + " Mode:" + packet.getS2() + ")");
      int blockNum = 0;
      int size = 512;
      int expectedNum = 1;
      DataOutputStream dos = null;
      String filename = tfFolder.getText() + File.separator + packet.getS1();
      try {
         dos = new DataOutputStream(new FileOutputStream(new File(filename)));
      } 
      catch (FileNotFoundException fnfe) {
         log("File Not Found");
         Packet error = new Packet(5, 2, "File not found " + packet.getS1(), null, null, 0, packet.getInaPeer(), packet.getPort());
         DatagramPacket errordgmp = error.buildPacket();
         try {
            csocket.send(errordgmp);
         } catch (IOException ioe) {}
         return;
      }
      while (true) {
         Packet out = new Packet(4, blockNum, filename, null, null, 0, packet.getInaPeer(), packet.getPort()); 
         DatagramPacket outPacket = out.buildPacket();
         log("Sending: " + PacketChecker.decipher(outPacket));
         try {
         csocket.send(outPacket);
         } catch (IOException ioe) {}
         if (size < 512) {
            break;
         }
         DatagramPacket datagram = new DatagramPacket(new byte[1500], 1500);
         try {
            csocket.receive(datagram);
         }
         catch (SocketTimeoutException ste) {
            log("Upload timed out waiting for Data");   
            return;
         }
         catch (IOException ioe) {
            log(ioe.toString());
            return;
         }
         log("received - " + PacketChecker.decipher(datagram));
         Packet inPacket = new Packet();
         inPacket.dissectPacket(datagram);
         if (inPacket.getOpcode() == 5) {
            return;
         }
         else if (inPacket.getOpcode() != 3 || inPacket.getNumber() != expectedNum) {
            log(String.format("Bad opcode (%d != 3) or block num (%d != %d)", inPacket.getOpcode(), inPacket.getNumber(), expectedNum));
            Packet error = new Packet(5, 0, String.format("Bad opcode (%d != 4) or block num (%d != %d)", inPacket.getOpcode(), inPacket.getNumber(), blockNum), null, null, 0, packet.getInaPeer(), packet.getPort());
            DatagramPacket errordgmp = error.buildPacket();
            try {
               csocket.send(errordgmp);
            } catch (IOException ioe) {}
            return;
         }
         size = inPacket.getDataLen();
         try {
         dos.write(inPacket.getData(), 0, inPacket.getDataLen());
         dos.flush();
         } catch (IOException ioe) {
            log(ioe.toString());
         }
         expectedNum++;
         blockNum++;
      } 
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
