package robothelp;

import java.util.List;

public class ContentBlock {
	private int id;
	private String caption;
	private String content;
	private List<String> keys;
	private List<ContentBlock> subContent;
	private String oldQuestions="";
	private int[] ref;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<String> getKeys() {
		return keys;
	}

	public void setKeys(List<String> keys) {
		this.keys = keys;
	}

	public int[] getRef() {
		return ref;
	}

	public void setRef(int[] ref) {
		this.ref = ref;
	}

	public void setOldQuestions(String oldQuestions) {
		this.oldQuestions = oldQuestions;
	}

	public String getOldQuestions() {
		return this.oldQuestions;
	}
}