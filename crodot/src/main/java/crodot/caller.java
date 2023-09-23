package crodot;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;


import java.util.Set;

import crodotStates.TokenState;


public class caller {

	static void printoken (ArrayList<Token> tcode) {
			for (Token i : tcode) {
			System.out.print(TokenStateToString(i.type));
			System.out.print(' ');
			System.out.println(i.value);
			}
	}
	
	static void indtree(ASTNode tree, int n) {
		for (int z = 0; z < n; z++) {
			System.out.print('-');
		}
		System.out.println(TokenStateToString(tree.type));
		for (ASTNode i : tree.next) {
			indtree(i, n+1);
		}
	}
	
	static void printree(ArrayList<ASTNode> ncode) {
		for (ASTNode tree : ncode) {
			indtree(tree, 0);
		}
	}
	static void printresults(AnaResults r) {
		for (Map.Entry<String, ClassInfo> Class: r.Classes.entrySet()) {
			for (Map.Entry<String, MethodInfo> Method: Class.getValue().methods.entrySet()) {
				System.out.println("Class: " + Class.getValue().name + " Key: " + Class.getKey() + " Method: " + Method.getValue().name + " Key: " + Method.getKey() + " RETURN: " + Method.getValue().returnType + " First: " + Method.getValue().args.toString());
			}
			for (Map.Entry<String, FieldInfo> Field: Class.getValue().fields.entrySet()) {
				System.out.println("Class: " + Class.getValue().name + " Key: " + Class.getKey() + " Field: " + Field.getValue().name + " Key: " + Field.getKey() + " Type: " + Field.getValue().type);
			}
		}
	}
	static void printhelpfulresults(AnaResults r) {
		Set<Entry<String, String>> types;
		for (Map.Entry<String, ClassInfo> Class: r.Classes.entrySet()) {
			if (!Class.getValue().name.startsWith("java/lang")) {
				for (Map.Entry<String, MethodInfo> Method: Class.getValue().methods.entrySet()) {
					System.out.print("Class: " + Class.getValue().name + " Key: " + Class.getKey() + " Method: " + Method.getValue().name + " Key: " + Method.getKey() + " RETURN: " + Method.getValue().returnType + " First: ");
					System.out.print("[");
					for (int i = 0; i < Method.getValue().args.size(); i++) {
						System.out.print(Method.getValue().args.get(i).toString());
						System.out.print(", ");
					}
					System.out.println("]");
				}
				for (Map.Entry<String, FieldInfo> Field: Class.getValue().fields.entrySet()) {
					System.out.println("Class: " + Class.getValue().name + " Key: " + Class.getKey() + " Field: " + Field.getValue().name + " Key: " + Field.getKey() + " Type: " + Field.getValue().type);
				}
				
				if (Class.getValue().genType != null) {
					types = Class.getValue().Generics();
					System.out.print(Class.getValue().name);
					for (Entry<String, String> entry : types) {
						System.out.print("   " + entry.getKey());
						System.out.print(" " + entry.getValue());
					}
					System.out.println();
				}
				
			}
			
		}
	}
	
	public static String TokenStateToString(Byte b) {
		switch(b) {
		case TokenState.IMPORT: return "IMPORT";
		case TokenState.CONDITIONAL: return "CONDITIONAL";
		case TokenState.LOOP: return  "LOOP";
		case TokenState.DECLARATION: return  "DECLARATION";
		case TokenState.DEFINITION: return "DEFINITION"; 
		case TokenState.ACCDEF: return  "ACCDEF";
		case TokenState.NULLVALUE: return  "NULLVALUE";
		case TokenState.RETURN: return "RETURN";  
		case TokenState.BOOLEAN: return  "BOOLEAN";
		case TokenState.ACCESS: return  "ACCESS";
		case TokenState.EQUIVALENCY: return  "EQUIVALENCY";
		case TokenState.DOT: return  "DOT";
		case TokenState.GREATERTHAN: return  "GREATERTHAN";
		case TokenState.LESSTHAN: return  "LESSTHAN";
		case TokenState.TRUEEQUALS: return  "TRUEEQUALS";
		case TokenState.TRUEGREATERTHAN: return  "TRUEGREATERTHAN";
		case TokenState.TRUELESSTHAN: return  "TRUELESSTHAN";
		case TokenState.NOTEQUALS: return  "NOTEQUALS";
		case TokenState.ADD: return  "ADD";
		case TokenState.SUB: return  "SUB";
		case TokenState.MUL: return  "MUL";
		case TokenState.DIV: return  "DIV";
		case TokenState.REM: return  "REM";
		case TokenState.EXP: return  "EXP";
		case TokenState.INCREMENT: return  "INCREMENT";
		case TokenState.DECREMENT: return  "DECREMENT";
		case TokenState.NOT: return  "NOT";
		case TokenState.LEFTBRACKET: return  "LEFTBRACKET";
		case TokenState.RIGHTBRACKET: return  "RIGHTBRACKET";
		case TokenState.LEFTBRACE: return  "LEFTBRACE";
		case TokenState.RIGHTBRACE: return  "RIGHTBRACE";
		case TokenState.LEFTCURLY: return  "LEFTCURLY";
		case TokenState.RIGHTCURLY: return  "RIGHTCURLY";
		case TokenState.SEPERATOR: return  "SEPERATOR";
		case TokenState.INFERRED: return  "INFERRED";
		case TokenState.CLASSMODIFIER: return  "CLASSMODIFIER";
		case TokenState.ENDOFLINE: return  "ENDOFLINE";
		case TokenState.LEFTGENERIC: return  "LEFTGENERIC";
		case TokenState.RIGHTGENERIC: return  "RIGHTGENERIC";
		case TokenState.IDENTIFIER: return  "IDENTIFIER";
		case TokenState.NUMBER: return  "NUMBER";
		case TokenState.STRING: return  "STRING";
		case TokenState.CHAR: return  "CHAR";
		case TokenState.FUN: return  "FUN";
		case TokenState.ARR: return  "ARR";
		case TokenState.GENFUN: return  "GENFUN";
		case TokenState.GENERIC: return  "GENERIC";
		case TokenState.CLASSNAME: return  "CLASSNAME";
		case TokenState.DESCRIPTION: return  "DESCRIPTION";
		case TokenState.START: return  "START";
		case TokenState.END: return  "END";
		
		case TokenState.INTEGER: return  "INTEGER";
		case TokenState.LONG: return  "LONG";
		case TokenState.DOUBLE: return  "DOUBLE";
		case TokenState.CODE: return  "CODE";
	}
		return null;


	}
	
	public static void main(String[] args) {
		String file = fileReader.ReadFileToString("C:\\Users\\Nolan Murray\\git\\cro\\crodot\\src\\main\\java\\crodot\\main.cr");
		System.out.println(file);
		
		LexerAttempt3 lex2 = new LexerAttempt3(file);
		ArrayList<Token> lexed = lex2.lex();
		printoken(lexed);
		
		lex2 = null;
		
		System.out.println("############");
		
		Parser newparse = new Parser(lexed);
		
		lexed = null;
		
		ASTNode parsed = newparse.parse();
		newparse = null;
		indtree(parsed, 0);
		
		Analyser analy = new Analyser(parsed);
		
		AnaResults results = analy.start();
		
		printhelpfulresults(results);
		Generator gen = new Generator(parsed, results );
		
		gen.createSys(null);
		

		
	}

}
