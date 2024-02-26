package crodotEnums;

import java.util.HashMap;

import crodotStates.TokenState;

public class PostOrder2 {
	private HashMap<Byte, Integer> map = new HashMap<>();
	
	
	public PostOrder2() {
		map.put(TokenState.SEPERATOR, 0);
		map.put(TokenState.EQUIVALENCY, 1);
		map.put(TokenState.TRUEEQUALS, 2);
		map.put(TokenState.NOTEQUALS, 2);
		map.put(TokenState.TRUEGREATERTHAN, 2);
		map.put(TokenState.TRUELESSTHAN, 2);
		map.put(TokenState.GREATERTHAN, 2);
		map.put(TokenState.LESSTHAN, 2);
		map.put(TokenState.NOT, 3);
		map.put(TokenState.ADD, 3);
		map.put(TokenState.SUB, 3);
		map.put(TokenState.MUL, 3);
		map.put(TokenState.DIV, 3);
		map.put(TokenState.REM, 3);
		map.put(TokenState.EXP, 3);
		map.put(TokenState.INCREMENT, 4);
		map.put(TokenState.DECREMENT, 4);
		map.put(TokenState.DOT, 5);

		map.put(TokenState.IDENTIFIER, 6);
		map.put(TokenState.DECLARATION, 6);
		map.put(TokenState.NUMBER, 6);
		map.put(TokenState.STRING, 6);
		map.put(TokenState.CHAR, 6);
		map.put(TokenState.BOOLEAN, 6);
		map.put(TokenState.LEFTCAST, 8);
		map.put(TokenState.RIGHTBRACE, 7);
		map.put(TokenState.RIGHTBRACKET, 7);
		map.put(TokenState.LEFTBRACE, 8);
		map.put(TokenState.LEFTBRACKET, 8);
		map.put(TokenState.RIGHTGENERIC, 8);
		map.put(TokenState.RIGHTCAST, 8);
//		FUN(6),
//		ARR(6),
//		GENFUN(6),
		
	}
	
	public boolean contains(Byte b) {
		return map.containsKey(b);
	}
	public int get(Byte b) {
		Integer x =  map.get(b);
		if (x != null) {
			return x;
		}
		return 238;
	}
	
}
