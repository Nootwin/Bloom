package crodot.OldFiles;

import java.util.ArrayList;

import crodot.ASTNode;
import crodot.token;

public class parser {
	static ASTNode fieldSolve(ASTNode tree, ArrayList<token> code, int goal) {
		int brackets = 0;
		for (int maths = code.size()-1; maths > 0; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			else if (code.get(maths).value.equals(".") && brackets ==  goal){
				tree.type = "DOT";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.fieldSolve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.fieldSolve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
		} 
		brackets = 0;
		
		for (int maths = code.size()-1; maths !=  -1; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			

			else if (code.get(maths).type.equals("IDENTIFIER") && brackets ==  goal){
				if (maths !=  code.size()-1 && code.get(maths+1).value.equals("(")) {
					brackets = -1;
					for (int j = maths+1; j < code.size(); j++) {
						if (code.get(j).value.equals("(")) {
							brackets++;
						}
						if (code.get(j).value.equals(")")) {
							if (brackets ==  0) {
								tree.type = "FUN";
								tree.value = code.get(maths).value;
								tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+2, j)), 0));
								return tree;
							}
							else {
								brackets--;
							}
						}
					}
				}
				
			}
			
		} 
		brackets = 0;
		
		for (int maths = code.size()-1; maths > 0; maths--) {
			if (code.get(maths).value.equals(")")) {
				if (brackets ==  goal) {
					return parser.fieldSolve(tree, code, goal+1);
				}
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			
			
		}
		
		for (int maths = 0; maths < code.size(); maths++) {
			if (code.get(maths).type.equals("IDENTIFIER")){
				tree.type = "VAR";
				tree.value = code.get(maths).value;	
				return tree;
			}
		}

		return tree;
		
	}
	
	static ASTNode solve(ASTNode tree, ArrayList<token> code, int goal) {
		int brackets = 0;
		if (code.size() < 1) {
			tree.type = "NULL";
			tree.value = "null";
			return tree;
		}
		
		for (int maths = code.size()-1; maths > 0; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			else if (code.get(maths).value.equals(",") && brackets ==  goal){
				tree.type = "SEP";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
		} 
		
		brackets = 0;
		
		for (int maths = code.size()-1; maths > -1; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			else if (code.get(maths).value.equals("==") && brackets ==  goal){
				tree.type = "TRUEEQUALS";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
				
			}
			else if (code.get(maths).value.equals("!=") && brackets ==  goal){
				tree.type = "NOTEQUALS";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
			else if (code.get(maths).value.equals(">=") && brackets ==  goal){
				tree.type = "TRUEGREATERTHAN";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
				
			}
			else if (code.get(maths).value.equals("<=") && brackets ==  goal){
				tree.type = "TRUELESSTHAN";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
			else if (code.get(maths).value.equals(">") && brackets ==  goal){
				tree.type = "GREATERTHAN";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
				
			}
			else if (code.get(maths).value.equals("<") && brackets ==  goal){
				tree.type = "LESSTHAN";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
		}
		brackets = 0;
		
		for (int maths = code.size()-1; maths > 0; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			else if (code.get(maths).value.equals("+") && brackets ==  goal){
				tree.type = "ADD";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
				
			}
			else if (code.get(maths).value.equals("-") && brackets ==  goal){
				tree.type = "SUB";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
		}
		brackets = 0;
		for (int maths = code.size()-1; maths > 0; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			else if (code.get(maths).value.equals("*") && brackets ==  goal){
				tree.type = "MUL";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
			else if (code.get(maths).value.equals("/") && brackets ==  goal){
				tree.type = "DIV";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
			else if (code.get(maths).value.equals("%") && brackets ==  goal){
				tree.type = "REM";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
		} 
		brackets = 0;
		for (int maths = code.size()-1; maths > 0; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			else if (code.get(maths).value.equals("^") && brackets ==  goal){
				tree.type = "EXP";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
		} 
		brackets = 0;
		
		for (int maths = code.size()-1; maths > 0; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			else if (code.get(maths).value.equals(".") && brackets ==  goal){
				tree.type = "DOT";
				tree.value = code.get(maths).value;
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(0, maths)), 0));
				tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+1, code.size())), 0));
				return tree;
			}
		} 
		brackets = 0;
		


		for (int maths = code.size()-1; maths !=  -1; maths--) {
			if (code.get(maths).value.equals(")")) {
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			

			else if (code.get(maths).type.equals("IDENTIFIER") && brackets ==  goal){
				if (maths !=  code.size()-1 && code.get(maths+1).value.equals("(")) {
					brackets = -1;
					for (int j = maths+1; j < code.size(); j++) {
						if (code.get(j).value.equals("(")) {
							brackets++;
						}
						if (code.get(j).value.equals(")")) {
							if (brackets ==  0) {
								tree.type = "FUN";
								tree.value = code.get(maths).value;
								tree.SetNode(parser.solve(new ASTNode(tree), new ArrayList<token>(code.subList(maths+2, j)), 0));
								return tree;
							}
							else {
								brackets--;
							}
						}
					}
				}
				
			}
			
		} 
		brackets = 0;
		
		for (int maths = code.size()-1; maths > 0; maths--) {
			if (code.get(maths).value.equals(")")) {
				if (brackets ==  goal) {
					return parser.solve(tree, code, goal+1);
				}
				brackets++;
			}
			else if (code.get(maths).value.equals("(")) {
				brackets--;
			}
			
			
		}
		
		for (int maths = 0; maths < code.size(); maths++) {
			if (code.get(maths).type.equals("IDENTIFIER")){
				tree.type = "VAR";
				tree.value = code.get(maths).value;	
				return tree;
			}
		}
		
		for (int maths = 0; maths < code.size(); maths++) {
			if (code.get(maths).type.equals("NUMBER")){

				if (tree.value ==  null) {
					if (code.get(maths).value.contains(".")) {
						tree.type = "D";
					}
					else if (Long.parseLong(code.get(maths).value) > Integer.MAX_VALUE && Long.parseLong(code.get(maths).value) < Integer.MIN_VALUE ) {
						tree.type = "L";
					}
					else {
						tree.type = "I";
					}
					tree.value = code.get(maths).value;	
				}
					
				
				else {
					System.out.println("\u001B[31m" + "Statement cannot take two inputs" + "\u001B[0m");
					System.exit(0);
				}
				
			}
			
		}
		for (int maths = 0; maths < code.size(); maths++) {
			if (code.get(maths).type.equals("BOOLEAN")){

				if (tree.value ==  null) {
					tree.type = "Z";
					tree.value = code.get(maths).value;	
				}
				else {
					System.out.println("\u001B[31m" + "Statement cannot take two inputs" + "\u001B[0m");
					System.exit(0);
				}
				
			}
			
		}
		for (int maths = 0; maths < code.size(); maths++) {
			if (code.get(maths).type.equals("STRING")){
				if (tree.type ==  null) {
					tree.type = "Ljava/lang/String;";
					tree.value = code.get(maths).value;		
				}
				else if (tree.type ==  "STRING"){
					tree.value += code.get(maths).value;
				}
				
			}
		}
		for (int maths = 0; maths < code.size(); maths++) {
			if (code.get(maths).type.equals("NULLVALUE")){

				if (tree.value == null) {
					tree.type = "NULL";
					tree.value = "null";	
				}
				else {
					System.out.println("\u001B[31m" + "Statement cannot take two inputs" + "\u001B[0m");
					System.exit(0);
				}
				
			}
			
		}
		
		return tree;
		

	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	static ArrayList<ASTNode> parse(ArrayList<ArrayList<token>> tcode) {
		ArrayList<ASTNode> ncode = new ArrayList<>();
		ArrayList<token> t = new ArrayList<>();
		ASTNode cur = null;
		int modifier;
		for (int y = 0; y < tcode.size(); y++) {
			t = tcode.get(y);
			modifier = 0;
			String[] order = {};
				switch (t.get(0).type) {
					case "DECLARATION":
						String ifor;
						if (t.get(t.size()-2).value.equals("{")) {
							ifor = "DESCRIPTION";
							order = new String[t.size()-2];
							order[0] = "IDENTIFIER"; order[1] = "LEFTBRACKET"; order[order.length-2] = "RIGHTBRACKET"; order[order.length-1] = "LEFTBRACKET";
							for (int a = 2; a < order.length-2; a++) {
								if (a % 3 == 2) {
									order[a] = "DECLARATION";
								}
								else if (a % 3 == 0) {
									order[a] = "IDENTIFIER";
								}
								else {
									order[a] = "SEPERATOR";
								}
							}
						}
						else {
							ifor = "DECLARATION";
							order = new String[] {"IDENTIFIER", "EQUIVALENCY", "VALUE"};
						}
						
						if (cur !=  null) {
							cur.SetNode(new ASTNode(cur, ifor, t.get(0).value));
						}
						else {
							ncode.add(new ASTNode(ifor, t.get(0).value));
						}
						break;
					case "DEFINITION":
						order = new String[] {"IDENTIFIER", "LEFTBRACKET"};
						if (cur !=  null) {
							cur.SetNode(new ASTNode(cur, "DEFINITION", t.get(0).value));
						}
						else {
							ncode.add(new ASTNode("DEFINITION", t.get(0).value));
						}
						break;
					case "CONDITIONAL", "LOOP":
						switch (t.get(0).value) {
							case "if":
								order = new String[] {"VALUE", "LEFTBRACKET"};
								break;
							case "else":
								order = new String[] {"LEFTBRACKET"};
								break;
							case "while":
								order = new String[] {"VALUE", "LEFTBRACKET"};
								break;
							case "for":
								order = new String[] {"DECLARATION", "IDENTIFIER", "EQUIVALENCY", "VALUE", "SEPERATOR", "VALUE", "SEPERATOR", "VALUE", "LEFTBRACKET"};
								break;
						}
						
						if (cur !=  null) {
							cur.SetNode(new ASTNode(cur, t.get(0).type, t.get(0).value));
						}
						else {
							ncode.add(new ASTNode(t.get(0).type, t.get(0).value));
						}
						break;
					case "IDENTIFIER":
						order = new String[] { "EQUIVALENCY", "VALUE"};
						for (int i = 0; i < t.size(); i++) {
							if (t.get(i).type.equals("EQUIVALENCY") || t.get(i).type.equals("ENDOFLINE") ) {
								if (cur !=  null) {
									cur.SetNode(fieldSolve(new ASTNode(cur), new ArrayList<token>(t.subList(0, i)) , 0));
								}
								else {
									ncode.add(fieldSolve(new ASTNode(), new ArrayList<token>(t.subList(0, i)) , 0));
								}
								break;
							}
						}
						

						break;
					case "ENDOFLINE":

						break;
					case "RETURN":
						order = new String[] {"VALUE"};
						if (cur !=  null) {
							cur.SetNode(new ASTNode(cur, "RETURN", "return"));
						}
						else {
							ncode.add(new ASTNode("RETURN", "return"));
						}
						break;
					case "RIGHTBRACKET":

						if (cur !=  null) {
							cur.SetNode(new ASTNode(cur, "END", "}"));
							cur = cur.prev.prev;
						}
						else {
							System.out.println("cringe");
						
						}
						break;
					default:
						System.out.println("\u001B[31m" + t.get(0).value + "cannot be used at start of line" + "\u001B[0m");
						System.exit(0);
						break;
					
				}
			for (int a = 1; a < order.length+1; a++) {
				if (t.get(a+modifier).type.equals("ENDOFLINE")) {
					break;
				}

				else if (order[a-1].equals(t.get(a+modifier).type)) {

					if (t.get(a+modifier).value.equals("{")) {
						if (cur !=  null) {
							cur.GetLastNode().SetNode(new ASTNode(cur.GetLastNode(), "START", "{"));
							cur = cur.GetLastNode().GetLastNode();
						}
						else {
							ncode.get(ncode.size()-1).SetNode(new ASTNode(ncode.get(ncode.size()-1), "START", "{"));
							cur = ncode.get(ncode.size()-1).GetLastNode();
						}
						
					}	
					else if (t.get(a+modifier).value.equals("}")) {
						if (cur !=  null) {
							cur.SetNode(new ASTNode(cur, "END", "}"));
							cur = cur.prev.prev;
						}
						else {
							System.out.println("cringe");
						
						}
					}
					else {
						if (cur !=  null) {
							cur.GetLastNode().SetNode(new ASTNode(cur.GetLastNode(), t.get(a+modifier).type, t.get(a+modifier).value));
						}
						else {
							ncode.get(ncode.size()-1).SetNode(new ASTNode(ncode.get(ncode.size()-1), t.get(a+modifier).type, t.get(a+modifier).value));
						}	
					
					}
				}
				
				
				else if (order[a-1].equals("VALUE")) {
					for (int val = a+modifier; val < t.size(); val++) {
						if (a == order.length) {
							if (cur !=  null) {
								cur.GetLastNode().SetNode(parser.solve(new ASTNode(cur.GetLastNode()), new ArrayList<token>(t.subList(a+modifier, t.size()-1)), 0));
								break;
							}
							else {
								ncode.get(ncode.size()-1).SetNode(parser.solve(new ASTNode(ncode.get(ncode.size()-1)), new ArrayList<token>(t.subList(a+modifier, t.size()-1)), 0));
								break;
							}
						}
						else if (order[a].equals(t.get(val).type)) {
							if (cur !=  null) {
								cur.GetLastNode().SetNode(parser.solve(new ASTNode(cur.GetLastNode()), new ArrayList<token>(t.subList(a+modifier, val)), 0));
								modifier = val-a-1;
								break;
							}
							else {

								ncode.get(ncode.size()-1).SetNode(parser.solve(new ASTNode(ncode.get(ncode.size()-1)), new ArrayList<token>(t.subList(a+modifier, val)), 0));
								
								modifier = val - a-1;
								break;
							}
						}
						else if (t.get(val).type.equals("ENDOFLINE")){
							break;
						}
					
					
					}
					
				}
				else {
					break;
				}
			}
			
			
		}
		return ncode;
	}
}
