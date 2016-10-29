package robothelp;

import java.io.File;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.FSDirectory;

import mi.project.core.datapackages.EvaluationField;
import mi.project.core.datapackages.Snippet;
import mi.project.core.syn.RequestSynonymSet;
import mi.project.core.syn.Syn;

public class Searcher {

	public static List<String> countries = new LinkedList<String>(); // list
																		// containing
																		// all
																		// countries
																		// how
																		// they
																		// appear
																		// in
																		// the
																		// index
	private static Map<String, String> rename = new HashMap<String, String>(); // how
																				// certain
																				// languages/keys
																				// should
																				// be
																				// replaced
																				// at
																				// representing

	private static Map<String, Map<Integer, Integer>> countMaps = new HashMap<String, Map<Integer, Integer>>();


	private static String indexDir;// = "idx";

	private static boolean initialized = false;

	private static DirectoryReader reader;

	private static IndexSearcher is;

	private static QueryParser parser;

	private static Analyzer analyzer;


	/**
	 * Initialize the index
	 */
	public static void initialize() {

		if (!initialized) {
			loadMetaInformation();
			try {
				reader = DirectoryReader.open(FSDirectory.open(new File(indexDir).toPath()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			is = new IndexSearcher(reader);
			analyzer = new NoStemmingAnalyzer();
			String fields[] = { Indexer.CONTENT, Indexer.COUNTRY, Indexer.LENGTH_STORE, Indexer.POS };
			parser = new MultiFieldQueryParser(fields, analyzer);
			initialized = true;
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
	 * Reads metainformations for renaming categories/languages, and for their
	 * general occurence
	 */
	private static void loadMetaInformation() {

		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(indexDir + "\\METAINFORMATION"));

			for (String line : lines) {
				String[] parts = line.split(" ");
				if (parts.length == 2) {
					rename.put(parts[0], parts[1]);
					if (!countries.contains(parts[0]))
						countries.add(parts[0]);
				}
			}
		} catch (IOException ignore) {
		} finally {
		}

		for (String country : countries) {
			countMaps.put(country, Indexer.readMetaInformation(indexDir, country));
		}
	}

	/**
	 * Rename languageIdentifier
	 * 
	 * @param language
	 *            - languageIdentifier which should be replaced
	 * @return replaced String
	 */
	private static String replaceLanguage(String language) {

		if (rename.containsKey(language))
			return (String) rename.get(language);
		else
			return language;
	}

	/**
	 * search
	 * 
	 * @param queryString
	 * @param rangeSettings
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void search(String queryString, int[] rangeSettings) throws IOException, ParseException {
		initialize(); // always checks whether search is initialized

		parser.setLowercaseExpandedTerms(false);

		List<EvaluationField> total = new ArrayList<EvaluationField>();
		List<EvaluationField> smallSentences = new ArrayList<EvaluationField>();
		List<EvaluationField> mediumSentences = new ArrayList<EvaluationField>();
		List<EvaluationField> longSentences = new ArrayList<EvaluationField>();

		// for all languages/categories
		for (String countryCode : countries) {
			System.out.println("CountryCode: " + countryCode + " " + queryString);
			// standard query
			Query query = parser.parse("content:" + queryString + " AND " + "country:" + countryCode); // 4
			// queries to combine
			Query queryShort = IntPoint.newRangeQuery(Indexer.LENGTH, rangeSettings[0], rangeSettings[1]);
			Query queryMedium = IntPoint.newRangeQuery(Indexer.LENGTH, rangeSettings[2], rangeSettings[3]);
			Query queryLong = IntPoint.newRangeQuery(Indexer.LENGTH, rangeSettings[4], rangeSettings[5]);

			long start = System.currentTimeMillis();
			TopDocs hitsTotal = is.search(query, 1000); // 5
			System.out.print("hits: " + hitsTotal.totalHits);

			// building query of standard query and other parameters (short,
			// medium, long)
			Builder builder = new Builder();
			builder.add(query, Occur.MUST);
			builder.add(queryShort, Occur.MUST);
			BooleanQuery booleanQ = builder.build();

			// searching and saving in TopDocs
			TopDocs hitsShort = is.search(booleanQ, 1000); // 5
			System.out.print("short: " + hitsShort.totalHits);

			// building query of standard query and other parameters (short,
			// medium, long)
			builder = new Builder();
			builder.add(query, Occur.MUST);
			builder.add(queryMedium, Occur.MUST);
			booleanQ = builder.build();

			// searching and saving in TopDocs
			TopDocs hitsMedium = is.search(booleanQ, 1000); // 5
			System.out.print("medium: " + hitsMedium.totalHits);

			// building query of standard query and other parameters (short,
			// medium, long)
			builder = new Builder();
			builder.add(query, Occur.MUST);
			builder.add(queryLong, Occur.MUST);
			booleanQ = builder.build();

			// searching and saving in TopDocs
			TopDocs hitsLong = is.search(booleanQ, 1000); // 5
			System.out.println("long: " + hitsMedium.totalHits);

			long end = System.currentTimeMillis();
			System.err.println("Found " + hitsTotal.totalHits + // 6
					" document(s) (in " + (end - start) + // 6
					" milliseconds) that matched query '" + // 6
					queryString + "':"); // 6

			// entries in list of evaluation fields
			total.add(new EvaluationField(replaceLanguage(countryCode), "total", hitsTotal.totalHits,
					(double) hitsTotal.totalHits / countMaps.get(countryCode).get(0), hitsTotal, queryString));
			
			// relative normalization with returnLanguageRangeSentenceNumber
			smallSentences.add(new EvaluationField(replaceLanguage(countryCode),
					"small: " + rangeSettings[0] + "-" + rangeSettings[1], hitsShort.totalHits,
					(double) hitsShort.totalHits / returnLanguageRangeSentenceNumber(1, 9, countryCode), hitsShort, queryString));
			mediumSentences.add(new EvaluationField(replaceLanguage(countryCode),
					"medium: " + rangeSettings[2] + "-" + rangeSettings[3], hitsMedium.totalHits,
					(double) hitsMedium.totalHits / returnLanguageRangeSentenceNumber(10, 20, countryCode), hitsMedium, queryString));
			longSentences.add(new EvaluationField(replaceLanguage(countryCode),
					"long: " + rangeSettings[4] + "-" + rangeSettings[5], hitsLong.totalHits,
					(double) hitsLong.totalHits / returnLanguageRangeSentenceNumber(21, 250, countryCode), hitsLong, queryString));
		}

		if (model != null) {
			// aggregate sentence lengths in a map
			Map<String, List<EvaluationField>> frequencySentenceLength = new HashMap<String, List<EvaluationField>>();
			frequencySentenceLength.put(smallSentences.get(0).getValuename(), smallSentences);

			frequencySentenceLength.put(longSentences.get(0).getValuename(), longSentences);

			frequencySentenceLength.put(mediumSentences.get(0).getValuename(), mediumSentences);

			frequencySentenceLength.put(total.get(0).getValuename(), total);

			// set data into model -> userinterface
			model.frequencyOfTerm(frequencySentenceLength);
		}

		//// Synonyms
		// create SynonymQuery
		String singleWordQuery = reduceToOneWordQuery(queryString); // reduce
																	// query to
																	// one word
		// request Synonyms
		List<Syn> syns = RequestSynonymSet.getSynonymSet(singleWordQuery);

		// create result list
		List<EvaluationField> synResultList = new LinkedList<EvaluationField>();

		// add original word to the list as well, for better comparising of the
		// results
		Syn oldSyn = new Syn();
		oldSyn.setWord(singleWordQuery);
		syns.add(0, oldSyn);

		// search in all languages/categories
		for (String countryCode : countries) {

			int i = 0;
			// for all synonyms
			for (Syn current : syns) {
				if (i < 7) {
					Query query = parser.parse("content:\"" + current.getWord() + "\" AND " + "country:" + countryCode); // 4
					TopDocs hitSyn = is.search(query, 1000);
					synResultList.add(new EvaluationField(replaceLanguage(countryCode), current.getWord(),
							hitSyn.totalHits, (double) hitSyn.totalHits / (double) countMaps.get(countryCode).get(0),
							hitSyn, current.getWord())); //relative normation with total number: countMaps(countryCode).get(0)
					i++;
				}
			}

		}

		// set data into model -> userinterface
		if (model != null)
			model.syns(synResultList);

		/// POS

		List<String> searchTerms = new LinkedList<String>();
		// important that * is the first search
		int allOccurenceField = 0; // "*" for norming the other results
		searchTerms.add("*");
		searchTerms.add("vb*");
		searchTerms.add("nn*");
		searchTerms.add("jj*");
		searchTerms.add("rb*");

		List<EvaluationField> listType = new LinkedList<EvaluationField>();

		// for all countries/categories
		for (String countryCode : countries) {

			// for all POS (main-categories)
			for (String current : searchTerms) {

				Query query = parser.parse("content:" + queryString + " AND " + Indexer.POS + ":" + singleWordQuery
						+ "_" + current + " AND " + "country:" + countryCode); // 4
				TopDocs hitPos = is.search(query, 1000);

				if (current.equals("*"))
					allOccurenceField = hitPos.totalHits; // for relative normation

				listType.add(new EvaluationField(replaceLanguage(countryCode), current, hitPos.totalHits,
						(double) hitPos.totalHits / (double) allOccurenceField, hitPos, singleWordQuery));

			}
		}

		// set data into model -> userinterface
		if (model != null)
			model.type(listType);

	}

	/**
	 * Method for reducing a phrase-query etc... to a one-wordquery (longest
	 * word), because not all types of query are accepted by synonym-search or
	 * POS-Search. The method for sure does not cover all cases yet
	 * 
	 * @param queryString
	 *            = original UserQuery
	 * @return reduced UserQuery
	 */
	public static String reduceToOneWordQuery(String queryString) {
		String edit = queryString.replaceAll("\"", "");
		edit = edit.replace('(', ' ');
		edit = edit.replace(')', ' ');
		edit = edit.replaceAll("AND", " ");
		edit = edit.replaceAll("NOT", " ");
		edit = edit.replaceAll("OR", " ");
		edit = edit.replace('+', ' ');

		String[] queryParts = edit.split(" ");

		int longest = 0;
		int lengthLongest = 0;
		for (int i = 0; i < queryParts.length; i++) {
			if (queryParts[i].length() > lengthLongest) {
				lengthLongest = queryParts[i].length();
				longest = i;
			}
		}

		return queryParts[longest];
	}

	/**
	 * singleSearch performs a query search just on all fields (without length
	 * restrictions)
	 * 
	 * @param queryString
	 *            - Searchterm
	 * @param addQuery
	 *            - optional Query which can be added
	 * @return List of EvaluationField containing the result
	 * @throws IOException
	 * @throws ParseException
	 */
	public static List<EvaluationField> singleSearch(String queryString, Query addQuery)
			throws IOException, ParseException {
		initialize();
		parser.setLowercaseExpandedTerms(false);

		List<EvaluationField> results = new LinkedList<EvaluationField>();

		for (String countryCode : countries) {
			System.out.println("CountryCode: " + countryCode);
			Query query = parser.parse("content:" + queryString + "	 AND " + "country:" + countryCode); // 4

			System.out.println(query.toString());
			TopDocs hits = is.search(query, 1000); // 5
			System.out.print("hits: " + hits.totalHits);

			Builder builder = new Builder();
			builder.add(query, Occur.MUST);
			if (addQuery != null)
				builder.add(addQuery, Occur.MUST);
			BooleanQuery booleanQ = builder.build();

			TopDocs searchHits = is.search(booleanQ, 1000);
			results.add(new EvaluationField(replaceLanguage(countryCode), queryString, searchHits.totalHits,
					(double) searchHits.totalHits / (double) countMaps.get(countryCode).get(0), searchHits,
					queryString));

		}

		return results;
	}

	/**
	 * searchOnAllLength performs a query search just on all fields (without length
	 * restrictions)
	 * 
	 * @param queryString
	 *            - Searchterm
	 * @return List of EvaluationField containing the result
	 */
	public static List<EvaluationField> searchOnAllLength(String text) {

		try {
			return singleSearch(text, null);
		} catch (IOException | ParseException e) {

		}
		return null;
	}

	/**
	 * singleSearchPOS performs a query search on all fields (without length
	 * restrictions) and on a certain POS
	 * 
	 * @param queryString
	 *            - Searchterm
	 * @param pos
	 *            - POS-Tag
	 * @return List of EvaluationField containing the result
	 */
	public static List<EvaluationField> singleSearchPos(String word, String pos) {
		try {
			// for calculating the relative frequency, we need the total occurrence 
			List<EvaluationField> totalList = singleSearch(word,
					parser.parse(Indexer.POS + ":" + reduceToOneWordQuery(word) + "_*"));

			List<EvaluationField> returnList = singleSearch(word,
					parser.parse(Indexer.POS + ":" + reduceToOneWordQuery(word) + "_" + pos));
			for (EvaluationField current : returnList) {
				for (EvaluationField total : totalList) {
					if (total.getLanguage().equals(current.getLanguage())) {
						current.setValuename(pos);
						current.setValue2((double) current.getValue() / (double) total.getValue());
					}
				}
			}

			return returnList;

		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the number of sentences within a certain length (lower and upper
	 * bound)
	 * 
	 * @param start
	 *            - start length
	 * @param end
	 *            - end length
	 * @param language
	 *            - language identifier
	 * @return - sum of the number of sentences
	 */
	private static int returnLanguageRangeSentenceNumber(int start, int end, String language) {
		if (!countMaps.containsKey(language)) {
			return -1;
		}

		int counter = 0;

		// aggregation
		for (int i = start; i <= end; i++) {
			if (countMaps.get(language).containsKey(i)) {
				counter += countMaps.get(language).get(i);
			}
		}

		return counter;
	}

	/**
	 * Creating snippets for search result !!!some search types don't work:
	 * wildcard-search, fuzzy-search...
	 * 
	 * @param queryString
	 *            - query of the original search
	 * @param scoreDocs
	 *            - results in form of ScoreDocs
	 * @return - returns a List of Snippets
	 * @throws IOException
	 * @throws InvalidTokenOffsetsException
	 * @throws ParseException
	 */
	@SuppressWarnings("deprecation")
	public static List<Snippet> highlightKeywords(String queryString, ScoreDoc[] scoreDocs)
			throws IOException, InvalidTokenOffsetsException, ParseException {
		initialize();
		Query query = parser.parse(queryString);
		QueryScorer queryScorer = new QueryScorer(query, Indexer.CONTENT);
		Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
		Formatter formatter = new SimpleHTMLFormatter();
		Highlighter highlighter = new Highlighter(formatter, queryScorer);

		highlighter.setTextFragmenter(fragmenter); // Set fragment to highlight
		List<Snippet> snippets = new ArrayList<Snippet>();
		for (ScoreDoc scoreDoc : scoreDocs) {
			Document document = is.doc(scoreDoc.doc);
			String content = document.get(Indexer.CONTENT);

			TokenStream tokenStream = TokenSources.getAnyTokenStream(is.getIndexReader(), scoreDoc.doc, Indexer.CONTENT,
					document, analyzer);
			String fragment;
			try {
				fragment = highlighter.getBestFragment(tokenStream, content);
			} catch (java.lang.NoClassDefFoundError e) {
				fragment = "error at creating the snippet";
				System.out.print(queryString);
			}

			snippets.add(new Snippet(scoreDoc.doc + "", fragment,
					Integer.parseInt(is.doc(scoreDoc.doc).get(Indexer.LENGTH_STORE)),
					is.doc(scoreDoc.doc).get(Indexer.CONTENT), is.doc(scoreDoc.doc).get(Indexer.POS),
					is.doc(scoreDoc.doc).get(Indexer.COUNTRY)));
		}
		return snippets;
	}

}
