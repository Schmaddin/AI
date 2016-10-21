package mi.project.core.snlp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.util.CoreMap;

/**
 * Class for reading in a file, and returning parsed sentences (StanfordNLP)
 *
 */
public class FileToSentences {

	/**
	 * static method for reading in a file, and return annotated sentences
	 * 
	 * @param path
	 *            to the file
	 * @return List of CoreMap representing single parsed and annotated
	 *         sentences
	 */
	public static final List<CoreMap> returnTestSentences(String path) {

		List<CoreMap> sentences = new LinkedList<CoreMap>();
		List<String> input = null;
		try {
			input = Files.readAllLines(Paths.get(path));
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		String current = "";
		int i = 0;
		int j = 0;
		for (String index : input) {
			// heuristic amount of line reading to prevent out of memory
			// exception in sentenceParser (StanfordNLP)
			// we want to index at least 20 lines, if there comes no "." at the
			// end
			if (i < 20) {
				current += " " + index;
				i++;
				j++;
			} else if (index.endsWith(".") || j > 50) { // BUT we split the
														// input latest after 50
														// lines if their occurs
														// no "." at the end of
														// a line
				current += " " + index;

				current = cleanDoubleWhiteSpace(current); // clean occuring
															// double
															// whitespaces
				for (CoreMap sentence : SentenceParser.returnSentences(current)) {
					sentences.add(sentence);
				}
				current = "";
				i = 0;
				j = 0;
			} else {
				current += " " + index;
				j++;
			}

		}
		for (CoreMap sentence : SentenceParser.returnSentences(current)) {
			sentences.add(sentence);
		}

		return sentences;
	}

	/**
	 * Helper function which cleans double white-space out of strings
	 * 
	 * @param input-String
	 * @return cleaned String
	 */
	public static String cleanDoubleWhiteSpace(String input) {
		char sentence[] = new char[input.length()];
		char sentenceOrig[] = input.toCharArray();
		int addChar = 0;
		for (int i = 0; i < sentenceOrig.length; i++) {
			if (addChar == 0 && sentenceOrig[i] != ' ') {
				sentence[addChar] = sentenceOrig[i];
				addChar++;
			} else if (addChar > 0) {
				if (sentence[addChar - 1] != ' ') {
					sentence[addChar] = sentenceOrig[i];
					addChar++;
				} else if (sentenceOrig[i] != ' ') {
					sentence[addChar] = sentenceOrig[i];
					addChar++;
				}

			}

		}

		return new String(sentence).substring(0, addChar);
	}

}
