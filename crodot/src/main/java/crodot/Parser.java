package crodot;

import java.util.ArrayList;
import java.util.Objects;

import crodotEnums.PostOrder;
import crodotEnums.PostOrder2;
import crodotEnums.PreOrder;
import crodotEnums.PreOrder2;
import crodotStates.TokenState;

public class Parser {
	private ASTNode parent;
	private int fieldCounter;
	private ASTNode cur;
	private int brackets;
	private ArrayList<Token> code;
	private final String[][] preOrder = {{"DOT", "DOT"}, {"IDENTIFIER", "VAR"}, {"RIGHTBRACKET", "BRACKET"}};
	private final String[][] postOrder = {{"SEPERATOR", "SEP"}, {"TRUEEQUALS", "TRUEEQUALS"}, {"NOTEQUALS", "NOTEQUALS"}, {"TRUEGREATERTHAN", "TRUEGREATERTHAN"}, {"TRUELESSTHAN", "TRUELESSTHAN"}, {"LESSTHAN", "LESSTHAN"}, {"GREATERTHAN", "GREATERTHAN"}, {"ADD", "ADD"}, {"SUB", "SUB"}, {"MUL", "MUL"}, {"DIV", "DIV"}, {"REM", "REM"}, {"EXP", "EXP"}, {"DOT", "DOT"}, {"IDENTIFIER", "VAR"}, {"DECLARATION", "VAR"}, {"NUMBER", "NUM"}, {"STRING", "Ljava/lang/String;"}, {"CHAR", "C"}, {"BOOLEAN", "Z"}, {"RIGHTBRACE", "BRACE"}, {"RIGHTBRACKET", "BRACKET"}};
	
	int Start = 0;
	Parser(ArrayList<Token> code) {
		parent = new ASTNode(TokenState.CODE, "Code");
		cur = parent;
		this.code = code;
	}
	
	//inclusive inclusive
	ASTNode genericSolve(ASTNode tree, int start, int end) { 
		if (end - start > 0) {
			final int two = end-1;
			
			if (code.get(two).type == TokenState.CLASSMODIFIER) {
				tree.SetNode(new ASTNode(tree, TokenState.CLASSMODIFIER, code.get(two).value));
				genericSolve(tree.GetFirstNode(), start, start);
				genericSolve(tree.GetFirstNode(), end, end);
				return tree;
			}
		}
		else if (end - start == 0) {
			if (code.get(start).type == TokenState.IDENTIFIER) {
				tree.SetNode(new ASTNode(tree, TokenState.CLASSNAME, code.get(start).value));
			}
			else if (code.get(start).type == TokenState.INFERRED) {
				tree.SetNode(new ASTNode(tree, TokenState.CLASSNAME, "?"));
			}
			return tree;
		}
		return null;
	}
	
	
	ASTNode preSolve(ASTNode tree, int start, int end) { 
		PreOrder2 po2 = new PreOrder2();
		int place = end;
		int unit;
		int bestUnit = -1;
		brackets = 0;
		
		for (int i = end; i > start; i--) {
			if (brackets == 0) {
				unit = po2.get(code.get(i).type);
				if (bestUnit == -1 || unit < bestUnit) {
					
					bestUnit = unit;
					place = i;
					
				}
		
//				for (int j = 0; j < index; j++) {
//					if (code.get(i).type.equals(preOrder[j][0])) {
//						index = j;
//						place = i;
//						tree.type = preOrder[j][1];
//						tree.value = code.get(i).value;
//						if (tree.type.equals("VAR")) {
//							if (code.get(i+1).type.equals("LEFTBRACKET")) {
//								tree.type = "FUN";
//							}
//							if (code.get(i+1).type.equals("LEFTBRACE")) {
//								tree.type = "ARR";
//							}
//						}
//						break;
//					}
//				}
			}
			if (code.get(i).type == TokenState.LEFTBRACKET || code.get(i).type == TokenState.LEFTBRACE) {
				brackets--;
			}
			else if (code.get(i).type == TokenState.RIGHTBRACKET|| code.get(i).type == TokenState.RIGHTBRACE) {
				brackets++;
			}
	
		}
		if (code.get(place).type == TokenState.DECLARATION) {
			code.get(place).type = TokenState.IDENTIFIER;
		}
		if (code.get(place).type == TokenState.IDENTIFIER) {
			if (code.get(place+1).type == TokenState.LEFTBRACKET) {
				code.get(place).type = TokenState.FUN;
			}
			else if (code.get(place+1).type == TokenState.LEFTBRACE) {
				code.get(place).type = TokenState.ARR;
			}
			else if (code.get(place+1).type == TokenState.LEFTGENERIC) {
				code.get(place).type = TokenState.GENFUN;
				
			}
		}
		tree.type = code.get(place).type;
		tree.value = code.get(place).value;
		
		switch (tree.type) {
		case TokenState.DOT:
			tree.SetNode(preSolve(new ASTNode(tree), start-1, place-1));
			tree.SetNode(preSolve(new ASTNode(tree), place, end));
			return tree;
		case TokenState.FUN:
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type == TokenState.LEFTBRACKET) {
					brackets++;
				}
				else if (code.get(i).type == TokenState.RIGHTBRACKET) {
					if (brackets == 0) {
						tree.SetNode(postSolve(new ASTNode(tree), place+1, i-1));
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case TokenState.RIGHTBRACKET:
			return postSolve(tree, start, end-1);
		case TokenState.ARR:
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type == TokenState.LEFTBRACE) {
					brackets++;
				}
				else if (code.get(i).type == TokenState.RIGHTBRACE) {
					if (brackets == 0) {
						tree.SetNode(postSolve(new ASTNode(tree), place+1, i-1));
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case TokenState.IDENTIFIER:
			return tree;
		}
		
		return tree;
	}
	
	//start exclusive, end inclusive
	ASTNode postSolve(ASTNode tree, int start, int end) {
		PostOrder2 po2 = new PostOrder2();
		int place = end;
		int unit;
		int bestUnit = -1;
		brackets = 0;
		if (end - start < 1) {
			tree.type = TokenState.NULLVALUE;
			tree.value = "null";
			return tree;
		}
		for (int i = end; i > start; i--) {
			
			if (brackets == 0) {
				unit = po2.get(code.get(i).type);
				if (bestUnit == -1 || unit < bestUnit) {

					
					
					bestUnit = unit;
					place = i;
					
				}
				
//				for (int j = 0; j < index; j++) {
//					if (code.get(i).type.equals(postOrder[j][0])) {
//						
//						index = j;
//						place = i;
//						if (index > 1 && index < 8) {
//							index = 1;
//						}
//						else if (index > 7 && index < 14) {
//							index = 8;
//						}
//						else if (index > 14 && index < 20) {
//							index = 14;
//						}
//						
//						//mark down if add or mul
//						tree.type = postOrder[j][1];
//						tree.value = code.get(i).value;
//						if (tree.type.equals("VAR")) {
//							if (code.get(i+1).type.equals("LEFTBRACKET")) {
//								tree.type = "FUN";
//							}
//							else if (code.get(i+1).type.equals("LEFTBRACE")) {
//								tree.type = "ARR";
//							}
//							else if (code.get(i+1).type.equals("LEFTGENERIC")) {
//								tree.type = "GENFUN";
//								
//							}
//						}
//						break;
//					}
//				}
			}
			if (code.get(i).type == TokenState.LEFTBRACKET || code.get(i).type == TokenState.LEFTBRACE || code.get(i).type == TokenState.LEFTGENERIC) {
				brackets--;
			}
			else if (code.get(i).type == TokenState.RIGHTBRACKET || code.get(i).type == TokenState.RIGHTBRACE || code.get(i).type == TokenState.RIGHTGENERIC) {
				brackets++;
			}
		}
		if (code.get(place).type == TokenState.DECLARATION) {
			code.get(place).type = TokenState.IDENTIFIER;
		}
		if (code.get(place).type == TokenState.IDENTIFIER) {
			if (code.get(place+1).type == TokenState.LEFTBRACKET) {
				code.get(place).type = TokenState.FUN;
			}
			else if (code.get(place+1).type == TokenState.LEFTBRACE) {
				code.get(place).type = TokenState.ARR;
			}
			else if (code.get(place+1).type == TokenState.LEFTGENERIC) {
				code.get(place).type = TokenState.GENFUN;
				
			}
		}
		tree.type = code.get(place).type;
		tree.value = code.get(place).value;
		
		switch (tree.type) {
		case TokenState.ADD, TokenState.SUB, TokenState.MUL, TokenState.DIV, TokenState.REM, TokenState.EXP, TokenState.SEPERATOR, TokenState.TRUEEQUALS, TokenState.NOTEQUALS, TokenState.TRUEGREATERTHAN, TokenState.TRUELESSTHAN, TokenState.GREATERTHAN, TokenState.LESSTHAN:
			tree.SetNode(postSolve(new ASTNode(tree), start, place-1));
			tree.SetNode(postSolve(new ASTNode(tree), place, end));
			return tree;
		case TokenState.DOT:
			tree.SetNode(preSolve(new ASTNode(tree), start-1, place-1));
			tree.SetNode(preSolve(new ASTNode(tree), place, end));
			return tree;
		case TokenState.GENFUN:
			brackets = 0;
			int dis=0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type == TokenState.LEFTGENERIC) {
					brackets++;
				}
				else if (code.get(i).type == TokenState.RIGHTGENERIC) {
					if (brackets == 0) {	
						tree.SetNode(genericSolve(new ASTNode(tree, TokenState.GENERIC, "<>"), place+2, i-1));
						dis = i;
					}
					else {
						brackets--;
					}
					
				}
				
			}
			for (int i = dis+2; i < end+1; i++) {
				if (code.get(i).type == TokenState.LEFTBRACKET) {
					brackets++;
				}
				else if (code.get(i).type == TokenState.RIGHTBRACKET) {
					if (brackets == 0) {	
						tree.SetNode(postSolve(new ASTNode(tree), dis+1, i-1));
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case TokenState.FUN:
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type == TokenState.LEFTBRACKET) {
					brackets++;
				}
				else if (code.get(i).type == TokenState.RIGHTBRACKET) {
					if (brackets == 0) {	
						tree.SetNode(postSolve(new ASTNode(tree), place+1, i-1));
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case TokenState.RIGHTBRACKET:
			return postSolve(tree, start, end-1);
		case TokenState.RIGHTBRACE:
			int brackets2 = 0;
			int next = start+1;
			for (int i = start+2; i < end+1; i++) {
				if (code.get(i).type == TokenState.SEPERATOR && brackets2 == 0) {
					tree.SetNode(postSolve(new ASTNode(tree), next, i-1));
					next = i;
				}
				else if (code.get(i).type == TokenState.LEFTBRACE) {
					brackets2++;
				}
				else if (code.get(i).type == TokenState.RIGHTBRACE) {
					brackets2--;
				}
				
			}
			tree.SetNode(postSolve(new ASTNode(tree), next, end-1));
			return tree;
		case TokenState.ARR:
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type == TokenState.LEFTBRACE) {
					brackets++;
				}
				else if (code.get(i).type == TokenState.RIGHTBRACE) {
					if (brackets == 0) {
						tree.SetNode(postSolve(new ASTNode(tree), place+1, i-1));
						if (code.get(i+1).type != TokenState.LEFTBRACE) {
							return tree; 
						}
						brackets--;
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case TokenState.NUMBER:
			if (tree.value.contains(".")) {
				tree.type = TokenState.DOUBLE;
			}
			else if (Long.parseLong(tree.value) > Integer.MAX_VALUE && Long.parseLong(tree.value) < Integer.MIN_VALUE ) {
				tree.type = TokenState.LONG;
			}
			else {
				tree.type = TokenState.INTEGER;
			}
			return tree;
		case TokenState.STRING, TokenState.BOOLEAN, TokenState.IDENTIFIER, TokenState.CHAR:
			return tree;
		}
		
		return tree;
	}

	
	
	
	int decide(int p) {
		switch(code.get(p).type) {
		case TokenState.ACCDEF:
			if (cur.prev.type == TokenState.DEFINITION) {
				code.get(p).type = TokenState.ACCESS;
			}
			else {
				code.get(p).type = TokenState.DEFINITION;
			}
			return decide(p);
		case TokenState.IMPORT:
			cur.SetNode(new ASTNode(cur, TokenState.IMPORT, "import"));
			cur = cur.GetLastNode();
			cur.SetNode(new ASTNode(cur, TokenState.STRING, code.get(p+1).value));
			return p+2;
		case TokenState.RETURN:
			cur.SetNode(new ASTNode(cur, TokenState.RETURN, code.get(p).value));
			cur = cur.GetLastNode();
			for (int i = p; i < code.size(); i++) {
				if (code.get(i).type == TokenState.ENDOFLINE || code.get(i).type == TokenState.LEFTCURLY) {
					cur.SetNode(postSolve(new ASTNode(cur), p, i-1));
					return i;
				}
			}
		case TokenState.ACCESS:
			cur.SetNode(new ASTNode(cur, TokenState.ACCESS, code.get(p).value));
			cur = cur.GetLastNode();
			return p+1;
		case TokenState.LEFTGENERIC:
			cur.SetNode(new ASTNode(cur, TokenState.GENERIC, "<>"));
			cur = cur.GetLastNode();
			brackets = 0;
			for (int i = p+1; i < code.size(); i++) {
				if (code.get(i).type == TokenState.SEPERATOR) {
					cur.SetNode(postSolve(new ASTNode(cur), p, i-1));
				}
				else if (code.get(i).type == TokenState.RIGHTGENERIC) {
					if (brackets == 0) {
						genericSolve(cur, p+1, i-1);
						cur = cur.prev;
						return i+1;
					}
					else brackets--;
					
				}
				else if (code.get(i).type == TokenState.LEFTGENERIC) {
					brackets++;
				}
			}
			return p+1;
		case TokenState.DEFINITION:
			cur.SetNode(new ASTNode(cur, TokenState.DEFINITION, code.get(p).value));
			cur = cur.GetLastNode();
			fieldCounter = 0;
			return p+1;
		case TokenState.CLASSMODIFIER:
			cur.SetNode(new ASTNode(cur, TokenState.CLASSMODIFIER, code.get(p+1).value));
			return p+2;
		case TokenState.DECLARATION:
			int lev;
			ASTNode node = null;
			if (code.get(p+1).type == TokenState.LEFTGENERIC) {
				node = new ASTNode(TokenState.GENERIC, "<>");
				lev = p+2;
				for (int i = p+2; i < code.size(); i++) {
					if (code.get(i).type == TokenState.SEPERATOR) {
						node = genericSolve(node, lev, i-1);
						lev = i;
					}
					else if (code.get(i).type == TokenState.RIGHTGENERIC) {
						node = genericSolve(node, lev, i-1);
						lev = i-p;
						break;
					}
				}
			}
			else {
				lev = 0;
			}
			if (code.get(p+lev+2).value.equals("(")) {
				cur.SetNode(new ASTNode(cur, TokenState.DESCRIPTION, code.get(p).value));
				cur = cur.GetLastNode();
				if (lev > 0) {
					node.prev = cur;
					cur.SetLast(node);
				}
				int genIndex = 0;
				for (int i = decide(p+lev+1)+1; i < code.size(); i++) {
					switch(code.get(i).type) {
					case TokenState.LEFTGENERIC:
						genIndex = i+1;
						break;
					case TokenState.RIGHTGENERIC:
						cur.SetLast(genericSolve(new ASTNode(cur, TokenState.GENERIC, "<>"), genIndex, i-1));
						genIndex = 0;
						break;
					case TokenState.RIGHTBRACKET:
						while (cur.type != TokenState.DESCRIPTION) {
							cur = cur.prev;
						}
						if (code.get(i+1).type != TokenState.LEFTCURLY) {
							
							cur.SetLast(new ASTNode(cur, TokenState.START, ";"));
							cur = cur.GetLastNode();
						}
						return i+1;
					case TokenState.DECLARATION:
						if (genIndex == 0) {
							cur.SetNode(new ASTNode(cur, TokenState.DECLARATION, code.get(i).value));
							cur = cur.GetLastRealNode();
						}
						break;
					case TokenState.LEFTBRACE:
						cur.value += "[]";
						break;
					case TokenState.IDENTIFIER:
						if (genIndex == 0) {
							cur.SetNode(new ASTNode(cur, TokenState.IDENTIFIER, code.get(i).value));
						}
						break;
					case TokenState.SEPERATOR:
						if (genIndex == 0) {
							cur = cur.prev;
						}
						break;
					}
				}
				
			}
			else if ((!Objects.isNull(cur.prev)) && cur.prev.type == TokenState.DEFINITION) {
					cur.PlaceNode(new ASTNode(cur, TokenState.DECLARATION, code.get(p).value), fieldCounter);
					cur = cur.GetNode(fieldCounter);
					if (lev > 0) {
						cur.SetLast(node);
					}
					fieldCounter++;
			}
			else {
				cur.SetNode(new ASTNode(cur, TokenState.DECLARATION, code.get(p).value));
				cur = cur.GetLastNode();
				if (lev > 0) {
					cur.SetLast(node);
				}
			}
			
			return p+1+lev;
		case TokenState.LEFTBRACE:
			switch(cur.type) {
			case TokenState.DECLARATION, TokenState.DESCRIPTION:
				cur.value = cur.value + "[]";
				return p+2;
			default:
				//
			}
			break;
		case TokenState.EQUIVALENCY :
			for (int i = p; i < code.size(); i++) {
				if (code.get(i).type == TokenState.ENDOFLINE || code.get(i).type == TokenState.LEFTCURLY) {
					cur.SetNode(postSolve(new ASTNode(cur), p, i-1));
					return i;
				}
			}
			break;
		case TokenState.CONDITIONAL:
			if (code.get(p).value.equals("if")) {
				cur.SetNode(new ASTNode(cur, TokenState.CONDITIONAL, "if"));
				cur = cur.GetLastNode();
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type == TokenState.LEFTCURLY) {
						cur.SetNode(postSolve(new ASTNode(cur), p, i-1));
						return i;
					}
				}
			}
			else {
				cur.SetNode(new ASTNode(cur, TokenState.CONDITIONAL, "else"));
				cur = cur.GetLastNode();
				return p+1;
			}
			
		case TokenState.LOOP:
			if (code.get(p).value.equals("while")) {
				cur.SetNode(new ASTNode(cur, TokenState.LOOP, "while"));
				cur = cur.GetLastNode();
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type == TokenState.LEFTCURLY) {
						cur.SetNode(postSolve(new ASTNode(cur), p-1, i-1));
						return i;
					}
				}
			}
			else {
				cur.SetNode(new ASTNode(cur, TokenState.LOOP, "for"));
				cur = cur.GetLastNode();
				boolean flag = false;
				int sec = 0;
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type == TokenState.ENDOFLINE) {
						if(flag) {
							cur.SetNode(postSolve(new ASTNode(cur), decide(decide(decide(decide(p+1))))-1, i-1));
							sec = i;
							;
						}
						else {
							flag = true;
						}
						
					}
					else if (flag && code.get(i).type == TokenState.LEFTCURLY) {
						cur.SetNode(postSolve(new ASTNode(cur), sec, i-1));
						return i;
					}
				}
				
				
				
				return p+1;
			}	
		case TokenState.IDENTIFIER:
			switch (cur.type) {
			case TokenState.DECLARATION, TokenState.DEFINITION, TokenState.DESCRIPTION:
				cur.SetNode(new ASTNode(cur, TokenState.IDENTIFIER, code.get(p).value));
				return p+1;
			default:
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type == TokenState.ENDOFLINE || code.get(i).type == TokenState.LEFTCURLY || code.get(i).type == TokenState.EQUIVALENCY) {
						cur.SetNode(preSolve(new ASTNode(cur), p-1, i-1));
						cur = cur.GetLastNode();
						return i;
					}
					
				}
			}
		case TokenState.ENDOFLINE:
			if (cur.type == TokenState.START && cur.value.equals(";")) {
				cur.SetNode(new ASTNode(cur, TokenState.END, "}"));
				cur = cur.prev.prev;
				if (cur.type == TokenState.ACCESS) {
					cur = cur.prev;
				}
				return p+1;
			}
			cur = cur.prev;
			
			if (cur.type == TokenState.ACCESS) {
				cur = cur.prev;
			}
			return p+1;
		case TokenState.LEFTCURLY:
			if (!(cur.type == TokenState.LOOP || cur.type == TokenState.CONDITIONAL || cur.type == TokenState.DEFINITION || cur.type == TokenState.DESCRIPTION)) {
				cur = cur.prev;
			}
			cur.SetLast(new ASTNode(cur, TokenState.START, "{"));
			cur = cur.GetLastNode();
			return p+1;
		case TokenState.RIGHTCURLY:
			cur.SetNode(new ASTNode(cur, TokenState.END, "}"));
			cur = cur.prev.prev;
			if (cur.type == TokenState.ACCESS) {
				cur = cur.prev;
			}
			return p+1;
	
		}
	return -69;
	}
	
	ASTNode parse() {
		int next = 0;
		while(code.size() > next) {
			next = decide(next);
		}
		
		return parent;
	}
	
}
