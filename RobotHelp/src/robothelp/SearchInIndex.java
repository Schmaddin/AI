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
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.PagedBytes.Reader;
import org.apache.lucene.util.Version;

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
		analyzer = new PorterStemmingAnalyzer();
		String fields[] = { Indexer.ID, Indexer.IDfield,Indexer.CAPTION, Indexer.CONTENT, Indexer.KEYS, Indexer.REFS, Indexer.FEEDBACK };
		parser = new MultiFieldQueryParser(fields, analyzer);
	}
	
	public static void close()
	{
		try {
			if(reader!=null)
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
	
	public static Document searchDocument(String caption)
	{

		TopDocs hitsTotal;
		try {
			hitsTotal = is.search(parser.parse("caption:" + caption),1000);
			
			
		
		System.out.println("hits: " + hitsTotal.totalHits);
		
		for(ScoreDoc scoreDocs:hitsTotal.scoreDocs)
		{
			Document document = is.doc(scoreDocs.doc);
		//if(document.get(Indexer.ID).equals(id))
		return document;
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}

	public static ConversationBlock searchWithNN(String queryString) throws Exception {
		List<String> nomen = SentenceParser.returnNN(queryString);
		
		String nomenComplet="";
		boolean searchWithNN=true;
		for(String current:nomen)
		{
			nomenComplet+=current+" ";
		}
		if(nomenComplet.equals(""))
			searchWithNN=false;
		
		parser.setLowercaseExpandedTerms(false);
		// standard query
		Query query;
		if(searchWithNN)
		query = parser
				.parse("content:" + queryString + " OR " + "topic:" + queryString + " OR keys:" + queryString + " OR feedback: "+queryString+" OR content:"+nomenComplet+" OR keys:"+nomenComplet+" OR topic: "+nomenComplet+" OR feedback:"+nomenComplet); // 4
		else
			query = parser
			.parse("content:" + queryString + " OR " + "topic:" + queryString + " OR keys:" + queryString); // 4
		
		System.out.println(query.toString());
		
		TopDocs hitsTotal = is.search(query, 1000); // 5
		System.out.println("hits: " + hitsTotal.totalHits);

		List<ContentBlock> contentBlocks=new LinkedList<>();
		int i = 0;
		for (ScoreDoc current : hitsTotal.scoreDocs) {
			if (i < 5) {
				Document document = is.doc(current.doc);
				String content = document.get(Indexer.CONTENT);
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

		String stopped=removeStopWords(queryString);
		int hits=0;
		if(!(stopped.equals("")||stopped.equals(" ")))
		{
		TopDocs hitsStopped = is.search(parser.parse(stopped), 1000); // 5
		hits=hitsStopped.totalHits;
		}
		
		if(hits==0)
		{
		contentBlocks=new LinkedList<>();
		System.out.println("kicked out by stopp-list");
		}
			
		return new ConversationBlock(queryString, contentBlocks, contentBlocks.get(0), 0);
	}
	
	public static ContentBlock createContentBlockFromDocument(Document doc)
	{

		List<ContentBlock> allblocks=ReadHelpFile.HelpFile.mainStructure;
		
		String id=doc.get(Indexer.IDfield);
		System.out.println(id);
		
		for(ContentBlock current: allblocks)
		{
			if(current.getId()==Integer.parseInt(id))
				return current;
		}
		return null;
	}
	
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
	    
	    AttributeFactory factory = AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY;

	    
	    StandardTokenizer tokenizer = new StandardTokenizer(factory);
	    tokenizer.setReader(new StringReader(textFile.toLowerCase()));
	    
	    TokenStream tokenStream=  new StopFilter(tokenizer, extendedStopWords);
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
