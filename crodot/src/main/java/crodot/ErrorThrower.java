package crodot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import crodotStates.TokenState;

public class ErrorThrower {
	String sourceFile;
	String outputLoc;
	Queue<Integer> lineNum;
	LinkedList<String> fileContents;
	
	ErrorThrower(Queue<Integer> lineNum, LinkedList<String> fileContents, String sourceFile, String outputLoc) {
		this.lineNum = lineNum;
		this.fileContents = fileContents;
		this.sourceFile = sourceFile;
		this.outputLoc = outputLoc;
	}
	
	
	
	Queue<Integer> getLineNumbers() {
		return lineNum;
	}
	void UnknownArithmeticInputException(int lineNumber, byte operation, String type1, String type2) {
		System.err.print("UnknownArithmeticInputException");
		switch(operation) {
		case TokenState.ADD:
			System.err.println(": \"" + type1 + "\" and \"" + type2 + "\" are not able to be added");
			break;
		case TokenState.SUB:
			System.err.println(": \"" + type1 + "\" and \"" + type2 + "\" are not able to be subtracted");
			break;
		case TokenState.MUL:
			System.err.println(": \"" + type1 + "\" and \"" + type2 + "\" are not able to be multiplied");
			break;
		case TokenState.DIV:
			System.err.println(": \"" + type1 + "\" and \"" + type2 + "\" are not able to be divided");
			break;
		case TokenState.REM:
			System.err.println(": \"" + type1 + "\" and \"" + type2 + "\" are not able to be modulused");
			break;
		case TokenState.EXP:
			System.err.println(": \"" + type1 + "\" and \"" + type2 + "\" are not able to be powered by");
			break;
		}
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
	}
	
	void NeverEndingStringException(int lineNumber) {
		System.err.print("NeverEndingStringException");
		System.err.println(": str is missing end quotes");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
	}
	
	void UnknownMethodException(int lineNumber, String name, String classn) {
		System.err.print("UnknownMethodException");
		System.err.println(": \"" + name + "\" is not a known method in class \"" + classn + "\"");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
	}
	void UnknownClassException(int lineNumber, String var, String name) {
		System.err.print("UnknownClassException");
		System.err.println(": tried assigning identifier \"" + var + "\" with the unknown class \"" + name + "\"");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
	}
	
	void UnknownIdentifierException(int lineNumber, String name) {
		System.err.print("UnknownIdentifierException");
		System.err.println(": \"" + name + "\" is not a defined variable or field");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
	}

	public void CharToBigException(int lineNumber) {
		System.err.println("CharToBigException");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);

		System.exit(-1);
	}
	
	void AmbiguousMethodCallExecption(int lineNumber, String name) {
		System.err.print("AmbiguousMethodCallExecption");
		System.err.println(": \"" + name + "\" cannot decide between multiple equal priority calls");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
	}
	
	void IncompatibleTypeException(int lineNumber, String perferType, String actType) {
		System.err.print("IncompatibleTypeException");
		System.err.println(": \"" + perferType + "\" does not inheret class \"" + actType + "\"");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
	}



	public void UnknownConstructorException(int lineNumber, String name, ArrayList<ArrayList<String>> stacks) {
		System.err.print("UnknownConstructorException");
		System.err.print(": \"" + name + "\" does not have a constructor for arguements \"");
		for (ArrayList<String> stack : stacks) {
			System.err.print(stacks.get(0) + " ");
		}
		System.err.println("\"");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
		
	}



	public void UnknownFieldTypeException(int lineNumber, String name, String classn) {
		System.err.print("UnknownFieldTypeException");
		System.err.print(": Field \"" + name + "\" in class \"" + classn + "\" is not given a type or a initial value");
		System.err.println("\"");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
		
	}



	public void AmbiguousAutoArrayTypeException(String name, int lineNumber) {
		System.err.print("AmbiguousAutoArrayTypeException");
		System.err.print(": Array \"" + name + "\" cannot generate an auto type due to conflicting input");
		System.err.println("\"");
		System.err.println(fileContents.get(lineNumber));
		System.err.println("Error at line " + lineNumber + " in: " + sourceFile);
		System.exit(-1);
		
	}
}
