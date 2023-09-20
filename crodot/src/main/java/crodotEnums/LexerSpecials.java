package crodotEnums;

import java.util.HashMap;

public class LexerSpecials {
	private HashMap<String, String> s = new HashMap<>();
	
	LexerSpecials() {
		s.put("=", "EQUIVALENCY");
		s.put(".", "DOT");
		s.put(">", "GREATERTHAN");
		s.put("<", "LESSTHAN");
		s.put("==", "TRUEEQUALS");
		s.put(">=", "TRUEGREATERTHAN");
		s.put("<=", "TRUELESSTHAN");
		s.put("!=", "NOTEQUALS");
		s.put("+", "ADD");
		s.put("-", "SUB");
		s.put("*", "MUL");
		s.put("/", "DIV");
		s.put("%", "REM");
		s.put("^", "EXP");
		s.put("++", "INCREMENT");
		s.put("--", "DECREMENT");
		s.put("!", "NOT");
		s.put("(", "LEFTBRACKET");
		s.put(")", "RIGHTBRACKET");
		s.put("[", "LEFTBRACE");
		s.put("]", "RIGHTBRACE");
		s.put("{", "LEFTCURLY");
		s.put("}", "RIGHTCURLY");
		s.put(",", "SEPERATOR");
		s.put("?", "INFERRED");
		s.put(":", "CLASSMODIFIER");	
	}
	
	public boolean contains(String key) {
		return s.containsKey(key);
	}
	public String get(String key) {
		return s.get(key);
	}
}
