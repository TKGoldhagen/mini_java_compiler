package miniJava.SyntacticAnalyzer;

public class Token {
	private TokenType _type;
	private String _text;
	private SourcePosition position;
	
	public Token(TokenType type, String text, SourcePosition in_position) {
		_type = type;
		_text = text;
		position = in_position;
	}
	
	public TokenType getTokenType() {
		return _type;
	}
	
	public String getTokenText() {
		return _text;
	}
	
	public SourcePosition getTokenPosition() {
		return position;
	}
}
