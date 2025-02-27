package miniJava;

import java.util.List;
import java.util.ArrayList;

//  First of all, errors are simple strings,
//  perhaps it may be worthwhile to augment this reporter
//  with requiring line numbers.
public class ErrorReporter {
	private List<String> _errorQueue;
	
	public ErrorReporter() {
		this._errorQueue = new ArrayList<String>();
	}
	
	public boolean hasErrors() {
		return _errorQueue.size() > 0;
	}
	
	public void outputErrors() {
		for (int i=0; i < _errorQueue.size(); i++) {
			System.out.println(_errorQueue.get(i));
		}
	}
	
	public void reportError(String ...error) {
		StringBuilder sb = new StringBuilder();
		
		for(String s : error)
			sb.append(s);
		
		_errorQueue.add(sb.toString());
	}
}
