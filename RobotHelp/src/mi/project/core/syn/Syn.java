package mi.project.core.syn;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

/**
 * Class representing a single synonym. 
 * Getters and setters necessary for the
 * mapping
 */
@JsonIgnoreType
public class Syn {
	String word; //the term of the synonym
	//score represents the frequency on datamuse
	int score;

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}
}
