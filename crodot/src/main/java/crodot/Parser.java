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
	private int lineNum;
	private ErrorThrower err;
	
	int Start = 0;
	Parser(ArrayList<Token> code, ErrorThrower err) {
		parent = new ASTNode(TokenState.CODE, "Code", -1);
		cur = parent;
		this.code = code;
		this.lineNum = 0;
		this.err = err;
	}
	
	//inclusive inclusive
	ASTNode genericSolve(ASTNode tree, int start, int end) { 
//		if (end - start > 0) {
//			final int two = end-1;
//			
//			if (code.get(two).type == TokenState.CLASSMODIFIER) {
//				tree.SetNode(new ASTNode(tree, TokenState.CLASSMODIFIER, code.get(two).value, lineNum));
//				genericSolve(tree.GetFirstNode(), start, start);
//				genericSolve(tree.GetFirstNode(), end, end);
//				return tree;
//			}
//		}
//		else if (end - start == 0) {
//			if (code.get(start).type == TokenState.IDENTIFIER) {
//				tree.SetNode(new ASTNode(tree, TokenState.CLASSNAME, code.get(start).value, lineNum));
//			}
//			else if (code.get(start).type == TokenState.INFERRED) {
//				tree.SetNode(new ASTNode(tree, TokenState.CLASSNAME, "?", lineNum));
//			}
//			return tree;
//		}
		int low = start;
		int high;
		ASTNode temp;
		int sizemin1;
		int brack = 0;
		for (int i = start; i < end+1; i++) {
			if (code.get(i).type == TokenState.LEFTGENERIC) {
				brack++;
			}
			else if (code.get(i).type == TokenState.RIGHTGENERIC) {
				brack--;
			}
			if (code.get(i).type == TokenState.SEPERATOR && brack == 0) {
				high = i-1;
				sizemin1 = high - low;
				if (sizemin1 < 0) {

				}
				else if (code.get(low).type == TokenState.INFERRED) {
					if (sizemin1 == 0) {
						tree.SetNode(new ASTNode(tree, TokenState.INFERRED, "?", lineNum));
					} 
					else if (code.get(low+1).type == TokenState.CLASSMODIFIER) {
						temp = new ASTNode(tree, TokenState.CLASSMODIFIER, code.get(low+1).value, lineNum);
						tree.SetNode(temp);
						temp.SetNode(new ASTNode(temp, TokenState.INFERRED,  "?", lineNum));
						genericSolve(temp, low+2, high);
					}
					else {
						//err
						
					}
				}
				else if (code.get(low).type == TokenState.IDENTIFIER) {
					tree.SetNode(temp = new ASTNode(tree, TokenState.CLASSNAME, code.get(low).value, lineNum));
					if (sizemin1 > 0) {
						temp.SetNode(genericSolve(new ASTNode(temp, TokenState.GENERIC, "<>", lineNum), low+1, high-1));
					}
				} 
				else {
					//err.throwError("Invalid generic type", code.get(start).lineNum);
					
				}
				low = i+1;
			}
		}
		sizemin1 = end - low;
		if (sizemin1 < 0) {
			return tree;
		}
		else if (code.get(low).type == TokenState.INFERRED) {
			if (sizemin1 == 0) {
				tree.SetNode(new ASTNode(tree, TokenState.INFERRED, "?", lineNum));
			} 
			else if (code.get(low+1).type == TokenState.CLASSMODIFIER) {
				temp = new ASTNode(tree, TokenState.CLASSMODIFIER, code.get(low+1).value, lineNum);
				tree.SetNode(temp);
				temp.SetNode(new ASTNode(temp, TokenState.INFERRED,  "?", lineNum));
				genericSolve(temp, low+2, end);
			}
			else {
				//err
				
			}
			
		}
		else if (code.get(low).type == TokenState.IDENTIFIER) {
			tree.SetNode(temp = new ASTNode(tree, TokenState.CLASSNAME, code.get(low).value, lineNum));
			if (sizemin1 > 0) {
				temp.SetNode(genericSolve(new ASTNode(temp, TokenState.GENERIC, "<>", lineNum), low+2, end-1));
			}
		} 
		else {
			//err.throwError("Invalid generic type", code.get(start).lineNum);
			
		}
		return tree;
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
			tree.SetNode(preSolve(new ASTNode(tree, lineNum), start, place-1));
			if (tree.GetNodeSize() < 2) {
				tree.SetNode(preSolve(new ASTNode(tree, lineNum), place, end));
			}
			else {
				tree.GetNode(1).SetNode(preSolve(new ASTNode(tree.GetNode(1), lineNum), place, end));
			}
			return tree;
		case TokenState.INCREMENT, TokenState.DECREMENT:
			if (place == end) {
				tree.value = "E";
				ASTNode temp = preSolve(new ASTNode(lineNum), start, end-1);
				//ur gonna have to add to the dot or if no dot do smth else
				if (temp.type == TokenState.DOT) {
					ASTNode liltemp;
					liltemp = temp.GetNode(1);
					temp.prev = tree.prev;
					tree.prev = temp;
					tree.SetNode(liltemp);
					liltemp.prev = tree;
					temp.ForceSet(tree, 1);
					return temp;	
				}
				else {
					tree.SetNode(temp);
					temp.prev = tree;
					return tree;
				}
			}
			else if (place == start+1) {
				tree.value = "S";
				ASTNode temp = preSolve(new ASTNode(lineNum), start+2, end);
				//ur gonna have to add to the dot or if no dot do smth else
				if (temp.type == TokenState.DOT) {
					ASTNode liltemp;
					liltemp = temp.GetNode(1);
					temp.prev = tree.prev;
					tree.prev = temp;
					tree.SetNode(liltemp);
					liltemp.prev = tree;
					temp.ForceSet(tree, 1);
					return temp;	
				}
				else {
					tree.SetNode(temp);
					temp.prev = tree;
					return tree;
				}
				
//				tree.prev = temp2.prev;
//				tree.prev.replace(temp2, tree);
//				temp2.prev = tree;
//				tree.SetNode(temp2);
			}
			else {
				System.exit(-1);
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
						tree.SetNode(postSolve(new ASTNode(tree, lineNum), place+1, i-1));
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
						tree.SetNode(postSolve(new ASTNode(tree, lineNum), place+1, i-1));
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
			tree.type = TokenState.NONE;
			tree.value = "";
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
			if (code.get(i).type == TokenState.LEFTBRACKET || code.get(i).type == TokenState.LEFTBRACE || code.get(i).type == TokenState.LEFTGENERIC || code.get(i).type == TokenState.LEFTCAST) {
				brackets--;
			}
			else if (code.get(i).type == TokenState.RIGHTBRACKET || code.get(i).type == TokenState.RIGHTBRACE || code.get(i).type == TokenState.RIGHTGENERIC || code.get(i).type == TokenState.RIGHTCAST) {
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
			else if (code.get(place+1).type == TokenState.LEFTCAST) {
				code.get(place).type = TokenState.CAST;
			}
			else if (code.get(place+1).type == TokenState.LEFTGENERIC) {
				code.get(place).type = TokenState.GENFUN;
				
			}
		}
		tree.type = code.get(place).type;
		tree.value = code.get(place).value;
		
		switch (tree.type) {
		case TokenState.CAST:
			brackets = 0;
			for (int i = place + 2; i < end + 1; i++) {
				if (code.get(i).type == TokenState.LEFTCAST) {
					brackets++;
				}
				else if (code.get(i).type == TokenState.RIGHTCAST) {
					if (brackets == 0 ) {
						tree.SetNode(postSolve(new ASTNode(tree, lineNum), place+1, i-1));
						return tree;
					}
					else {
						brackets--;
					}
					
				}

			}
			return tree;
		case TokenState.EQUIVALENCY:
			tree.SetNode(preSolve(new ASTNode(tree, lineNum), start, place-1));
			tree.SetNode(postSolve(new ASTNode(tree, lineNum), place, end));
			return tree;
		case TokenState.ADD, TokenState.SUB, TokenState.MUL, TokenState.DIV, TokenState.REM, TokenState.EXP, TokenState.SEPERATOR, TokenState.TRUEEQUALS, TokenState.NOTEQUALS, TokenState.TRUEGREATERTHAN, TokenState.TRUELESSTHAN, TokenState.GREATERTHAN, TokenState.LESSTHAN, TokenState.IS:
			tree.SetNode(postSolve(new ASTNode(tree, lineNum), start, place-1));
			tree.SetNode(postSolve(new ASTNode(tree, lineNum), place, end));
			return tree;
		case TokenState.DOT:
			tree.SetNode(preSolve(new ASTNode(tree, lineNum), start-1, place-1));
			tree.SetNode(preSolve(new ASTNode(tree, lineNum), place, end));
			return tree;
		case TokenState.INCREMENT, TokenState.DECREMENT:
			if (place == end) {
				tree.value = "E";
				ASTNode temp = preSolve(new ASTNode(lineNum), start, end-1);
				//ur gonna have to add to the dot or if no dot do smth else
				if (temp.type == TokenState.DOT) {
					ASTNode liltemp;
					liltemp = temp.GetNode(1);
					temp.prev = tree.prev;
					tree.prev = temp;
					tree.SetNode(liltemp);
					liltemp.prev = tree;
					temp.ForceSet(tree, 1);
					return temp;	
				}
				else {
					tree.SetNode(temp);
					temp.prev = tree;
					return tree;
				}
			}
			else if (place == start+1) {
				tree.value = "S";
				ASTNode temp = preSolve(new ASTNode(lineNum), start+2, end);
				//ur gonna have to add to the dot or if no dot do smth else
				if (temp.type == TokenState.DOT) {
					ASTNode liltemp;
					liltemp = temp.GetNode(1);
					temp.prev = tree.prev;
					tree.prev = temp;
					tree.SetNode(liltemp);
					liltemp.prev = tree;
					temp.ForceSet(tree, 1);
					return temp;	
				}
				else {
					tree.SetNode(temp);
					temp.prev = tree;
					return tree;
				}
				
//				tree.prev = temp2.prev;
//				tree.prev.replace(temp2, tree);
//				temp2.prev = tree;
//				tree.SetNode(temp2);
			}
			else {
				System.exit(-1);
			}
			return tree;
		case TokenState.NOT:
			tree.SetNode(postSolve(new ASTNode(tree, lineNum), place, end));
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
						tree.SetNode(genericSolve(new ASTNode(tree, TokenState.GENERIC, "<>", lineNum), place+2, i-1));
						
						dis = i;
						break;
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
						tree.SetNode(postSolve(new ASTNode(tree, lineNum), dis+1, i-1));
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
						tree.SetNode(postSolve(new ASTNode(tree, lineNum), place+1, i-1));
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
					tree.SetNode(postSolve(new ASTNode(tree, lineNum), next, i-1));
					next = i;
				}
				else if (code.get(i).type == TokenState.LEFTBRACE) {
					brackets2++;
				}
				else if (code.get(i).type == TokenState.RIGHTBRACE) {
					brackets2--;
				}
				
			}
			if (next != end-1) {
				tree.SetNode(postSolve(new ASTNode(tree, lineNum), next, end-1));
			}
			return tree;
		case TokenState.ARR:
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type == TokenState.LEFTBRACE) {
					brackets++;
				}
				else if (code.get(i).type == TokenState.RIGHTBRACE) {
					if (brackets == 0) {
						tree.SetNode(postSolve(new ASTNode(tree, lineNum), place+1, i-1));
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
			else if (Long.parseLong(tree.value) > Integer.MAX_VALUE || Long.parseLong(tree.value) < Integer.MIN_VALUE ) {
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
		case TokenState.SUBDEFINITION:
			cur.SetNode(new ASTNode(cur, TokenState.SUBDEFINITION, code.get(p).value, lineNum));
			cur = cur.GetLastNode();
			fieldCounter = 0;
			return p+1;
			
		case TokenState.ACCDEF:
			if (cur.prev.type == TokenState.DEFINITION || cur.prev.type == TokenState.SUBDEFINITION) {
				code.get(p).type = TokenState.ACCESS;
			}
			else {
				code.get(p).type = TokenState.DEFINITION;
			}
			return decide(p);
		case TokenState.INCREMENT:
			for (int j = p+1; j < code.size(); j++) {
				if (code.get(j).type == TokenState.ENDOFLINE) {
					cur.SetNode(preSolve(new ASTNode(cur, lineNum), p-1, j-1));
					return j+1;
				}
			}
		case TokenState.IMPORT:
			cur.SetNode(new ASTNode(cur, TokenState.IMPORT, "import", lineNum));
			cur = cur.GetLastNode();
			cur.SetNode(new ASTNode(cur, TokenState.STRING, code.get(p+1).value, lineNum));
			return p+2;
		case TokenState.RETURN:
			cur.SetNode(new ASTNode(cur, TokenState.RETURN, code.get(p).value, lineNum));
			cur = cur.GetLastNode();
			for (int i = p; i < code.size(); i++) {
				if (code.get(i).type == TokenState.ENDOFLINE || code.get(i).type == TokenState.LEFTCURLY) {
					cur.SetNode(postSolve(new ASTNode(cur, lineNum), p, i-1));
					return i;
				}
			}
		case TokenState.ACCESS:
			cur.SetNode(new ASTNode(cur, TokenState.ACCESS, code.get(p).value, lineNum));
			cur = cur.GetLastNode();
			return p+1;
		case TokenState.LEFTGENERIC:
			cur.SetNode(new ASTNode(cur, TokenState.GENERIC, "<>", lineNum));
			cur = cur.GetLastNode();
			brackets = 0;
			for (int i = p+1; i < code.size(); i++) {
				if (code.get(i).type == TokenState.SEPERATOR) {
					cur.SetNode(postSolve(new ASTNode(cur, lineNum), p, i-1));
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
			cur.SetNode(new ASTNode(cur, TokenState.DEFINITION, code.get(p).value, lineNum));
			cur = cur.GetLastNode();
			fieldCounter = 0;
			return p+1;
		case TokenState.CLASSMODIFIER:
			cur.SetNode(new ASTNode(cur, TokenState.CLASSMODIFIER, code.get(p+1).value, lineNum));
			return p+2;
		case TokenState.DECLARATION:
			int lev;
			ASTNode node = null;
			int brack = 0;
			if (code.get(p+1).type == TokenState.LEFTGENERIC) {
				node = new ASTNode(TokenState.GENERIC, "<>", lineNum);
				lev = p+2;
				for (int i = p+2; i < code.size(); i++) {
					if (code.get(i).type == TokenState.LEFTGENERIC) {
						brack++;
					}
					else if (code.get(i).type == TokenState.RIGHTGENERIC) {
						if (brack == 0) {
							node = genericSolve(node, lev, i-1);
							lev = i-p;
							break;
						}
						else {
                            brack--;
                        }
					}
				}
			}
			else {
				lev = 0;
			}
			if (code.get(p+lev+2).type == TokenState.LEFTBRACKET) {
				cur.SetNode(new ASTNode(cur, TokenState.DESCRIPTION, code.get(p).value, lineNum));
				cur.debugCheck();
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
						cur.SetLast(genericSolve(new ASTNode(cur, TokenState.GENERIC, "<>", lineNum), genIndex, i-1));
						genIndex = 0;
						break;
					case TokenState.RIGHTBRACKET:
						while (cur.type != TokenState.DESCRIPTION) {
							cur = cur.prev;
						}
//						if (code.get(i+1).type != TokenState.LEFTCURLY) {
//							
//							cur.SetLast(new ASTNode(cur, TokenState.START, ";", lineNum));
//							cur = cur.GetLastNode();
//						}
						return i+1;
					case TokenState.DECLARATION:
						if (genIndex == 0) {
							cur.SetNode(new ASTNode(cur, TokenState.DECLARATION, code.get(i).value, lineNum));
							cur = cur.GetLastRealNode();
						}
						break;
					case TokenState.LEFTBRACE:
						cur.value += "[]";
						break;
					case TokenState.IDENTIFIER:
						if (genIndex == 0) {
							cur.SetNode(new ASTNode(cur, TokenState.IDENTIFIER, code.get(i).value, lineNum));
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
			else if ((!Objects.isNull(cur.prev)) && (cur.prev.type == TokenState.DEFINITION || cur.prev.type == TokenState.SUBDEFINITION)) {
					cur.PlaceNode(new ASTNode(cur, TokenState.DECLARATION, code.get(p).value, lineNum), fieldCounter);
					cur = cur.GetNode(fieldCounter);
					if (lev > 0) {
						cur.SetLast(node);
					}
					fieldCounter++;
			}
			else {
				cur.SetNode(new ASTNode(cur, TokenState.DECLARATION, code.get(p).value, lineNum));
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
			if (!code.get(p).value.equals("=")) {
				ASTNode parent = cur.prev;
				ASTNode newOne = new ASTNode(parent, TokenState.EQUIVALENCY, code.get(p).value, lineNum);
				parent.remove(cur);
				parent.SetNode(newOne);
				cur.prev = newOne;
				newOne.SetNode(cur);
				cur = newOne;
			}
			for (int i = p; i < code.size(); i++) {
				if (code.get(i).type == TokenState.ENDOFLINE || code.get(i).type == TokenState.LEFTCURLY) {
					cur.SetNode(postSolve(new ASTNode(cur, lineNum), p, i-1));
					return i;
				}
			}
			break;
		case TokenState.CONDITIONAL:
			if (code.get(p).value.equals("if")) {
				cur.SetNode(new ASTNode(cur, TokenState.CONDITIONAL, "if", lineNum));
				cur = cur.GetLastNode();
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type == TokenState.LEFTCURLY) {
						cur.SetNode(postSolve(new ASTNode(cur, lineNum), p, i-1));
						return i;
					}
				}
			}
			else if (code.get(p).value.equals("elif")) {
				cur.SetNode(new ASTNode(cur, TokenState.CONDITIONAL, "elif", lineNum));
				cur = cur.GetLastNode();
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type == TokenState.LEFTCURLY) {
						cur.SetNode(postSolve(new ASTNode(cur, lineNum), p, i-1));
						return i;
					}
				}
			}
			else {
				cur.SetNode(new ASTNode(cur, TokenState.CONDITIONAL, "else", lineNum));
				cur = cur.GetLastNode();
				return p+1;
			}
			
		case TokenState.LOOP:
			if (code.get(p).value.equals("while")) {
				cur.SetNode(new ASTNode(cur, TokenState.LOOP, "while", lineNum));
				cur = cur.GetLastNode();
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type == TokenState.LEFTCURLY) {
						cur.SetNode(postSolve(new ASTNode(cur, lineNum), p-1, i-1));
						return i;
					}
				}
			}
			else {
				cur.SetNode(new ASTNode(cur, TokenState.LOOP, "for", lineNum));
				cur = cur.GetLastNode();
				boolean flag = false;
				int sec = 0;
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type == TokenState.ENDOFLINE) {
						if(flag) {
							int dobre2 = decide(p+1);
							int dobre;
							if (cur.type == TokenState.DECLARATION) {
								 dobre = decide(decide(decide(dobre2)))-1;
							}
							else {
								dobre = decide(decide(dobre2))-1;
							}
							
							cur.SetNode(postSolve(new ASTNode(cur, lineNum), dobre, i-1));
							sec = i;
			
						}
						else {
							flag = true;
						}
						
					}
					else if (flag && code.get(i).type == TokenState.LEFTCURLY) {
						cur.SetNode(postSolve(new ASTNode(cur, lineNum), sec, i-1));
						return i;
					}
				}
				
				
				
				return p+1;
			}	
		case TokenState.IDENTIFIER:


			switch (cur.type) {
			case TokenState.DECLARATION, TokenState.DEFINITION, TokenState.DESCRIPTION, TokenState.SUBDEFINITION:	
				cur.SetNode(new ASTNode(cur, TokenState.IDENTIFIER, code.get(p).value, lineNum));

				return p+1;
			default:
				int b = 0;
				for (int j = p+1; j < code.size(); j++) {
					if (code.get(j).type == TokenState.LEFTBRACKET) {
						b++;
					}
					else if (code.get(j).type == TokenState.RIGHTBRACKET) {
						b--;
					}
					else if (b == 0 && (code.get(j).type == TokenState.ENDOFLINE || code.get(j).type == TokenState.EQUIVALENCY)) {
						cur.SetNode(preSolve(new ASTNode(cur, lineNum), p-1, j-1));
						cur = cur.GetLastNode();

						return j;
					}
					else if (code.get(j).type == TokenState.LEFTCURLY) {
						cur.SetNode(new ASTNode(cur, TokenState.DESCRIPTION, code.get(p).value, lineNum));
						cur.debugCheck();
						cur = cur.GetLastNode();
						
						int genIndex = 0;
						int genBrack = 0;
						for (int i = decide(p)+1; i < code.size(); i++) {


							switch(code.get(i).type) {
							case TokenState.LEFTGENERIC:
								genIndex = i+1;
								genBrack++;
								break;
							case TokenState.RIGHTGENERIC:
								if (genBrack == 0) {
									cur.SetLast(genericSolve(new ASTNode(cur, TokenState.GENERIC, "<>", lineNum), genIndex, i-1));
									genIndex = 0;
								}
								else {
									genBrack--;
								}
								
								break;
							case TokenState.RIGHTBRACKET:
								while (cur.type != TokenState.DESCRIPTION) {
									cur = cur.prev;
								}
								if (code.get(i+1).type != TokenState.LEFTCURLY) {
									
									cur.SetLast(new ASTNode(cur, TokenState.START, ";", lineNum));
									cur = cur.GetLastNode();
								}
								return i+1;
							case TokenState.DECLARATION:
								if (genIndex == 0) {
									cur.SetNode(new ASTNode(cur, TokenState.DECLARATION, code.get(i).value, lineNum));
									cur = cur.GetLastRealNode();
								}
								break;
							case TokenState.LEFTBRACE:
								cur.value += "[]";
								break;
							case TokenState.IDENTIFIER:
								if (genIndex == 0) {
									cur.SetNode(new ASTNode(cur, TokenState.IDENTIFIER, code.get(i).value, lineNum));
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
					
				}
			}
		case TokenState.ENDOFLINE:
			if (code.get(p - 1).type == TokenState.RIGHTCURLY) {
				return p + 1;
			}
			if (cur.type == TokenState.START && cur.value.equals(";")) {
				cur.SetNode(new ASTNode(cur, TokenState.END, "}", lineNum));
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
			if (!(cur.type == TokenState.LOOP || cur.type == TokenState.CONDITIONAL || cur.type == TokenState.DEFINITION || cur.type == TokenState.DESCRIPTION || cur.type == TokenState.SUBDEFINITION || cur.type == TokenState.START || cur.type == TokenState.SUBDEFINITION)) {
				cur = cur.prev;
			}
			cur.SetLast(new ASTNode(cur, TokenState.START, "{", lineNum));
			cur = cur.GetLastNode();
			return p+1;
		case TokenState.RIGHTCURLY:
			cur.SetNode(new ASTNode(cur, TokenState.END, "}", lineNum));
			cur = cur.prev.prev;
			if (cur.type == TokenState.ACCESS) {
				cur = cur.prev;
			}
			return p+1;
		case TokenState.NEXTLINE:
			lineNum++;
			return p+1;
		default :
			return -69;
		
		}
		return -34;
	
	}
	
	ASTNode parse() {
		int next = 0;
		
		while(code.size() > next) {
			//System.out.println(next);
			next = decide(next);
			
			
		}
		
		return parent;
	}
	
}
