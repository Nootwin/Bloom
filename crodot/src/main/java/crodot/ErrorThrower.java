package crodot;

import java.util.LinkedList;
import java.util.Queue;

public class ErrorThrower {
	Queue<Integer> lineNum;
	LinkedList<String> fileContents;
	
	ErrorThrower(Queue<Integer> lineNum, LinkedList<String> fileContents) {
		this.lineNum = lineNum;
		this.fileContents = fileContents;
	}
	
	
	
	Queue<Integer> getLineNumbers() {
		return lineNum;
	}
	
	void NeverEndingStringException(int lineNumber) {
		System.err.println("NeverEndingStringException");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber);
		System.exit(-1);
	}
	
	void UnknownIdentifierException(int lineNumber, String name) {
		System.err.print("UnknownIdentifierException");
		System.err.println(": \"" + name + "\" is not a defined variable or field");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber);
		System.exit(-1);
	}

	public void CharToBigException(int lineNumber) {
		System.err.println("CharToBigException");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber);

		System.exit(-1);
	}
}
