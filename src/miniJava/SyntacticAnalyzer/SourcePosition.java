package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	private int line_number;
	private int column_number;
	
	public SourcePosition (int in_line_number, int in_column_number) {
		line_number = in_line_number;
		column_number = in_column_number;
	}
	
	@Override public String toString() {
		return "Line " + line_number + ", Column " + column_number;
	}
}
