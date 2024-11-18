package crodotEnums;

import java.util.HashMap;

import crodotStates.TokenState;

public class LexerSpecials {
	private HashMap<String, Byte> s = new HashMap<>();
	
	public LexerSpecials() {
		s.put("=", TokenState.EQUIVALENCY);
		s.put("+=", TokenState.EQUIVALENCY);
		s.put("*=", TokenState.EQUIVALENCY);
		s.put("-=", TokenState.EQUIVALENCY);
		s.put("/=", TokenState.EQUIVALENCY);
		s.put("%=", TokenState.EQUIVALENCY);
		s.put(".", TokenState.DOT);
		s.put(">", TokenState.GREATERTHAN);
		s.put("<", TokenState.LESSTHAN);
		s.put("==", TokenState.TRUEEQUALS);
		s.put(">=", TokenState.TRUEGREATERTHAN);
		s.put("<=", TokenState.TRUELESSTHAN);
		s.put("!=", TokenState.NOTEQUALS);
		s.put("+", TokenState.ADD);
		s.put("-", TokenState.SUB);
		s.put("*", TokenState.MUL);
		s.put("/", TokenState.DIV);
		s.put("//", TokenState.COMMENT);
		s.put("%", TokenState.REM);
		s.put("^", TokenState.EXP);
		s.put("++", TokenState.INCREMENT);
		s.put("--", TokenState.DECREMENT);
		s.put("!", TokenState.NOT);
		s.put("(", TokenState.LEFTBRACKET);
		s.put(")", TokenState.RIGHTBRACKET);
		s.put("[", TokenState.LEFTBRACE);
		s.put("]", TokenState.RIGHTBRACE);
		s.put("{", TokenState.LEFTCURLY);
		s.put("}", TokenState.RIGHTCURLY);
		s.put(",", TokenState.SEPERATOR);
		s.put("?", TokenState.INFERRED);
		s.put(":", TokenState.CLASSMODIFIER);	
		s.put(";", TokenState.ENDOFLINE);
	}
	
	public boolean contains(String key) {
		return s.containsKey(key);
	}
	public byte get(String key) {
		return s.get(key);
	}
}
