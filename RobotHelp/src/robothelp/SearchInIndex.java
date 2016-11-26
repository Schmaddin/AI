package robothelp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
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
import org.apache.lucene.store.FSDirectory;

public class SearchInIndex {

	private static String indexDir = "idx";

	private static boolean initialized = false;

	private static DirectoryReader reader;

	private static IndexSearcher is;

	private static QueryParser parser;

	private static Analyzer analyzer;

	/**
	 * Initialize the index
	 */
	public static void initialize() {

		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(indexDir).toPath()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		is = new IndexSearcher(reader);
		analyzer = new NoStemmingAnalyzer();
		String fields[] = { Indexer.ID, Indexer.CAPTION, Indexer.CONTENT, Indexer.KEYS, Indexer.REFS };
		parser = new MultiFieldQueryParser(fields, analyzer);
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

	public static String search(String queryString) throws ParseException, IOException {
		parser.setLowercaseExpandedTerms(false);
		// standard query
		Query query = parser
				.parse("content:" + queryString + " OR " + "topic:" + queryString + " OR keys:" + queryString); // 4
		// queries to combine

		String returnString = "";
		long start = System.currentTimeMillis();
		TopDocs hitsTotal = is.search(query, 1000); // 5
		System.out.print("hits: " + hitsTotal.totalHits);

		int i = 0;
		for (ScoreDoc current : hitsTotal.scoreDocs) {
			if (i < 3) {
				Document document = is.doc(current.doc);
				String content = document.get(Indexer.CONTENT);
				returnString += content + "</br></br>";
			}
			i++;
		}

		// building query of standard query and other parameters (short,
		// medium, long)
		Builder builder = new Builder();
		builder.add(query, Occur.MUST);
		BooleanQuery booleanQ = builder.build();

		// searching and saving in TopDocs
		// TopDocs hitsShort = is.search(booleanQ, 1000); // 5
		// System.out.print("short: " + hitsShort.totalHits);

		return returnString;

	}

	public static ConversationBlock searchWithNN(String queryString) throws ParseException, IOException {
		List<String> nomen = SentenceParser.returnNN(queryString);
		
		String nomenComplet="";
		for(String current:nomen)
		{
			nomenComplet+=current+" ";
		}
//		if(nomenComplet.equals(""))
//			return search(queryString);
		
		parser.setLowercaseExpandedTerms(false);
		// standard query
		Query query = parser
				.parse("content:" + queryString + " OR " + "topic:" + queryString + " OR keys:" + queryString + " OR content:"+nomenComplet+" OR keys:"+nomenComplet+" OR topic: "+nomenComplet); // 4
		// queries to combine

		String returnString = "";
		long start = System.currentTimeMillis();
		TopDocs hitsTotal = is.search(query, 1000); // 5
		System.out.print("hits: " + hitsTotal.totalHits);

		List<ContentBlock> contentBlocks=new LinkedList<>();
		int i = 0;
		for (ScoreDoc current : hitsTotal.scoreDocs) {
			if (i < 5) {
				Document document = is.doc(current.doc);
				String content = document.get(Indexer.CONTENT);
				returnString += content + "</br></br>";
				contentBlocks.add(createContentBlockFromDocument(document));
			}
			i++;
		}

		// building query of standard query and other parameters (short,
		// medium, long)
		Builder builder = new Builder();
		builder.add(query, Occur.MUST);
		BooleanQuery booleanQ = builder.build();

		// searching and saving in TopDocs
		// TopDocs hitsShort = is.search(booleanQ, 1000); // 5
		// System.out.print("short: " + hitsShort.totalHits);

		
		return new ConversationBlock(queryString, contentBlocks, contentBlocks.get(0), 0);
	}
	
	public static ContentBlock createContentBlockFromDocument(Document doc)
	{

		List<ContentBlock> allblocks=ReadHelpFile.HelpFile.mainStructure;
		
		String id=doc.get(Indexer.ID);
		
		for(ContentBlock current: allblocks)
		{
			System.out.println(current.getId()+"  "+id+"  "+Integer.parseInt(id));
			if(current.getId()==Integer.parseInt(id))
				return current;
		}
		return null;
	}
}
