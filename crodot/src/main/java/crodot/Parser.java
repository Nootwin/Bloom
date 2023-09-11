package crodot;

import java.util.ArrayList;
import java.util.Objects;

public class Parser {
	private ASTNode parent;
	private int fieldCounter;
	private ASTNode cur;
	private int brackets;
	private ArrayList<token> code;
	private final String[][] preOrder = {{"DOT", "DOT"}, {"IDENTIFIER", "VAR"}, {"RIGHTBRACKET", "BRACKET"}};
	private final String[][] postOrder = {{"SEPERATOR", "SEP"}, {"TRUEEQUALS", "TRUEEQUALS"}, {"NOTEQUALS", "NOTEQUALS"}, {"TRUEGREATERTHAN", "TRUEGREATERTHAN"}, {"TRUELESSTHAN", "TRUELESSTHAN"}, {"LESSTHAN", "LESSTHAN"}, {"GREATERTHAN", "GREATERTHAN"}, {"ADD", "ADD"}, {"SUB", "SUB"}, {"MUL", "MUL"}, {"DIV", "DIV"}, {"REM", "REM"}, {"EXP", "EXP"}, {"DOT", "DOT"}, {"IDENTIFIER", "VAR"}, {"DECLARATION", "VAR"}, {"NUMBER", "NUM"}, {"STRING", "Ljava/lang/String;"}, {"CHAR", "C"}, {"BOOLEAN", "Z"}, {"RIGHTBRACE", "BRACE"}, {"RIGHTBRACKET", "BRACKET"}};
	
	int Start = 0;
	Parser(ArrayList<token> code) {
		parent = new ASTNode("CODE", "Code");
		cur = parent;
		this.code = code;
	}
	
	//inclusive inclusive
	ASTNode genericSolve(ASTNode tree, int start, int end) { 
		if (end - start > 0) {
			final int two = end-1;
			
			System.out.println(code.get(two).value);
			if (code.get(two).type.equals("CLASSMODIFIER")) {
				tree.SetNode(new ASTNode(tree, "CLASSMODIFIER", code.get(two).value));
				genericSolve(tree.GetFirstNode(), start, start);
				genericSolve(tree.GetFirstNode(), end, end);
				return tree;
			}
		}
		else if (end - start == 0) {
			if (code.get(start).type.equals("IDENTIFIER")) {
				tree.SetNode(new ASTNode(tree, "CLASSNAME", code.get(start).value));
			}
			else if (code.get(start).type.equals("INFERRED")) {
				tree.SetNode(new ASTNode(tree, "CLASSNAME", "?"));
			}
			return tree;
		}
		return null;
	}
	
	
	ASTNode preSolve(ASTNode tree, int start, int end) { 
		int index = preOrder.length;
		int place = end;
		brackets = 0;
		
		for (int i = end; i > start; i--) {
			if (brackets == 0) {
				for (int j = 0; j < index; j++) {
					if (code.get(i).type.equals(preOrder[j][0])) {
						index = j;
						place = i;
						tree.type = preOrder[j][1];
						tree.value = code.get(i).value;
						if (tree.type.equals("VAR")) {
							if (code.get(i+1).type.equals("LEFTBRACKET")) {
								tree.type = "FUN";
							}
							if (code.get(i+1).type.equals("LEFTBRACE")) {
								tree.type = "ARR";
							}
						}
						break;
					}
				}
			}
			if (code.get(i).type.equals("LEFTBRACKET") || code.get(i).type.equals("LEFTBRACE")) {
				brackets--;
			}
			else if (code.get(i).type.equals("RIGHTBRACKET")|| code.get(i).type.equals("RIGHTBRACE")) {
				brackets++;
			}
		}
		switch (tree.type) {
		case "DOT":
			tree.SetNode(preSolve(new ASTNode(tree), start-1, place-1));
			tree.SetNode(preSolve(new ASTNode(tree), place, end));
			return tree;
		case "FUN":
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type.equals("LEFTBRACKET")) {
					brackets++;
				}
				else if (code.get(i).type.equals("RIGHTBRACKET")) {
					if (brackets == 0) {
						tree.SetNode(postSolve(new ASTNode(tree), place+1, i-1));
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case "RIGHTBRACKET":
			return postSolve(tree, start, end-1);
		case "ARR":
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type.equals("LEFTBRACE")) {
					brackets++;
				}
				else if (code.get(i).type.equals("RIGHTBRACE")) {
					if (brackets == 0) {
						tree.SetNode(postSolve(new ASTNode(tree), place+1, i-1));
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case "VAR":
			return tree;
		}
		
		return tree;
	}
	
	//start exclusive, end inclusive
	ASTNode postSolve(ASTNode tree, int start, int end) {
		int index = postOrder.length;
		int place = end;
		brackets = 0;
		if (end - start < 1) {
			tree.type = "NULL";
			tree.value = "null";
			return tree;
		}
		for (int i = end; i > start; i--) {
			
			System.out.println("BRACERS   " + code.get(i).value  + "   " + code.get(i).type + postOrder.length);
			if (brackets == 0) {
				for (int j = 0; j < index; j++) {
					if (code.get(i).type.equals(postOrder[j][0])) {
						
						index = j;
						place = i;
						if (index > 1 && index < 8) {
							index = 1;
						}
						else if (index > 7 && index < 14) {
							index = 8;
						}
						else if (index > 14 && index < 20) {
							index = 14;
						}
						
						//mark down if add or mul
						tree.type = postOrder[j][1];
						tree.value = code.get(i).value;
						if (tree.type.equals("VAR")) {
							if (code.get(i+1).type.equals("LEFTBRACKET")) {
								tree.type = "FUN";
							}
							else if (code.get(i+1).type.equals("LEFTBRACE")) {
								tree.type = "ARR";
							}
							else if (code.get(i+1).type.equals("LEFTGENERIC")) {
								tree.type = "GENFUN";
								
							}
						}
						break;
					}
				}
			}
			if (code.get(i).type.equals("LEFTBRACKET") || code.get(i).type.equals("LEFTBRACE") || code.get(i).type.equals("LEFTGENERIC")) {
				brackets--;
			}
			else if (code.get(i).type.equals("RIGHTBRACKET")|| code.get(i).type.equals("RIGHTBRACE") || code.get(i).type.equals("RIGHTGENERIC")) {
				brackets++;
			}
		}
		System.out.println("SELECTED   " + tree.value);
		switch (tree.type) {
		case "ADD", "SUB", "MUL", "DIV", "REM", "EXP", "SEP", "TRUEEQUALS", "NOTEQUALS", "TRUEGREATERTHAN", "TRUELESSTHAN", "GREATERTHAN", "LESSTHAN":
			tree.SetNode(postSolve(new ASTNode(tree), start, place-1));
			tree.SetNode(postSolve(new ASTNode(tree), place, end));
			return tree;
		case "DOT":
			tree.SetNode(preSolve(new ASTNode(tree), start-1, place-1));
			tree.SetNode(preSolve(new ASTNode(tree), place, end));
			return tree;
		case "GENFUN":
			brackets = 0;
			int dis=0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type.equals("LEFTGENERIC")) {
					brackets++;
				}
				else if (code.get(i).type.equals("RIGHTGENERIC")) {
					if (brackets == 0) {	
						tree.SetNode(genericSolve(new ASTNode(tree, "GENERIC", "<>"), place+2, i-1));
						dis = i;
					}
					else {
						brackets--;
					}
					
				}
				
			}
			for (int i = dis+2; i < end+1; i++) {
				if (code.get(i).type.equals("LEFTBRACKET")) {
					brackets++;
				}
				else if (code.get(i).type.equals("RIGHTBRACKET")) {
					if (brackets == 0) {	
						tree.SetNode(postSolve(new ASTNode(tree), dis+1, i-1));
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case "FUN":
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type.equals("LEFTBRACKET")) {
					brackets++;
				}
				else if (code.get(i).type.equals("RIGHTBRACKET")) {
					if (brackets == 0) {	
						tree.SetNode(postSolve(new ASTNode(tree), place+1, i-1));
					}
					else {
						brackets--;
					}
					
				}
				
			}
			return tree;
		case "BRACKET":
			return postSolve(tree, start, end-1);
		case "BRACE":
			int brackets2 = 0;
			int next = start+1;
			for (int i = start+2; i < end+1; i++) {
				if (code.get(i).type.equals("SEPERATOR") && brackets2 == 0) {
					tree.SetNode(postSolve(new ASTNode(tree), next, i-1));
					next = i;
				}
				else if (code.get(i).type.equals("LEFTBRACE")) {
					brackets2++;
				}
				else if (code.get(i).type.equals("RIGHTBRACE")) {
					brackets2--;
				}
				
			}
			tree.SetNode(postSolve(new ASTNode(tree), next, end-1));
			return tree;
		case "ARR":
			brackets = 0;
			for (int i = place+2; i < end+1; i++) {
				if (code.get(i).type.equals("LEFTBRACE")) {
					brackets++;
				}
				else if (code.get(i).type.equals("RIGHTBRACE")) {
					if (brackets == 0) {
						tree.SetNode(postSolve(new ASTNode(tree), place+1, i-1));
						if (!code.get(i+1).type.equals("LEFTBRACE")) {
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
		case "NUM":
			if (tree.value.contains(".")) {
				tree.type = "D";
			}
			else if (Long.parseLong(tree.value) > Integer.MAX_VALUE && Long.parseLong(tree.value) < Integer.MIN_VALUE ) {
				tree.type = "L";
			}
			else {
				tree.type = "I";
			}
			return tree;
		case "Ljava/lang/String;", "Z", "VAR", "C":
			return tree;
		}
		
		return tree;
	}

	
	
	
	int decide(int p) {
		switch(code.get(p).type) {
		case "ACCDEF":
			if (cur.prev.type.equals("DEFINITION")) {
				code.get(p).type = "ACCESS";
			}
			else {
				code.get(p).type = "DEFINITION";
			}
			return decide(p);
		case "IMPORT":
			cur.SetNode(new ASTNode(cur, "IMPORT", "import"));
			cur = cur.GetLastNode();
			cur.SetNode(new ASTNode(cur, "Ljava/lang/String;", code.get(p+1).value));
			return p+2;
		case "RETURN":
			cur.SetNode(new ASTNode(cur, "RETURN", code.get(p).value));
			cur = cur.GetLastNode();
			for (int i = p; i < code.size(); i++) {
				if (code.get(i).type.equals("ENDOFLINE") || code.get(i).type.equals("LEFTCURLY")) {
					cur.SetNode(postSolve(new ASTNode(cur), p, i-1));
					return i;
				}
			}
		case "ACCESS":
			cur.SetNode(new ASTNode(cur, "ACCESS", code.get(p).value));
			cur = cur.GetLastNode();
			return p+1;
		case "LEFTGENERIC":
			cur.SetNode(new ASTNode(cur, "GENERIC", "<>"));
			cur = cur.GetLastNode();
			brackets = 0;
			for (int i = p+1; i < code.size(); i++) {
				if (code.get(i).type.equals("SEPERATOR")) {
					cur.SetNode(postSolve(new ASTNode(cur), p, i-1));
				}
				else if (code.get(i).type.equals("RIGHTGENERIC")) {
					if (brackets == 0) {
						genericSolve(cur, p+1, i-1);
						cur = cur.prev;
						return i+1;
					}
					else brackets--;
					
				}
				else if (code.get(i).type.equals("LEFTGENERIC")) {
					brackets++;
				}
			}
			return p+1;
		case "DEFINITION":
			cur.SetNode(new ASTNode(cur, "DEFINITION", code.get(p).value));
			cur = cur.GetLastNode();
			fieldCounter = 0;
			return p+1;
		case "CLASSMODIFIER":
			cur.SetNode(new ASTNode(cur, "PARENT", code.get(p+1).value));
			return p+2;
		case "DECLARATION":
			int lev;
			ASTNode node = null;
			if (code.get(p+1).type.equals("LEFTGENERIC")) {
				node = new ASTNode("GENERIC", "<>");
				lev = p+2;
				for (int i = p+2; i < code.size(); i++) {
					if (code.get(i).type.equals("SEPERATOR")) {
						node = genericSolve(node, lev, i-1);
						lev = i;
					}
					else if (code.get(i).type.equals("RIGHTGENERIC")) {
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
				cur.SetNode(new ASTNode(cur, "DESCRIPTION", code.get(p).value));
				cur = cur.GetLastNode();
				if (lev > 0) {
					node.prev = cur;
					cur.SetLast(node);
				}
				int genIndex = 0;
				for (int i = decide(p+lev+1)+1; i < code.size(); i++) {
					switch(code.get(i).type) {
					case "LEFTGENERIC":
						genIndex = i+1;
						break;
					case "RIGHTGENERIC":
						cur.SetLast(genericSolve(new ASTNode(cur, "GENERIC", "<>"), genIndex, i-1));
						genIndex = 0;
						break;
					case "RIGHTBRACKET":
						while (!cur.type.equals("DESCRIPTION")) {
							cur = cur.prev;
						}
						return i+1;
					case "DECLARATION":
						if (genIndex == 0) {
							cur.SetNode(new ASTNode(cur, "DECLARATION", code.get(i).value));
							cur = cur.GetLastRealNode();
						}
						break;
					case "LEFTBRACE":
						cur.value += "[]";
						break;
					case "IDENTIFIER":
						if (genIndex == 0) {
							cur.SetNode(new ASTNode(cur, "IDENTIFIER", code.get(i).value));
						}
						break;
					case "SEPERATOR":
						if (genIndex == 0) {
							cur = cur.prev;
						}
						break;
					}
				}
				
			}
			else if ((!Objects.isNull(cur.prev)) && cur.prev.type.equals("DEFINITION")) {
					cur.PlaceNode(new ASTNode(cur, "DECLARATION", code.get(p).value), fieldCounter);
					cur = cur.GetNode(fieldCounter);
					if (lev > 0) {
						cur.SetLast(node);
					}
					fieldCounter++;
			}
			else {
				cur.SetNode(new ASTNode(cur, "DECLARATION", code.get(p).value));
				cur = cur.GetLastNode();
				if (lev > 0) {
					cur.SetLast(node);
				}
			}
			
			return p+1+lev;
		case "LEFTBRACE":
			switch(cur.type) {
			case "DECLARATION", "DESCRIPTION":
				System.out.println("YEAAAA" + code.get(p).value);
				cur.value = cur.value + "[]";
				return p+2;
			default:
				System.out.println("NEAAAA" + code.get(p).value);
				//
			}
			break;
		case "EQUIVALENCY" :
			for (int i = p; i < code.size(); i++) {
				if (code.get(i).type.equals("ENDOFLINE") || code.get(i).type.equals("LEFTCURLY")) {
					cur.SetNode(postSolve(new ASTNode(cur), p, i-1));
					return i;
				}
			}
			break;
		case "CONDITIONAL":
			if (code.get(p).value.equals("if")) {
				cur.SetNode(new ASTNode(cur, "CONDITIONAL", "if"));
				cur = cur.GetLastNode();
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type.equals("LEFTCURLY")) {
						cur.SetNode(postSolve(new ASTNode(cur), p, i-1));
						return i;
					}
				}
			}
			else {
				cur.SetNode(new ASTNode(cur, "CONDITIONAL", "else"));
				cur = cur.GetLastNode();
				return p+1;
			}
			
		case "LOOP":
			if (code.get(p).value.equals("while")) {
				cur.SetNode(new ASTNode(cur, "LOOP", "while"));
				cur = cur.GetLastNode();
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type.equals("LEFTCURLY")) {
						cur.SetNode(postSolve(new ASTNode(cur), p-1, i-1));
						return i;
					}
				}
			}
			else {
				cur.SetNode(new ASTNode(cur, "LOOP", "for"));
				cur = cur.GetLastNode();
				boolean flag = false;
				int sec = 0;
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type.equals("ENDOFLINE")) {
						if(flag) {
							cur.SetNode(postSolve(new ASTNode(cur), decide(decide(decide(decide(p+1))))-1, i-1));
							sec = i;
							;
						}
						else {
							flag = true;
						}
						
					}
					else if (flag && code.get(i).type.equals("LEFTCURLY")) {
						cur.SetNode(postSolve(new ASTNode(cur), sec, i-1));
						return i;
					}
				}
				
				
				
				return p+1;
			}	
		case "IDENTIFIER":
			switch (cur.type) {
			case "DECLARATION", "DEFINITION", "DESCRIPTION":
				cur.SetNode(new ASTNode(cur, "IDENTIFIER", code.get(p).value));
				return p+1;
			default:
				for (int i = p; i < code.size(); i++) {
					if (code.get(i).type.equals("ENDOFLINE") || code.get(i).type.equals("LEFTCURLY") || code.get(i).type.equals("EQUIVALENCY")) {
						cur.SetNode(preSolve(new ASTNode(cur), p-1, i-1));
						cur = cur.GetLastNode();
						return i;
					}
					
				}
			}
		case "ENDOFLINE":
			cur = cur.prev;
			if (cur.type.equals("ACCESS")) {
				cur = cur.prev;
			}
			return p+1;
		case "LEFTCURLY":
			if (!(cur.type.equals("LOOP") || cur.type.equals("CONDITIONAL") || cur.type.equals("DEFINITION") || cur.type.equals("DESCRIPTION"))) {
				cur = cur.prev;
			}
			cur.SetLast(new ASTNode(cur, "START", "{"));
			cur = cur.GetLastNode();
			return p+1;
		case "RIGHTCURLY":
			cur.SetNode(new ASTNode(cur, "END", "}"));
			cur = cur.prev.prev;
			if (cur.type.equals("ACCESS")) {
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
