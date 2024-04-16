package miniJava.ContextualAnalysis;

public class IdentificationError extends RuntimeException {
	public IdentificationError(String str) {
		super(str);
	}
	
	public IdentificationError() {
		super("");
	}
}
