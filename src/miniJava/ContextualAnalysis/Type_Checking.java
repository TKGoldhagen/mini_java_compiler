package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import java.util.Iterator;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class Type_Checking implements Visitor<Object, TypeDenoter> {
	private ErrorReporter reporter;

	public Type_Checking(ErrorReporter in_reporter) {
		this.reporter = in_reporter;
	}

	public TypeDenoter synthesize_types(TypeDenoter first, TypeDenoter second, Operator op, String error) {
		//if both are ArrayTypes, then recursively call this method until their base types can be evaluated
		if (first.typeKind == TypeKind.ARRAY && second.typeKind == TypeKind.ARRAY) {
			ArrayType first_array = (ArrayType) first;
			ArrayType second_array = (ArrayType) second;
			
			return synthesize_types(first_array.eltType, second_array.eltType, op, error);
		}
		
		//the rest of the method represents the "base case" for the recursive call
		
		//if either is TypeKind.ERROR, then an error has already been reported
		if (first.typeKind == TypeKind.ERROR || second.typeKind == TypeKind.ERROR) {
			TypeDenoter error_type = new BaseType(TypeKind.ERROR, first.posn);
			return error_type;
		}
		
		//if none of the types are TypeKind.ERROR, but either is TypeKind.UNSUPPORTED, then report an error
		if (first.typeKind == TypeKind.UNSUPPORTED || second.typeKind == TypeKind.UNSUPPORTED) {
			reporter.reportError(error);
			TypeDenoter error_type = new BaseType(TypeKind.ERROR, first.posn);
			return error_type;
		}
		
		//non-object x null with any operator is an error
		if (first.typeKind != TypeKind.CLASS && second.typeKind == TypeKind.NULL) {
			reporter.reportError(first.posn.toString() + ": Can't assign null to a non-object.");
			TypeDenoter error_type = new BaseType(TypeKind.ERROR, first.posn);
			return error_type;
		}
		
		//TODO: do you need this?
		//null x non-object with any operator is an error
		if (first.typeKind == TypeKind.NULL && second.typeKind != TypeKind.CLASS) {
			reporter.reportError(first.posn.toString() + ": Can't assign null to a non-object.");
			TypeDenoter error_type = new BaseType(TypeKind.ERROR, first.posn);
			return error_type;
		}
		
		//object x null with == or != results in type boolean
		if (first.typeKind == TypeKind.CLASS && second.typeKind == TypeKind.NULL) {
			if (op.spelling.equals("==") || op.spelling.equals("!=")) {
				TypeDenoter bool_type = new BaseType(TypeKind.BOOLEAN, first.posn);
				return bool_type;
			}
		}
		
		//boolean × boolean with operators && or || results in type boolean
		if (first.typeKind == TypeKind.BOOLEAN && second.typeKind == TypeKind.BOOLEAN) {
			if (op.spelling.equals("&&") || op.spelling.equals("||")) {
				TypeDenoter bool_type = new BaseType(TypeKind.BOOLEAN, first.posn);
				return bool_type;
			}
		}
		
		//int x int
		if (first.typeKind == TypeKind.INT && second.typeKind == TypeKind.INT) {
			//int × int with operators >, >=, <, or <= results in type boolean
			if (op.spelling.equals(">") || op.spelling.equals(">=") || op.spelling.equals("<") || op.spelling.equals("<=")) {
				TypeDenoter bool_type = new BaseType(TypeKind.BOOLEAN, first.posn);
				return bool_type;
			}
			
			//int × int with operators +, -, *, or / results in type int
			if (op.spelling.equals("+") || op.spelling.equals("-") || op.spelling.equals("*") || op.spelling.equals("/")) {
				TypeDenoter int_type = new BaseType(TypeKind.INT, first.posn);
				return int_type;
			}
		}
		
		//any type x same type with operators == or != results in type boolean
		if (first.typeKind == second.typeKind && (op.spelling.equals("==") || op.spelling.equals("!=") )) {
			//if the two types are class types, then make sure they are the same class
			if (first.typeKind == TypeKind.CLASS) {				
				ClassType first_class = (ClassType) first;
				ClassType second_class = (ClassType) second;
				
				if (first_class.className.spelling.equals(second_class.className.spelling)) {
					//TypeDenoter bool_type = new BaseType(TypeKind.BOOLEAN, first.posn);
					//return bool_type;
					return first;
				}
				else {
					reporter.reportError(error);
					TypeDenoter error_type = new BaseType(TypeKind.ERROR, first.posn);
					return error_type;
				}				
			}
			
			//otherwise, no error neccessary!
			TypeDenoter bool_type = new BaseType(TypeKind.BOOLEAN, first.posn);
			return bool_type;
		}
		
		//if none of the cases are satisfied, then report the error passed
		reporter.reportError(error);
		TypeDenoter error_type = new BaseType(TypeKind.ERROR, first.posn);
		return error_type;
	}
	
	//overload of synthesize_types() for unary expressions
	public TypeDenoter synthesize_types(TypeDenoter type, Operator op, String error) {
		//type int with unary operator - results in type int
		if (type.typeKind == TypeKind.INT && op.spelling == "-") {
			return type;
		}
		
		//type boolean with unary operator ! results in type boolean
		if (type.typeKind == TypeKind.BOOLEAN && op.spelling == "!") {
			return type;
		}
		
		//if none of the cases are satisfied, then report the error passed
		reporter.reportError(error);
		TypeDenoter error_type = new BaseType(TypeKind.ERROR, type.posn);
		return error_type;
	}
	
	//overload of synthesize_types() for checking equality
	public TypeDenoter synthesize_types(TypeDenoter first, TypeDenoter second, String error) {
		Token equals = new Token(TokenType.OPERATOR, "==", null);
		Operator op = new Operator(equals);
		return synthesize_types(first, second, op, error);
	}
	
	public void type_check_tree(AST tree) {
		tree.visit(this, null);
	}
	
	//-----------------------------------PACKAGE--------------------------------------------------
    public TypeDenoter visitPackage(Package prog, Object arg){
    	Iterator<ClassDecl> class_decl_iterator = prog.classDeclList.iterator();
    	ClassDecl current_decl;
    	
    	while (class_decl_iterator.hasNext()) {
    		current_decl = class_decl_iterator.next();
    		current_decl.visit(this, null);
    	}
    	
    	return null;
    }
    
    //----------------------------------DECLARATIONS-------------------------------------------
    public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
    	//visit all field declarations
    	/*
    	Iterator<FieldDecl> field_iterator = cd.fieldDeclList.iterator();
    	FieldDecl current_field_decl;
    	while (field_iterator.hasNext()) {
    		current_field_decl = field_iterator.next();
    		current_field_decl.visit(this, null);
    	}
    	*/
    	
    	//and all method declarations
    	Iterator<MethodDecl> method_iterator = cd.methodDeclList.iterator();
    	MethodDecl current_method_decl;
    	while (method_iterator.hasNext()) {
    		current_method_decl = method_iterator.next();
    		current_method_decl.visit(this, null);
    	}
    	
    	return null;
    }
    

    public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
    	return null;
    }
    
    public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
    	//type check each statement in the method
    	
    	Iterator<Statement> statement_iterator = md.statementList.iterator();
    	Statement current_statement;
    	
    	while(statement_iterator.hasNext()) {
    		current_statement = statement_iterator.next();
    		
    		//TODO: spaghetti :(
        	//if the statement is a return statement, it should match the return type of the method.
    		//also need to check IfStmt, WhileStmt, and BlockStmt, because a ReturnStmt could be nested inside those
    		if (current_statement instanceof ReturnStmt || current_statement instanceof IfStmt 
    				|| current_statement instanceof WhileStmt || current_statement instanceof BlockStmt) {
    			TypeDenoter return_type = current_statement.visit(this, null);
    			if (return_type != null) {
        			synthesize_types(return_type, md.type, return_type.posn.toString() + ": Return type should match method signature");
    			}
    		}
    		else {
    			current_statement.visit(this, null);
    		}
    	}
    	
    	return null;
    }
    
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
    	return pd.type;
    }
    
    public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
    	return decl.type;
    }
    
    //--------------------------------------TYPES-----------------------------------------------
    public TypeDenoter visitBaseType(BaseType type, Object arg) {
    	return type;
    }
    
    public TypeDenoter visitClassType(ClassType type, Object arg) {
    	return type;
    }
    
    public TypeDenoter visitArrayType(ArrayType type, Object arg) {
    	return type;
    }
    
    //---------------------------------------STATEMENTS------------------------------------------
    public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
    	//visit each statement
    	Iterator<Statement> iterator = stmt.sl.iterator();
    	Statement current_statement;
    	TypeDenoter ret_type = null;
    	TypeDenoter temp_type = null;
    	
    	while (iterator.hasNext()) {
    		current_statement = iterator.next();
    		temp_type = current_statement.visit(this, null);
    		
    		if (temp_type != null && ret_type != null) {
    			ret_type = synthesize_types(ret_type, temp_type, stmt.posn.toString() + ": This block statement has more than one return statement with different types.");
    		}
    		else if (temp_type != null && ret_type == null) {
    			ret_type = temp_type;
    		}
    	}
    	
    	return ret_type;
    }
    
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
    	TypeDenoter decl_type = stmt.varDecl.visit(this, null);
    	
    	/*
    	//trying to assign a null reference
    	if (stmt.initExp instanceof RefExpr) {
    		RefExpr ref_expr = (RefExpr) stmt.initExp;
    		if (ref_expr.ref instanceof NullReference) {
    			//if you assign null to an object, it's all good!
    			if (decl_type.typeKind == TypeKind.CLASS) {
	    			return null;
    			}
    			//if you assign null to a non-object, that should throw an error
    			else {
	    			reporter.reportError(stmt.posn.toString() + ": Can't assign null to a non-object.");
    				return null;
    			}
    		}
    	}*/
    	
    	//make sure that types on both sides match 
    	TypeDenoter expr_type = stmt.initExp.visit(this, null);
    	synthesize_types(decl_type, expr_type, stmt.posn.toString() + ": Expression of type " + expr_type.typeKind.name() + " doesn't match variable declaration of type " + decl_type.typeKind.name());
    	
    	return null;
    }
    
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
    	
    	TypeDenoter ref_type = stmt.ref.visit(this, null);
    	/*
    	//trying to assign a null reference
    	if (stmt.val instanceof RefExpr) {
    		RefExpr ref_expr = (RefExpr) stmt.val;
    		if (ref_expr.ref instanceof NullReference) {
    			//if you assign null to an object, it's all good!
    			if (ref_type.typeKind == TypeKind.CLASS) {
	    			return null;
    			}
    			//if you assign null to a non-object, that should throw an error
    			else {
	    			reporter.reportError(stmt.posn.toString() + ": Can't assign null to a non-object.");
    				return null;
    			}
    		}
    	}*/
    	
    	//make sure that types on both sides match
    	TypeDenoter expr_type = stmt.val.visit(this, null);
    	synthesize_types(ref_type, expr_type, stmt.posn.toString() + ": Expression type doesn't match variable declaration type.");
    	
    	return null;
    }
    
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
    	//first, check that the index expression is of type int    	
    	TypeDenoter index_type = stmt.ix.visit(this, null);
    	if (index_type.typeKind != TypeKind.INT) {
    		reporter.reportError(stmt.posn.toString() + ": Index expression is not of type int.");
    	}
    	
    	//make sure that the reference is an array
    	TypeDenoter ref_type = stmt.ref.visit(this, null);
    	if (ref_type.typeKind != TypeKind.ARRAY) {
    		reporter.reportError(stmt.posn.toString() + ": Reference is not of type array");
    		return null;
    	}
    	
    	TypeDenoter element_type = ((ArrayType) ref_type).eltType;

    	/*
    	//trying to assign a null reference
    	if (stmt.exp instanceof RefExpr) {
    		RefExpr ref_expr = (RefExpr) stmt.exp;
    		if (ref_expr.ref instanceof NullReference) {
    			//if you assign null to an object, it's all good!
    			if (ref_type.typeKind == TypeKind.CLASS) {
	    			return null;
    			}
    			//if you assign null to a non-object, that should throw an error
    			else {
	    			reporter.reportError(stmt.posn.toString() + ": Can't assign null to a non-object.");
    				return null;
    			}
    		}
    	}
		*/
    	
    	//otherwise, check that the elements in the array are of the same type as the value being assigned
    	TypeDenoter value_type = stmt.exp.visit(this, null);
    	synthesize_types(element_type, value_type, stmt.posn.toString() + ": Elements in this array have a different type than the value being assigned.");
    	
    	return null;
	}
    
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {    	
    	Identifier ref_id = null;
    	if (stmt.methodRef instanceof QualRef) {
    		//reference is a qualified reference
        	ref_id = ((QualRef) stmt.methodRef).id;
    	}
    	else if (stmt.methodRef instanceof IdRef){
    		//reference is an id reference
        	ref_id = ((IdRef) stmt.methodRef).id;
    	}
    	else {
    		//reference is a this ref
    		reporter.reportError(stmt.posn.toString() + ": \"this\" is not a method.");
    		TypeDenoter error_type = new BaseType(TypeKind.ERROR, stmt.posn);
    		return error_type;
    	}
    	
    	//make sure the declaration is a method
    	if (!(ref_id.decl instanceof MethodDecl)) {
    		reporter.reportError(stmt.posn.toString() + ": this is not a method.");
    	}
    	
    	Iterator<ParameterDecl> param_iterator = ((MethodDecl) ref_id.decl).parameterDeclList.iterator();
		Iterator<Expression> arg_iterator = stmt.argList.iterator();
		ParameterDecl current_param;
		Expression current_arg;
		
		//check that each parameter and argument match type
		while (param_iterator.hasNext() && arg_iterator.hasNext()) {
			current_param = param_iterator.next();
			current_arg = arg_iterator.next();
			TypeDenoter param_type = current_param.type;
			TypeDenoter arg_type = current_arg.visit(this, null);
			
			synthesize_types(param_type, arg_type, stmt.posn.toString() + ": Method is expecting type " + param_type.typeKind.name() + ", not " + arg_type.typeKind.name());
		}
		
		//finally check that the number of arguments and parameters match 
		if (param_iterator.hasNext() || arg_iterator.hasNext()) {
			reporter.reportError(stmt.posn.toString() + ": Number of arguments passed doesn't match method declaration.");
		}
		
		return null;
    }
    
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
    	//if the return statement has nothing in the parenthesis, then it should be returning from a void method
    	if (stmt.returnExpr == null) {
    		TypeDenoter void_type = new BaseType(TypeKind.VOID, stmt.posn);
    		return void_type;
    	}
    	
    	//otherwise, return the type of the expression
    	else {
    		return stmt.returnExpr.visit(this, null);
    	}
    }
    
    public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
    	//check that the expression in the conditional is a boolean
    	TypeDenoter bool_type = new BaseType(TypeKind.BOOLEAN, stmt.posn);
    	TypeDenoter cond_type = stmt.cond.visit(this, null);
    	
    	synthesize_types(cond_type, bool_type, stmt.posn.toString() + ": The conditional expression should be of type boolean.");
    	
    	//visit the nested statement, remember the type for returning
    	TypeDenoter then_type = stmt.thenStmt.visit(this, null);
    	
    	//optionally visit the else statement
    	TypeDenoter else_type = null;
    	if (stmt.elseStmt != null) {
    		else_type = stmt.elseStmt.visit(this, null);
    	}
    	
    	//a null type represents that the corresponding statement does not include a return statement
    	if (then_type == null && else_type == null) {
    		return null;
    	}
    	else if (then_type == null && else_type != null) {
    		return else_type;
    	}
    	else if (then_type != null && else_type == null) {
    		return then_type;
    	}
    	else {
    		return synthesize_types(then_type, else_type, stmt.posn.toString() + ": Multiple return types in this if statement.");
    	}
    }
    
    public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
    	//check that the expression in the conditional is a boolean
    	TypeDenoter bool_type = new BaseType(TypeKind.BOOLEAN, stmt.posn);
    	TypeDenoter cond_type = stmt.cond.visit(this, null);
    	synthesize_types(cond_type, bool_type, stmt.posn.toString() + ": The conditional expression should be of type boolean.");
    	
    	//visit the nested statement
    	TypeDenoter body_type = stmt.body.visit(this, null);
    	return body_type;
    }
    
    //----------------------------------EXPRESSIONS-----------------------------------
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
    	TypeDenoter expr_type = expr.expr.visit(this, null);
    	String error = expr.posn.toString() + ": Expression of type " + expr_type.typeKind.name() + " is incompatible with unary operator " + expr.operator.spelling;
    	return synthesize_types(expr_type, expr.operator, error);
    }
    
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
    	TypeDenoter first_type = expr.left.visit(this, null);
    	TypeDenoter second_type = expr.right.visit(this, null);
    	String error = expr.posn.toString() + ": Expressions of type " + first_type.typeKind.name() + " and " + second_type.typeKind.name() + " are incompatible with operator " + expr.operator.spelling;
    	return synthesize_types(first_type, second_type, expr.operator, error);
    }
    
    public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
    	return expr.ref.visit(this, null);
    }
    
    public TypeDenoter visitIxExpr(IxExpr expr, Object arg) { 
    	//check that index expression is an int
    	TypeDenoter int_type = new BaseType(TypeKind.INT, null);
    	TypeDenoter expr_type = expr.ixExpr.visit(this, null);
    	String error = expr.posn.toString() + ": Index expression must be of type int.";
    	synthesize_types(expr_type, int_type, error);
    	
    	//check that reference is an array
    	TypeDenoter ref_type = expr.ref.visit(this, null);
    	if (ref_type.typeKind != TypeKind.ARRAY) {
    		reporter.reportError(expr.posn.toString() + ": " + expr.ref.getClass().getName() + "is not an array.");
    		TypeDenoter err_type = new BaseType(TypeKind.ERROR, expr.posn);
    		return err_type;
    	}
    	
    	ArrayType array_type = (ArrayType) ref_type;
    	return array_type.eltType;
    }
    
    public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
    	Identifier ref_id = null;
    	if (expr.functionRef instanceof QualRef) {
    		ref_id = ((QualRef) expr.functionRef).id;
    	}
    	else if (expr.functionRef instanceof IdRef) {
    		ref_id = ((IdRef) expr.functionRef).id;
    	}
    	else {
    		//reference is a this ref
    		reporter.reportError(expr.posn.toString() + ": \"this\" is not a method.");
    		TypeDenoter error_type = new BaseType(TypeKind.ERROR, expr.posn);
    		return error_type;
    	}
    	
    	//make sure the declaration is a method
       	if (!(ref_id.decl instanceof MethodDecl)) {
    		reporter.reportError(expr.posn.toString() + ": " + ref_id.spelling + " is not a method.");
    		TypeDenoter err_type = new BaseType(TypeKind.ERROR, null);
    		return err_type;
    	}
    	
		//check that each parameter and argument match type
    	Iterator<ParameterDecl> param_iterator = ((MethodDecl) ref_id.decl).parameterDeclList.iterator();
		Iterator<Expression> arg_iterator = expr.argList.iterator();
		ParameterDecl current_param;
		Expression current_arg;
		
		//check that each parameter and argument match type
		while (param_iterator.hasNext() && arg_iterator.hasNext()) {
			current_param = param_iterator.next();
			current_arg = arg_iterator.next();
			TypeDenoter param_type = current_param.type;
			TypeDenoter arg_type = current_arg.visit(this, null);
			
			TypeDenoter ret = synthesize_types(param_type, arg_type, expr.posn.toString() + ": Method is expecting type " + param_type.typeKind.name() + ", not " + arg_type.typeKind.name());
			if (ret.typeKind == TypeKind.ERROR) {
				return ret;
			}
		}
		
		//finally check that the number of arguments and parameters match 
		if (param_iterator.hasNext() || arg_iterator.hasNext()) {
			reporter.reportError(expr.posn.toString() + ": Number of arguments passed doesn't match method declaration.");
    		TypeDenoter err_type = new BaseType(TypeKind.ERROR, null);
    		return err_type;
		}
		
		//if nothing threw an error, then return the return type of the method called
		return ref_id.decl.type;
    }

    public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
    	return expr.lit.visit(this, null);
    }
    
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg){ 
    	return expr.classtype;
    }
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg){ 
    	//check that the expression is of type int
    	TypeDenoter int_type = new BaseType(TypeKind.INT, null);
    	TypeDenoter expr_type = expr.sizeExpr.visit(this, null);
    	String error = expr.posn.toString() + ": Array size must be of type int.";
    	synthesize_types(expr_type, int_type, error);
    	
    	//return array type with element type nested
    	TypeDenoter array_type = new ArrayType(expr.eltType, expr.posn);
    	return array_type;
    }
    
  //-----------------------------------------REFERENCES----------------------------------------------------
    public TypeDenoter visitThisRef(ThisRef ref, Object arg){
    	return ref.class_decl.type;
    }
    
    public TypeDenoter visitIdRef(IdRef ref, Object arg){
    	return ref.id.visit(this, null);
    }
    
    public TypeDenoter visitQRef(QualRef ref, Object arg){ 
    	return ref.id.visit(this, null);
    }
    
    public TypeDenoter visitNullReference(NullReference ref, Object arg){ 
    	TypeDenoter null_type = new BaseType(TypeKind.NULL, ref.posn);
    	return null_type;
    }

  //--------------------------------------------TERMINALS---------------------------------------------------
    public TypeDenoter visitIdentifier(Identifier id, Object arg){
    	return id.decl.type;
    }
    
    public TypeDenoter visitOperator(Operator op, Object arg){ 
    	return null;
    }
    
    public TypeDenoter visitIntLiteral(IntLiteral num, Object arg){ 
    	TypeDenoter int_type = new BaseType(TypeKind.INT, num.posn);
    	return int_type;
    }
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg){ 
    	TypeDenoter bool_type = new BaseType(TypeKind.BOOLEAN, bool.posn);
    	return bool_type;
    }
    
    // --------------------------------HELPER FUNCTIONS-------------------------------------
    public void print_flag(String in) {
    	System.out.println("FLAG: " + in);
    }
}
