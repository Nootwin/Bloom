package crodotEnums;

import java.util.HashMap;

import crodotStates.TokenState;

public class PostOrder2 {
	private HashMap<Byte, Integer> map = new HashMap<>();
	
	
	public PostOrder2() {
		map.put(TokenState.SEPERATOR, 0);
		map.put(TokenState.TRUEEQUALS, 1);
		map.put(TokenState.NOTEQUALS, 1);
		map.put(TokenState.TRUEGREATERTHAN, 1);
		map.put(TokenState.TRUELESSTHAN, 1);
		map.put(TokenState.GREATERTHAN, 1);
		map.put(TokenState.LESSTHAN, 1);
		map.put(TokenState.ADD, 2);
		map.put(TokenState.SUB, 2);
		map.put(TokenState.MUL, 2);
		map.put(TokenState.DIV, 2);
		map.put(TokenState.REM, 2);
		map.put(TokenState.EXP, 2);
		map.put(TokenState.DOT, 3);
		map.put(TokenState.IDENTIFIER, 4);
		map.put(TokenState.DECLARATION, 4);
		map.put(TokenState.NUMBER, 4);
		map.put(TokenState.STRING, 4);
		map.put(TokenState.CHAR, 4);
		map.put(TokenState.BOOLEAN, 4);
		map.put(TokenState.RIGHTBRACE, 5);
		map.put(TokenState.RIGHTBRACKET, 5);
		map.put(TokenState.LEFTBRACE, 6);
		map.put(TokenState.LEFTBRACKET, 6);
		map.put(TokenState.RIGHTGENERIC, 6);
//		FUN(4),
//		ARR(4),
//		GENFUN(4),
		
	}
	
	public boolean contains(Byte b) {
		return map.containsKey(b);
	}
	public int get(Byte b) {
		Integer x =  map.get(b);
		if (x != null) {
			return x;
		}
		return 128;
	}
	
}
