package miniJava.ContextualAnalysis;

public class OtherError extends RuntimeException{
	public OtherError(String str) {
		super(str);
	}
	
	public OtherError() {
		super("");
	}
}
