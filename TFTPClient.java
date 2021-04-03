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
      return;
   }
   
   /**
    * doUpload()
    * method to upload file to server
    */
   public void doUpload() {
      return;
   }
   
   // Threading for Upload??
   class UploadThread extends Thread {
   
   
      public void run() {
         return;
      }
   }
   
   /**
    * doDownload()
    * method to download file from server
    */
   public void doDownload() {
     return;
   }
   
   // Threading for Download??
   class DownloadThread extends Thread {
   
   
      public void run() {
         return;
      }
   }
}
