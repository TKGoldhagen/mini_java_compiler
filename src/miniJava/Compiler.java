package miniJava;

import java.io.InputStream;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Parser;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.Type_Checking;
import miniJava.ContextualAnalysis.OtherContextualErrors;

import miniJava.CodeGeneration.x64.*;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) {
		//FLAG
		//Push push = new Push(4500);
		//for (int i = 0; i < push.getBytes().length; i++) {
		//	System.out.println(Integer.toHexString(push.getBytes()[i] & 0xFF));
		//}
		//FLAG
		System.out.println((short) 8);
		
		/*
		//Instantiate error reporter
		ErrorReporter reporter = new ErrorReporter();
		
		//FOR TESTING
		String test_name = "pass332";
		String file_path = "C:\\Users\\Tomer\\Projects\\COMP520\\pa3_tests\\" + test_name + ".java";
		//FOR TESTING
		
		//Get file to compile from command line argument 
		//String file_path = "";
		//if (args.length > 0) {
		//	file_path = args[0];
		//}
		//else {
		//	System.out.println("Error");
		//	System.out.println("Please specify a file path");
		//	return;
		//}
		
		//Try to read from file, report error if file DNE
		InputStream in_stream = null;
		try {
			in_stream = new FileInputStream(file_path);
		}
		catch (FileNotFoundException e) {
			reporter.reportError("File " + file_path + " not found");
		}
		
		//Instantiate scanner and parse the file
		AST full_tree = null;
		if (!reporter.hasErrors()) {
			Scanner scanner = new Scanner(in_stream, reporter);
			Parser parser = new Parser(scanner, reporter);
			full_tree = parser.parse();
		}
		
		//For debugging
		ASTDisplay display = new ASTDisplay();
		//display.showTree(full_tree);
		
		//do identification
		if (!reporter.hasErrors()) {
			Identification identifier = new Identification(reporter);
			identifier.identify_tree(full_tree);
		}
		
		//do type checking
		if (!reporter.hasErrors()) {
			Type_Checking type_checker = new Type_Checking(reporter);
			type_checker.type_check_tree(full_tree);
		}
		
		//do other contextual error checking
		if (!reporter.hasErrors()) {
			OtherContextualErrors other_checker = new OtherContextualErrors(reporter);
			other_checker.check_other_errors(full_tree);
		}
		
		//Report errors
		if (reporter.hasErrors()) {
			System.out.println("Error");
			reporter.outputErrors();
		}
		else {
			System.out.println("Success");
		}*/
	}
}
