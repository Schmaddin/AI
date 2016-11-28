package robothelp;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Version;

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
import javafx.scene.image.Image;


public class ChatBotInterface extends Application {

	private String conversation = "";
	private String userName = "user";

	private boolean questionFromBot = false;

	private List<ConversationBlock> conversationList = new LinkedList<ConversationBlock>();
	private List<String> robot = new LinkedList<String>();
	private List<String> user = new LinkedList<String>();

	private final TextField input = new TextField();

	private final WebView browser = new WebView();
	private final WebEngine webEngine = browser.getEngine();

	private int lastUser = -1;
	private String helpName = "Aeromexico";
	private boolean developer = false;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("FAQ Bot - " + helpName);
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

		input.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.ENTER) {
				inputAction();
			}
		});

		Button likeButton = new Button("Like Answer");
		likeButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				if(conversationList.size()>0 && lastUser==0)
				{
				ConversationBlock block=conversationList.get(conversationList.size()-1);

				Document doc=SearchInIndex.searchDocument(block.getCurrent().getCaption());
				System.out.println("current oldquestion: "+block.getCurrent().getId()+" "+block.getCurrent().getOldQuestions());
				
				block.getCurrent().setOldQuestions(block.getCurrent().getOldQuestions()+" "+block.getQuestion());
				SearchInIndex.close();
				
				Indexer indexer=new Indexer();
				indexer.updateDocument();
				try {
					indexer.close();
				} catch (IOException e) {

				}
				
				SearchInIndex.initialize();
				
				botResponse("</br><b>Great I could help you! :-)</b>",100);
				}
			}
		});

		Button dislikeButton = new Button("Dislike Answer");
		dislikeButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				if(conversationList.size()>0 && lastUser==0)
				{
				ConversationBlock conversationBlock = conversationList.get(conversationList.size() - 1);
				String textAnswer = conversationBlock.nextAnswer().getContent();
				botResponse("<b><br>well you don't liked the answer...</br>is this better:</br></b>", 900);
				botResponse(textAnswer,1400);
				webEngine.loadContent(conversation);
				}
				else
				{
					botResponse("So how can I help you?",400);
				}
			}
		});

		webEngine.loadContent(conversation);

		VBox root = new VBox();
		HBox chatBar = new HBox();
		chatBar.getChildren().addAll(input, sendButton, likeButton, dislikeButton);
		root.getChildren().addAll(browser, chatBar);
		primaryStage.setScene(new Scene(root, 300, 250));
		primaryStage.getIcons().add(new Image("file:logo.jpg"));
		primaryStage.show();

		ReadHelpFile.readHelp(true);
		SearchInIndex.initialize();

		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();

		char replaceA = '\\';
		char replaceB = '/';
		s = s.replace(replaceA, replaceB);
		StringBuilder html = new StringBuilder().append("<html>");
		html.append("<head>");
		html.append("   <script language=\"javascript\" type=\"text/javascript\">");
		html.append("       function toBottom(){");
		html.append("           window.scrollTo(0, document.body.scrollHeight);");
		html.append("       }");
		html.append("   </script>");
		html.append("</head>");
		html.append("<body onload='toBottom()'>" + "<img src=\"file:///" + s + "/logo.jpg\">");

		conversation += html.toString();
		System.out.println(s + "/logo.jpg");
		addRespond(
				"Welcome at the FAQ Bot of " + helpName
						+ "</br> Our Chatbot will help you with answering your questions.</br> Please give Feedback in \"Liking\" or \"Disliking\" the answer!",
				2);

		addRespond("Hello Iam your chatbot,</br> I hope I can help you, what is your name?", 0);

		SentenceParser.init();
	}

	private void inputAction() {

		addRespond(input.getText(), 1);

		processInput(input.getText());

		// TODO processing
		input.setText("");

	}

	private void processInput(String input) {

		String stopped = "";
		try {
			stopped = SearchInIndex.removeStopWords(input);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (developer == true)
			addRespond(collorTaggedSentence(input), 2);

		boolean answered = false;

		List<String> nomen = new LinkedList<>();
		List<String> verbs = new LinkedList<>();

		List<CoreMap> sentences = SentenceParser.returnSentences(SentenceParser.makeAnnotation(input));

		boolean questionTag = false;
		boolean questionMark = false;
		boolean negation = false;

		// filter verbs and nomen

		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

				String word = token.get(TextAnnotation.class);
				// // this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);

				if (pos.equals("WRB"))
					questionTag = true;

				if (pos.equals(".") && word.equals("?"))
					questionMark = true;

				if ((pos.equals("RB") && (word.equals("not") || word.equals("n't")))
						|| pos.equals("DT") && word.equals("no"))
					negation = true;

				if (pos.startsWith("NN"))
					nomen.add(word);
				if (pos.startsWith("VB"))
					verbs.add(word);

			}

			// // this is the text of the token
		}

		float respondValue = 0.0f;

		if (robot.size() > 0) {

			String robotLast = robot.get(robot.size() - 1);
			for (String current : nomen) {

				if (robotLast.contains(current))
					respondValue += 1.0f;
			}
			for (String current : verbs) {

				if (robotLast.contains(current))
					respondValue += 1.0f;
			}
			
			if(nomen.size()+verbs.size()>0)
			respondValue /= (nomen.size() + verbs.size());
			else
			respondValue = 0.0f;
		}

		if (userName.equals("user")) {

			if (input.toLowerCase().contains("my name is")) {
				String ana = input.substring(input.toLowerCase().indexOf("my name is ") + 11);
				String splitString[] = ana.split(" ");

				answered = setName(nomen, splitString);
			} else if (input.toLowerCase().contains("iam")) {
				String ana = input.substring(input.toLowerCase().indexOf("iam ") + 4);
				String splitString[] = ana.split(" ");

				answered = setName(nomen, splitString);
			} else if (input.toLowerCase().contains("call me")) {
				String ana = input.substring(input.toLowerCase().indexOf("call me ") + 8);
				String splitString[] = ana.split(" ");

				answered = setName(nomen, splitString);
			} else if (robot.size() > 0) {
				if (robot.get(robot.size() - 1).contains("what is your name?")) {
					String splitString[] = input.split(" ");
					answered = setName(nomen, splitString);
				}
			}

		}
		
		
		if(input.split(" ").length < 3 && respondValue<0.3f && questionMark==false && questionTag ==false && !answered)
		{
			if(input.contains("thank"))
			botResponse("Nice, if I could help you somehow. Anything else?",200);
			else if(conversationList.size()<3)
			botResponse("With what can I help?",200);
			else if(conversationList.size()>3)
			botResponse("Anything else I can help you with?",200);
			
			answered=true;
		}

		if (developer)
			addRespond("stopped: " + stopped + "</br>QuestionIndicator:  -TAG: " + questionTag + "   -?: "
					+ questionMark + "  Negation?: " + negation + "  Respond: " + respondValue, 2);

		if (!answered) {
			try {
				if ((input.split(" ")).length < 3)
					addRespond("This is a very short request?.... let's see whether I find something</br>", 0);

				// artificial sleeping of programm

				ConversationBlock block = SearchInIndex.searchWithNN(input);
				conversationList.add(block);
				String textAnswer = block.getCurrent().getContent();

				botResponse(textAnswer, 800);

			} catch (Exception e) {
				e.printStackTrace();

				if(nomen.size() + verbs.size()<2)
				botResponse("Sorry, I could not find anything about his. What do you mean? Specify it please!", 800);
				else
				botResponse("Hmmm...nothing to find about this topic... </br>maybe try it in other words.",1000);
			}
		}
	}

	private boolean setName(List<String> nomen, String[] splitString) {
		boolean nn = false;
		for (String current : nomen) {
			if (current.equals(splitString[0]))
				nn = true;
		}
		if (nn) {
			userName = splitString[0];

			botResponse("Hello " + userName + "!", 100);

			return true;
		}

		return false;
	}

	private String collorTaggedSentence(String text) {
		List<CoreMap> sentences = SentenceParser.returnSentences(SentenceParser.makeAnnotation(text));

		String returnString = "";

		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

				String word = token.get(TextAnnotation.class);
				// // this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);

				returnString += " <font color=\"" + Indexer.getColorFromPosTag(pos) + "\">" + word + "_" + pos
						+ "</font>";
			}

			// // this is the text of the token
		}

		returnString += "</br>";
		return returnString;
	}

	public void botResponse(String content, int time) {
		Task<Void> sleeper = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				try {
					Thread.sleep(time);
				} catch (InterruptedException e) {
				}
				return null;
			}
		};
		sleeper.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {

				addRespond(content, 0);
			}
		});
		new Thread(sleeper).start();
	}

	/**
	 * add respond to console
	 * 
	 * @param content
	 *            String which shall be added
	 * @param userType
	 */
	public void addRespond(String content, int userType) {
		String toAdd = "";
		if (userType == 0) {
			if (lastUser != userType)
				toAdd = "</br><font color=\"red\">Robot: </font>";
			toAdd += content;

			robot.add(toAdd);

			lastUser = userType;

			if (content.contains("?"))
				questionFromBot = true;
			else
				questionFromBot = false;
		} else if (userType == 1) {
			if (lastUser != userType)
				toAdd = "</br><font color=\"blue\">" + userName + ": </font>";
			toAdd += content;

			user.add(toAdd);

			lastUser = userType;

		} else if (userType == 2) {
			toAdd = "</br><b>" + content + "</b>";

			lastUser = userType;
		}

		conversation += toAdd + "</br>";

		webEngine.loadContent(conversation + "</body></html>");
		System.out.println(conversation);
	}

}
