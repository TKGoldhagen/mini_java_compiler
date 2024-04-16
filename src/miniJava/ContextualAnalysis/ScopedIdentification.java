package miniJava.ContextualAnalysis;

import java.util.Stack;
import java.util.HashMap;
import java.util.Iterator;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import java.util.List;
import java.util.ArrayList;
//import java.util.EmptyStackException;

public class ScopedIdentification {
	//Pair should be [String, ""] for all levels other than 1
	private Stack<HashMap<String, Declaration>> id_table_stack;
	private HashMap<Pair, Declaration> level_1; //copy of level 1 of id_table_stack but with context 
	
	public ScopedIdentification() {
		id_table_stack = new Stack<HashMap<String, Declaration>>();
		level_1 = new HashMap<Pair, Declaration>();
	}
	
	public void openScope() {
		HashMap<String, Declaration> id_table = new HashMap<String, Declaration>();
		id_table_stack.push(id_table);
	}
	
	public void closeScope() {
		id_table_stack.pop();
	}
	
	public void addDeclaration(String id, Declaration decl, String context) throws IdentificationError{
		//Adds str to decl mapping to current scope
		//throws an error if name already exists at current level or elsewhere in level 2+

		//check the current level
		HashMap<String, Declaration> id_table = id_table_stack.get(id_table_stack.size() - 1);
		if (id_table.containsKey(id)) {
			throw new IdentificationError("Declaration of variable " + id + " already exists in current scope.");
		}
		
		//check all 2+ levels (from the top down)
		for (int i = id_table_stack.size() - 1; i >= 2; i--) {
			id_table = id_table_stack.get(i);
			if (id_table.containsKey(id)) {
				throw new IdentificationError("Declaration of variable " + id + " already exists in local scope.");
			}
		}
		
		//otherwise, add declaration to table
		id_table = id_table_stack.pop();
		id_table.put(id, decl);
		id_table_stack.push(id_table);
		
		//if level is 1, then also add it to level_1 var
		if (id_table_stack.size() == 2) {
			Pair temp = new Pair(id, context);
			level_1.put(temp, decl);
		}
	}
	
	public Declaration findDeclaration(String id) throws IdentificationError{
		//look for id in id table stack, starting at the end of the stack object (top of the stack) and 
		//working your way back
		
		HashMap<String, Declaration> id_table;
		
		for (int i = id_table_stack.size() - 1; i >= 0; i--) {
			id_table = id_table_stack.get(i);
			if (id_table.containsKey(id)) {
				return id_table.get(id);
			}
		}
		
		throw new IdentificationError("Variable " + id + " has not been declared.");
	}
	
	public Declaration findDeclaration(String id, String context) throws IdentificationError{
		//look for id in id table stack, starting at the end of the stack object (top of the stack) and 
		//working your way back
		//for all levels except 1, check without context
		
		//first check levels 2+, if any (from the top down)
		HashMap<String, Declaration> id_table;
		
		for (int i = id_table_stack.size() - 1; i >= 2; i--) {
			id_table = id_table_stack.get(i);
			if (id_table.containsKey(id)) {				
				return id_table.get(id);
			}
		}
		
		//now check level 1
		Pair temp = new Pair(id, context);
		if (level_1.containsKey(temp)) {
			return level_1.get(temp);
		}
		
		//and finally check level 0
		id_table = id_table_stack.get(0);
		if (id_table.containsKey(id)) {
			return id_table.get(id);
		}
		
		//if not found anywhere, throw an error
		throw new IdentificationError("Variable " + id + " in class " + context + " has not been declared.");
	}
	
	public Declaration find_member_decl(String id, String context) {
		//check level 1 (with context)
		Pair temp = new Pair(id, context);
		if (level_1.containsKey(temp)) {
			return level_1.get(temp);
		}
		
		//if not found anywhere, throw an error
		throw new IdentificationError("Undeclared member " + id);
	}
	
	public Declaration check_level_zero(String id) {
		//check level 0
		HashMap<String, Declaration> id_table = id_table_stack.get(0);
		if (id_table.containsKey(id)) {
			return id_table.get(id);
		}
		
		//if not found anywhere, throw an error
		throw new IdentificationError("Undeclared class " + id);
	}
	
	public void display_scope_stack() {
		HashMap<String, Declaration> id_table;
		
		for (int i = id_table_stack.size() - 1; i >= 0; i--) {
			System.out.println("Level " + i + ":");
			
			id_table = id_table_stack.get(i);
			Iterator<String> map_iterator = id_table.keySet().iterator();
			String current_string;
			
			while (map_iterator.hasNext()) {
				current_string = map_iterator.next();
				System.out.println(current_string + " -> " + id_table.get(current_string));
			}
		}
	}
}
