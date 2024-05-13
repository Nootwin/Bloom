package crodotEnums;

import java.util.HashMap;
import crodotStates.TokenState;

public class LexerKeywords {
	private HashMap<String, Byte> s = new HashMap<>();
	
	public LexerKeywords() {
		s.put("import", TokenState.IMPORT);
		s.put("if", TokenState.CONDITIONAL);
		s.put("else", TokenState.CONDITIONAL);
		s.put("while", TokenState.LOOP);
		s.put("for", TokenState.LOOP);
		s.put("int", TokenState.DECLARATION);
		s.put("byte", TokenState.DECLARATION);
		s.put("shrt", TokenState.DECLARATION);
		s.put("long", TokenState.DECLARATION);
		s.put("doub", TokenState.DECLARATION);
		s.put("str", TokenState.DECLARATION);
		s.put("char", TokenState.DECLARATION);
		s.put("flt", TokenState.DECLARATION);
		s.put("bool", TokenState.DECLARATION);
		s.put("void", TokenState.DECLARATION);
		s.put("class", TokenState.DEFINITION);
		s.put("subclass", TokenState.DEFINITION);
		s.put("abstract", TokenState.ACCDEF);
		s.put("interface", TokenState.DEFINITION);
		s.put("null", TokenState.NULLVALUE);
		s.put("return", TokenState.RETURN);
		s.put("true", TokenState.BOOLEAN);
		s.put("false", TokenState.BOOLEAN);
		s.put("local", TokenState.ACCESS);
		s.put("proc", TokenState.ACCESS);
		s.put("priv", TokenState.ACCESS);
		s.put("static", TokenState.ACCESS);	
		s.put("is", TokenState.IS);
	}
	
	public boolean contains(String key) {
		return s.containsKey(key);
	}
	public byte get(String key) {
		return s.get(key);
	}
}
