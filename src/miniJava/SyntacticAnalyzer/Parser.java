package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}

	
	public AST parse() {
		try {
			AST full_tree = parseProgram();
			return full_tree;
		} 
		catch( SyntaxError e ) {
			return null;
		}
	}
	
	// Program ::= ClassDeclaration* eot
	private Package parseProgram() throws SyntaxError {
		ClassDeclList class_decl_list = new ClassDeclList();
		
		//if file is empty, just return
		if (_currentToken == null) {
			return new Package(class_decl_list, null);
		}
		
		//Otherwise, keep parsing class declarations until eot
		SourcePosition start = _currentToken.getTokenPosition();	
		while (_currentToken != null) {
			class_decl_list.add(parseClassDeclaration());
		}

		Package package_object = new Package(class_decl_list, start); 
		return package_object;
	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		//remember start position and accept class
		SourcePosition start = _currentToken.getTokenPosition();
		accept(TokenType.CLASS);
		
		//remember identifier text value and accept id and lcurly
		String id = _currentToken.getTokenText();
		accept(TokenType.IDENTIFIER);
		accept(TokenType.LCURLY);
		
		//for each member declaration, add it to its respective list
		FieldDeclList field_decl_list = new FieldDeclList();
		MethodDeclList method_decl_list = new MethodDeclList();
		while (_currentToken.getTokenType() != TokenType.RCURLY) {
			MemberDecl field_or_method_decl = parseFieldOrMethodDeclaration();
			
			if (field_or_method_decl instanceof FieldDecl) {
				field_decl_list.add((FieldDecl) field_or_method_decl);
			}
			else if (field_or_method_decl instanceof MethodDecl) {
				method_decl_list.add((MethodDecl) field_or_method_decl);
			}
			else {
				_errors.reportError("Incorrect formatting of class declaration.");
				throw new SyntaxError();
			}
		}
		
		accept(TokenType.RCURLY);
		
		//pack into a class declaration object and return
		ClassDecl class_decl = new ClassDecl(id, field_decl_list, method_decl_list, start);
		return class_decl;
	}
	
	// FieldDeclaration ::= Visibility? Access? Type id;
	// MethodDeclaration ::= Visibility? Access? (Type|void) id ( ParameterList? ) { Statement* }
	private MemberDecl parseFieldOrMethodDeclaration() throws SyntaxError {
		//remember start position and if its private
		SourcePosition start = _currentToken.getTokenPosition();
		boolean is_private = false;
		if (_currentToken.getTokenType() == TokenType.VISIBILITY) {
			if (_currentToken.getTokenText().equals("private")) {
				is_private = true;
			}
			accept(TokenType.VISIBILITY);
		}
		
		//remember if its static
		boolean is_static = false;
		if (_currentToken.getTokenType() == TokenType.ACCESS) {
			if (_currentToken.getTokenText().equals("static")) {
				is_static = true;
			}
			accept(TokenType.ACCESS); 
		}
		
		boolean seen_void = false;
		TypeDenoter type;
		if (_currentToken.getTokenType() == TokenType.VOID) {
			type = new BaseType(TypeKind.VOID, _currentToken.getTokenPosition());
			accept(TokenType.VOID);
			seen_void = true;
		}
		else {
			type = parseType();
		}
		
		//remember id
		String name = _currentToken.getTokenText();
		accept(TokenType.IDENTIFIER);
		
		if (_currentToken.getTokenType() == TokenType.SEMICOLON && seen_void == false) {
			//return a FieldDeclaration
			accept(TokenType.SEMICOLON);
			FieldDecl field_decl = new FieldDecl(is_private, is_static, type, name, start);
			return field_decl;
		}
		else {
			//return a MethodDeclaration
			FieldDecl temp_field_decl = new FieldDecl(is_private, is_static, type, name, start);
			ParameterDeclList parameter_list = new ParameterDeclList(); //by default, empty list
			StatementList statement_list = new StatementList();
			
			accept(TokenType.LPAREN);
			if (_currentToken.getTokenType() != TokenType.RPAREN) {
				parameter_list = parseParameterList();
			}
			accept(TokenType.RPAREN);
			
			accept(TokenType.LCURLY);
			while (_currentToken.getTokenType() != TokenType.RCURLY) {
				statement_list.add(parseStatement());
			}
			accept(TokenType.RCURLY);
			
			MethodDecl method_decl = new MethodDecl(temp_field_decl, parameter_list, statement_list, start);
			return method_decl;
		}
	}
	
	// Type ::= int | int[] | id | id[] | boolean
	private TypeDenoter parseType() throws SyntaxError {
		SourcePosition start = _currentToken.getTokenPosition();
		
		if (_currentToken.getTokenType() == TokenType.INT) {
			accept(TokenType.INT);
			TypeDenoter int_type = new BaseType(TypeKind.INT, start); 
			
			if (_currentToken.getTokenType() == TokenType.LSQUARE) {
				//return an int array
				accept(TokenType.LSQUARE);
				accept(TokenType.RSQUARE);
				TypeDenoter int_array = new ArrayType(int_type, start);
				return int_array;				
			}
			
			//if not an int array, has to be an int
			return int_type;
		}
		else if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
			//remember id
			Identifier class_id = new Identifier(_currentToken);
			accept(TokenType.IDENTIFIER);
			TypeDenoter class_type = new ClassType(class_id, start);
			
			if (_currentToken.getTokenType() == TokenType.LSQUARE) {
				//return an object array
				accept(TokenType.LSQUARE);
				accept(TokenType.RSQUARE);
				TypeDenoter object_array = new ArrayType(class_type, start);
				return object_array;
			}		
			
			//if not an object array, has to be an object
			return class_type;
		}
		else if (_currentToken.getTokenType() == TokenType.BOOLEAN){
			//has to be boolean
			accept(TokenType.BOOLEAN);
			TypeDenoter boolean_type = new BaseType(TypeKind.BOOLEAN, start);
			return boolean_type;
		}
		else {
			_errors.reportError("Incorrect formatting of type");
			throw new SyntaxError();
		}
	}
	
	// ParameterList ::= Type id (, Type id)*
	private ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList param_list = new ParameterDeclList();
		
		//add first parameter object to list 
		SourcePosition start = _currentToken.getTokenPosition();
		TypeDenoter param_type = parseType();
		String param_id = _currentToken.getTokenText();
		accept(TokenType.IDENTIFIER);
		ParameterDecl parameter = new ParameterDecl(param_type, param_id, start);
		param_list.add(parameter);
		
		//add all subsequent parameters to list 
		while (_currentToken.getTokenType() == TokenType.COMMA) {
			start = _currentToken.getTokenPosition();
			accept(TokenType.COMMA);
			
			param_type = parseType();
			
			param_id = _currentToken.getTokenText();
			accept(TokenType.IDENTIFIER);
			
			parameter = new ParameterDecl(param_type, param_id, start);
			param_list.add(parameter);
		}
		
		return param_list;
	}
	
	// ArgumentList ::= Expression (, Expression)*
	private ExprList parseArgumentList() throws SyntaxError {
		//add first expression to list
		ExprList expr_list = new ExprList();
		Expression expr = parseExpression();
		expr_list.add(expr);
		
		//if any, add subsequent expressions to list
		while (_currentToken.getTokenType() == TokenType.COMMA) {
			accept(TokenType.COMMA);
			expr = parseExpression();
			expr_list.add(expr);
		}
		
		return expr_list;
	}
	
	// Reference ::= (id | this) (.id)*
	private Reference parseReference() throws SyntaxError {
		Reference ref; 
		SourcePosition start = _currentToken.getTokenPosition();
		
		if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
			Identifier id = new Identifier(_currentToken);
			accept(TokenType.IDENTIFIER);
			ref = new IdRef(id, start);
		}
		else {
			accept(TokenType.THIS);
			ref = new ThisRef(start);
		}
		
		while (_currentToken.getTokenType() == TokenType.DOT) {
			accept(TokenType.DOT);
			Identifier id = new Identifier(_currentToken);
			accept(TokenType.IDENTIFIER);
			ref = new QualRef(ref, id, ref.posn);
		}
		
		return ref;
	}
	
	// Stratified grammar for expressions
	// Expression ::= Disjunction
	// Disjunction ::= Conjunction (|| Conjuction)*
	// Conjunction ::= Equality (&& Equality)*
	// Equality ::= Relational ((== | !=) Relational)*
	// Relational ::= Additive ((<= | >= | < | >) Additive)*
	// Additive ::= Multiplicative ((+ | -) Multiplicative)*
	// Multiplicative ::= Unary ((* | /) Unary)*
	// Unary ::= (- | !) HighestPrecedence | HighestPrecedence
	// HighestPrecedence ::= num | true | false | new id() | new (int|id) [Expression]
	//						| Reference [Expression] | Reference (ArgumentList?) | Reference 
	//						| (Expression)
	private Expression parseExpression() {	
		Expression expression = parseDisjunction();
		return expression;
	}
	
	// Disjunction ::= Conjunction (|| Conjuction)*
	private Expression parseDisjunction() {
		Expression disjunction = parseConjunction();
		SourcePosition start = _currentToken.getTokenPosition();
		
		while (_currentToken.getTokenText().equals("||")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			disjunction = new BinaryExpr(op, disjunction, parseConjunction(), start);
		}
		
		return disjunction;
	}
	
	// Conjunction ::= Equality (&& Equality)*
	private Expression parseConjunction() {
		Expression conjunction = parseEquality();
		SourcePosition start = _currentToken.getTokenPosition();
		
		while (_currentToken.getTokenText().equals("&&")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			conjunction = new BinaryExpr(op, conjunction, parseEquality(), start);
		}
		
		return conjunction;
	}
	
	// Equality ::= Relational ((== | !=) Relational)*
	private Expression parseEquality() {
		Expression equality = parseRelational();
		SourcePosition start = _currentToken.getTokenPosition();
		
		while (_currentToken.getTokenText().equals("==") || _currentToken.getTokenText().equals("!=")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			equality = new BinaryExpr(op, equality, parseRelational(), start);
		}
		
		return equality;		
	}
	
	// Relational ::= Additive ((<= | >= | < | >) Additive)*
	private Expression parseRelational() {
		Expression relational = parseAdditive();
		SourcePosition start = _currentToken.getTokenPosition();
		
		while (_currentToken.getTokenText().equals("<=") || _currentToken.getTokenText().equals(">=") || _currentToken.getTokenText().equals("<") || _currentToken.getTokenText().equals(">")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			relational = new BinaryExpr(op, relational, parseAdditive(), start);
		}
		
		return relational;		
	}
	
	// Additive ::= Multiplicative ((+ | -) Multiplicative)*
	private Expression parseAdditive() {
		Expression additive = parseMultiplicative();
		SourcePosition start = _currentToken.getTokenPosition();
		
		while (_currentToken.getTokenText().equals("+") || _currentToken.getTokenText().equals("-")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			additive = new BinaryExpr(op, additive, parseMultiplicative(), start);
		}
		
		return additive;		
	}
	
	// Multiplicative ::= Unary ((* | /) Unary)*
	private Expression parseMultiplicative() {
		Expression multiplicative = parseUnary();
		SourcePosition start = _currentToken.getTokenPosition();
		
		while (_currentToken.getTokenText().equals("*") || _currentToken.getTokenText().equals("/")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			multiplicative = new BinaryExpr(op, multiplicative, parseUnary(), start);
		}
		
		return multiplicative;	
	}
	
	// Unary ::= (- | !) HighestPrecedence | HighestPrecedence
	private Expression parseUnary() {
		SourcePosition start = _currentToken.getTokenPosition();
		
		if (_currentToken.getTokenText().equals("-") || _currentToken.getTokenText().equals("!")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			Expression unary = new UnaryExpr(op, parseUnary(), start);
			return unary;
		}
		else {
			Expression unary = parseHighestPrecedence();
			return unary;
		}
	}
	
	// HighestPrecedence ::= Reference [Expression] | Reference (ArgumentList?) | Reference
	//						| new id() | new (int|id) [Expression]
	//						| (Expression) | num | true | false | null
	private Expression parseHighestPrecedence() {
		SourcePosition start = _currentToken.getTokenPosition();

		// HighestPrecedence ::= Reference [Expression] | Reference (ArgumentList?) | Reference
		if (_currentToken.getTokenType() == TokenType.IDENTIFIER || _currentToken.getTokenType() == TokenType.THIS) {
			Reference reference = parseReference();
			Expression expression = new RefExpr(reference, start);
			
			//HighestPrecedence ::= Reference [Expression]
			if (_currentToken.getTokenType() == TokenType.LSQUARE) {
				accept(TokenType.LSQUARE);
				expression = new IxExpr(reference, parseExpression(), start);
				accept(TokenType.RSQUARE);
				return expression;
			}
			
			//HighestPrecedence ::= Reference (ArgumentList?)
			if (_currentToken.getTokenType() == TokenType.LPAREN) {
				accept(TokenType.LPAREN);
				ExprList arg_list = new ExprList(); //intialize as empty
				
				if (_currentToken.getTokenType() != TokenType.RPAREN) {
					arg_list = parseArgumentList();
				}
				accept(TokenType.RPAREN);
				
				expression = new CallExpr(reference, arg_list, start);
				return expression;
			}
			
			return expression;
		}
		
		// HighestPrecedence ::= new id() | new (int|id) [Expression]
		if (_currentToken.getTokenType() == TokenType.NEW) {
			accept(TokenType.NEW);
			
			//HighestPrecedence ::= new int [Expression]
			if (_currentToken.getTokenType() == TokenType.INT) {
				accept(TokenType.INT);
				TypeDenoter type = new BaseType(TypeKind.INT, start);
				accept(TokenType.LSQUARE);
				Expression array_expression = new NewArrayExpr(type, parseExpression(), start);
				accept(TokenType.RSQUARE);
				return array_expression;
			}
			
			//HighestPrecedence ::= new id() | new id [Expression]
			if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
				Identifier id = new Identifier(_currentToken);
				ClassType class_type = new ClassType(id, _currentToken.getTokenPosition());
				accept(TokenType.IDENTIFIER);
				
				//HighestPrecedence ::= new id()
				if (_currentToken.getTokenType() == TokenType.LPAREN) {
					accept(TokenType.LPAREN);
					accept(TokenType.RPAREN);
					Expression object_expression = new NewObjectExpr(class_type, start);
					return object_expression;
				}
				
				//HighestPrecedence ::= new id [Expression]
				if (_currentToken.getTokenType() == TokenType.LSQUARE) {
					accept(TokenType.LSQUARE);
					Expression inner_expr = parseExpression();
					accept(TokenType.RSQUARE);
					Expression array_expression = new NewArrayExpr(class_type, inner_expr, start);
					return array_expression;
				}
				
				_errors.reportError("expected ( or [ after new id in highest precedence expression");
				throw new SyntaxError();
			}
				
			_errors.reportError("expected id or int token after new in highest precedence expression");
			throw new SyntaxError();
		}
		
		//HighestPrecedence ::= (Expression)
		if (_currentToken.getTokenType() == TokenType.LPAREN) {
			accept(TokenType.LPAREN);
			Expression expression = parseExpression();
			accept(TokenType.RPAREN);
			return expression;
		}
		
		//HighestPrecedence ::= num
		if (_currentToken.getTokenType() == TokenType.INT_LITERAL) {
			IntLiteral literal = new IntLiteral(_currentToken);
			accept(TokenType.INT_LITERAL);
			Expression literal_expression = new LiteralExpr(literal, start);
			return literal_expression;
		}
		
		//HighestPrecedence ::= true | false
		if (_currentToken.getTokenType() == TokenType.BOOL_LITERAL) {
			BooleanLiteral literal = new BooleanLiteral(_currentToken);
			accept(TokenType.BOOL_LITERAL);
			Expression literal_expression = new LiteralExpr(literal, start);
			return literal_expression;
		}
		
		//HighestPrecedence ::= null
		if (_currentToken.getTokenType() == TokenType.NULL) {
			Reference null_ref = new NullReference(start, _currentToken);
			accept(TokenType.NULL);
			Expression ref_expr = new RefExpr(null_ref, start);
			return ref_expr;
		}
		
		_errors.reportError("highest precedence expression not formatted correctly");
		throw new SyntaxError();
	}
	
	// Statement ::= { Statement*} | Type id = Expression; | Reference = Expression;
	// 				| Reference [ Expression ] = Expression ; | Reference ( ArgumentList? ) ;
	//				| return Expression? ; | if ( Expression ) Statement (else Statement)?
	//				| while (Expression) Statement
	private Statement parseStatement() throws SyntaxError {
		SourcePosition start = _currentToken.getTokenPosition();
		
		//Try Statement ::= {Statement*}
		if (_currentToken.getTokenType() == TokenType.LCURLY) {
			accept(TokenType.LCURLY);
			StatementList list = new StatementList();
			
			while (_currentToken.getTokenType() != TokenType.RCURLY) {
				list.add(parseStatement());
			}
			accept(TokenType.RCURLY);
			
			Statement block_statement = new BlockStmt(list, start);
			return block_statement;
		}
		
		//Try Statement ::= return Expression? ;
		if (_currentToken.getTokenType() == TokenType.RETURN) {
			accept(TokenType.RETURN);
			Expression inner_expression = null; //no expression by default
			
			if (_currentToken.getTokenType() != TokenType.SEMICOLON) {
				inner_expression = parseExpression();
			}
			accept(TokenType.SEMICOLON);
			
			Statement return_statement = new ReturnStmt(inner_expression, start);
			return return_statement;
		}
		
		//Try Statement ::= if ( Expression ) Statement (else Statement)?
		if (_currentToken.getTokenType() == TokenType.IF) {
			accept(TokenType.IF);
			accept(TokenType.LPAREN);
			Expression expression = parseExpression();
			accept(TokenType.RPAREN);
			Statement statement = parseStatement();
			
			Statement else_statement = null; //no else statement by default
			if (_currentToken.getTokenType() == TokenType.ELSE) {
				accept(TokenType.ELSE);
				else_statement = parseStatement();
			}
			
			Statement if_statement = new IfStmt(expression, statement, else_statement, start);
			return if_statement;
		}
		
		//Try Statement ::= while ( Expression ) Statement
		if (_currentToken.getTokenType() == TokenType.WHILE) {
			accept(TokenType.WHILE);
			accept(TokenType.LPAREN);
			Expression expression = parseExpression();
			accept(TokenType.RPAREN);
			Statement statement = parseStatement();
			Statement while_statement = new WhileStmt(expression, statement, start);
			return while_statement;
		}
		
		//Statement ::= Type id = Expression; | Reference = Expression; | Reference [Expression] = Expression; 
		//				| Reference (ArgumentList?);
		//can be converted into 
		//Statement ::= boolean id = Expression; | int id = Expression; | id id = Expression; | int[] id = Expression; | id[] id = Expression;
		//				| id (.id)* = Expression; | this (.id)* = Expression;
		//				| id [Expression] = Expression; | id (.id)+ [Expression] = Expression | this (id.)* [Expression] = Expression;
		//				| id (.id)* (ArgumentList?); | this (id.)* (ArgumentList?);
		if (_currentToken.getTokenType() == TokenType.BOOLEAN) {
			//Statement ::= boolean id = Expression;
			BaseType type = new BaseType(TypeKind.BOOLEAN, _currentToken.getTokenPosition());
			accept(TokenType.BOOLEAN);
			String id = _currentToken.getTokenText();
			accept(TokenType.IDENTIFIER);
			accept(TokenType.EQUALS);
			Expression expression = parseExpression();
			accept(TokenType.SEMICOLON);
			
			//package into a VarDeclStmt
			VarDecl var_decl = new VarDecl(type, id, start);
			Statement var_decl_statement = new VarDeclStmt(var_decl, expression, start);
			return var_decl_statement;
		}
		else if (_currentToken.getTokenType() == TokenType.INT) {
			//Statement ::= int id = Expression; | int[] id = Expression;
			accept(TokenType.INT);
			if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
				//Statement ::= int id = Expression;
				BaseType type = new BaseType(TypeKind.INT, start);
				String id = _currentToken.getTokenText();
				accept(TokenType.IDENTIFIER);
				accept(TokenType.EQUALS);
				Expression expression = parseExpression();
				accept(TokenType.SEMICOLON);
				
				//package into a VarDeclStmt
				VarDecl var_decl = new VarDecl(type, id, start);
				Statement var_decl_statement = new VarDeclStmt(var_decl, expression, start);
				return var_decl_statement;
			}
			else {
				//Statement ::= int[] id = Expression;
				BaseType base_type = new BaseType(TypeKind.INT, start);
				ArrayType type = new ArrayType(base_type, start);
				accept(TokenType.LSQUARE);
				accept(TokenType.RSQUARE);
				String id = _currentToken.getTokenText();
				accept(TokenType.IDENTIFIER);
				accept(TokenType.EQUALS);
				Expression expression = parseExpression();
				accept(TokenType.SEMICOLON);
				
				//package into a VarDeclStmt
				VarDecl var_decl = new VarDecl(type, id, start);
				Statement var_decl_statement = new VarDeclStmt(var_decl, expression, start);
				return var_decl_statement;
			}
		}
		else if (_currentToken.getTokenType() == TokenType.THIS) {
			//Statement ::= this (.id)* = Expression; | this (.id)* [ Expression ] = Expression; | this (.id)* ( ArgumentList? ) ;
			Reference reference = parseReference();
			
			if (_currentToken.getTokenType() == TokenType.EQUALS) {
				//Statement ::= this (.id)* = Expression;
				accept(TokenType.EQUALS);
				Expression expression = parseExpression();
				accept(TokenType.SEMICOLON);
				
				Statement assign_statement = new AssignStmt(reference, expression, start);
				return assign_statement;
			}
			else if (_currentToken.getTokenType() == TokenType.LSQUARE) {
				//Statement ::= this (.id)* [ Expression ] = Expression;
				accept(TokenType.LSQUARE);
				Expression index_expression = parseExpression();
				accept(TokenType.RSQUARE);
				accept(TokenType.EQUALS);
				Expression equals_expression = parseExpression();
				accept(TokenType.SEMICOLON);
				
				Statement indexed_assign_statement = new IxAssignStmt(reference, index_expression, equals_expression, start);
				return indexed_assign_statement;
			}
			else {
				//Statement ::= this (.id)* ( ArgumentList? ) ;

				accept(TokenType.LPAREN);
				
				ExprList argument_list = new ExprList(); //empty list by default
				if (_currentToken.getTokenType() != TokenType.RPAREN) {
					argument_list = parseArgumentList();
				}
				accept(TokenType.RPAREN);
				accept(TokenType.SEMICOLON);
				
				Statement call_statement = new CallStmt(reference, argument_list, start);
				return call_statement;
			}
		}
		else {
			//Statement ::= id[] id = Expression; | id (.id)* = Expression; | id [ Expression ] = Expression; 
			//				| id (.id)* [ Expression ] = Expression; | id (.id)* ( ArgumentList? ) ; | id id = Expression; 
			Identifier id = new Identifier(_currentToken);
			ClassType class_type = new ClassType(id, start);
			accept(TokenType.IDENTIFIER);
			
			if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
				//Statement ::= id id = Expression;
				String name = _currentToken.getTokenText();
				accept(TokenType.IDENTIFIER);
				accept(TokenType.EQUALS);
				Expression expression = parseExpression();
				accept(TokenType.SEMICOLON);
				
				//package into a VarDeclStmt
				VarDecl var_decl = new VarDecl(class_type, name, start);
				Statement var_decl_statement = new VarDeclStmt(var_decl, expression, start);
				return var_decl_statement;
			}
			else if (_currentToken.getTokenType() == TokenType.LSQUARE) {
				//Statement ::= id[] id = Expression; | id [Expression] = Expression;
				accept(TokenType.LSQUARE);
				ArrayType type = new ArrayType(class_type, start);
				
				if (_currentToken.getTokenType() == TokenType.RSQUARE) {					
					//Statement ::= id[] id = Expression;
					accept(TokenType.RSQUARE);
					String name = _currentToken.getTokenText();
					accept(TokenType.IDENTIFIER);
					accept(TokenType.EQUALS);
					Expression expression = parseExpression();
					accept(TokenType.SEMICOLON);
					
					//package into a VarDeclStmt
					VarDecl var_decl = new VarDecl(type, name, start);
					Statement var_decl_statement = new VarDeclStmt(var_decl, expression, start);
					return var_decl_statement;
				}
				else {
					//Statement ::= id [Expression] = Expression;
					//turns out id was a reference
					Reference reference = new IdRef(id, start);
					
					//parse the rest
					Expression index_expression = parseExpression();
					accept(TokenType.RSQUARE);
					accept(TokenType.EQUALS);
					Expression equals_expression = parseExpression();
					accept(TokenType.SEMICOLON);
					
					//package into an indexed assignment statement
					Statement indexed_assign_statement = new IxAssignStmt(reference, index_expression, equals_expression, start);
					return indexed_assign_statement;
				}
			}
			else {
				//Statement ::= id (.id)* = Expression; | id (.id)* [ Expression ] = Expression; | id (.id)* ( ArgumentList? ) ;
				
				//turns out the id was a reference, so parse it
				Reference reference = new IdRef(id, start);
				while (_currentToken.getTokenType() == TokenType.DOT) {
					accept(TokenType.DOT);
					Identifier iden = new Identifier(_currentToken);
					accept(TokenType.IDENTIFIER);
					reference = new QualRef(reference, iden, reference.posn);
				}
				
				if (_currentToken.getTokenType() == TokenType.EQUALS) {
					//Statement ::= id (.id)* = Expression;
					accept(TokenType.EQUALS);
					Expression expression = parseExpression();
					accept(TokenType.SEMICOLON);
					
					Statement assign_statement = new AssignStmt(reference, expression, start);
					return assign_statement;
				}
				else if (_currentToken.getTokenType() == TokenType.LSQUARE) {
					//Statement ::= id (.id)* [ Expression ] = Expression;
					accept(TokenType.LSQUARE);
					Expression index_expression = parseExpression();
					accept(TokenType.RSQUARE);
					accept(TokenType.EQUALS);
					Expression equals_expression = parseExpression();
					accept(TokenType.SEMICOLON);
					
					Statement indexed_assign_statement = new IxAssignStmt(reference, index_expression, equals_expression, start);
					return indexed_assign_statement;
				}
				else {
					//Statement ::= id (.id)* ( ArgumentList? ) ;

					accept(TokenType.LPAREN);
					
					ExprList argument_list = new ExprList(); //by default, list is empty
					if (_currentToken.getTokenType() != TokenType.RPAREN) {
						argument_list = parseArgumentList();
					}
					
					accept(TokenType.RPAREN);
					accept(TokenType.SEMICOLON);
					
					Statement call_statement = new CallStmt(reference, argument_list, start);
					return call_statement;
				}
			}
		}
	}
	
	// This method will accept the token and retrieve the next token.
		private void accept(TokenType expectedType) throws SyntaxError {
			if( _currentToken.getTokenType() == expectedType ) {
				_currentToken = _scanner.scan();
				return;
			}
			
			//if the expected token doesn't match the current token, report an error and throw a syntax error
			_errors.reportError(_currentToken.getTokenPosition().toString() + ": Expected token " + expectedType.name() + ", but got " + _currentToken.getTokenType().name());
			throw new SyntaxError();
		}
}
