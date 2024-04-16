/**
 * miniJava Abstract Syntax Tree classes
 * @author Tomer G
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class NullReference extends Reference {
	
	public NullReference(SourcePosition posn, Token input_token) {
		super(posn);
		this.tok = input_token;
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitNullReference(this, o);
	}
	
	public String get_spelling() {
		return this.tok.getTokenText();
	}
	
	private Token tok;
}
