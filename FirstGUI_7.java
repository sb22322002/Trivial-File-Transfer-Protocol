import javafx.application.Application;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;

/**
 * FirstGUI_7 - demo javaFX program
 * @author  D. Patric
 * @version 2205
 */

public class FirstGUI_7 extends Application
   implements EventHandler<ActionEvent> {       // Interior of window
   // Attributes are GUI components (buttons, text fields, etc.)
   // are declared here.
   private Stage stage;        // The entire window, including title bar and borders
   private Scene scene;        // Interior of window
   private BorderPane root  ;  // Rules for how components are layed out in the window
   
   // Top components
   private Label lblN1 = new Label("N1");
   private Label lblN2 = new Label("N2");
   private Label lblRes = new Label("Result");
   
   private TextField tfN1 = new TextField();
   private TextField tfN2 = new TextField();
   private TextField tfRes = new TextField();
   
   // Bottom components
   private Button btnAdd = new Button("Add");
   private Button btnClear = new Button("Clear");
   private Button btnExit = new Button("Exit");
        
   // Main just instantiates an instance of this GUI class
   public static void main(String[] args) {
      launch(args);
   }
   
   // Called automatically after launch sets up javaFX
   public void start(Stage _stage) throws Exception {
      stage = _stage;                   // save stage as an attribute
      stage.setTitle("My First GUI");       // set the text in the title bar
      root = new BorderPane();          // create a BorderPane layout 
      
      // Create a FlowPane layout for the TOP and BOTTOM
      // The 10, 10 gives horizontal and vertical spacing so components
      // are not crammed together. The setAlignment calls cause
      // components to be centered in the region.
      FlowPane paneTop = new FlowPane(10, 10);
      paneTop.setAlignment(Pos.CENTER);
      FlowPane paneBot = new FlowPane(10, 10);
      paneBot.setAlignment(Pos.CENTER);
        
      // Fill up the top
      paneTop.getChildren().add(lblN1);
      paneTop.getChildren().add(tfN1);
      paneTop.getChildren().add(lblN2);
      paneTop.getChildren().add(tfN2);
      paneTop.getChildren().add(lblRes);
      paneTop.getChildren().add(tfRes);
      
      // Fill up the bottom
      paneBot.getChildren().add(btnAdd);
      paneBot.getChildren().add(btnClear);
      paneBot.getChildren().add(btnExit);
      
      // Apply the layouts to the main layout’s TOP and BOTTOM areas
      root.setTop(paneTop);
      root.setBottom(paneBot);
      
      scene = new Scene(root, 650, 100);   // create scene of 
                                           // specified size 
                                           // with root layout
      stage.setScene(scene);            // associate the scene with 
                                        // the stage
   
      // Add behavior
      btnAdd.setOnAction(this);
      btnClear.setOnAction(this);
      btnExit.setOnAction(this);

      stage.show();                     // display the stage (window)
   }
   
   public void handle(ActionEvent evt) {    
      // Get the butto  that was clicked
      Button btn = (Button)evt.getSource();
      
      // Switch on the button's name
      switch(btn.getText()) {
         case "Add":
            String sN1 = tfN1.getText();
            String sN2 = tfN2.getText();
            Double dN1 = Double.parseDouble(sN1);
            Double dN2 = Double.parseDouble(sN2);
            Double dRes = dN1 + dN2;
            tfRes.setText("" + dRes);
            break;
         case"Clear":
            tfN1.setText("");
            tfN2.setText("");
            tfRes.setText("");
            break;
         case "Exit":
            System.exit(0);
            break;
      }
   }
}	
