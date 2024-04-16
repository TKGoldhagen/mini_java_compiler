/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import  miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.ClassType;

public class ClassDecl extends Declaration {

  public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  //Token id_token = new Token(TokenType.IDENTIFIER, cn, posn);
	  //Identifier id = new Id(id_token);
	  //ClassType class_type = new ClassType(id, posn);
	  //i know this looks disgusting but java requires that the super() call is on the first line of the constructor :(
	  
	  super(cn, new ClassType(new Identifier(new Token(TokenType.IDENTIFIER, cn, posn)), posn), posn);
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
  }
  
  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }
      
  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
}
