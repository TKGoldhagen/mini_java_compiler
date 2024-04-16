package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import java.util.Iterator;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class Identification implements Visitor<String, Declaration> {
	//return type is used to visit references, it represents the declaration where the passed reference was declared
	//argument type represents context (class name) to be passed down
	
	//TODO: you can only access static member variables, other static member methods, and locals in a static method
	
	private boolean is_static; //set whenever entering a method
							   //used to check that only static member variables/methods and locals are referenced
				
	private ScopedIdentification scope_stack;
	private ErrorReporter reporter;
	
	public Identification(ErrorReporter in_reporter) {
		scope_stack = new ScopedIdentification();
		this.reporter = in_reporter;
	}
	
	public void identify_tree(AST tree) {
		try {
			tree.visit(this, null);
		}
		catch (IdentificationError e) {
			reporter.reportError(e.getMessage());
		}
	}
	
	//-----------------------------------PACKAGE--------------------------------------------------
    public Declaration visitPackage(miniJava.AbstractSyntaxTrees.Package prog, String arg){
    	//add some predefined custom classes
    	scope_stack.openScope();
    	    	
    	//class String { }
    	SourcePosition zero_posn = new SourcePosition(0, 0);
    	FieldDeclList str_fields = new FieldDeclList();
    	MethodDeclList str_methods = new MethodDeclList();
    	
    	Declaration str_decl = new ClassDecl("String", str_fields, str_methods, zero_posn);
    	scope_stack.addDeclaration("String", str_decl, "");
    	
    	//class _PrintStream { public void println( int n ){} }
    	FieldDeclList stream_fields = new FieldDeclList();
    	MethodDeclList stream_methods = new MethodDeclList();
    	
    	ParameterDeclList print_params = new ParameterDeclList();
    	TypeDenoter int_type = new BaseType(TypeKind.INT, zero_posn);
    	ParameterDecl print_param = new ParameterDecl(int_type, "n", zero_posn);
    	print_params.add(print_param);
    	
    	StatementList print_statements = new StatementList();
    	TypeDenoter void_type = new BaseType(TypeKind.VOID, zero_posn);
    	MemberDecl temp = new FieldDecl(false, false, void_type, "println", zero_posn);
    	MethodDecl print_decl = new MethodDecl(temp, print_params, print_statements, zero_posn);
    	stream_methods.add(print_decl);
    	
    	Declaration stream_decl = new ClassDecl("_PrintStream", stream_fields, stream_methods, zero_posn);
    	scope_stack.addDeclaration("_PrintStream", stream_decl, "");
    	
    	//class System { public static _PrintStream out; }
    	FieldDeclList sys_fields = new FieldDeclList();
    	MethodDeclList sys_methods = new MethodDeclList();
    	
    	Token out_id_token = new Token(TokenType.IDENTIFIER, "_PrintStream", zero_posn);
    	Identifier out_id = new Identifier(out_id_token);
    	TypeDenoter out_type = new ClassType(out_id, zero_posn);
    	FieldDecl out_decl = new FieldDecl(false, true, out_type, "out", zero_posn);
    	sys_fields.add(out_decl);
    	
    	Declaration sys_decl = new ClassDecl("System", sys_fields, sys_methods, zero_posn);
    	scope_stack.addDeclaration("System", sys_decl, "");  	
    	
    	//iterate through class declarations and add them to scope level 0
    	Iterator<ClassDecl> iterator = prog.classDeclList.iterator();
    	ClassDecl current_decl;
    	
    	while (iterator.hasNext()) {
    		current_decl = iterator.next();
    		scope_stack.addDeclaration(current_decl.name, current_decl, ""); 
    	}
    	
    	//add predefined methods to the custom classes:
    	scope_stack.openScope();
    	scope_stack.addDeclaration("out", out_decl, "System");
    	scope_stack.addDeclaration("println", print_decl, "_PrintStream");

    	//now add each class' field/method declarations to scope level 1
    	//we have to iterate through these before starting to recurse through the tree to implement
    	//out-of-order identification
    	iterator = prog.classDeclList.iterator();
    	while (iterator.hasNext()) {
			current_decl = iterator.next();
			
			Iterator<FieldDecl> field_iterator = current_decl.fieldDeclList.iterator();
	    	FieldDecl current_field_decl;
	    	while (field_iterator.hasNext()) {
	    		current_field_decl = field_iterator.next();
    			scope_stack.addDeclaration(current_field_decl.name, current_field_decl, current_decl.name);
    			
    			//remember where the field was declared for private check later
    			current_field_decl.parent_name = current_decl.name;
	    	}
	    	
	    	Iterator<MethodDecl> method_iterator = current_decl.methodDeclList.iterator();
	    	MethodDecl current_method_decl;
	    	while (method_iterator.hasNext()) {
	    		current_method_decl = method_iterator.next();
    			scope_stack.addDeclaration(current_method_decl.name, current_method_decl, current_decl.name);
    			
    			//remember where the method was declared for private check later
    			current_method_decl.parent_name = current_decl.name;
	    	}
    	}
    	
    	//now that level 0 and level 1 declarations are in the stack, continue recursing through the tree
    	iterator = prog.classDeclList.iterator();
    	while (iterator.hasNext()) {
    		current_decl = iterator.next();
    		current_decl.visit(this, current_decl.name);
    	}
    	
    	return null;
    }
    
    //----------------------------------DECLARATIONS-------------------------------------------
    public Declaration visitClassDecl(ClassDecl cd, String arg) {   
    	//visit all field declarations
    	Iterator<FieldDecl> field_iterator = cd.fieldDeclList.iterator();
    	FieldDecl current_field_decl;
    	while (field_iterator.hasNext()) {
    		current_field_decl = field_iterator.next();
    		current_field_decl.visit(this, arg);
    	}
    	
    	//and all method declarations
    	Iterator<MethodDecl> method_iterator = cd.methodDeclList.iterator();
    	MethodDecl current_method_decl;
    	while (method_iterator.hasNext()) {
    		current_method_decl = method_iterator.next();
    		is_static = current_method_decl.isStatic;
    		current_method_decl.visit(this, arg);
    	}
    	
    	return null;
    }
    
    public Declaration visitFieldDecl(FieldDecl fd, String arg) {
    	fd.type.visit(this, arg);
    	return null;
    }
    
    public Declaration visitMethodDecl(MethodDecl md, String arg) {
    	md.type.visit(this, arg);
    	
    	//open a new scope, which will include parameters 
    	scope_stack.openScope();
    	
    	//visit the parameter list
    	Iterator<ParameterDecl> parameter_iterator = md.parameterDeclList.iterator();
    	ParameterDecl current_parameter;
    	while (parameter_iterator.hasNext()) {
    		current_parameter = parameter_iterator.next();
    		current_parameter.visit(this, arg);
    	}
    	
    	//visit the statement list
    	Iterator<Statement> statement_iterator = md.statementList.iterator();
    	Statement current_statement;
    	while (statement_iterator.hasNext()) {
    		current_statement = statement_iterator.next();
    		current_statement.visit(this, arg);
    	}
    	
    	scope_stack.closeScope();
    	return null;
    }
    
    public Declaration visitParameterDecl(ParameterDecl pd, String arg) {
    	pd.type.visit(this, arg);
    	scope_stack.addDeclaration(pd.name, pd, "");
    	return null;
    }
    
    public Declaration visitVarDecl(VarDecl decl, String arg) {
    	decl.type.visit(this, arg);
		scope_stack.addDeclaration(decl.name, decl, "");
		return null;
    }

    //--------------------------------------TYPES-----------------------------------------------
    public Declaration visitBaseType(BaseType type, String arg) {    	
    	return null;
    }
    
    public Declaration visitClassType(ClassType type, String arg) { 
    	//make sure that the class name exists in level 0
    	scope_stack.check_level_zero(type.className.spelling);
    	
    	//TODO: visit Identifier to add it to stack?
    	//type.className.visit(this, arg);
    	return null;
    }
    
    public Declaration visitArrayType(ArrayType type, String arg) {  
    	type.eltType.visit(this, arg);
    	return null;
    }
    
    //---------------------------------------STATEMENTS------------------------------------------
    public Declaration visitBlockStmt(BlockStmt stmt, String arg) {
    	//open a new scope, visit each statement, and close the scope
    	scope_stack.openScope();
    	
    	Iterator<Statement> statement_iterator = stmt.sl.iterator();
    	Statement current_statement;
    	
    	while(statement_iterator.hasNext()) {
    		current_statement = statement_iterator.next();
    		current_statement.visit(this, arg);
    	}
    	
    	scope_stack.closeScope();
    	return null;
    }
    
    public Declaration visitVardeclStmt(VarDeclStmt stmt, String arg) {
    	//visit both the variable declaration and the right hand statement
    	stmt.varDecl.visit(this, arg);
    	Declaration init_decl = stmt.initExp.visit(this, arg);	
    	
    	//if trying to assign a reference to a variable, it has to be either a field or a variable
    	if (stmt.initExp instanceof RefExpr && !(((RefExpr) stmt.initExp).ref instanceof ThisRef)) {
			if (!(init_decl instanceof FieldDecl || init_decl instanceof LocalDecl) && init_decl != null) {
	    		throw new IdentificationError(stmt.posn.toString() + ": reference does not denote field or a variable");
	    	}
    	}
    	
    	return null;
    }
    
    public Declaration visitAssignStmt(AssignStmt stmt, String arg) {
    	//visit both the reference and the expression
    	stmt.ref.visit(this, arg);
    	Declaration val_decl = stmt.val.visit(this, arg);
    	
    	
    	//if trying to assign a reference to a variable, it has to be either a field or a variable
    	if (stmt.val instanceof RefExpr && !(((RefExpr) stmt.val).ref instanceof ThisRef)) {
    		if (!(val_decl instanceof FieldDecl || val_decl instanceof LocalDecl)  && val_decl != null) {
    			throw new IdentificationError(stmt.posn.toString() + ": reference does not denote field or a variable");
    		}
    	}
    	
    	return null;
    }
    
    public Declaration visitIxAssignStmt(IxAssignStmt stmt, String arg) {
    	//visit the reference, inner expression, and right hand side expression
    	stmt.ref.visit(this, arg);
    	stmt.ix.visit(this, arg);
    	Declaration exp_decl = stmt.exp.visit(this, arg);
    	
    	//if trying to assign a reference to a variable, it has to be either a field or a variable
    	if (stmt.exp instanceof RefExpr && !(((RefExpr) stmt.exp).ref instanceof ThisRef)) {
    		if (!(exp_decl instanceof FieldDecl || exp_decl instanceof LocalDecl) && exp_decl != null) {
    			throw new IdentificationError(stmt.posn.toString() + ": reference does not denote field or a variable");
    		}
    	}
    	
    	return null;
    }
    
    public Declaration visitCallStmt(CallStmt stmt, String arg) {    	
    	//visit the reference
    	stmt.methodRef.visit(this, arg);
    	
    	//and each expression in the argument list
    	Iterator<Expression> expression_iterator = stmt.argList.iterator();
    	Expression current_expression;
    	while (expression_iterator.hasNext()) {
    		current_expression = expression_iterator.next();
    		current_expression.visit(this, arg);
    	}
    	
    	return null;
    }
    
    public Declaration visitReturnStmt(ReturnStmt stmt, String arg) {
    	//if one exists, visit expression
    	if (stmt.returnExpr != null) {
    		stmt.returnExpr.visit(this, arg);
    	}
    	
    	return null;
    }
    
    public Declaration visitIfStmt(IfStmt stmt, String arg) {
    	//visit condition expression and then statement
    	stmt.cond.visit(this, arg);
    	stmt.thenStmt.visit(this, arg);
    			
    	//optionally, visit the else statement
    	if (stmt.elseStmt != null) {
    		stmt.elseStmt.visit(this, arg);
    	}
    	
    	return null;
    }
    
    public Declaration visitWhileStmt(WhileStmt stmt, String arg) {
    	//visit both condition expression and body statement
    	stmt.cond.visit(this, arg);
    	stmt.body.visit(this, arg);
    	return null;
    }
    
    //-----------------------------------EXPRESSIONS---------------------------------------
    public Declaration visitUnaryExpr(UnaryExpr expr, String arg) {
    	expr.expr.visit(this, arg);
    	return null;
    }
    
    public Declaration visitBinaryExpr(BinaryExpr expr, String arg) {
    	expr.left.visit(this, arg);
    	expr.right.visit(this, arg);
    	return null;
    }
    
    public Declaration visitRefExpr(RefExpr expr, String arg) {
    	return expr.ref.visit(this, arg);
    }

    public Declaration visitIxExpr(IxExpr expr, String arg) {
    	//visit both reference and index expression
    	expr.ref.visit(this, arg);
    	expr.ixExpr.visit(this, arg);
    	return null;
    }
    
    public Declaration visitCallExpr(CallExpr expr, String arg) {
    	//visit function reference
    	expr.functionRef.visit(this, arg);
    	
    	//and also each argument
    	Iterator<Expression> expression_iterator = expr.argList.iterator();
    	Expression current_expression;
    	while (expression_iterator.hasNext()) {
    		current_expression = expression_iterator.next();
    		current_expression.visit(this, arg);
    	}
    	
    	return null;
    }
    
    public Declaration visitLiteralExpr(LiteralExpr expr, String arg) {
    	//just a literal, no chance of seeing an ID here
    	return null;
    }
    
    public Declaration visitNewObjectExpr(NewObjectExpr expr, String arg) {
    	expr.classtype.visit(this, arg);
    	return null;
    }
    
    public Declaration visitNewArrayExpr(NewArrayExpr expr, String arg) {
    	expr.eltType.visit(this, arg);
    	expr.sizeExpr.visit(this, arg);
    	return null;
    }
    
    //-------------------------------------REFERENCES----------------------------------------
	public Declaration visitThisRef(ThisRef ref, String arg) {
		//"this" in a static member is an error
		if (is_static) {
			throw new IdentificationError(ref.posn.toString() + ": Cannot use \"this\" in a static method.");
		}
		
		ref.class_decl = (ClassDecl) scope_stack.check_level_zero(arg);
		return ref.class_decl;
	}
	
    public Declaration visitIdRef(IdRef ref, String arg) {    	
    	return ref.id.visit(this, arg);
    }
    
    public Declaration visitQRef(QualRef ref, String arg) {    	
    	Declaration context = ref.ref.visit(this, arg);
    	
    	//left hand side can not be a method reference
    	if (context instanceof MethodDecl) {
			throw new IdentificationError(ref.posn.toString() + ": Method reference not allowed in left side of a qualified reference");
		}
    	
    	//if context is a class type, then right hand side should be resolved in terms of that class
    	if (context.type instanceof ClassType) {
    		ref.id.decl = scope_stack.find_member_decl(ref.id.spelling, ((ClassType) context.type).className.spelling);
    	}
    	//otherwise, resolve it in terms of the original context
    	else{
    		ref.id.decl = scope_stack.find_member_decl(ref.id.spelling, context.name);
    	}
    	
    	//if context is a classname (except for "this"), then the right hand side must be static
		if (context instanceof ClassDecl && !(ref.ref instanceof ThisRef)) {
			MemberDecl member_decl = (MemberDecl) ref.id.decl;
			if (!member_decl.isStatic) {
				throw new IdentificationError(ref.posn.toString() + ": Reference " + ref.id.spelling + " must be static.");
			}
		}
    	
    	//check if declaration is private in this context
		if (ref.id.decl instanceof MemberDecl) {
			MemberDecl member_decl = (MemberDecl) ref.id.decl;
			if (member_decl.isPrivate && !member_decl.parent_name.equals(arg)) {
	    		throw new IdentificationError(ref.posn.toString() + ": Reference " + ref.id.spelling + " is private in this context");
			}
		}
		
		//if we're in a static method, declaration should also be static or local
    	
    	return ref.id.decl;
    }
    
    public Declaration visitNullReference(NullReference ref, String arg) {
    	return null;
    }

  //-----------------------------------TERMINALS-------------------------------------------
    public Declaration visitIdentifier(Identifier id, String arg) {
    	//set the declaration field in the identifier for ease of lookup during Type Checking
    	if (id.decl == null) {
        	id.decl = scope_stack.findDeclaration(id.spelling);
    	}
    	
    	//return the identifier's declaration
    	return id.decl;
    }
    
    public Declaration visitOperator(Operator op, String arg) { return null; }
    
    public Declaration visitIntLiteral(IntLiteral num, String arg) { return null; }
    
    public Declaration visitBooleanLiteral(BooleanLiteral bool, String arg) { return null; }
    
    // --------------------------------HELPER FUNCTIONS-------------------------------------
    public void print_flag(String in) {
    	System.out.println("FLAG: " + in);
    }
}
