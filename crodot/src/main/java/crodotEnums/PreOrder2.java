package crodotEnums;

import java.util.HashMap;

import crodot.caller;
import crodotStates.TokenState;

public class PreOrder2 {
	private HashMap<Byte, Integer> map = new HashMap<>();
	
	
	public PreOrder2() {
		map.put(TokenState.DOT, 3);
		map.put(TokenState.INCREMENT, 2);
		map.put(TokenState.DECREMENT, 2);
		map.put(TokenState.IDENTIFIER, 4);
		map.put(TokenState.RIGHTBRACE, 6);
		map.put(TokenState.RIGHTBRACKET, 5);
		map.put(TokenState.LEFTBRACE, 6);
		map.put(TokenState.LEFTBRACKET, 6);
		map.put(TokenState.RIGHTGENERIC, 6);
	}
	
	public boolean contains(Byte b) {
		return map.containsKey(b);
	}
	public Integer get(Byte b) {
		Integer x =  map.get(b);
		if (x != null) {
			return x;
		}
		return 128;
	}
	
}
