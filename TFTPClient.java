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
   
   // GUI Components
   private TextField tfServer = new TextField("localhost");
   private TextField tfFolder = new TextField();
   private Button btnChooseFolder = new Button("Choose Folder");
   private Button btnDownload = new Button("Download");
   private Button btnUpload = new Button("Upload");
   private TextArea taLog = new TextArea();
   
   // Client-Server Components
   private DatagramSocket dgmSocket;
   
   /** main */
   public static void main(String[] args) {
      launch(args);
   }
   
   /** start */
   public void start(Stage _stage) {
      stage = _stage;
      stage.setTitle("GHS Squad TFTP Client");
      
      // Handle WindowEvent
      stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
         public void handle(WindowEvent wevt) {
            System.exit(0);
         }
      });
      
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
      HBox hbBtn = new HBox(10);
      hbBtn.getChildren().addAll(btnUpload, btnDownload);
      root.getChildren().add(hbBtn);
      
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
      
      scene = new Scene(root, 500, 600);
      stage.setScene(scene);
      stage.setX(10);
      stage.setY(75);
      stage.show();
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
      File localFile = chooser.showOpenDialog(stage);
      
      // Return nothing if File is not chosen
      if (localFile == null)
      return;
      
      System.out.println(localFile.getAbsolutePath());
      TextInputDialog dialog = new TextInputDialog();
      
      // Prompt user for remote file name
      dialog.setTitle("Remote Name");
      dialog.setHeaderText("Enter the name to file on the\nserver for saving the upload.");
      dialog.setX(100);
      dialog.showAndWait();
      
      // Store remote file name
      String remoteName = dialog.getEditor().getText();
      System.out.println(remoteName);
      // Create UploadThread and start it
      Thread ulThread = new UploadThread(remoteName, localFile);
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
      InetAddress inaServer = null;
      int port = TFTP_PORT;
      
      /**
       * UploadThread() - Parameterized constructor for UploadThread
       * @param   String      _remoteName
       * @param   File        _localFile
       */
      public UploadThread(String _remoteName, File _localFile) {
         remote = _remoteName;
         local = _localFile;
         try {
            dgmSocket = new DatagramSocket(0);
            dgmSocket.setSoTimeout(1000);
            inaServer = InetAddress.getByName(tfServer.getText());
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
            // Create new WRQ Packet
            Packet outContents = new Packet(WRQ, UNDEF, remote, "octet", null, 0, inaServer, port);
            // Call buildPacket() method and store result
            DatagramPacket wrqPkt = outContents.buildPacket();
            // Call decipher() method from PacketChecker.jar
            log("Client sending -- " + PacketChecker.decipher(wrqPkt));
            dgmSocket.send(outContents.buildPacket());
            int blockNo = 1;
            int lastSize = 512;
            DataInputStream fdis = null;
            
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
                  Packet inContents = new Packet();
                  inContents.dissectPacket(inPkt);
                  
                  /*
                   * If recieved packet is not an ACK packet
                   * or error code is not 0 then build
                   * and send ERROR Packet
                   */
                  if (inContents.getOpcode() != ACK || inContents.getNumber() != blockNo) {
                     log("Bad opcode (" + inContents.getOpcode() + "...4 expected) or block # (" + inContents.getNumber() + " -- " + blockNo + " expected) - DISCARDED");
                     Packet errContents = new Packet(ERROR, UNDEF, "Bad opcode (" + inContents.getOpcode() + "...4 expected) or block # (" + inContents.getNumber() + " -- " + blockNo + " expected) - DISCARDED", null, null, 0, inContents.getInaPeer(), inContents.getPort());
                     DatagramPacket errPkt = errContents.buildPacket();
                     log("Client sending -- " + PacketChecker.decipher(errPkt));
                     dgmSocket.send(errPkt);
                     return;
                  }
                  // Store Ip and port of Server sending ACK Packet
                  inaServer = inContents.getInaPeer();
                  port = inContents.getPort();
                  if (lastSize < 512)
                  break;
                   
                  if (fdis == null) {
                     String fullName = local.getAbsolutePath();
                     log("doWRQ -- Opening " + fullName);
                     // Try to open file for reading
                     try {
                        fdis = new DataInputStream(new FileInputStream(local));
                     }
                     // IOException...
                     catch (IOException ioe) {
                        log("doWRQ -- Cannot open file -- " + local.getName());
                        Packet contents_e = new Packet(ERROR, ACCESS, "Cannot open file " + local.getName(), null, null, 0, inaServer, port);
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
                  outContents = new Packet(DATA, blockNo, null, null, block, actSize, inaServer, port);
                  // Call buildPacket() method and store result
                  DatagramPacket outPkt = outContents.buildPacket();
                  // Call decipher() method from PacketChecker.jar
                  log("Client sending -- " + PacketChecker.decipher(outPkt));
                  dgmSocket.send(outContents.buildPacket());
                  lastSize = actSize;
                  blockNo++;
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
      String remoteName = dialog.getEditor().getText();
      
      // Create FileChooser
      FileChooser chooser = new FileChooser();
      chooser.setInitialDirectory(new File(tfFolder.getText()));
      chooser.setTitle("Select/Enter the Name of the File for Saving the Download");
      // Create File to store chosen local file for upload
      File localFile = chooser.showSaveDialog(stage);
      
      // Return nothing if File is not chosen
      if (localFile == null) {
         log("Canceled!");
         return;
      } 
      
      // Create DownloadThread and start it
      Thread dlThread = new DownloadThread(remoteName, localFile);
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
      InetAddress inaServer = null;
      int port = TFTP_PORT;
      
      /**
       * DownloadThread() - Parameterized constructor for DownloadThread
       * @param   String      _remoteName
       * @param   File        _localFile
       */
      public DownloadThread(String _remoteName, File _localFile) {
         remote = _remoteName;
         local = _localFile;
         // Try to store new DatagramSocket
         try {
            dgmSocket = new DatagramSocket();
            dgmSocket.setSoTimeout(1000);
            inaServer = InetAddress.getByName(tfServer.getText());
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
            Packet outContents = new Packet(RRQ, UNDEF, remote, "octet", null, 0, inaServer, TFTP_PORT);
            // Call buildPacket() method and store result
            DatagramPacket outPkt = outContents.buildPacket();
            // Call decipher() method from PacketChecker.jar
            log("Client sending..." + PacketChecker.decipher(outPkt));
            dgmSocket.send(outContents.buildPacket());
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
               Packet inContents = new Packet();
               // Dissect DATA Packet
               inContents.dissectPacket(dpkt);
               lastSize = inContents.getDataLen();
               
               /*
                * If recived packet is not a DATA packet
                * or error code is sent insted of expected block number
                * then build and send ERROR Packet
                */
               if (inContents.getOpcode() != DATA || inContents.getNumber() != expectedBlock) {
                  Packet errContents = new Packet(ERROR, ILLOP, "Bad DATA packet: " + inContents.getOpcode() + " block# " + inContents.getNumber(), null, null, 0, inContents.getInaPeer(), inContents.getPort());
                  DatagramPacket errPkt = errContents.buildPacket();
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
               fdos.write(inContents.getData(), 0, inContents.getDataLen());
               fdos.flush();
               // Create new ACK Packet
               outContents = new Packet(ACK, inContents.getNumber(), null, null, null, 0, inContents.getInaPeer(), inContents.getPort());
               // Call buildPacket() method and store result
               outPkt = outContents.buildPacket();
               // Call decipher() method from PacketChecker.jar
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
}
