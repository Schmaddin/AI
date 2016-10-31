package robothelp;
	import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
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
			conversation+=taggedSentence(input.getText());
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //TODO processing
        input.setText("");
        
        webEngine.loadContent(conversation);
        
		
	}

	private String taggedSentence(String text) {
		List<CoreMap> sentences = SentenceParser.returnSentences(SentenceParser.makeAnnotation(text));
		
		String returnString="";
		
		for(CoreMap sentence:sentences)
		{
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {

				 String word = token.get(TextAnnotation.class);
				// // this is the POS tag of the token
				 String pos = token.get(PartOfSpeechAnnotation.class);
				// // this is the NER label of the token
				 String ne = token.get(NamedEntityTagAnnotation.class);

				 returnString+=" <font color=\""+Indexer.getColorFromPosTag(pos)+"\">"+word+"_"+pos+"</font>";
			}
			
			// // this is the text of the token
		}
		
		returnString+="</br></br></br>";
		return returnString;
	}
}
