package robothelp;
	import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.*;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.control.ScrollPane;
	
public class ChatBotInterface extends Application  {
	
	
	private String conversation="";
    private final TextField input = new TextField();
    
    private final WebView browser = new WebView();
    private final WebEngine webEngine = browser.getEngine();
	
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("FAQ Bot!");
        primaryStage.setWidth(500);
        primaryStage.setHeight(500);
        Button sendButton = new Button();
        sendButton.setText("Send");
        sendButton.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
            	inputAction();
            }
        });
        
        input.setOnKeyPressed((event) -> { if(event.getCode() == KeyCode.ENTER) { inputAction(); } });

        Button likeButton = new Button("Like Answer");
        likeButton.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });
        
        Button dislikeButton = new Button("Dislike Answer");
        dislikeButton.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });
        


        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(browser);
        webEngine.loadContent(conversation);

        VBox root = new VBox();
        HBox chatBar = new HBox();
        chatBar.getChildren().addAll(input,sendButton,likeButton,dislikeButton);
        root.getChildren().addAll(scrollPane,chatBar);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
        
        ReadHelpFile.readHelp(false);
        SearchInIndex.initialize();
    }

	private void inputAction() {
        conversation+="user: "+input.getText()+"</br>";
        
        try {
			conversation+="bot: "+SearchInIndex.search(input.getText())+"</br>";
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //TODO processing
        input.setText("");
        
        webEngine.loadContent(conversation);
        
		
	}
}
