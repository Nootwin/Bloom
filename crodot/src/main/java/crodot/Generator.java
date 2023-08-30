package crodot;

import java.util.Stack;

import org.objectweb.asm.Opcodes;

public class Generator {
	private ASTNode trees;
	private CodeCreator create;
	private boolean unClass = true;
	private String[] printType;
	private boolean unMethod = true;
	private boolean wanderingIf = false;
	private Stack<String> indentObj = new Stack<>();
	
	Generator(ASTNode trees, AnaResults results) {
		this.trees = trees;
		this.create = new CodeCreator(results);
	}
	
	public void createSys(String jarName) {
		for (int j = 0; j < trees.GetNodeSize(); j++) {
			decision(trees.GetNode(j));
			
		}
		
		create.closeMain();
		create.saveMain();
		create.runClass("Main");
	}
	
	public void decision(ASTNode tree) {
		switch(tree.type) {
		case "ACCESS":
			decision(tree.GetFirstNode());
			break;
		case "FUN":
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			if (tree.value.equals("print")) {
				create.staticField("out", "java/lang/System", "Ljava/io/PrintStream;");
				create.evalE(tree.GetLastNode());
				printType = create.checkMethodvStack("println", "PrintStream", 1);
				create.invokePublic("println", "java/io/PrintStream", printType[0] + "V");
			}
			else {
				create.invokeEasy(tree);
			}
			break;
		case "DOT":
			create.evalE(tree);
			break;
		case "VAR":
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			if (tree.GetNodeSize() > 0) {
				create.evalE(tree.GetFirstNode());
				create.storeVar(tree.value, tree);
			}
			else if (create.isClass(tree.value)) {
				create.pushStack(tree.value);
			}
			else {
				create.loadVar(tree.value, tree);
			}
			break;
		case "ARR":
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			if (tree.GetNodeSize() > 1) {
				create.evalE(tree.GetNode(1));
				create.storeArr(tree.value, tree, tree.GetNode(1));
			}
			else {
				create.LoadArrIndex(create.loadVar(tree.value, tree), tree);
			}
			break;
		case "DECLARATION":
			if (!indentObj.isEmpty() && indentObj.peek().equals("class")) {
				create.newField(tree.GetFirstNode().value, tree.value, Opcodes.ACC_PUBLIC, tree);
			}
			else {
				wanderingIf = create.accMain(unClass, unMethod, wanderingIf); 
				if (tree.GetNodeSize() > 1) {
					create.evalE(tree.GetNode(1));
					create.newVar(tree.GetFirstNode().value, tree.value, tree.Grab("GENERIC"));
				}
				else {
					create.uninitnewVar(tree.GetFirstNode().value, tree.value);
				}
				
			}
			
			break;
		case "DEFINITION":
			indentObj.push("class");
			unClass = create.newClass(tree);
			ASTNode start = tree.Grab("START");
			for (int i = 0; i < start.GetNodeSize(); i++) {
				decision(start.GetNode(i));
			}
			break;
		case "DESCRIPTION":
			wanderingIf = create.accMain(unClass, false, false);
			indentObj.push("method");
			unMethod = create.newMethod(tree.GetFirstNode().value, tree );
			int num = 0;
			for (int i = 0; i < tree.GetNodeSize(); i++) {
				if (tree.GetNode(i).type.equals("START")) {
					num = i;
					break;
				}
			}
			for (int i = 0; i < tree.GetNode(num).GetNodeSize(); i++) {
				decision(tree.GetNode(num).GetNode(i));
			}
			break;
		
		case "CONDITIONAL":
			wanderingIf = create.accMain(unClass, unMethod, false);
			if (tree.value.equals("if")) {
				create.If(tree.GetFirstNode());
				indentObj.push("if");
				for (int i = 0; i < tree.GetNode(1).GetNodeSize(); i++) {
					decision(tree.GetNode(1).GetNode(i));
				}
			}
			else {
				create.Else();
				indentObj.push("else");
				for (int i = 0; i < tree.GetFirstNode().GetNodeSize(); i++) {
					decision(tree.GetFirstNode().GetNode(i));
				}
			}
			
			
			break;
		case "LOOP":
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			if (tree.value.equals("while")) {
				indentObj.push("while");
				create.While(tree.GetFirstNode());
				for (int i = 0; i < tree.GetNode(1).GetNodeSize(); i++) {
					decision(tree.GetNode(1).GetNode(i));
				}
			}
			else {
				indentObj.push("for");
				create.For(tree);
				for (int i = 0; i < tree.GetNode(3).GetNodeSize(); i++) {
					decision(tree.GetNode(3).GetNode(i));
				}
			}
			break;
		case "RETURN":
			create.Return(tree.GetFirstNode());
			break;
		case "END":
			switch(indentObj.pop()) {
			case "class":
				unClass = create.closeClass();
				create.saveClass();
				break;
			case "method":
				unMethod = create.closeMethod();
				break;
			case "if":
				create.EndOfIf();
				wanderingIf = true;
				break;
			case "else":
				create.EndElse();
				wanderingIf = false;
				break;
			case "while":
				create.EndWhile(tree.prev.prev.GetFirstNode());
				break;
			case "for":
				create.EndFor(tree.prev.prev);
				break;
			}
		}
		create.ClearStack();
		if (create.mv != null) create.mv.apply();
	}

}
