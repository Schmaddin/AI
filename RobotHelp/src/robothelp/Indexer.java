package robothelp;

import java.io.BufferedWriter;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymMap.Parser;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.time.TimeExpression.Annotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class Indexer {
	// FIELD NAMES:
	public static final String COUNTRY = "country";
	public static final String CONTENT = "content";
	public static final String LENGTH = "length";
	public static final String LENGTH_STORE = "lengthstore";
	public static final String TOPIC = "topic";
	public static final String POS = "pos";

	public static final String ID = "id";
	public static final String IDfield = "idfield";
	public static final String CAPTION = "caption";
	public static final String KEYS = "keys";
	public static final String REFS = "ref";
	public static final String FEEDBACK = "feedback";

	private static final String[] POSTAGS3 = { "PDT", "POS", "PRP", "SYM" };

	// the description of all POS-Types are saved in a Map and declared in
	// following static block
	private static final Map<String, String> posDescription;
	static {
		Map<String, String> posTags = new HashMap<>();
		posTags.put("CC", "Coordinating conjunction");
		posTags.put("CD", "Cardinal number");
		posTags.put("DT", "Determiner");
		posTags.put("EX", "Existential there");
		posTags.put("IN", "Preposition or subordinating conjunction");
		posTags.put("JJ", "Adjective");
		posTags.put("JJR", "Adjective, comparative");
		posTags.put("JJS", "Adjective, superlative");
		posTags.put("LS", "List item marker");
		posTags.put("MD", "Modal");
		posTags.put("NN", "Noun, singular or mass");
		posTags.put("NNS", "Noun, plural");
		posTags.put("NNP", "Proper noun, singular");
		posTags.put("NNPS", "Proper noun, plural");
		posTags.put("PRP", "Personal pronoun");
		posTags.put("PRP$", "Possesive pronoun");
		posTags.put("RB", "Adverb");
		posTags.put("RBR", "Adverb, comparative");
		posTags.put("RBS", "Adverb, superlative");
		posTags.put("RP", "Particle");
		posTags.put("SYM", "Symbol");
		posTags.put("TO", "to");
		posTags.put("UH", "Interjection");
		posTags.put("VB", "Verb, base form");
		posTags.put("VBD", "Verb, past tense");
		posTags.put("VBG", "Verb, gerund or present participle");
		posTags.put("VBN", "Verb, past particle");
		posTags.put("VBP", "Verb, non-3rd person singular present");
		posTags.put("VBZ", "Verb, 3rd person singular present");
		posTags.put("WDT", "Wh-determiner");
		posTags.put("WP", "Wh-pronoun");
		posTags.put("WP$", "possesive wh-pronoun");
		posTags.put("WRB", "Wh-adverb");

		posTags.put("WH", "Wh");
		posTags.put(".", "Sentence ending");

		posDescription = Collections.unmodifiableMap(posTags);
	}

	// analyzer
	private Analyzer analyzer = new PorterStemmingAnalyzer();

	// index writer
	private String indexDir;
	private IndexWriter writer;

	/**
	 * Constructor for opening an Index in a certain directory
	 * 
	 * @param indexDir
	 *            - path to folder
	 * @throws IOException
	 *             is thrown if the index is corrupt or not existing
	 */
	public Indexer(String indexDir) throws IOException {
		this.indexDir = indexDir;

		Directory dir = FSDirectory.open(new File(indexDir).toPath());

		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		writer = new IndexWriter(dir, iwc);
	}

	/**
	 * standard-constructor tries to open the index in a certain subdirectory
	 */
	public Indexer() {
		indexDir = "idx";
		Directory dir = null;
		try {
			dir = FSDirectory.open(new File(indexDir).toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		try {
			writer = new IndexWriter(dir, iwc);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Closing the index writer
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		writer.close();
	}

	/**
	 * Delete old fields and update the new structure
	 */
	public void updateDocument() {

		try {
			writer.deleteAll();
			addHelp(ReadHelpFile.HelpFile.mainStructure);
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}


	public void addHelp(List<ContentBlock> content) {

		System.out.println("StartIndexing");

		TextField captionField = new TextField(CAPTION, "", Field.Store.YES);
		TextField contentField = new TextField(CONTENT, "", Field.Store.YES);
		TextField keyField = new TextField(KEYS, "", Field.Store.YES);
		keyField.setBoost(0.35f);
		TextField refsField = new TextField(REFS, "", Field.Store.YES);
		TextField feedbackField = new TextField(FEEDBACK, " ", Field.Store.YES);
		feedbackField.setBoost(0.35f);
		for (ContentBlock entry : content) {
			Document doc = new Document();

			contentField.setStringValue(entry.getContent());
			captionField.setStringValue(entry.getCaption());
			
			
			doc.add(new IntPoint(ID, entry.getId()));
			doc.add(new StoredField(IDfield, entry.getId()));


			doc.add(captionField);
			doc.add(contentField);

			String keys = "";
			for (String key : entry.getKeys())
				keys = keys + " " + key;
			keyField.setStringValue(keys);
			doc.add(keyField);

			String refs = "";
			if (entry.getRef() != null) {
				for (int ref : entry.getRef())
					refs = refs + " " + ref;
			}
			refsField.setStringValue(refs);
			doc.add(refsField);

			feedbackField.setStringValue(entry.getOldQuestions());
			doc.add(feedbackField);

			try {
				writer.addDocument(doc);
			} catch (IOException e) {
				System.out.println("catch");
				e.printStackTrace();
			}

		}
	}

	/**
	 * Colorisation for POS-Tags
	 * 
	 * @param pos
	 *            - POS-code
	 * @return String representing the color in HEX
	 */
	public static String getColorFromPosTag(String pos) {
		// Classes: CC, CD, DT, EX, FW, IN, JJ, LS, MD, NN, PDT, POS, PRP, RB,
		// RP, SYM, TO, UH, VB, WH, .
		// + some subclasses
		// Color color = Color.web("#FFFFFF");
		System.out.println(pos);
		String color = "-none-";
		if (pos.length() == 0) {
			return "-none-";
		}
		if (pos.equals(".") || pos.equals("SYM")) {
			// white
			return "#FFFFFF";
		}

		String posClass = "";
		String posSuffix = "";
		if (pos.length() == 2) {
			posClass = pos.substring(0, 2);
		} else if (pos.length() >= 3) {
			for (int i = 0; i < POSTAGS3.length; i++) {
				if (pos.equals(POSTAGS3[i])) {
					posClass = pos;
					posSuffix = pos.substring(3);
				} else {
					posClass = pos.substring(0, 2);
					posSuffix = pos.substring(2);
				}
			}
		}
		System.out.println(posClass);
		if (posClass.equals("NN")) {
			// light-blue
			color = "#0174DF";
		} else if (posClass.equals("JJ")) {
			// red
			color = "#FA5858";
		} else if (posClass.equals("RB")) {
			// orange
			color = "#FA8258";
		} else if (posClass.equals("VB")) {
			// green
			color = "#2EFE2E";
		} else if (posClass.equals("RP")) {
			// yellow
			color = "#F3F781";
		} else if (posClass.equals("CC") || posClass.equals("IN")) {
			// yellowgreen
			color = "#BFFF00";
		} else if (posClass.equals("CD") || posClass.equals("SYM") || posClass.equals("TO")) {
			// blueviolet
			color = "#BCA9F5";
		} else if (posClass.equals("DT") || posClass.equals("PDT")) {
			// greenblue
			color = "#58FAF4";
		} else if (posClass.equals("FW")) {
			// violett
			color = "#FA58F4";
		} else if (posClass.equals("MD")) {
			// redviolett
			color = "#FA5882";
		} else if (posClass.equals("MD")) {
			// brown
			color = "#F5ECCE";
		} else if (posClass.equals("UH") || posClass.equals("WH")) {
			// grey
			color = "#E6E6E6";
		} else if (posClass.equals("EX") || posClass.equals("LS") || posClass.equals("POS") || posClass.equals("LS")
				|| posClass.equals("PRP")) {
			// grey
			color = "#E6E6E6";
		} else {
			color = "-none-";
		}
		if (posSuffix.length() > 0) {
			// TODO: Think about the subclasses,..
		}
		return color;
	}

	/**
	 * Getting description for a pos-tag
	 * 
	 * @param pos
	 *            - POS-code
	 * @return description to the corresponding POS-code
	 */
	public static String getExplanationPosTag(String pos) {
		String description;
		description = Indexer.posDescription.get(pos);
		if (description == null) {
			description = "description missing";
		}
		return description;
	}

	/**
	 * returns all POS
	 * 
	 * @return all POS in a Set
	 */
	public static Set<String> getPosSet() {
		return Indexer.posDescription.keySet();
	}

	/**
	 * Returns the superclass of a certain POS
	 * 
	 * @param pos
	 *            - POS-code
	 * @return POS-code (superclass)
	 */
	public static String getPosClass(String pos) {
		// Remaining Classes: CC, CD, DT, EX, FW, IN, JJ, LS, MD, NN, PDT, POS,
		// PRP, RB, RP, SYM, TO, UH, VB, WH
		if (pos.equals("WP")) {
			return "WH";
		}
		if (pos.length() <= 2) {
			return pos;
		} else if (pos.startsWith("W")) {
			return "WH";
		} else {
			for (int i = 0; i < POSTAGS3.length; i++) {
				if (POSTAGS3[i].equals(pos)) {
					return pos;
				} else if (pos.equals("PRP$")) {
					return "PRP";
				}
			}
			return pos.substring(0, 2);
		}
	}


}