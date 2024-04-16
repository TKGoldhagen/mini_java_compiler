package miniJava.ContextualAnalysis;

import java.util.Iterator;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;

public class OtherContextualErrors implements Visitor<String, Object> {
	//1. You cannot have a declaration in its own scope. 
	//2. You cannot reference a variable currently being declared.
	
	//ArgType is used for rule 2. A declaration will pass down its name, and any node below it that references
	//that name should throw an error.
	
	private ErrorReporter reporter;

	public OtherContextualErrors(ErrorReporter in_reporter) {
		this.reporter = in_reporter;
	}
	
	public void check_other_errors(AST tree) {
		try {
			tree.visit(this, null);
		}
		catch (OtherError e) {
			reporter.reportError(e.getMessage());
		}
	}
	
	// --------------------------------------PACKAGE---------------------------------------------------
    public Object visitPackage(miniJava.AbstractSyntaxTrees.Package prog, String arg) {
    	Iterator<ClassDecl> class_iterator = prog.classDeclList.iterator();
    	ClassDecl current_class;
    	while (class_iterator.hasNext()) {
    		current_class = class_iterator.next();
    		current_class.visit(this, null);
    	}
    	
    	return null;
    }

  // -------------------------------------DECLARATIONS--------------------------------------------------
    public Object visitClassDecl(ClassDecl cd, String arg) {
    	Iterator<MethodDecl> method_iterator = cd.methodDeclList.iterator();
    	MethodDecl current_method_decl;
    	while (method_iterator.hasNext()) {
    		current_method_decl = method_iterator.next();
    		current_method_decl.visit(this, null);
    	}
    	
    	return null;
    }
        
    public Object visitMethodDecl(MethodDecl md, String arg) {    	
    	Iterator<Statement> statement_iterator = md.statementList.iterator();
    	Statement current_statement;
    	while(statement_iterator.hasNext()) {
    		current_statement = statement_iterator.next();
    		current_statement.visit(this, null);
    	}
    	
    	return null;
    }
    
    public Object visitFieldDecl(FieldDecl fd, String arg) { return null; }
    public Object visitParameterDecl(ParameterDecl pd, String arg) { return null; }
    public Object visitVarDecl(VarDecl decl, String arg) { return null; }
 
  // ------------------------------------TYPES-----------------------------------------------------
    public Object visitClassType(ClassType type, String arg) { 
    	if (type.className.spelling == arg) {
    		throw new OtherError(type.posn.toString() + ": You cannot reference a variable currently being declared.");
    	}
    	
    	return null;
    }
    
    public Object visitBaseType(BaseType type, String arg) { return null; }
    public Object visitArrayType(ArrayType type, String arg) { return null; }
    
  // --------------------------------------STATEMENTS----------------------------------------------
    public Object visitBlockStmt(BlockStmt stmt, String arg) {
    	Iterator<Statement> stmt_iterator = stmt.sl.iterator();
    	Statement current_stmt;
    	while (stmt_iterator.hasNext()) {
    		current_stmt = stmt_iterator.next();
    		current_stmt.visit(this, arg);
    	}
    	
    	return null;
    }
    
    public Object visitVardeclStmt(VarDeclStmt stmt, String arg) {
    	stmt.initExp.visit(this, stmt.varDecl.name);
    	return null;
    }
    
    public Object visitIfStmt(IfStmt stmt, String arg) {
    	//check rule 1
    	if (stmt.thenStmt instanceof VarDeclStmt) {
    		reporter.reportError(stmt.posn.toString() + ": You cannot have a declaration in its own scope. java moment ¯\\_(ツ)_/¯");
    	}
    	
    	stmt.thenStmt.visit(this, arg);
    	if (stmt.elseStmt != null) {
    		//check rule 1
        	if (stmt.elseStmt instanceof VarDeclStmt) {
        		reporter.reportError(stmt.posn.toString() + ": You cannot have a declaration in its own scope. java moment ¯\\_(ツ)_/¯");
        	}
    		stmt.elseStmt.visit(this, null);
    	}
    	return null;
    }
    
    public Object visitWhileStmt(WhileStmt stmt, String arg) {
    	//check rule 1
    	if (stmt.body instanceof VarDeclStmt) {
    		reporter.reportError(stmt.posn.toString() + ": You cannot have a declaration in its own scope. java moment ¯\\_(ツ)_/¯");
    	}
    	
    	stmt.body.visit(this, arg);
    	return null;
    }
    
    public Object visitAssignStmt(AssignStmt stmt, String arg) { return null; }
    public Object visitIxAssignStmt(IxAssignStmt stmt, String arg) { return null; }
    public Object visitCallStmt(CallStmt stmt, String arg) { return null; }
    public Object visitReturnStmt(ReturnStmt stmt, String arg) { return null; }
    
  // ---------------------------------EXPRESSIONS-------------------------------------------
    public Object visitUnaryExpr(UnaryExpr expr, String arg) {
    	expr.expr.visit(this, arg);
    	return null;
    }
    
    public Object visitBinaryExpr(BinaryExpr expr, String arg) {
    	expr.left.visit(this, arg);
    	expr.right.visit(this, arg);
    	return null;
    }
    
    public Object visitRefExpr(RefExpr expr, String arg) {
    	expr.ref.visit(this, arg);
    	return null;
    }
    
    public Object visitIxExpr(IxExpr expr, String arg) {
    	expr.ref.visit(this, arg);
    	expr.ixExpr.visit(this, arg);
    	return null;
    }
    
    public Object visitCallExpr(CallExpr expr, String arg) {
    	expr.functionRef.visit(this, arg);
    	
    	Iterator<Expression> expr_iterator = expr.argList.iterator();
    	Expression current_expr;
    	while(expr_iterator.hasNext()) {
    		current_expr = expr_iterator.next();
    		current_expr.visit(this, arg);
    	}
    	
    	return null;
    }
    
    
    public Object visitNewObjectExpr(NewObjectExpr expr, String arg) {
    	expr.classtype.visit(this, arg);
    	return null;
    }
    
    public Object visitNewArrayExpr(NewArrayExpr expr, String arg) {
    	expr.eltType.visit(this, arg);
    	expr.sizeExpr.visit(this, arg);
    	return null;
    }
    
    public Object visitLiteralExpr(LiteralExpr expr, String arg) { return null; }
    
  // References    
    public Object visitIdRef(IdRef ref, String arg) {
    	ref.id.visit(this, arg);
    	return null;
    }
    
    public Object visitQRef(QualRef ref, String arg) {
    	ref.ref.visit(this, arg);
    	return null;
    }
    
    public Object visitNullReference(NullReference ref, String arg) { return null; }  
    public Object visitThisRef(ThisRef ref, String arg) { return null; }

  // Terminals
    public Object visitIdentifier(Identifier id, String arg) {
    	if (id.spelling.equals(arg)) {
    		throw new OtherError(id.posn.toString() + ": You cannot reference a variable currently being declared.");
    	}
    	return null;
    }
    
    public Object visitOperator(Operator op, String arg) { return null; }
    public Object visitIntLiteral(IntLiteral num, String arg) { return null; }
    public Object visitBooleanLiteral(BooleanLiteral bool, String arg) { return null; }
    
    // --------------------------------HELPER FUNCTIONS-------------------------------------
    public void print_flag(String in) {
    	System.out.println("FLAG: " + in);
    }
}
