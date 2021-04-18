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
 * @author GHS Squad
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
   
   // main
   public static void main(String[] args) {
      launch(args);
   }
   
   // start
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
      DirectoryChooser chooser = new DirectoryChooser();
      chooser.setInitialDirectory(new File(tfFolder.getText()));
      chooser.setTitle("Select Folder for Uploads and Downloads");
      File file = chooser.showDialog(stage);
      tfFolder.setText(file.getAbsolutePath());
      tfFolder.setPrefColumnCount(tfFolder.getText().length());
   }
   
   /**
    * doUpload()
    * method to upload file to server
    */
   public void doUpload() {
      FileChooser chooser = new FileChooser();
      chooser.setInitialDirectory(new File(tfFolder.getText()));
      chooser.setTitle("Select the Local File to Upload");
      File localFile = chooser.showOpenDialog(stage);
      
      if (localFile == null)
      return;
      
      System.out.println(localFile.getAbsolutePath());
      TextInputDialog dialog = new TextInputDialog();
      
      dialog.setTitle("Remote Name");
      dialog.setHeaderText("Enter the name to file on the\nserver for saving the upload.");
      dialog.setX(100);
      dialog.showAndWait();
      
      String remoteName = dialog.getEditor().getText();
      System.out.println(remoteName);
      Thread ulThread = new UploadThread(remoteName, localFile);
      ulThread.start();
   }
   
   // Threading for Upload??
   class UploadThread extends Thread {
      private DatagramSocket dgmSocket;
      private String remote;
      private File local;
      InetAddress inaServer = null;
      int port = TFTP_PORT;
      
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
   
      public void run() {
         log("Starting upload " + local.getName() + " --> " + remote);
         try {
            Packet outContents = new Packet(WRQ, UNDEF, remote, "octet", null, 0, inaServer, port);
            DatagramPacket wrqPkt = outContents.buildPacket();
            log("Client sending -- " + PacketChecker.decode(wrqPkt));
            dgmSocket.send(outContents.buildPacket());
            int blockNo = UNDEF;
            int lastSize = 512;
            DataInputStream fdis = null;
            try {
               while (true) {
               DatagramPacket inPkt = new DatagramPacket(new byte[MAX_PACKET], MAX_PACKET);
               try {
                  dgmSocket.receive(inPkt);
               }
               catch (SocketTimeoutException ste) {
                  log("Upload timed out waiting for ACK!");
                  return;
               }
               
               log("Client received -- " + PacketChecker.decode(inPkt));
               Packet inContents = new Packet();
               inContents.parsePacket(inPkt);
               
               if (inContents.getOpcode() != ACK || inContents.getNumber() != blockNo) {
                  Packet errContents = new Packet(ERROR, UNDEF, "Bad opcode (" + inContents.getOpcode() + "...4 expected) or block # (" + inContents.getNumber() + " -- " + blockNo + " expected) - DISCARDED", null, null, 0, inContents.getInaPeer(), inContents.getPort());
                  DatagramPacket errPkt = errContents.buildPacket();
                  log("Client sending -- " + PacketChecker.decode(errPkt));
                  dgmSocket.send(errPkt);
                  return;
               }
               inaServer = inContents.getInaPeer();
               port = inContents.getPort();
               if (lastSize < 512)
               break;
                
               blockNo++;
               if (fdis == null) {
                  String fullName = local.getAbsolutePath();
                  log("doWRQ -- Opening " + fullName);
                  try {
                     fdis = new DataInputStream(new FileInputStream(local));
                  }
                  catch (IOException ioe) {
                     log("doWRQ -- Cannot open file -- " + local.getName());
                     Packet contents_e = new Packet(ERROR, ACCESS, "Cannot open file " + local.getName(), null, null, 0, inaServer, port);
                     dgmSocket.send(contents_e.buildPacket());
                     return;
                  } 
               } 
               byte[] block = new byte[512];
               int actSize = 0;
               try {
                  actSize = fdis.read(block);
               }
               catch (EOFException eofe) {
                  actSize = 0;
               }
               outContents = new Packet(DATA, blockNo, null, null, block, actSize, inaServer, port);
               DatagramPacket outPkt = outContents.buildPacket();
               log("Client sending -- " + PacketChecker.decode(outPkt));
               dgmSocket.send(outContents.buildPacket());
               lastSize = actSize;
            }
         }
         catch (Exception e) {
            log("doWRQ -- Exception during WRQ: " + e);
            return;
         }
      }
      catch (Exception e) {
         log("doWRQ -- Exception during WRQ: " + e);
      } 
      try {
        dgmSocket.close();
      }
      catch (Exception exception) {}
         log("Uploaded " + local.getName() + " --> " + remote);
      }
   }
   
   /**
    * doDownload()
    * method to download file from server
    */
   public void doDownload() {
      TextInputDialog dialog = new TextInputDialog();
      dialog.setTitle("Remote Name");
      dialog.setHeaderText("Enter the name of\nthe remote file to download.");
      dialog.setX(75.0D);
      dialog.showAndWait();
      
      String remoteName = dialog.getEditor().getText();
      FileChooser chooser = new FileChooser();
      chooser.setInitialDirectory(new File(tfFolder.getText()));
      chooser.setTitle("Select/Enter the Name of the File for Saving the Download");
      File localFile = chooser.showSaveDialog(stage);
      
      if (localFile == null) {
         log("Canceled!");
         return;
      } 
      
      Thread dlThread = new DownloadThread(remoteName, localFile);
      dlThread.start();
   }
   
   // Threading for Download??
   class DownloadThread extends Thread {
      private DatagramSocket dgmSocket;
      private String remote;
      private File local;
      InetAddress inaServer = null;
      int port = TFTP_PORT;
      
      public DownloadThread(String _remoteName, File _localFile) {
         remote = _remoteName;
         local = _localFile;
         try {
            dgmSocket = new DatagramSocket();
            dgmSocket.setSoTimeout(1000);
            inaServer = InetAddress.getByName(tfServer.getText());
         }
         catch (Exception e) {
            log("Problem starting download " + e);
            System.exit(1);
         }
      }
   
      public void run() {
         log("Starting download " + remote + " --> " + local.getName());
         try {
            Packet outContents = new Packet(RRQ, UNDEF, remote, "octet", null, 0, inaServer, TFTP_PORT);
            DatagramPacket outPkt = outContents.buildPacket();
            log("Client sending..." + PacketChecker.decode(outPkt));
            dgmSocket.send(outContents.buildPacket());
            int lastSize = 512;
            int port = TFTP_PORT;
            DataOutputStream fdos = null;
            int expectedBlock = NOTFD;
            
            while (lastSize == 512) {
               DatagramPacket dpkt = new DatagramPacket(new byte[MAX_PACKET], MAX_PACKET);
               try {
                  dgmSocket.receive(dpkt);
               }
               catch (SocketTimeoutException ste) {
                  log("Download timed out waiting for DATA!");
                  return;
               }
               
               log("Client received -- " + PacketChecker.decode(dpkt));
               Packet inContents = new Packet();
               inContents.parsePacket(dpkt);
               lastSize = inContents.getDataLen();
               
               if (inContents.getOpcode() == ERROR)
               return;
               
               if (inContents.getOpcode() != DATA || inContents.getNumber() != expectedBlock) {
                  Packet errContents = new Packet(ERROR, ILLOP, "Bad DATA packet: " + inContents.getOpcode() + " block# " + inContents.getNumber(), null, null, 0, inContents.getInaPeer(), inContents.getPort());
                  DatagramPacket errPkt = errContents.buildPacket();
                  log("Client sending -- " + PacketChecker.decode(errPkt));
                  dgmSocket.send(errPkt);
                  return;
               }
               
               expectedBlock++;
               if (fdos == null) {
                  log("Opening -- " + local.getAbsolutePath());
                  fdos = new DataOutputStream(new FileOutputStream(local));
               }
               fdos.write(inContents.getData(), 0, inContents.getDataLen());
               fdos.flush();
               outContents = new Packet(ACK, inContents.getNumber(), null, null, null, 0, inContents.getInaPeer(), inContents.getPort());
               outPkt = outContents.buildPacket();
               log("Client sending -- " + PacketChecker.decode(outPkt));
               dgmSocket.send(outPkt);
            }
            fdos.close();
         }
         catch (Exception e) {
            log("Exception during RRQ: " + e);
            e.printStackTrace();
            return;
         }
         
         log("Downloaded " + remote + " --> " + local.getName());
      }
   }
}
