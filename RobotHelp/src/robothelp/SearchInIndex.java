package robothelp;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.PagedBytes.Reader;
import org.apache.lucene.util.Version;

/**
 * Class for search operations
 * 
 * @author Martin
 *
 */
public class SearchInIndex {

	/**
	 * path to index
	 */
	private static String indexDir = "idx";

	/**
	 * reader initialized?
	 */
	private static boolean initialized = false;

	/**
	 * Directory Reader for file operations
	 */
	private static DirectoryReader reader;

	/**
	 * IndexSearcher for search operations
	 */
	private static IndexSearcher is;

	/**
	 * QueryParser for transforming the query for lucene
	 */
	private static QueryParser parser;

	/**
	 * Analyzer for search operations (analyzer are for stemming, stopwords...)
	 */
	private static Analyzer analyzer;

	/**
	 * Initialize the index and basic search operations
	 */
	public static void initialize() {

		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(indexDir).toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		is = new IndexSearcher(reader);
		analyzer = new PorterStemmingAnalyzer();
		String fields[] = { Indexer.ID, Indexer.IDfield, Indexer.CAPTION, Indexer.CONTENT, Indexer.KEYS, Indexer.REFS,
				Indexer.FEEDBACK };
		parser = new MultiFieldQueryParser(fields, analyzer);
	}

	/**
	 * closing the index
	 */
	public static void close() {
		try {
			if (reader != null)
				reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Choose Indexdirectory
	 * 
	 * @param dir
	 *            - path to index
	 */
	public static void chooseIndex(String dir) {
		indexDir = dir;
		if (initialized) { // if already initialized
			try {
				reader.close(); // close
			} catch (IOException ignore) {

			}
			try {
				reader = DirectoryReader.open(FSDirectory.open(new File(indexDir).toPath()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			initialize();
		}
	}

	/**
	 * Search for a document through it's caption
	 * 
	 * @param caption
	 * @return Lucene Document which include all fields
	 */
	public static Document searchDocument(String caption) {

		TopDocs hitsTotal;
		try {
			hitsTotal = is.search(parser.parse("caption:" + caption), 1000);

			System.out.println("hits: " + hitsTotal.totalHits);

			for (ScoreDoc scoreDocs : hitsTotal.scoreDocs) {
				Document document = is.doc(scoreDocs.doc);
				return document;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * Search for a query with focus on NN 
	 * @param queryString
	 * @return a ConversationBlock with several answers, ordered by search value
	 * @throws Exception
	 */
	public static ConversationBlock searchWithNN(String queryString) throws Exception {

		if (queryString.contains("?"))
			queryString = queryString.replace('?', ' ');

		if (queryString.contains("!"))
			queryString = queryString.replace('!', ' ');
		System.out.println(queryString);

		List<String> nomen = SentenceParser.returnNN(queryString);

		String nomenComplet = "";
		boolean searchWithNN = true;
		for (String current : nomen) {
			nomenComplet += current + " ";
		}
		if (nomenComplet.equals(""))
			searchWithNN = false;

		parser.setLowercaseExpandedTerms(false);

		Query query1;
		Query query1F = parser.parse("feedback: " + queryString);
		Query query2;

		query1 = parser.parse("content:" + queryString + " OR " + "topic:" + queryString + " OR keys:" + queryString); // 4

		query2 = parser.parse("content:" + nomenComplet + " OR keys:" + nomenComplet + " OR topic: " + nomenComplet
				+ " OR feedback:" + nomenComplet);

		BoostQuery boost1 = new BoostQuery(query1, 0.25f);
		BoostQuery boost1f = new BoostQuery(query1F, 0.5f);
		BoostQuery boost2 = new BoostQuery(query2, 0.25f);
		// query2.s
		Builder builder = new Builder();
		builder.add(boost1, Occur.SHOULD);
		builder.add(boost1f, Occur.SHOULD);

		if (searchWithNN)
			builder.add(boost2, Occur.MUST);

		BooleanQuery booleanQ = builder.build();

		TopDocs hitsTotal = is.search(booleanQ, 1000); // 5
		System.out.println("hits: " + hitsTotal.totalHits);

		List<ContentBlock> contentBlocks = new LinkedList<>();
		int i = 0;
		for (ScoreDoc current : hitsTotal.scoreDocs) {
			if (i < 7) {
				Document document = is.doc(current.doc);
				String content = document.get(Indexer.CONTENT);
				contentBlocks.add(createContentBlockFromDocumentID(document));
			}
			i++;
		}

		// if query contains a lot of stop words, search is invalid
		String stopped = removeStopWords(queryString);
		int hits = 0;
		if (!(stopped.equals("") || stopped.equals(" "))) {
			TopDocs hitsStopped = is.search(parser.parse(stopped), 1000); // 5
			hits = hitsStopped.totalHits;
		}

		if (hits == 0) {
			contentBlocks = new LinkedList<>();
			System.out.println("kicked out by stopp-list");
		}

		return new ConversationBlock(queryString, contentBlocks, contentBlocks.get(0), 0);
	}

	/**
	 * Returns a single ContentBlock from a Lucene Document
	 * @param doc
	 * @return ContentBlock
	 */
	public static ContentBlock createContentBlockFromDocumentID(Document doc) {

		List<ContentBlock> allblocks = ReadHelpFile.HelpFile.mainStructure;

		String id = doc.get(Indexer.IDfield);
		System.out.println(id);

		for (ContentBlock current : allblocks) {
			if (current.getId() == Integer.parseInt(id))
				return current;
		}
		return null;
	}

	/**
	 * Removes Stopwords (Lucene List and manual extension)
	 * @param textFile, which is going to be cleaned
	 * @return cleaned String
	 * @throws Exception
	 */
	public static String removeStopWords(String textFile) throws Exception {
		org.apache.lucene.analysis.CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
		org.apache.lucene.analysis.CharArraySet extendedStopWords = stopWords.copy(stopWords);
		extendedStopWords.add("so");
		extendedStopWords.add("you");
		extendedStopWords.add("can");
		extendedStopWords.add("me");
		extendedStopWords.add("is");
		extendedStopWords.add("are");
		extendedStopWords.add("why");
		extendedStopWords.add("how");
		extendedStopWords.add("are");
		extendedStopWords.add("what");
		extendedStopWords.add("up");
		extendedStopWords.add("my");
		extendedStopWords.add("your");
		extendedStopWords.add("this");
		extendedStopWords.add("that");
		extendedStopWords.add("do");
		extendedStopWords.add("have");
		extendedStopWords.add("i");
		extendedStopWords.add("about");
		extendedStopWords.add("for");
		extendedStopWords.add("where");
		extendedStopWords.add("with");

		AttributeFactory factory = AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY;

		StandardTokenizer tokenizer = new StandardTokenizer(factory);
		tokenizer.setReader(new StringReader(textFile.toLowerCase()));

		TokenStream tokenStream = new StopFilter(tokenizer, extendedStopWords);
		StringBuilder sb = new StringBuilder();
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			sb.append(term + " ");
		}

		tokenStream.close();

		return sb.toString();

	}
}
