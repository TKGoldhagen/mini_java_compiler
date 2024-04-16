package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.Declaration;

public class Pair {
	private String id;
	private String class_name;
	
	public Pair(String in_id, String in_cn) {
		this.id = in_id;
		this.class_name = in_cn;
	}
	
	public String get_id() {
		return this.id;
	}
	
	public String get_class_name() {
		return this.class_name;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode() ^ class_name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair)) {
			return false;
		}
		
		if (this.id == null || this.class_name == null) {
			return false;
		}
		
		Pair input_pair = (Pair) obj;
		return this.id.equals(input_pair.get_id()) && this.class_name.equals(input_pair.get_class_name());
	}
}
