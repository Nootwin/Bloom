package crodot;

import java.util.ArrayList;


public class Lexer {
	int genericCheck =0;
	
	
	static String checkKey(String a) {
		final String[][] keywords = {{"import", "IMPORT"}, {"if", "CONDITIONAL"}, {"else", "CONDITIONAL"}, {"while", "LOOP"}, {"for", "LOOP"}, {"foreach", "LOOP"}, {"int", "DECLARATION"}, {"byte", "DECLARATION"}, {"shrt", "DECLARATION"}, {"long", "DECLARATION"}, {"doub", "DECLARATION"}, {"str", "DECLARATION"}, {"char", "DECLARATION"}, {"flt", "DECLARATION"}, {"bool", "DECLARATION"}, {"void", "DECLARATION"}, {"class", "DEFINITION"}, {"null", "NULLVALUE"}, {"return", "RETURN"}, {"true", "BOOLEAN"}, {"false", "BOOLEAN"}, {"local", "ACCESS"}, {"static", "ACCESS"}};
		for (String[] i: keywords) {
			if (i[0].equals(a)) {
				return i[1];
			}
		}
		return "IDENTIFIER";
	}
	
	static String checkSpec(String a) {
		final String[][] keywords = {{"=", "EQUIVALENCY"}, {".", "DOT"}, {">", "GREATERTHAN"}, {"<", "LESSTHAN"}, {"==", "TRUEEQUALS"}, {">=", "TRUEGREATERTHAN"}, {"<=", "TRUELESSTHAN"}, {"!=", "NOTEQUALS"}, {"+", "ADD"}, {"-", "SUB"}, {"*", "MUL"}, {"/", "DIV"}, {"%", "REM"}, {"^", "EXP"}, {"++", "INCREMENT"}, {"--", "DECREMENT"}, {"!", "NOT"}, {"(", "LEFTBRACKET"}, {")", "RIGHTBRACKET"}, {"[", "LEFTBRACE"}, {"]", "RIGHTBRACE"}, {"{", "LEFTCURLY"}, {"}", "RIGHTCURLY"}, {",", "SEPERATOR"}, {"?", "INFERRED"}, {":", "CLASSMODIFIER"} };
		for (String[] i: keywords) {
			if (i[0].equals(a)) {
				
				return i[1];
			}
		}
		return "Error";
	}
	
	ArrayList<token> lex(String code) {
		String a = "";
		String b = "";
		ArrayList<token> ts = new ArrayList<>();
		char i;
		for (int j = 0; j < code.length(); j++) {
			i = code.charAt(j);
			if (b ==  "") {
				if (Character.isLetter(i)) {
					a += i;
					b = "LETTER";
				}
				else if (Character.isDigit(i)) {
					a += i;
					b = "NUMBER";
				}
				else if (i == '"') {
					b = "STRING";
				}
				else if (i == '\'') {
					b = "CHAR";
				}
				else if (i == ';') {
					ts.add(new token("ENDOFLINE", ";"));
				}
				else if (!Character.isWhitespace(i)) {
					a += i;
					b = "SPECIAL";
				}
			}
			else if (b ==  "LETTER") {
				
				if (Character.isLetter(i) || Character.isDigit(i)) {
					a += i;
				}
				else {
					ts.add(new token(checkKey(a), a));
					if (ts.get(ts.size()-1).type.equals("IDENTIFIER") && ts.size() > 1) {
						for (int o = ts.size()-2; o > 0; o--) {
							if (ts.get(o).type.equals("IDENTIFIER")) {
								ts.get(o).type = "DECLARATION";
								break;
							}
							else if (!(ts.get(o).type.equals("LEFTBRACE") || ts.get(o).type.equals("RIGHTBRACE"))) {
								break;
							}
							
						}
						
						
					}
					if (ts.get(ts.size()-1).value.equals("")) {
						ts.remove(ts.size()-1);
					}
					a = "";
					if (i == ';') {
						ts.add(new token("ENDOFLINE", ";"));
						if (genericCheck > 0) {
							genericCheck = 0;
						}
					}
					else if (i == '"') {
						b = "STRING";
					}
					else if (i == '\'') {
						b = "CHAR";
					}
					else if (!Character.isWhitespace(i)) {
						a += i;
						b = "SPECIAL";
					}
					else { b = "";}
				}
			}
			else if (b ==  "NUMBER") {
				if (Character.isDigit(i) || i ==  '.') {
					a += i;
				}
				else {
					if (a.length() > 0) {ts.add(new token("NUMBER", a));}
					a = "";
					if (Character.isLetter(i)) {
						a += i;
						b = "LETTER";
					}
					else if (i == ';') {
						ts.add(new token("ENDOFLINE", ";"));
						if (genericCheck > 0) {
							genericCheck = 0;
						}
					}
					else if (i == '"') {
						b = "STRING";
					}				
					else if (i == '\'') {
						b = "CHAR";
					}
					else if (!Character.isWhitespace(i)) {
						a += i;
						b = "SPECIAL";
					}
					else { b = "";}
				}
			}
			else if (b ==  "STRING") {
				if (i !=  '"') {
					a += i;
				}
				else {
					ts.add(new token("STRING", a));
					a = "";
					b = "";
				}	
			}
			else if (b == "CHAR") {
				if (i !=  '\'') {
					a += i;
				}
				else {
					ts.add(new token("CHAR", a));
					a = "";
					b = "";
				}	
				
			}
			else if (b ==  "SPECIAL") {
				if (a.equals("-") && Character.isDigit(i)) {
					a += i;
					b = "NUMBER";
				}
				else if (!Character.isDigit(i) && !Character.isLetter(i) && !Character.isWhitespace(i) && i !=  '"' && i !=  ';' && i != '(' && i != ')' && i != '{' && i != '}' && i != '[' && i != ']' && i!= '?') {
					a += i;
				}
				else {
					
					ts.add(new token(checkSpec(a), a));
					if (ts.get(ts.size()-1).type.equals("Error")) {
						ts.remove(ts.size()-1);
					}
					else if (genericCheck > 0 && ts.get(ts.size()-1).type.equals("EQUIVALENCY")) {
						genericCheck = 0;
					}
					else if (ts.get(ts.size()-1).type.equals("LESSTHAN")) {
						System.out.println("smash");
						genericCheck = ts.size()-1;
					}
					else if (genericCheck > 0 && ts.get(ts.size()-1).type.equals("GREATERTHAN")) {
						boolean flag = true;
						for (int k = genericCheck+1; k < ts.size()-1; k++) {
							if (!(ts.get(k).type.equals("IDENTIFIER") || ts.get(k).type.equals("SEPERATOR") || ts.get(k).type.equals("INFERRED") || ts.get(k).type.equals("CLASSMODIFIER"))) {
								flag = false;
								break;
							}
						}
						if (flag) {
							ts.set(genericCheck, new token("LEFTGENERIC", "<"));
							ts.set(ts.size()-1, new token("RIGHTGENERIC", ">"));
							if (!(code.charAt(j) == '(' || ts.get(genericCheck-2).type.equals("DEFINITION"))) {
								ts.set(genericCheck-1, new token("DECLARATION", ts.get(genericCheck-1).value));
							}
						}
						
						genericCheck = 0;
					}
					a = "";
					if (Character.isLetter(i)) {
						a += i;
						b = "LETTER";
					}
					else if (i == ';') {
						ts.add(new token("ENDOFLINE", ";"));
						if (genericCheck > 0) {
							genericCheck = 0;
						}
					}
					else if (i == '"') {
						b = "STRING";
					}
					else if (Character.isDigit(i)) {
						a += i;
						b = "NUMBER";
					}
					else if (!Character.isWhitespace(i)) {
						a += i;
						b = "SPECIAL";
					}
					else { b = "";}
				}
			}
		}
		
		
		return ts;
	}
}
