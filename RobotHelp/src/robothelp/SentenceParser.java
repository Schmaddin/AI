package robothelp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

/**
 * Class for parsing and annotating sentences with the StanfordNLP (POS,
 * lemmatization, NER, parsing and coreference resolution.
 */
public class SentenceParser {
	private static Properties props;
	private static StanfordCoreNLP pipeline;
	
	// is going to be called in first usage of the static class
	static {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		props = new Properties();
		// props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse,
		// dcoref");
		props.put("annotators", "tokenize, ssplit, pos, parse");
		// props.put("parse.model",

		// "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
		pipeline = new StanfordCoreNLP(props);

	}
	
	public static void init() {} // can be used, to initialize the static initialisation section

	/**
	 * Annotates a Inputstring with Stanford Annotations
	 * @param text
	 * @return Stanford Annotation-format
	 */
	public static Annotation makeAnnotation(String text)
	{
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);
		
		return document;
	}
	
	/**
	 * Static function for parsing and annotating Text
	 * 
	 * @param text
	 *            Text which shall be parsed. Can contain multiple sentences,
	 *            but take care the String is not too long.
	 * @return List of CoreMap, representing annotated and parsed sentence
	 */
	public static List<CoreMap> returnSentences(Annotation document) {


		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		return sentences;
	}

	/**
	 * Returns a List containing all Nouns of a sentence
	 * @param sentence
	 * @return List of Nouns
	 */
	public static List<String> returnNN(String sentence)
	{
		List<String> listOfNN=new LinkedList<String>();
		
		List<CoreMap> annoted = returnSentences(makeAnnotation(sentence));
		
		for(CoreMap current:annoted)
		{
			for (CoreLabel token: current.get(TokensAnnotation.class)) {

				 String word = token.get(TextAnnotation.class);
				// // this is the POS tag of the token
				 String pos = token.get(PartOfSpeechAnnotation.class);
				 
				 if(pos.startsWith("NN"))
					 listOfNN.add(word);
			}
		}
		
		return listOfNN;
	}

	/**
	 * NOT USED : for getting a tree representation of a sentence
	 * @param document
	 * @return
	 */
	public static List<Tree> getParseTree(Annotation document) {


		List<Tree> forest = new ArrayList<Tree>();
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			// for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			// // this is the text of the token
			// String word = token.get(TextAnnotation.class);
			// // this is the POS tag of the token
			// String pos = token.get(PartOfSpeechAnnotation.class);
			// // this is the NER label of the token
			// String ne = token.get(NamedEntityTagAnnotation.class);
			// }

			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			// Alternatively, this is the Stanford dependency graph of the
			// current sentence, but without punctuations
			// SemanticGraph dependencies =
			// sentence.get(BasicDependenciesAnnotation.class);
			forest.add(tree);
		}
		return forest;
	}

}
