package mi.project.core.syn;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class for requesting the syn-set from datamuse
 */
public class RequestSynonymSet {

	/**
	 * static method, that requests a synset from datamuse(HTTP-Request) and
	 * maps it to a List of 'Syn'
	 * 
	 * @param queryString
	 * @return List of 'Syn' - representing the set of the synonyms
	 */
	public static List<Syn> getSynonymSet(String queryString) {
		String response;

		try {
			queryString = queryString.replaceAll(" ", "%20");
			response = HttpRequest.request("http://api.datamuse.com/words?rel_syn=" + queryString);

		} catch (IOException ignore) { // returns the result set of 'hello' if
										// their was an error at executing the
										// search
			System.out.println("exception");
			response = "[{\"word\":\"hi\",\"score\":1664},{\"word\":\"howdy\",\"score\":145},{\"word\":\"hullo\",\"score\":56}]";
		}

		System.out.println(response);

		// mapping on class Syn
		ObjectMapper mapper = new ObjectMapper();
		List<Syn> synSet;

		try {
			synSet = mapper.readValue(response, new TypeReference<List<Syn>>() {
			});
		} catch (IOException e) {
			synSet = new LinkedList<>();
		}

		return synSet;

	}

}
