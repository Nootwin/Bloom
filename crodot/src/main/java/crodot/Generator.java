package crodot;

import java.util.Stack;

import org.objectweb.asm.Opcodes;

import crodotStates.TokenState;

public class Generator {
	private Analyser analy;
	private ASTNode trees;
	private CodeCreator create;
	private boolean unClass = true;
	private String[] printType;
	private boolean unMethod = true;
	private boolean wanderingIf = false;
	private Stack<String> indentObj = new Stack<>();
	private ErrorThrower err;
	private int line;
	
	Generator(ASTNode trees, AnaResults results, ErrorThrower err, Analyser analy, String sourceFile) {
		this.trees = trees;
		this.err = err;
		this.analy = analy;
		this.create = new CodeCreator(results, err, analy, sourceFile);
		
		line = -3;
	}
	
	public void lineCheck(ASTNode tree) {
		if (create.mv != null && tree.line > line) {
			line = tree.line;
			create.addLineNumber(tree.line);
			
		}
	}
	
	public void createSys(String jarName) {
		for (int j = 0; j < trees.GetNodeSize(); j++) {
			decision(trees.GetNode(j));
			
		}
		
		if (wanderingIf) {
			create.EndOfIf();
		}
		create.closeMain();
		create.saveMain();
		create.runClass(err.outputLoc + "\" Main");
	}
	
	public void decision(ASTNode tree) {

		switch(tree.type) {
		case TokenState.INCREMENT, TokenState.DECREMENT, TokenState.EQUIVALENCY:
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			create.evalE(tree);
			break;
		
		case TokenState.ACCESS:
			decision(tree.GetFirstNode());
			break;
		case TokenState.FUN:
			
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			lineCheck(tree);
			if (tree.value.equals("print")) {
				create.staticField("out", "java/lang/System", "Ljava/io/PrintStream;");
				create.evalE(tree.GetLastNode());
				printType = create.checkMethodvStack("println", "java/io/PrintStream", 1, tree.line);
				create.invokePublic("println", "java/io/PrintStream", printType[0] + "V");
			}
			else {
				create.invokeEasy(tree);
			}
			break;
		case TokenState.DOT:
			create.evalE(tree);
			break;
		case TokenState.IDENTIFIER:
			
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			lineCheck(tree);
			if (!indentObj.isEmpty() && indentObj.peek().equals("class")) {
				create.newFieldUnkType(tree.value, Opcodes.ACC_PUBLIC, tree);
			}
			else if (tree.GetNodeSize() > 0) {
				if (create.getVar(tree.value) != null) {
					if (create.getVar(tree.value) instanceof GenVarInfo) {
						create.cc.curGenType = ((GenVarInfo) create.getVar(tree.value)).InferredTypes;
					}
					create.evalE(tree.GetFirstNode(), create.getVar(tree.value).type);
					create.storeVar(tree.value, tree);
					create.cc.curGenType = null;
				}
				else {
					create.evalE(tree.GetFirstNode());
					create.newUnknownVar(tree.value);
				}
				
				create.cc.curGenType = null;
				
			}
			else if (create.isClass(tree.value)) {
				create.pushStack(tree.value);
			}
			else {
				create.loadVar(tree.value, tree);
			}
			break;
		case TokenState.ARR:
			
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			lineCheck(tree);
			if (tree.GetNodeSize() > 1) {
				create.evalE(tree.GetNode(1));
				create.storeArr(tree.value, tree, tree.GetNode(1));
			}
			else {
				create.LoadArrIndex(create.loadVar(tree.value, tree), tree, 0);
			}
			break;
		case TokenState.SUBDEFINITION:
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			lineCheck(tree);
			create.newSubClass(tree);
			indentObj.push("subclass");
            break;
		case TokenState.DECLARATION:

			if (!indentObj.isEmpty() && indentObj.peek().equals("class")) {

				create.newField(tree.GetFirstNode().value, tree.value, Opcodes.ACC_PUBLIC, tree);
			}
			else {
				
				wanderingIf = create.accMain(unClass, unMethod, wanderingIf); 
				lineCheck(tree);
				if (tree.GetNodeSize() > 1) {
					if (tree.Grab(TokenState.GENERIC) != null) {
						create.setCurGenType(tree.Grab(TokenState.GENERIC), tree.value);
					}
						
					create.evalE(tree.GetNode(1), create.strToByte(tree.value) + create.genToString(tree.Grab(TokenState.GENERIC)));
					create.newVar(tree.GetFirstNode().value, tree.value, tree.Grab(TokenState.GENERIC), tree.line);
					
					create.cc.curGenType = null;
				}
				else {
					create.uninitnewVar(tree.GetFirstNode().value, tree.value, tree.line);
				}
				
			}
			

			break;
		case TokenState.DEFINITION:
			indentObj.push("class");
			unClass = create.newClass(tree);
			ASTNode start = tree.Grab(TokenState.START);
			for (int i = 0; i < start.GetNodeSize(); i++) {
				decision(start.GetNode(i));
			}
			break;
		case TokenState.DESCRIPTION:
			wanderingIf = create.accMain(unClass, false, false);
			lineCheck(tree);
			indentObj.push("method");
			unMethod = create.newMethod(tree.GetFirstNode().value, tree );
			int num = 0;
			for (int i = 0; i < tree.GetNodeSize(); i++) {
				if (tree.GetNode(i).type == TokenState.START) {
					num = i;
					break;
				}
			}
			for (int i = 0; i < tree.GetNode(num).GetNodeSize(); i++) {
				decision(tree.GetNode(num).GetNode(i));
			}
			break;
		
		case TokenState.CONDITIONAL:
			wanderingIf = create.accMain(unClass, unMethod, false);
			lineCheck(tree);
			create.getVarManager().createBlackPoint();
			if (tree.value.equals("if")) {
				create.If(tree.GetFirstNode());
				indentObj.push("if");
				for (int i = 0; i < tree.GetNode(1).GetNodeSize(); i++) {
					decision(tree.GetNode(1).GetNode(i));
				}
			}
			else if (tree.value.equals("elif")) {
				create.Elif(tree.GetFirstNode());
				indentObj.push("elif");
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
		case TokenState.LOOP:
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			lineCheck(tree);
			create.getVarManager().createBlackPoint();
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
		case TokenState.RETURN:
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			lineCheck(tree);
			create.Return(tree.GetFirstNode());
			break;
		case TokenState.END:
			wanderingIf = create.accMain(unClass, unMethod, wanderingIf);
			switch(indentObj.pop()) {
			case "class":
				unClass = create.closeClass();
				create.saveClass();
				break;
			case "method":
				unMethod = create.closeMethod();
				break;
			case "if", "elif":
				lineCheck(tree);
				wanderingIf = true;
				create.getVarManager().blast();
				break;
			case "else":
				lineCheck(tree);
				create.EndElse();
				wanderingIf = false;
				create.getVarManager().blast();
				break;
			case "while":
				lineCheck(tree);
				create.EndWhile(tree.prev.prev.GetFirstNode());
				create.getVarManager().blast();
				break;
			case "for":
				lineCheck(tree);
				create.EndFor(tree.prev.prev);
				create.getVarManager().blast();
				break;
			}
		}
		create.ClearStack();
		if (create.mv != null) create.mv.apply();
	}

}
