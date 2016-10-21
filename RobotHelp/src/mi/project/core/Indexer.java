package mi.project.core;

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

import mi.project.core.lucene.NoStemmingAnalyzer;
import mi.project.core.snlp.SentenceParser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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
	private Analyzer analyzer = new NoStemmingAnalyzer();

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
	 * Index sentences into certain category/country and topic
	 * 
	 * @param sentenceList
	 *            - sentences represented through a list of a CoreMap
	 * @param country
	 *            - country or category-Code
	 * @param topic
	 *            - topic (not used at the moment)
	 * @return a Map representing the number of certain sentence-lengths
	 *         "[5]=20" - 5 sentences with token-length 5
	 */
	public Map<Integer, Integer> createAndAddSentenceDoc(List<CoreMap> sentenceList, String country, String topic) {

		Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();
		// set Value of all sentences (INdex:0)
		countMap.put(0, sentenceList.size());

		System.out.println("StartIndexing");

		StringField countryField = new StringField(COUNTRY, country.toLowerCase() + "language", Field.Store.YES);
		StringField topicField = new StringField(TOPIC, topic, Field.Store.YES);
		TextField contentField = new TextField(CONTENT, "", Field.Store.YES);
		TextField posField = new TextField(POS, "", Field.Store.YES);

		for (CoreMap sentence : sentenceList) {
			Document doc = new Document();
			// only index sentences consisting of more than 2 tokens.
			if (sentence.get(TokensAnnotation.class).size() > 2) {

				int tokenCount = sentence.get(TokensAnnotation.class).size();

				// checks whether Index is already used: increment
				if (countMap.containsKey(tokenCount))
					countMap.put(tokenCount, countMap.get(tokenCount) + 1);
				// otherwise intitialize
				else
					countMap.put(tokenCount, 1);

				contentField.setStringValue(sentence.toString());
				doc.add(contentField);
				doc.add(countryField);
				doc.add(topicField);

				doc.add(new IntPoint(LENGTH, tokenCount));
				doc.add(new StoredField(LENGTH_STORE, tokenCount));

				/* POS Tagging */
				String posTaggedSentence = "";
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					// this is the text of the token
					String word = token.get(TextAnnotation.class);
					// System.out.print("Token: "+word);
					// this is the POS tag of the token
					String pos = token.get(PartOfSpeechAnnotation.class);

					posTaggedSentence = posTaggedSentence + word + "_" + pos + " ";
					// System.out.println(" Pos: "+pos);
					// this is the NER label of the token
					// String ne = token.get(NamedEntityTagAnnotation.class);
				}
				posField.setStringValue(posTaggedSentence);
				doc.add(posField);
				// End POS Tagging

				try {
					writer.addDocument(doc);
				} catch (IOException e) {
					System.out.println("catch");
					e.printStackTrace();
				}

			} else {
				// reduce Number of sentences if, one sentence is not accepted
				countMap.put(0, countMap.get(0) - 1);
			}
		}
		System.out.println("Neue Sätze indiziert: " + sentenceList.size());

		return countMap;
	}

	/**
	 * Saves metainformation of a certain language/category
	 * 
	 * @param language
	 *            - language/category
	 * @param countMap
	 *            - a Map representing the number of certain sentence-lengths
	 *            "[5]=20" - 5 sentences with token-length 5
	 */

	public void saveMetaInformation(String language, Map<Integer, Integer> countMap) {

		// create a new file with an ObjectOutputStream
		try (ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(indexDir + "\\" + language))) {

			// write something in the file
			oout.writeObject(countMap);
			System.out.println(language + " size: " + countMap.size());
			// close the stream
			oout.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * Reads metainformation of a certain language/category
	 * 
	 * @param indexDir
	 *            - path to index
	 * @param language
	 *            - name of language (->inducts path of language)
	 * @return a Map representing the number of certain sentence-lengths
	 *         "[5]=20" - 5 sentences with token-length 5
	 */
	public static Map<Integer, Integer> readMetaInformation(String indexDir, String language) {
		Map<Integer, Integer> countMap = null;

		String languageKey = language;
		if (language.contains("language")) // special case, because of early
											// indexing
			languageKey = language.substring(0, language.indexOf("language"));
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexDir + "\\" + languageKey))) {
			// read and print what we wrote before
			countMap = (Map<Integer, Integer>) ois.readObject();
		} catch (ClassNotFoundException ignore) {
			ignore.printStackTrace();
		} catch (IOException ignore) {
		}
		return countMap;
	}

	/**
	 * Returns list of languages/categories
	 * 
	 * @param indexDir
	 *            - path to index
	 * @return List of language/category-keys
	 */
	public static List<String> getIndexInformation(String indexDir) {
		List<String> meta = new LinkedList<String>();
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(indexDir + "\\METAINFORMATION"));

			for (String line : lines) {
				String[] parts = line.split(" ");
				if (parts.length == 2) {
					meta.add(parts[1]);
				}
			}
		} catch (IOException ignore) {

		}
		return meta;
	}

	/**
	 * Saves Indexinformations (which kind of languages/categories) exist
	 * 
	 * @param indexDir
	 *            - path to index
	 * @param languageKey
	 *            - key of language
	 * @param language
	 *            - transformation e.g. key= EN transformation=English (not
	 *            implemented for the integrated indexer)
	 */
	public void saveIndexInformation(String indexDir, String[] languageKey, String[] language) {
		List<String> meta = getIndexInformation(indexDir);
		if (!meta.contains(language) && languageKey.length == language.length) {
			try (BufferedWriter output = new BufferedWriter(new FileWriter(indexDir + "\\METAINFORMATION", true))) {
				for (int i = 0; i < languageKey.length; i++) {
					output.write(languageKey[i] + " " + language[i] + System.lineSeparator());
				}
				output.close();

			} catch (IOException e) {

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

	public static void main(String[] args) {

		Scanner in = new Scanner(System.in);
		String input = "";
		while (!input.equals("close")) {
			input = in.nextLine();
			
			edu.stanford.nlp.pipeline.Annotation document = SentenceParser.makeAnnotation(input);
			List<CoreMap> returnMap = SentenceParser.returnSentences(document);

			for (CoreMap current : returnMap) {

				boolean questionMark=false;
				boolean auxVerbBefore=false;
				int positionAuxVerb=150;
				//https://www.englishclub.com/grammar/verbs-questions_structure.htm
				if(current.toString().contains("?"))
				questionMark=true;
				
				System.out.println("sentence: " + current.toString());
				System.out.print("TAGS:");
				

				for (CoreLabel token : current.get(TokensAnnotation.class)) {
					// this is the text of the token
					String word = token.get(TextAnnotation.class);
					String pos = token.get(PartOfSpeechAnnotation.class);
					System.out.print(word + "_" + pos + "  ");

				}
				
				List<Tree> tree=SentenceParser.getParseTree(document);
				for(Tree currentTree:tree)
				{
					System.out.print(currentTree.toString()+"  ");
				}
				
				auxVerbBefore=SentenceParser.auxVerb(document,current.toString());
					
				System.out.println("Questionmark: "+questionMark+" (AuxVerb) "+auxVerbBefore);
			
			}
			
		}
		
		in.close();
	}

}