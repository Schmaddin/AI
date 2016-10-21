package mi.project.core.lucene;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mi.project.core.Indexer;
import mi.project.core.snlp.FileToSentences;

/**
 * MainIndexer is the main part of the indexing mechanism. Responsible for creating a new index
 * and adding documents to it.
 */
public class MainIndexer {
	static Indexer indexer;

	/**
	 * Takes a folder and adds all *.txt files to the indexing process which are part of a folder
	 * with the name "written.
	 * 
	 * @param directory - Path of folder
	 *         language - String classifying the language (Use language code here)
	 *         map - <Integer,Integer> Map for counting the sentences of each length (length,number)
	 *         topic - String representing the topic of the folder
	 */
	private static void recursevlyAdd(Path directory, String language, Map<Integer, Integer> map, String topic) {
		System.out.println(directory.toString() + " " + directory.getFileName());

		Map<Integer, Integer> countMap;
		if (map != null)
			countMap = map;
		else
			countMap = new HashMap<Integer, Integer>();

		try (DirectoryStream<Path> dir = Files.newDirectoryStream(directory)) {

			if (directory.getFileName().toString().equals("written")) {
				for (Path file : dir) {
					if (file.toAbsolutePath().toString().endsWith("txt")
							|| file.toAbsolutePath().toString().endsWith("TXT")) {

						joinIntegerMap(countMap,
								indexer.createAndAddSentenceDoc(
										FileToSentences.returnTestSentences(file.toAbsolutePath().toString()), language,
										topic));
						System.out.println(file.toString());
					}
				}
			} else {
				// Go into subfolders until the folder contains "written"
				for (Path folder : dir) {
					if (Files.isDirectory(folder)) {
						recursevlyAdd(folder, language, countMap, topic);
					}
				}
			}
		} catch (IOException exception) {
			exception.printStackTrace();
		}

	}
	
	/**
	 * Takes a folder and adds all *.txt files to the indexing process. 
	 * (Different to the recursivelyAdd function without boolean!)
	 * 
	 * @param directory - Path of folder
	 *         language - String classifying the language (Use language code here)
	 *         map - <Integer,Integer> Map for counting the sentences of each length (length,number)
	 *         topic - String representing the topic of the folder
	 *		   includeSubfolders - if subfolders shall be searched for txt files to.
	 */
	private static void recursevlyAdd(Path directory, String language, Map<Integer, Integer> map, String topic,
			boolean includeSubfolders) {
		System.out.println(directory.toString() + " " + directory.getFileName());

		Map<Integer, Integer> countMap;
		if (map != null)
			countMap = map;
		else
			countMap = new HashMap<Integer, Integer>();
		System.out.println(directory.toString());
		try (DirectoryStream<Path> dir = Files.newDirectoryStream(directory)) {
			for (Path file : dir) {
				if (file.toAbsolutePath().toString().endsWith("txt")
						|| file.toAbsolutePath().toString().endsWith("TXT")) {

					joinIntegerMap(countMap, indexer.createAndAddSentenceDoc(
							FileToSentences.returnTestSentences(file.toAbsolutePath().toString()), language, topic));
					System.out.println(file.toString());

				} else if (includeSubfolders && Files.isDirectory(file)) {
					recursevlyAdd(file, language, countMap, topic);
				}
			}
		} catch (IOException exception) {
			exception.printStackTrace();
		}

	}

	/**
	 * Merge two Maps.
	 *
	 * @param 
	 *         mergeIntoLeft - <Integer,Integer> Map one
	 *		   mergeRight - <Integer,Integer> Map two
	 *  @return Map<Integer, Integer> merged Map
	 */
	private static Map<Integer, Integer> joinIntegerMap(Map<Integer, Integer> mergeIntoLeft,
			Map<Integer, Integer> mergeRight) {

		Iterator<Entry<Integer, Integer>> it = mergeRight.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();

			Integer leftValue = 0;
			if (mergeIntoLeft.containsKey(pair.getKey()))
				leftValue = mergeIntoLeft.get(pair.getKey());

			mergeIntoLeft.put((Integer) pair.getKey(), leftValue + (Integer) pair.getValue());
		}
		return mergeRight;
	}

	/**
	 * Add Documents to Index. 
	 * Keeps track of the Metainformation and adds the files to the indexing process.
	 *
	 * @param 
	 *         indexDir - String of the index directory path
	 *         dirPath - String of the path the documents to add
	 *         language - String of the language of the documents
	 *		   topic - String of the topic of the documents
	 */
	public static void addDocuments(String indexDir, String dirPath, String languageKey, String language, String topic)
			throws IOException {
		// RETHINK: add Index information if languageKey/language is not present
		if (Files.isDirectory(Paths.get(dirPath))) {

			System.out.println("Start indexing");

			List<String> metainfo = Indexer.getIndexInformation(indexDir);
			if (!metainfo.contains(language)) {
				metainfo.add(language);
			}

			indexer = new Indexer(indexDir);

			Map<Integer, Integer> countMap = Indexer.readMetaInformation(indexDir, languageKey + "language");
			if (countMap == null) {
				countMap = new HashMap<Integer, Integer>();
			}
			recursevlyAdd(Paths.get(dirPath), language, countMap, topic, true);
			indexer.saveMetaInformation(language, countMap);
			String[] languageKeyArray = {languageKey+"language"};
			String[] languageArray = {language};
			indexer.saveIndexInformation(indexDir, languageKeyArray, languageArray);

			indexer.close();
			System.out.println("Finished indexing");

		} else {
			MainIndexer.addDocument(indexDir, dirPath, languageKey, language, topic);
		}

	}

	/**
	 * Add one Document to Index.
	 * Keeps track of the Metainformation and adds the files to the indexing process.
	 *
	 * @param 
	 *         indexDir - String of the index directory path
	 *         dirPath - String of the path the document to add
	 *         language - String of the language of the document
	 *		   topic - String of the topic of the document
	 */
	public static void addDocument(String indexDir, String filePath, String languageKey, String language, String topic)
			throws IOException {
		// RETHINK add Index information if languageKey/language is not present

		System.out.println("Start indexing");

		List<String> metainfo = Indexer.getIndexInformation(indexDir);
		if (!metainfo.contains(language)) {
			metainfo.add(language);
		}

		indexer = new Indexer(indexDir);
		Map<Integer, Integer> countMap = null;
		countMap = Indexer.readMetaInformation(indexDir, languageKey + "language");
		if (countMap == null) {
			countMap = new HashMap<Integer, Integer>();
		}
		if (filePath.endsWith("txt") || filePath.endsWith("TXT")) {

			joinIntegerMap(countMap,
					indexer.createAndAddSentenceDoc(FileToSentences.returnTestSentences(filePath), language, topic));
			System.out.println(filePath);
		}

		indexer.saveMetaInformation(language, countMap);
		String[] languageKeyArray = {languageKey+"language"};
		String[] languageArray = {language};
		indexer.saveIndexInformation(indexDir, languageKeyArray, languageArray);

		indexer.close();
		System.out.println("Finished indexing");

	}

}
