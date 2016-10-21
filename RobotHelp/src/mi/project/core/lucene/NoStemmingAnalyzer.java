package mi.project.core.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/*
 * Standard Tokenizer and lowerCaseFilter
 * No Stopword Elimination
 */
public class NoStemmingAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer source = new StandardTokenizer();
		TokenStream filter = new LowerCaseFilter(source);
		return new TokenStreamComponents(source,filter);
	}
}