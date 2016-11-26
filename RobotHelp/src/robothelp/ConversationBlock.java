package robothelp;

import java.util.List;

public class ConversationBlock {
	public ConversationBlock()
	{}
	
	public ConversationBlock(String question, List<ContentBlock> answers, ContentBlock current, int evaluationIndex) {
		super();
		this.question = question;
		this.answers = answers;
		this.current = current;
		this.evaluationIndex = evaluationIndex;
	}
	public String getQuestion() {
		return question;
	}
	public void setQuestion(String question) {
		this.question = question;
	}
	
	public ContentBlock nextAnswer(){
		evaluationIndex++;
		
		if(evaluationIndex<answers.size())
		current=answers.get(evaluationIndex);
		
		return current;
		
	}
	
	public List<ContentBlock> getAnswers() {
		return answers;
	}
	public void setAnswers(List<ContentBlock> answers) {
		this.answers = answers;
	}
	public ContentBlock getCurrent() {
		return current;
	}
	public void setCurrent(ContentBlock current) {
		this.current = current;
	}
	public int getEvaluationIndex() {
		return evaluationIndex;
	}
	public void setEvaluationIndex(int evaluationIndex) {
		this.evaluationIndex = evaluationIndex;
	}
	private String question;
	private List<ContentBlock> answers;
	private ContentBlock current;
	/**
	 * for the evaluation of the question(which was the one shown)
	 */
	private int evaluationIndex=0;
}
