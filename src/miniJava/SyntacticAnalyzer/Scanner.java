package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;
	private int current_line;
	private int current_column;
	
	private boolean end_of_file = false;
	private boolean end_of_comment;
	private boolean start_of_token;

	private TokenType[] single_char_non_alphanumeric_token_types = {
		TokenType.LCURLY, TokenType.RCURLY, TokenType.LPAREN, TokenType.RPAREN, TokenType.EQUALS, TokenType.OPERATOR, 
		TokenType.OPERATOR, TokenType.LSQUARE, TokenType.RSQUARE, TokenType.SEMICOLON, TokenType.COMMA, 
		TokenType.OPERATOR, TokenType.OPERATOR, TokenType.OPERATOR, TokenType.OPERATOR, 
		TokenType.OPERATOR, TokenType.DOT
	};

	private String[] single_char_non_alphanumeric_token_literals = {
		"{", "}", "(", ")", "=", "-", "!", "[", "]", ";", ",", ">", "<", "+", "*", "/", "."
	};
		
	private TokenType[] multi_char_non_alphanumeric_token_types = {
		TokenType.OPERATOR, TokenType.OPERATOR, TokenType.OPERATOR, TokenType.OPERATOR, 
		TokenType.OPERATOR, TokenType.OPERATOR
	};

	private String[] multi_char_non_alphanumeric_token_literals = {
		"==", "<=", ">=", "!=", "&&", "||"
	};
	
	private TokenType[] alphanumeric_token_types = {
		TokenType.CLASS, TokenType.INT, TokenType.BOOLEAN, TokenType.VOID, TokenType.THIS, TokenType.IF, TokenType.WHILE, 
		TokenType.ELSE, TokenType.RETURN, TokenType.NEW, TokenType.VISIBILITY, TokenType.VISIBILITY, TokenType.ACCESS, 
		TokenType.BOOL_LITERAL, TokenType.BOOL_LITERAL, TokenType.NULL
	};
	
	private String[] alphanumeric_token_literals = {
		"class", "int", "boolean", "void", "this", "if", "while", "else", "return", "new", "public", "private", 
		"static", "true", "false", "null"
	};
	
	public Scanner( InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		this.current_line = 1;
		this.current_column = 1;
		
		nextChar();
	}
	
	public Token scan() {
		this._currentText = new StringBuilder();

		//-----------------Get rid of whitespace and comments------------------------
		start_of_token = false;
		while (end_of_file == false && start_of_token == false) {
			//if current character is whitespace, skip it
			if (is_whitespace(_currentChar)) {
				nextChar();
			}
			//if current character is '/', it could be the start of a comment
			else if (_currentChar == '/') {
				takeIt();
				nextChar();
				//check if it's a single-line comment
				if (_currentChar == '/') {
					//if so, then progress cursor until you reach a newline
					forget_spelling();
					end_of_comment = false;
					while (end_of_comment == false) {
						if (_currentChar == '\n' || _currentChar == '\r') { end_of_comment = true; }
						nextChar();
					}
				}
				//check if it's a multiline comment
				else if (_currentChar == '*') {
					//if so, then progress cursor until '*/'
					end_of_comment = false;
					int length = _currentText.length();
					while (end_of_comment == false) {
						nextChar();
						takeIt();
						length++;
						if (end_of_file == true) {
							_errors.reportError("Block comment not terminated by */");
							return null;
						}
						
						if (_currentText.charAt(length-2) == '*' && _currentText.charAt(length-1) == '/') {
							nextChar();
							forget_spelling();
							end_of_comment = true;
						}
					}
				}
				//otherwise, it's a divides operator
				else {
					return makeToken(TokenType.OPERATOR);
				}
			}
			//if current character is anything else, it's the start of a token, so break out of the loop
			else {
				start_of_token = true;
			}
		}
		
		//Return null if end of file reached
		if (end_of_file == true) { return null; }
		
		//----------------------------Determine token type--------------------------------
		//If the first character is a digit, then read digits and return an INT_LITERAL token
		if (Character.isDigit(_currentChar)) {
			while (Character.isDigit(_currentChar)) {
				takeIt();
				nextChar();
			}
			return makeToken(TokenType.INT_LITERAL);
		}
		
		//Either search for an alphanumeric token or not
		if (is_alphanumeriscore(_currentChar)) {
			//First, read the whole token until the next non-alphanumeric or underscore character
			while (is_alphanumeriscore(_currentChar) && end_of_file == false) {
				takeIt();
				nextChar();
			}
			
			//Check if the token is a fixed value alphanumeric token (class, new, etc.)
			String token_text = _currentText.toString();
			for (int i = 0; i < alphanumeric_token_literals.length; i++) {
				if (token_text.equals(alphanumeric_token_literals[i])) {
					return makeToken(alphanumeric_token_types[i]); 
				}
			}

			//Check if the token is an id (must start with a letter)
			if (Character.isLetter(token_text.charAt(0))) {
				//and that all other characters are letters, digits, or underscore
				boolean valid_id = true;
				for (int j = 0; j < token_text.length(); j++) {
					if (!is_alphanumeriscore(token_text.charAt(j))) {
						valid_id = false;
					}
				}
				
				if (valid_id == true) { return makeToken(TokenType.IDENTIFIER); }
			}
		}
		else {
			Token output = null;
			
			//First, check for non-alphanumeric tokens that are only 1 char long
			takeIt();
			nextChar();
			String current_spelling = _currentText.toString();
			for (int i = 0; i < single_char_non_alphanumeric_token_literals.length; i++) {
				if (current_spelling.equals(single_char_non_alphanumeric_token_literals[i])) {
					output = makeToken(single_char_non_alphanumeric_token_types[i]); 
				}
			}
			
			//Then, check for tokens that are two chars long, overriding previous finding if any
			takeIt();
			current_spelling = _currentText.toString();
			for (int k = 0; k < multi_char_non_alphanumeric_token_literals.length; k++) {
				if (current_spelling.equals(multi_char_non_alphanumeric_token_literals[k])) { 
					output = makeToken(multi_char_non_alphanumeric_token_types[k]);
					nextChar();
				}
			}
			
			if (output != null) { return output; }
		}	
		
		//If none of those worked, the token is not valid  
		_errors.reportError("Invalid token read: " + _currentText.toString());
		return makeToken(TokenType.INVALID);
	}
	
	private boolean is_whitespace(char input) {
		if (input == ' ' || input == '\t' || input == '\n' || input == '\r') {
			return true;
		}
		else {
			return false;
		}
	}
	
	private boolean is_alphanumeriscore(char in) {
		//returns true if input is alphanumeric or an underscore
		return Character.isLetterOrDigit(in) || (in == '_');
	}
	
	private void forget_spelling() {
		this._currentText = new StringBuilder();
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
	}
	
	private void nextChar() {
		try {
			int c;
			boolean ascii_found = false;
			
			//reads in characters until it finds ASCII (otherwise it ignores it)
			while (ascii_found == false) {
				c = _in.read();
				if (c <= 127 && c >= -1) {
					ascii_found = true;
					_currentChar = (char)c;
					if (c == -1) { 
						end_of_file = true; 
					}
					
					if (c == 10) {
						//if a new line is read, increment line counter and reset column counter
						current_line++;
						current_column = 0;
					}
					else {
						//otherwise, increment column counter
						current_column++;
					}
				}
			}
			
		} catch( IOException e ) {
			_errors.reportError("Scan Error: I/O Exception!");
		}
	}
	
	private Token makeToken( TokenType toktype ) {
		SourcePosition output_position = new SourcePosition(current_line, current_column);
		Token output_token = new Token(toktype, _currentText.toString(), output_position);
		return output_token;
	}
}
