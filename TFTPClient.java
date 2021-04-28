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
 * TFTPClient - A Trivial File Transfer Protocol client
 * class to upload and download files
 * @author H Rose
 * @author Guiseppe Giambanco
 * @author Seth Button-Mosher
 * version 2205
 */
public class TFTPClient extends Application implements TFTPConstants {
   private Stage stage;
   private Scene scene;
   private VBox root = new VBox(8);
   
   // Client-Server Components
   private DatagramSocket dgmSocket;
   
   // GUI Components
   private TextField tfServer = new TextField("localhost");
   private TextField tfFolder = new TextField();
   private Button btnChooseFolder = new Button("Choose Folder");
   private Button btnDownload = new Button("Download");
   private Button btnUpload = new Button("Upload");
   private TextArea taLog = new TextArea();
   
   /** main */
   public static void main(String[] args) {
      launch(args);
   }
   
   /** start */
   public void start(Stage _stage) {
      stage = _stage;
      stage.setTitle("GHS Squad TFTP Client");
      
      HBox hbServer = new HBox(10);
      tfServer.setPrefColumnCount(30);
      hbServer.getChildren().addAll(new Label("Server: "), tfServer);
      root.getChildren().add(hbServer);
      tfFolder.setFont(Font.font("MONOSPACED",
      FontWeight.NORMAL,tfFolder.getFont().getSize()));
      
      // Create new file
      File initial = new File(".");
      // Set text to current directory
      tfFolder.setText(initial.getAbsolutePath());
      tfFolder.setPrefColumnCount(tfFolder.getText().length());
      
      // ScrollPane for Choose Folder Button and TextField
      ScrollPane sp = new ScrollPane();
      sp.setContent(tfFolder);
      root.getChildren().addAll(btnChooseFolder, sp);
      
      // HBox for Upload and Download Buttons
      HBox hboxBtn = new HBox(10);
      hboxBtn.getChildren().addAll(btnUpload, btnDownload);
      root.getChildren().add(hboxBtn);
      
      // TextArea Log
      taLog.setPrefRowCount(50);
      root.getChildren().addAll(new Label("Log:"), taLog);
      taLog.setEditable(false);
      
      // Handle ChooseFolder Button
      btnChooseFolder.setOnAction(new EventHandler<ActionEvent>() {
         public void handle(ActionEvent aevt) {
            doChooseFolder();
         }
      });
      
      // Handle Upload Button
      btnUpload.setOnAction(new EventHandler<ActionEvent>() {
         public void handle(ActionEvent aevt) {
            doUpload();
         }
      });
      
      // Handle Download Button
      btnDownload.setOnAction(new EventHandler<ActionEvent>() {
         public void handle(ActionEvent aevt) {
            doDownload();
         }
      });
      
      // Handle WindowEvent
      stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
         public void handle(WindowEvent wevt) {
            System.exit(0);
         }
      });
      
      scene = new Scene(root, 500, 600);
      stage.setScene(scene);
      stage.setX(10);
      stage.setY(75);
      stage.show();
   }
   
   /**
    * doChooseFolder()
    * method to choose a directory
    */
   public void doChooseFolder() {
      // Create DirectoryChooser
      DirectoryChooser chooser = new DirectoryChooser();
      chooser.setInitialDirectory(new File(tfFolder.getText()));
      chooser.setTitle("Select Folder for Uploads and Downloads");
      // Create File to store chosen directory
      File file = chooser.showDialog(stage);
      tfFolder.setText(file.getAbsolutePath());
      tfFolder.setPrefColumnCount(tfFolder.getText().length());
   }
   
   /**
    * doUpload()
    * method to upload file to server
    */
   public void doUpload() {
      // Create FileChooser
      FileChooser chooser = new FileChooser();
      chooser.setInitialDirectory(new File(tfFolder.getText()));
      chooser.setTitle("Select the Local File to Upload");
      // Create File to store chosen local file for upload
      File locFile = chooser.showOpenDialog(stage);
      
      // Return nothing if File is not chosen
      if (locFile == null)
      return;
      
      System.out.println(locFile.getAbsolutePath());
      TextInputDialog dialog = new TextInputDialog();
      
      // Prompt user for remote file name
      dialog.setTitle("Remote Name");
      dialog.setHeaderText("Enter the name to file on the\nserver for saving the upload.");
      dialog.setX(100);
      dialog.showAndWait();
      
      // Store remote file name
      String remFileName = dialog.getEditor().getText();
      System.out.println(remFileName);
      // Create UploadThread and start it
      Thread ulThread = new UploadThread(remFileName, locFile);
      ulThread.start();
   }
   
   /**
    * UploadThread - A threaded innner class to send to and recieve
    * packets from a server when uploading a file
    * @author Seth Button-Mosher
    * @version 2205
    */
   class UploadThread extends Thread {
      private DatagramSocket dgmSocket;
      private String remote;
      private File local;
      InetAddress inetServer = null;
      int port = TFTP_PORT;
      
      /**
       * UploadThread() - Parameterized constructor for UploadThread
       * @param   String      _remFileName
       * @param   File        _locFile
       */
      public UploadThread(String _remFileName, File _locFile) {
         remote = _remFileName;
         local = _locFile;
         try {
            dgmSocket = new DatagramSocket(0);
            dgmSocket.setSoTimeout(2000);
            inetServer = InetAddress.getByName(tfServer.getText());
         }
         catch (Exception e) {
            log("Problem  starting upload: " + e);
            System.exit(1);
         }
      }
      
      /** run */
      public void run() {
         log("Starting upload " + local.getName() + " --> " + remote);
         try {
            int blockNum = 0;
            int lastSize = 512;
            DataInputStream fdis = null;
         
            // Create new WRQ Packet
            Packet outPacket = new Packet(WRQ, UNDEF, remote, "octet", null, 0, inetServer, port);
            // Call buildPacket() method and store result
            DatagramPacket wrqPkt = outPacket.buildPacket();
            // Call decipher() method from the PacketChecker class
            log("Client sending -- " + PacketChecker.decipher(wrqPkt));
            dgmSocket.send(outPacket.buildPacket());
            
            try {
               while (true) {
                  // Create a DatagramPacket for ACK
                  DatagramPacket inPkt = new DatagramPacket(new byte[MAX_PACKET], MAX_PACKET);
                  // Try to recieve ACK packet
                  try {
                     dgmSocket.receive(inPkt);
                  }
                  // Error message if ACK is not recieved or timeout
                  catch (SocketTimeoutException ste) {
                     log("Upload timed out waiting for ACK!");
                     return;
                  }
                  
                  log("Client received -- " + PacketChecker.decipher(inPkt));
                  // Dissect ACK Packet
                  Packet inPacket = new Packet();
                  inPacket.dissectPacket(inPkt);
                  
                  /*
                   * If recieved packet is not an ACK packet
                   * or number code is not blockNum then build
                   * and send ERROR Packet
                   */
                  if (inPacket.getOpcode() != ACK || inPacket.getNumber() != blockNum) {
                     log("Bad opcode (" + inPacket.getOpcode() + "...4 expected) or block # (" + inPacket.getNumber() + " -- " + blockNum + " expected) - DISCARDED");
                     Packet errPacket = new Packet(ERROR, UNDEF, "Bad opcode (" + inPacket.getOpcode() + "...4 expected) or block # (" + inPacket.getNumber() + " -- " + blockNum + " expected) - DISCARDED", null, null, 0, inPacket.getInaPeer(), inPacket.getPort());
                     DatagramPacket errPkt = errPacket.buildPacket();
                     log("Client sending -- " + PacketChecker.decipher(errPkt));
                     dgmSocket.send(errPkt);
                     return;
                  }
                  blockNum++;
                  // Store Ip and port of Server sending ACK Packet
                  inetServer = inPacket.getInaPeer();
                  port = inPacket.getPort();
                  if (lastSize < 512)
                  break;
                   
                  if (fdis == null) {
                     String fileName = local.getAbsolutePath();
                     log("doWRQ -- Opening " + fileName);
                     // Try to open file for reading
                     try {
                        fdis = new DataInputStream(new FileInputStream(local));
                     }
                     // IOException...
                     catch (IOException ioe) {
                        log("doWRQ -- Cannot open file -- " + local.getName());
                        Packet contents_e = new Packet(ERROR, ACCESS, "Cannot open file " + local.getName(), null, null, 0, inetServer, port);
                        dgmSocket.send(contents_e.buildPacket());
                        return;
                     } 
                  }
                  byte[] block = new byte[512];
                  int actSize = 0;
                  // Try to read in data block by block
                  try {
                     actSize = fdis.read(block);
                  }
                  // EOFException...
                  catch (EOFException eofe) {
                     actSize = 0;
                  }
                  // Create new DATA Packet
                  outPacket = new Packet(DATA, blockNum, null, null, block, actSize, inetServer, port);
                  // Call buildPacket() method and store result
                  DatagramPacket outPkt = outPacket.buildPacket();
                  // Call decipher() method from PacketChecker.jar
                  log("Client sending -- " + PacketChecker.decipher(outPkt));
                  dgmSocket.send(outPacket.buildPacket());
                  lastSize = actSize;
                  lastSize = actSize;
               }
            }
            // General Exception...
            catch (Exception e) {
               log("doWRQ -- Exception during WRQ: " + e);
               return;
            }
         }
         // General Exception...
         catch (Exception e) {
            log("doWRQ -- Exception during WRQ: " + e);
            return;
         }
         
         // Try to close DatagramSocket
         try {
            dgmSocket.close();
         }
         // General Exception...
         catch (Exception e) {
            return;
         }
         
         log("Uploaded " + local.getName() + " --> " + remote);
      }
   }
   
   /**
    * doDownload()
    * method to download file from server
    */
   public void doDownload() {
      // Prompt user for remote file name
      TextInputDialog dialog = new TextInputDialog();
      dialog.setTitle("Remote Name");
      dialog.setHeaderText("Enter the name of\nthe remote file to download.");
      dialog.setX(75);
      dialog.showAndWait();
      // Store remote file name
      String remFileName = dialog.getEditor().getText();
      
      // Create FileChooser
      FileChooser chooser = new FileChooser();
      chooser.setInitialDirectory(new File(tfFolder.getText()));
      chooser.setTitle("Select/Enter the Name of the File for Saving the Download");
      // Create File to store chosen local file for upload
      File locFile = chooser.showSaveDialog(stage);
      
      // Return nothing if File is not chosen
      if (locFile == null) {
         log("Canceled!");
         return;
      } 
      
      // Create DownloadThread and start it
      Thread dlThread = new DownloadThread(remFileName, locFile);
      dlThread.start();
   }
   
   /**
    * DownloadThread - A threaded innner class to send to and recieve
    * packets from a server when downloading a file
    * @author Seth Button-Mosher
    * @version 2205
    */
   class DownloadThread extends Thread {
      private DatagramSocket dgmSocket;
      private String remote;
      private File local;
      InetAddress inetServer = null;
      int port = TFTP_PORT;
      
      /**
       * DownloadThread() - Parameterized constructor for DownloadThread
       * @param   String      _remFileName
       * @param   File        _locFile
       */
      public DownloadThread(String _remFileName, File _locFile) {
         remote = _remFileName;
         local = _locFile;
         // Try to store new DatagramSocket
         try {
            dgmSocket = new DatagramSocket();
            dgmSocket.setSoTimeout(2000);
            inetServer = InetAddress.getByName(tfServer.getText());
         }
         // General Exception...
         catch (Exception e) {
            log("Problem starting download " + e);
            System.exit(1);
         }
      }
      
      /** run */
      public void run() {
         log("Starting download " + remote + " --> " + local.getName());
         try {
            // Create new RRQ Packet
            Packet outPacket = new Packet(RRQ, UNDEF, remote, "octet", null, 0, inetServer, TFTP_PORT);
            // Call buildPacket() method and store result
            DatagramPacket outPkt = outPacket.buildPacket();
            // Call decipher() method from the PacketChecker class
            log("Client sending..." + PacketChecker.decipher(outPkt));
            dgmSocket.send(outPacket.buildPacket());
            int lastSize = 512;
            int port = TFTP_PORT;
            DataOutputStream fdos = null;
            int expectedBlock = NOTFD;
            
            while (lastSize == 512) {
               // Create a DatagramPacket for DATA
               DatagramPacket dpkt = new DatagramPacket(new byte[MAX_PACKET], MAX_PACKET);
               // Try to recieve DATA packet
               try {
                  dgmSocket.receive(dpkt);
               }
               // SocketTimeoutException...
               catch (SocketTimeoutException ste) {
                  log("Download timed out waiting for DATA!");
                  return;
               }
               
               log("Client received -- " + PacketChecker.decipher(dpkt));
               Packet inPacket = new Packet();
               // Dissect DATA Packet
               inPacket.dissectPacket(dpkt);
               lastSize = inPacket.getDataLen();
               
               /*
                * If recived packet is not a DATA packet
                * or error code is sent insted of expected block number
                * then build and send ERROR Packet
                */
               if (inPacket.getOpcode() != DATA || inPacket.getNumber() != expectedBlock) {
                  Packet errPacket = new Packet(ERROR, ILLOP, "Bad DATA packet: " + inPacket.getOpcode() + " block# " + inPacket.getNumber(), null, null, 0, inPacket.getInaPeer(), inPacket.getPort());
                  DatagramPacket errPkt = errPacket.buildPacket();
                  log("Client sending -- " + PacketChecker.decipher(errPkt));
                  dgmSocket.send(errPkt);
                  return;
               }
               
               expectedBlock++;
               // Try to open local file for writing
               if (fdos == null) {
                  log("Opening -- " + tfFolder.getText() + File.separator + remote);
                  fdos = new DataOutputStream(new FileOutputStream(tfFolder.getText() + File.separator + remote));
               }
               // Write in byte[] data to local file
               fdos.write(inPacket.getData(), 0, inPacket.getDataLen());
               fdos.flush();
               // Create new ACK Packet
               outPacket = new Packet(ACK, inPacket.getNumber(), null, null, null, 0, inPacket.getInaPeer(), inPacket.getPort());
               // Call buildPacket() method and store result
               outPkt = outPacket.buildPacket();
               // Call decipher() method from the PacketChecker class
               log("Client sending -- " + PacketChecker.decipher(outPkt));
               dgmSocket.send(outPkt);
            }
            // Try to close DataOutputStream
            fdos.close();
         }
         // General Exception...
         catch (Exception e) {
            log("Exception during RRQ: " + e);
            e.printStackTrace();
            return;
         }
         
         log("Downloaded " + remote + " --> " + local.getName());
      }
   }
   
   /**
    * log()
    * method to append client taLog
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
}
