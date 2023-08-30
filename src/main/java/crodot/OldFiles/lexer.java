package crodot.OldFiles;

import java.util.ArrayList;

import crodot.token;

public class lexer {
	
	
	
	static String checkKey(String a) {
		final String[][] keywords = {{"if", "CONDITIONAL"}, {"else", "CONDITIONAL"}, {"while", "LOOP"}, {"for", "LOOP"}, {"foreach", "LOOP"}, {"int", "DECLARATION"}, {"byte", "DECLARATION"}, {"shrt", "DECLARATION"}, {"long", "DECLARATION"}, {"doub", "DECLARATION"}, {"str", "DECLARATION"}, {"char", "DECLARATION"}, {"flt", "DECLARATION"}, {"bool", "DECLARATION"}, {"void", "DECLARATION"}, {"class", "DEFINITION"}, {"null", "NULLVALUE"}, {"return", "RETURN"}, {"true", "BOOLEAN"}, {"false", "BOOLEAN"}};
		for (String[] i: keywords) {
			if (i[0].equals(a)) {
				return i[1];
			}
		}
		return "IDENTIFIER";
	}
	
	static String checkSpec(String a) {
		final String[][] keywords = {{"=", "EQUIVALENCY"}, {".", "OPERATOR"}, {">", "BOOLEANOPERATOR"}, {"<", "BOOLEANOPERATOR"}, {"==", "BOOLEANOPERATOR"}, {">=", "BOOLEANOPERATOR"}, {"<=", "BOOLEANOPERATOR"}, {"!=", "BOOLEAN OPERATOR"}, {"+", "OPERATOR"}, {"-", "OPERATOR"}, {"*", "OPERATOR"}, {"/", "OPERATOR"}, {"%", "OPERATOR"}, {"^", "OPERATOR"}, {"++", "MONOOPERATOR"}, {"--", "MONOOPERATOR"}, {"!", "MONOOPERATOR"}, {"(", "LEFTBRACKET"}, {")", "RIGHTBRACKET"}, {"[", "LEFTBRACKET"}, {"]", "RIGHTBRACKET"}, {"{", "LEFTBRACKET"}, {"}", "RIGHTBRACKET"}, {",", "SEPERATOR"} };
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
		
		for (char i :code.toCharArray()) {
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
					if (ts.get(ts.size()-1).value.equals("")) {
						ts.remove(ts.size()-1);
					}
					a = "";
					if (i == ';') {
						ts.add(new token("ENDOFLINE", ";"));
					}
					else if (i == '"') {
						b = "STRING";
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
					}
					else if (i == '"') {
						b = "STRING";
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
			else if (b ==  "SPECIAL") {
				if (a.equals("-") && Character.isDigit(i)) {
					a += i;
					b = "NUMBER";
				}
				else if (!Character.isDigit(i) && !Character.isLetter(i) && !Character.isWhitespace(i) && i !=  '"' && i !=  ';' && !a.equals(")") && !a.equals("(") && !a.equals("}") && !a.equals("{") && !a.equals("[") && !a.equals("]")) {
					a += i;
				}
				else {
					ts.add(new token(checkSpec(a), a));
					if (ts.get(ts.size()-1).type.equals("Error")) {
						ts.remove(ts.size()-1);
					}
					a = "";
					if (Character.isLetter(i)) {
						a += i;
						b = "LETTER";
					}
					else if (i == ';') {
						ts.add(new token("ENDOFLINE", ";"));
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
	
	@SuppressWarnings("unchecked")
	ArrayList<ArrayList<token>> linebyline(ArrayList<token> t) {
		ArrayList<ArrayList<token>> r = new ArrayList<ArrayList<token>>();
		ArrayList<token> l = new ArrayList<>();
		for (token i : t) {
			l.add(i);
			if (i.type.equals("ENDOFLINE")) {
				r.add((ArrayList<token>) l.clone());
				
				l.clear();
			}
		}
		return r;
	}
}
