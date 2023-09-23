package crodot;

import java.util.ArrayList;
import java.util.Stack;

import crodotEnums.LexerKeywords;
import crodotEnums.LexerSpecials;
import crodotStates.LexerStatus;
import crodotStates.TokenState;

public class LexerAttempt3 {
	private String code;
	private LexerKeywords words;
	private LexerSpecials specials;
	private ArrayList<Token> tokens;
	private Stack<Integer> genCounter;
	
	LexerAttempt3(String code) {
		this.code = code;
		this.words = new LexerKeywords();
		this.specials = new LexerSpecials();
		this.tokens = new ArrayList<>(code.length() >> 1);
		this.genCounter = new Stack<>();
	}
	
	public void addSpecial(String value) {
		byte type;
		if (specials.contains(value)) {
			tokens.add(new Token(type = specials.get(value), value));
			if (type == TokenState.LESSTHAN) {
				genCounter.add(tokens.size()-1);
			}
			else if (type == TokenState.GREATERTHAN) {
				int gc = genCounter.pop();
				boolean flag = true;
				for (int i = tokens.size()-2; i > gc; i--) {
					if (!(tokens.get(i).type == TokenState.IDENTIFIER || tokens.get(i).type == TokenState.DECLARATION || tokens.get(i).type == TokenState.SEPERATOR || tokens.get(i).type == TokenState.LEFTGENERIC || tokens.get(i).type == TokenState.RIGHTGENERIC)) {
						flag = false;
						break;
					}
				}
				if (flag) {
					tokens.get(gc).type = TokenState.LEFTGENERIC;
					tokens.get(tokens.size()-1).type = TokenState.RIGHTGENERIC;
				}
			}
		}
		else {
			if (value.length() > 1) {
				addSpecial(value.substring(0, value.length()-1));
				addSpecial(value.substring(value.length()-1, value.length()));
			}
			else {
				System.out.println("Unknown special");
			}
		}
	}
	
	public void addKeyword(String value) {
		if (words.contains(value)) {
			tokens.add(new Token(words.get(value), value));
		}
		else {
			tokens.add(new Token(TokenState.IDENTIFIER, value));
			int genBrack = 0;
			for (int i = tokens.size()-2; i > 0; i--) {
				if (tokens.get(i).type == TokenState.RIGHTGENERIC) {
					genBrack++;
				}
				else if (tokens.get(i).type == TokenState.LEFTGENERIC) {
					genBrack--;
				}
				else if (genBrack == 0) {
					if (tokens.get(i).type == TokenState.IDENTIFIER) {
						tokens.get(i).type = TokenState.DECLARATION;
						break;
					}
					else if (tokens.get(i).type == TokenState.LEFTBRACE) {
						if (tokens.get(i+1).type != TokenState.RIGHTBRACE) {
							break;
						}
					}
					else if (tokens.get(i).type != TokenState.RIGHTBRACE) {
						break;
							
					}
				}
				
				
					
			}
			
			
		}
	}
	
	public ArrayList<Token> lex() {
		StringBuilder b = new StringBuilder();
		char c;
		byte status = LexerStatus.NULL;
		for (int i = 0; i < code.length(); i++) {
			c = code.charAt(i);
			switch(status) {
			case LexerStatus.NULL:
				if (Character.isLetter(c)) {
					b.append(c);
					status = LexerStatus.KEYWORD;
				}
				else if (Character.isDigit(c)) {
					b.append(c);
					status = LexerStatus.NUMBER;
				}
				else if (c == '"') {
					status = LexerStatus.STRING;
				}
				else if (c == '\'') {
					status = LexerStatus.CHAR;
				}
				else if (!Character.isWhitespace(c)) {
					if (c != ';') {
						b.append(c);
						status = LexerStatus.SPECIAL;
					}
					else {
						addSpecial(";");
					}
					
				}
				break;
			case LexerStatus.KEYWORD:
				if (Character.isLetter(c)) {
					b.append(c);
				}
				else if (Character.isDigit(c)) {
					b.append(c);
					status = LexerStatus.LETTERNUM;
				}
				else if (c == '"') {
					//error
				}
				else if (c == '\'') {
					//error
				}
				else if (!Character.isWhitespace(c)) {
					addKeyword(b.toString());
					b.setLength(0);
					
					if (c != ';') {
						b.append(c);
						status = LexerStatus.SPECIAL;
					}
					else {
						addSpecial(";");
						status = LexerStatus.NULL;
					}
				}
				else {
					addKeyword(b.toString());
					b.setLength(0);
					status = LexerStatus.NULL;
				}
				break;
			case LexerStatus.LETTERNUM:
				if (Character.isLetter(c) || Character.isDigit(c)) {
					b.append(c);
				}
				else if (c == '"') {
					//error
				}
				else if (c == '\'') {
					//error
				}
				else if (!Character.isWhitespace(c)) {
					addKeyword(b.toString());
					b.setLength(0);
					if (c != ';') {
						b.append(c);
						status = LexerStatus.SPECIAL;
					}
					else {
						addSpecial(";");
						status = LexerStatus.NULL;
					}
				}
				else {
					addKeyword(b.toString());
					b.setLength(0);
					status = LexerStatus.NULL;
				}
				break;
			case LexerStatus.NUMBER:
				if (Character.isDigit(c) || c == '.') {
					b.append(c);
				}
				else if (Character.isLetter(c) || c == '"' || c == '\'') {
					//error
				}
				else if (!Character.isWhitespace(c)) {
					tokens.add(new Token(TokenState.NUMBER, b.toString()));
					b.setLength(0);
					if (c != ';') {
						b.append(c);
						status = LexerStatus.SPECIAL;
					}
					else {
						addSpecial(";");
						status = LexerStatus.NULL;
					}
				}
				else {
					tokens.add(new Token(TokenState.NUMBER, b.toString()));
					b.setLength(0);
					status = LexerStatus.NULL;
				}
				break;
			case LexerStatus.SPECIAL:
				if (Character.isLetter(c)) {
					addSpecial(b.toString());
					b.setLength(0);
					b.append(c);
					status = LexerStatus.KEYWORD;
				}
				else if (Character.isDigit(c)) {
					addSpecial(b.toString());
					b.setLength(0);
					b.append(c);
					status = LexerStatus.NUMBER;
				}
				else if (c == '"') {
					addSpecial(b.toString());
					b.setLength(0);
					status = LexerStatus.STRING;
				}
				else if (c == '\'') {
					addSpecial(b.toString());
					b.setLength(0);
					status = LexerStatus.CHAR;
				}
				else if (!Character.isWhitespace(c)) {
					if (c != ';') {
						b.append(c);
					}
					else {
						addSpecial(b.toString());
						b.setLength(0);
						addSpecial(";");
						status = LexerStatus.NULL;
					}
				}
				else {
					addSpecial(b.toString());
					b.setLength(0);
					status = LexerStatus.NULL;
				}
				break;
			case LexerStatus.STRING:
				if (c != '"') {
					b.append(c);
				}
				else {
					tokens.add(new Token(TokenState.STRING, b.toString()));
					b.setLength(0);
					status = LexerStatus.NULL;
				}
			case LexerStatus.CHAR:
				if (c != '\'') {
					b.append(c);
				}
				else {
					tokens.add(new Token(TokenState.CHAR, b.toString()));
					b.setLength(0);
					status = LexerStatus.NULL;
				}
			}
			
		}
		
		return tokens;
	}
	
}


