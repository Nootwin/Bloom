package crodot;

import java.util.ArrayList;
import java.util.Queue;
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
	private Stack<Integer> castCount;
	private ErrorThrower err;
	private Queue<Integer> lineNum;
	private int linePop;
	private boolean comment = false;
	
	LexerAttempt3(String code, ErrorThrower err) {
		this.code = code;
		this.err = err;
		this.lineNum = err.getLineNumbers();
		this.words = new LexerKeywords();
		this.specials = new LexerSpecials();
		this.tokens = new ArrayList<>(code.length() >> 1);
		this.genCounter = new Stack<>();
		this.castCount = new Stack<>();
		this.linePop = 0;

	}
	
	public void addSpecial(String value) {
		byte type;
		if (specials.contains(value)) {
			
			tokens.add(new Token(type = specials.get(value), value, linePop));
			if (type == TokenState.LESSTHAN) {
				genCounter.add(tokens.size()-1);
			}
			else if (type == TokenState.LEFTCURLY) {
				castCount.add(tokens.size()-1);
			}
			else if (type == TokenState.RIGHTCURLY) {
				if (!castCount.isEmpty() && castCount.peek() != tokens.size()-2) {
					tokens.get(castCount.pop()).type = TokenState.LEFTCAST;
					tokens.get(tokens.size()-1).type = TokenState.RIGHTCAST;
				}
			}
			else if (type == TokenState.COMMENT) {
				comment = true;
			}
			else if (type == TokenState.GREATERTHAN) {
				if (!genCounter.isEmpty()) {
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
		}
		else {
			if (value.length() > 1) {
				int size = value.length()-1;
				while (size > 0) {
					for (int i = 0; i+size < value.length(); i++) {
						if (specials.contains(value.substring(i, i+size))) {
							if (i > 0) {
								addSpecial(value.substring(0, i));
							}
								addSpecial(value.substring(i, i+size));
							if (i+size < value.length()) {
								addSpecial(value.substring(i+size, value.length()));
							}
							return;
						}
					}
					size--;
				}
			}
			else {
				System.out.println("Unknown special");
			}
		}
	}
	
	public void addKeyword(String value) {
		
		if (words.contains(value)) {
			Token t;
			tokens.add(t = new Token(words.get(value), value, linePop));
			if (t.type == TokenState.CONDITIONAL && t.value.equals("if") && tokens.size() > 2) {
				t = tokens.get(tokens.size()-2);
				if (t.type == TokenState.CONDITIONAL && t.value.equals("else")) {
					tokens.remove(tokens.size()-1);
					t.value = "elif";
				}
			}
		}
		else {
			tokens.add(new Token(TokenState.IDENTIFIER, value, linePop));
			if (0 == genCounter.size()) {
				int genBrack = 0;
				
				for (int i = tokens.size()-2; i > -1; i--) {
					System.out.println(tokens.get(i).value + genBrack);
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
				System.out.println("___");
				
				
					
			}
			else if (tokens.size() > 1 && tokens.get(tokens.size() - 2).type == TokenState.IDENTIFIER) {
				tokens.get(tokens.size() - 2).type = TokenState.DECLARATION;
			}
			
			
			
		}
	}
	
	public ArrayList<Token> lex() {
		StringBuilder b = new StringBuilder();
		char c;
		byte status = LexerStatus.NULL;
		for (int i = 0; i < code.length(); i++) {
			c = code.charAt(i);
			
			while (!lineNum.isEmpty() && lineNum.peek() == i) {
				lineNum.poll();
				linePop++;
				comment = false;
				castCount.clear();
			}
			if (!comment) {
			switch(status) {
			case LexerStatus.NULL:
				if (Character.isLetter(c) || c == '$') {
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
				if (Character.isLetter(c) || c == '$') {
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
				if (Character.isLetter(c) || Character.isDigit(c) || c == '$') {
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
				else if (Character.isLetter(c) || c == '"' || c == '\'' || c == '$') {
					//error
				}
				else if (!Character.isWhitespace(c)) {
					tokens.add(new Token(TokenState.NUMBER, b.toString(), linePop));
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
					tokens.add(new Token(TokenState.NUMBER, b.toString(), linePop));
					b.setLength(0);
					status = LexerStatus.NULL;
				}
				break;
			case LexerStatus.SPECIAL:
				if (Character.isLetter(c) || c == '$') {
					addSpecial(b.toString());
					b.setLength(0);
					if (tokens.get(tokens.size() - 1).type == TokenState.COMMENT) {
						status = LexerStatus.NULL;
						tokens.remove(tokens.size()-1);
					} else {
						b.append(c);
						status = LexerStatus.KEYWORD;
					}
					
				}
				else if (Character.isDigit(c)) {
					addSpecial(b.toString());
					b.setLength(0);
					if (tokens.get(tokens.size() - 1).type == TokenState.COMMENT) {
						status = LexerStatus.NULL;
						tokens.remove(tokens.size()-1);
					} else {
						b.append(c);
						status = LexerStatus.NUMBER;
					}
				}
				else if (c == '"') {
					addSpecial(b.toString());
					b.setLength(0);
					if (tokens.get(tokens.size() - 1).type == TokenState.COMMENT) {
						status = LexerStatus.NULL;
						tokens.remove(tokens.size()-1);
					} else {
						status = LexerStatus.STRING;
					}
				}
				else if (c == '\'') {
					addSpecial(b.toString());
					b.setLength(0);
					if (tokens.get(tokens.size() - 1).type == TokenState.COMMENT) {
						status = LexerStatus.NULL;
						tokens.remove(tokens.size()-1);
					} else {
						status = LexerStatus.CHAR;
					}
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
					if (tokens.get(tokens.size() - 1).type == TokenState.COMMENT) {
						tokens.remove(tokens.size()-1);
					}
				}
				break;
			case LexerStatus.STRING:
				if (c != '"') {
					b.append(c);
					if (lineNum.peek() == i) {
						err.NeverEndingStringException(linePop);
					}
				}
				else {
					tokens.add(new Token(TokenState.STRING, b.toString(), linePop));
					b.setLength(0);
					status = LexerStatus.NULL;
					
				}
				break;
			case LexerStatus.CHAR:
				if (c != '\'') {
					b.append(c);
					if (b.length() > 1) {
						err.CharToBigException(linePop);
					}
					if (lineNum.peek() == i) {
						err.NeverEndingStringException(linePop);
					}
				}
				else {
					tokens.add(new Token(TokenState.CHAR, b.toString(), linePop));
					b.setLength(0);
					status = LexerStatus.NULL;
				}
				break;
			}
			}
			
			
			
		}
		
		return tokens;
	}
	
}


