package crodot;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Stack;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import crodotInsn.CrodotInsn;
import crodotInsn.CrodotMethod;
import crodotInsn.CrodotType;
import javassist.bytecode.Opcode;


public class CodeCreator {
	private String curName;
	private String returnType;
	private ClassWriter cw;
	public CrodotMethodVisitor mv;
	private ClassWriter OtherClass;
	private ClassWriter MainClass;
	private CrodotMethodVisitor MainMethod;
	private ArrayList<HashMap<String, VarInfo>> varPos;
	private int varSwitch;
	private Stack<Label> labelList;
	private String top;
	private int size;
	private int[] varCount = new int[2];
	private Stack<StackInfo> curStack;
	AnaResults results;
	private ArrayList<Integer> getAllRangeStackPos;
	
	
	CodeCreator(AnaResults results) {
		labelList = new Stack<>();
		curStack = new Stack<>();
		varSwitch = 1;
		varPos = new ArrayList<>();
		varPos.add(null);
		varPos.add(null);
		this.results = results;
		
		MainClass = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		MainClass.visit(Opcodes.V19, Opcodes.ACC_PUBLIC, "Main", null, "java/lang/Object", null);
		
		MainMethod = new CrodotMethodVisitor(MainClass.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null));
		varPos.set(0, new HashMap<>());
		varPos.get(0).put("args", new VarInfo("args", "str", 0));
		varCount[0] = 1;
		labelList = new Stack<>();
	}
	
	public void ClearStack() {
		while (!curStack.isEmpty()) {
			curStack.pop();
			mv.visitInsn(Opcodes.POP);
		}
	}
	public String[] checkMethodvStack(String Methodname, String Classname, int size) {
		int[] priority = null;
		int[] tempPrio;
		boolean flag;
		int indexOf;
		MethodInfo info = results.Classes.get(Classname).methods.get(Methodname);
		ArrayList<ArrayList<String>> stacks = getAllRangeStack(size);
		if ((!Objects.isNull(info))) {
			for (int i = 0; i < info.args.size(); i++) {
				if (info.args.get(i).size() == stacks.size()) {
					tempPrio = new int[size+1];
					tempPrio[0] = i;
					flag = true;
					for (int j = 0; j < stacks.size(); j++) {
						if ((indexOf = stacks.get(j).indexOf(info.args.get(i).get(j))) != -1) {
							tempPrio[j+1] = indexOf;
						}
						else {
							flag = false;
							break;
						}
					}
					if (flag) {
						if (priority == null) {
							priority = tempPrio;
						}
						else {
							int turner = 0;
							for (int k = 1; k < tempPrio.length; k++) {
								turner += priority[k] - tempPrio[k];
							}
							if (turner > 0) {
								priority = tempPrio;
							}
							else if (turner == 0) {
								//error ambiguous
							}
						}
					}	
				}
			}
			if (priority != null) {
				addCastings(info.args.get(priority[0]), stacks);
				if (results.Classes.get(curName).canGeneric()) {
					return new String[] {replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(curName).genType), replaceAll(info.returnType.get(priority[0]), results.Classes.get(curName).genType), info.AccessModifiers};
				}
				else {
					return new String[] {info.args.get(priority[0]).toArgs(), info.returnType.get(priority[0]), info.AccessModifiers};
				}
			}
				
			
		}
		System.out.println("damn daniel" + Methodname + curName);
		return null;
	}
	
	private void addCastings(ArgsList<String> argsList, ArrayList<ArrayList<String>> stacks) {

		for (int i = 0; i < argsList.size(); i++) {
			if (stacks.get(i).get(0).length() < 2 && argsList.get(i).length() > 1 && stacks.get(i).get(0) != argsList.get(i) ) {
				switch(stacks.get(i).get(0)) {
				case "Z":
					mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false), getAllRangeStackPos.get(i)-1);
					break;
				case "B":
					mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false), getAllRangeStackPos.get(i)-1);
					break;
				case "S":
					mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false), getAllRangeStackPos.get(i)-1);
					break;
				case "I":
					mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false), getAllRangeStackPos.get(i)-1);
					break;
				case "C":
					mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false), getAllRangeStackPos.get(i)-1);
					break;
				case "F":
					mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false), getAllRangeStackPos.get(i)-1);
					break;
				case "D":
					mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false), getAllRangeStackPos.get(i)-1);
					break;
				case "J":
					mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false), getAllRangeStackPos.get(i)-1);
					break;
				}

			}
		}
		
	}

	
	

	public String[] checkMethodvStack(String Methodname, String Classname, int size, LinkedHashMap<String, String> genTypeInfo) {
		MethodInfo info = results.Classes.get(Classname).methods.get(Methodname);
		ArrayList<ArrayList<String>> stacks = getAllRangeStack(size);
		int[] priority = null;
		int[] tempPrio;
		boolean flag;
		int indexOf = -1;
		String tempParam;
		if ((!Objects.isNull(info))) {
			for (int i = 0; i < info.args.size(); i++) {
				if (info.args.get(i).size() == stacks.size()) {
					flag = true;
					tempPrio = new int[size+1];
					tempPrio[0] = i;
					for (int j = 0; j < stacks.size(); j++) {
						if (info.args.get(i).get(j).startsWith("T")) {
							tempParam = info.args.get(i).get(j);
							
							if (results.Classes.get(Classname).canGeneric() && (indexOf = stacks.get(j).indexOf(results.Classes.get(Classname).genType.get(tempParam))) == -1) {
								flag = false;
								break;
							}
							else if (!stacks.get(j).contains(genTypeInfo.get(tempParam))) {
								System.out.println(tempParam);
								System.out.println(genTypeInfo.get(tempParam));
								System.out.println("Sorry right type but wrong type");
								flag = false;
								break;
							}
							else{
								tempPrio[j+1] = indexOf;
							}
							
						}
						else {
							if ((indexOf = stacks.get(j).indexOf(info.args.get(i).get(j))) != -1) {
								tempPrio[j+1] = indexOf;
							}
							else {
								flag = false;
								break;
							}
						}
						
					}
					if (flag) {
						if (priority == null) {
							priority = tempPrio;
						}
						else {
							int turner = 0;
							for (int k = 1; k < tempPrio.length; k++) {
								turner += priority[k] - tempPrio[k];
							}
							if (turner > 0) {
								priority = tempPrio;
							}
							else if (turner == 0) {
								//error ambiguous
							}
						}
						
					}
				}
			}
			if (priority != null) {
				addCastings(info.args.get(priority[0]), stacks);
				if (results.Classes.get(curName).canGeneric()) {
					return new String[] {replaceAll(replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(curName).genType) , results.Classes.get(Classname).genType), replaceAll(replaceAll(info.returnType.get(priority[0]), results.Classes.get(curName).genType), results.Classes.get(Classname).genType), info.AccessModifiers, genTypeInfo.get(info.returnType.get(priority[0]))};
				}
				else {
					return new String[] {replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(Classname).genType), replaceAll(info.returnType.get(priority[0]), results.Classes.get(Classname).genType), info.AccessModifiers, genTypeInfo.get(info.returnType.get(priority[0]))};
				}
			}
				
			
		}
		System.out.println("damn daniel");
		return null;
	}
	
	private String replaceAll(String base, LinkedHashMap<String, String> genPairs) {
		String returner = base;
		for (Entry<String, String> entry : genPairs.entrySet()) {
			returner = returner.replace(entry.getKey(), entry.getValue());
			
		}
		return returner;
	}
	
	ClassInfo returnCurClassInfo() {
		return results.Classes.get(curName);
	}
	
	private ArrayList<ArrayList<String>> getAllRangeStack(int size){
		ArrayList<ArrayList<String>> returnList = new ArrayList<>();
		getAllRangeStackPos = new ArrayList<>();
		ArrayList<String> list;
		StackInfo str;
		for (int i = 0; i < size; i++) {
			str = curStack.pop();
			list = new ArrayList<>();
			list.add(str.type);
			getAllRangeStackPos.add(str.posInQueue);
			switch(str.type) {
			case "Z":
				list.add("Ljava/lang/Boolean;");
				list.add("Ljava/lang/Object;");
				break;
			case "C":
				list.add("I");
				list.add("Ljava/lang/Character;");
				list.add("Ljava/lang/Object;");
				break;
			case "B":
				list.add("I");
				list.add("Ljava/lang/Byte;");
				list.add("Ljava/lang/Object;");
				break;
			case "S":
				list.add("I");
				list.add("Ljava/lang/Short;");
				list.add("Ljava/lang/Object;");
				break;
			case "I":
				list.add("Ljava/lang/Integer;");
				list.add("Ljava/lang/Object;");
				break;
			case "J":
				list.add("Ljava/lang/Long;");
				list.add("Ljava/lang/Object;");
				break;
			case "F":
				list.add("Ljava/lang/Float;");
				list.add("Ljava/lang/Object;");
				break;
			case "D":
				list.add("Ljava/lang/Double;");
				list.add("Ljava/lang/Object;");
				break;
			case "Ljava/lang/String;":
				list.add("Ljava/lang/Object;");
				break;
			case "Ljava/lang/Object;":
				break;
			default:
				if (str.type.contains("[") || results.Classes.get(curName).genType.containsKey(str.type)) {
					list.add("Ljava/lang/Object;");
					break;
				}
				try {
					Class <?> C = Class.forName(ImportFormat(str.type));
					while ((C = C.getSuperclass()) != null) {
						list.add("L" + C.getName().replace(".", "/") + ";");
					}
					break;
				}
				catch (ClassNotFoundException e){
					list.add("Ljava/lang/Object;");
					e.printStackTrace();
				}
				
			}
			returnList.add(list);
		}
		return returnList;
	}
	
	
	private String ImportFormat(String ObjectName) {
		return ObjectName.substring(1, ObjectName.length()-1).replace("/", ".");
	}
	private String[] constructorDo(String Classname, ASTNode tree) {
		boolean flag;
		mv.visitTypeInsn(Opcodes.NEW, IfImport(Classname));
		mv.visitInsn(Opcodes.DUP);
		size = curStack.size();
		if (tree.GetNodeSize() > 0) evalE(tree.GetLastNode());
		MethodInfo info = results.Classes.get(Classname).methods.get(Classname);
		ArrayList<ArrayList<String>> stacks = getAllRangeStack(curStack.size()-size);
		if ((!Objects.isNull(info))) {
			for (int i = 0; i < info.args.size(); i++) {
				if (info.args.get(i).size() == stacks.size()) {
					flag = true;
					for (int j = 0; j < stacks.size(); j++) {
						if (!stacks.get(j).contains(info.args.get(i).get(j))) {
							flag = false;
						}
					}
					if (flag) {
						return new String[] {info.args.get(i).toArgs(), "V", "public"};
					}
				}
			}
				
			
		}
		
		return null;
	}
	
	public int getMethodAccess(String Classname, String Methodname) {
		switch(results.Classes.get(Classname).methods.get(Methodname).AccessModifiers) {
		case "private":
			return Opcodes.ACC_PRIVATE;
		case "static":
			return Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC;
		default:
			return Opcodes.ACC_PUBLIC;
		}
	}
	
	public String stripObj(String obj) {
		if (obj.charAt(obj.length()-1) == ';') {
			if (obj.contains("<")) {
				String newObj = obj.substring(1, obj.indexOf('<'));
				if (newObj.contains("/")) {
					return newObj.substring(newObj.lastIndexOf('/')+1, newObj.length());
				}
				return newObj;
			}
			if (obj.contains("/")) {
				return obj.substring(obj.lastIndexOf('/')+1, obj.length()-1);
			}
			return obj.substring(1, obj.length()-1);
		}
		return obj;
	}
	
	public String getStack(int i) {
		return curStack.get(i).type;
	}
	public String stackTop() {
		if (curStack.empty()) {
			return "";
		}
		return curStack.peek().type;
	}
	public String popStack() {
		if (curStack.empty()) {
			return "";
		}
		return curStack.pop().type;
	}
	public void pushStack(String val) {
		curStack.push(new StackInfo(val));
	}
	public String getClassName() {
		return curName;
	}
	public String getRangeStack(int i) {
		StringBuilder build = new StringBuilder();
		for (int j = 0; j < i; j++) {
			build.append(curStack.pop());
		}
		
		return build.toString();
	}
	public String returnStack() {
		StringBuilder build = new StringBuilder();
		for (int i = 0; i < curStack.size(); i++) {
			build.append(curStack.get(i));
			
		}
		return build.toString();
		
	}
	
	public boolean newClass(ASTNode classnode) {
		OtherClass = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		curName = classnode.GetFirstNode().value;
		System.out.println( "pop" + curName);
		cw = OtherClass;
		ASTNode temp = classnode;
		int privacy = 0;
		boolean isPublic = true;
		while ((temp = temp.prev).type.equals("ACCESS")) {
			switch(temp.value) {
			case "local":
				privacy += Opcodes.ACC_OPEN;
				isPublic = false;
				break;
			case "final":
				privacy += Opcodes.ACC_FINAL;
				break;
			}
		}
		if (isPublic) {
			privacy += Opcodes.ACC_PUBLIC;
		}
		System.out.println(classnode.value);
		if (classnode.value.equals("abstract")) {
			privacy += Opcodes.ACC_ABSTRACT;
		}
		else if (classnode.value.equals("interface")) {
			privacy += Opcodes.ACC_INTERFACE;
		}
		if ((temp = classnode.Grab("PARENT")) != null) {
			cw.visit(Opcodes.V19, privacy, curName, signatureWriterClass(classnode), IfImport(temp.value), null);
		}
		else {
			cw.visit(Opcodes.V19, privacy, curName, signatureWriterClass(classnode), "java/lang/Object", null);
		}
		
		return false;
	}
	
	public boolean closeClass() {
		if (!results.Classes.get(curName).construct) {
			mv = new CrodotMethodVisitor(cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null));
			addDefaultstoConst(curName);
			returnType = "V";
			closeMethod();
		}
		cw.visitEnd();

		return true;
	}
	public boolean closeMethod() {
		if (returnType.equals("V")) {
			mv.visitInsn(Opcodes.RETURN);
		}
		returnType = null;
		mv.visitMaxs(0, 0);

		mv.visitEnd();
		return true;
	}
	
	public boolean newMethod(String methodName, ASTNode tree) {
		varSwitch = 1;
		varCount[1] = 0;
		varPos.set(1, new HashMap<>()); 
		String returnType = strToByte(tree.value);
		this.returnType = returnType;
		String signature = signatureWriterMethod(tree);
		int privacy = 0;
		boolean isPublic = true;
		ASTNode temp = tree;
		while ((temp = temp.prev).type.equals("ACCESS")) {
			switch(temp.value) {
			case "local":
				if (isPublic) {
					privacy += Opcodes.ACC_OPEN;
					isPublic = false;
				}
				else {
					//error
				}
				break;
			case "priv":
				if (isPublic) {
					System.out.println("PRIVATE");
					privacy += Opcodes.ACC_PRIVATE;
					isPublic = false;
				}
				else {
					//error
				}
				break;
			case "proc":
				if (isPublic) {
					privacy += Opcodes.ACC_PROTECTED;
					isPublic = false;
				}
				else {
					//error
				}
				break;
			case "final":
				privacy += Opcodes.ACC_FINAL;
				break;
			case "static":
				privacy += Opcodes.ACC_STATIC;
				break;
			case "abstract":
				privacy += Opcodes.ACC_ABSTRACT;
				break;
			}
		}
		if (isPublic) {
			privacy += Opcodes.ACC_PUBLIC;
		}
		if (!results.Classes.get(curName).methods.get(methodName).AccessModifiers.contains("static")) {
			varPos.get(1).put("this", new VarInfo("this" , curName, 0));
			varCount[1] = 1;
			
		}
		ArgsList<String> args = fromNodetoArg(tree);
		if (methodName.equals(curName)) {
			mv = new CrodotMethodVisitor(cw.visitMethod(privacy, "<init>", args.toArgs() + returnType, signature, null));
			addDefaultstoConst(curName);
			results.Classes.get(curName).construct = true;
		}
		else {
			mv = new CrodotMethodVisitor(cw.visitMethod(privacy, methodName,  args.toArgs() + returnType, signature, null));
		}
		
		labelList = new Stack<>();
		return false;
	}
	
	private void addDefaultstoConst(String Classname) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
	    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, results.Classes.get(Classname).parent, "<init>", "()V", false);
	    for (Map.Entry<String, FieldInfo> entry : results.Classes.get(Classname).fields.entrySet()) {
	    	if (entry.getValue().HasCroValue()) {
	    		mv.visitVarInsn(Opcodes.ALOAD, 0);
	    		evalE(entry.getValue().OwnerValue);
	    		mv.visitFieldInsn(Opcodes.PUTFIELD, curName, entry.getKey(), entry.getValue().type);
	    	}
	    }
	}

	public String strToByte(String str) {
		if (str.endsWith(";")) {
			return str;
		}
		if (str.contains("<")) {
			return strToByteGeneric(str, null, results.Classes.get(curName).genType);
		}
		else if (str.contains("[")) {
			String rep = str.replace("[]", "");
			StringBuilder build = new StringBuilder();
			for (int i = 0; i < str.chars().filter(ch -> ch == '[').count(); i++) {
				build.append("[");
			}
			if (results.Classes.get(curName).canGeneric() && results.Classes.get(curName).genType.containsKey("T" + rep + ";")) {
				build.append("T");
				build.append(rep);
				build.append(";");
			}
			else {
				switch(rep) {
				case "doub":
					build.append("D");
				case "flt":
					build.append("F");
				case "void":
					build.append("V");
					break;
				case "str":
					build.append("Ljava/lang/String;");
					break;
				case "char":
					build.append("C");
					break;
				case "long":
					build.append("J");
					break;
				case "int":
					build.append("I");
					break;
				case "shrt":
					build.append("S");
					break;
				case "byte":
					build.append("B");
					break;
				case "bool":
					build.append("Z");
					break;
				default:
					build.append('L');
					build.append(IfImport(str.replace("[]", "")));
					build.append(';');
					break;
				}
			}
			return build.toString();
		}
		else {
			switch(str) {
			case "doub":
				return "D";
			case "flt":
				return "F";
			case "void":
				return "V";
			case "str":
				return "Ljava/lang/String;";
			case "char":
				return "C";
			case "long":
				return "J";
			case "int":
				return "I";
			case "shrt":
				return "S";
			case "byte":
				return "B";
			case "bool":
				return "Z";
			default:
				System.out.println(curName);
				if (results.Classes.get(curName).canGeneric() && results.Classes.get(curName).genType.containsKey("T" + str + ";")) {
					return "T" + str + ";";
				}
				return 'L' + IfImport(str) + ';';
			}
		}
		

	}
	public boolean isClass(String name) {
		if (results.Classes.containsKey(name)) {
			return true;
		}
		return false;
	}
	
	public String IfImport(String type) {

		if (results.Classes.containsKey(type)) {
			return results.Classes.get(type).name;
		}

		return type;
	}
	
	public boolean accMain(boolean classM, boolean method, boolean If) {
		if (classM) {
			OtherClass = cw;
			cw = MainClass;
			curName = "Main";
			returnType = "V";
			
		
			if (method) {
				mv = (CrodotMethodVisitor) MainMethod;
				varSwitch = 0;
			}
		}
		if (If) {
			Else();
			EndElse();
			
		}
		return false;
	}
	
	public static ArgsList<String> staticfromNodetoArg(ASTNode parent) {
		ArgsList<String> list = new ArgsList<>();
		boolean skip = false;
		for (int i = 1; i < parent.GetNodeSize()-1; i++) {
			switch(parent.GetNode(i).value) {
			case ",":
				break;
			case "str":
				list.add("Ljava/lang/String;");
				skip = true;
				break;
			case "char":
				list.add("C");
				skip = true;
				break;
			case "long":
				list.add("J");
				skip = true;
				break;
			case "int":
				list.add("I");
				skip = true;
				break;
			case "shrt":
				list.add("S");
				skip = true;
				break;
			case "byte":
				list.add("B");
				skip = true;
				break;
			case "bool":
				list.add("Z");
				skip = true;
				break;
			default:
				if (skip) {
					skip = false;
				}
				else {
					skip = true;
					list.add("L" + parent.GetNode(i).value + ';');
					// this is where you put the obj path when you build a obj finder
				}
				
			}
			
			
		}
		return list;
		
	}
	public void parampass(ASTNode parent) {
		for (int i = 1; i < parent.GetNodeSize()-1; i++) {
			switch(parent.GetNode(i).value) {
			case "<>":
				break;
			case "str":
				varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "str", varCount[varSwitch]++));
				break;
			case "char":
				varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "char", varCount[varSwitch]++));
				break;
			case "long":
				varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "long", varCount[varSwitch]++));
				break;
			case "int":
				varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "int", varCount[varSwitch]++));
				break;
			case "shrt":
				varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "shrt", varCount[varSwitch]++));
				break;
			case "byte":
				varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "byte", varCount[varSwitch]++));
				break;
			case "bool":
				varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "bool", varCount[varSwitch]++));
				break;
			default:
				varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, parent.GetNode(i).value, varCount[varSwitch]++));
				}
				
			}
		
	}
	
	public ArgsList<String> fromNodetoArg(ASTNode parent) {
		ArgsList<String> build = new ArgsList<>();
		for (int i = 1; i < parent.GetNodeSize()-1; i++) {
			
			if (results.Classes.get(curName).canGeneric() && results.Classes.get(curName).genType.containsKey("T" + parent.GetNode(i).value + ";")) {
				build.add(results.Classes.get(curName).genType.get("T" + parent.GetNode(i).value + ";"));
			}
			else {
				build.add(strToByte(parent.GetNode(i).value));
			}
			
			
			varPos.get(varSwitch).put(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, parent.GetNode(i).value, varCount[varSwitch]++));
			
			

			
			
		}
		return build;
		
	}
	
	public void closeMain() {
		if (!Objects.isNull(MainMethod)) {
			MainMethod.visitInsn(Opcodes.RETURN);
			MainMethod.visitMaxs(0, 0);
			MainMethod.visitEnd();
			
		}
		if (!Objects.isNull(MainClass)) {
			MainClass.visitEnd();
		}
	}
	public void newField(String name, String type, int Access, ASTNode node) {
		System.out.println(name + type + Access);
		cw.visitField(Access, name, strToByte(type), signatureWriterField(node), null).visitEnd();
		
	}
	public void uninitnewVar(String name, String type) {
		varPos.get(varSwitch).put(name, new VarInfo(name, type, varCount[varSwitch]++));
	}
	
	public void newVar(String name, String type, ASTNode generic) {
		System.out.println("FLY: " + name + " " + type);
		String conf = strToByte(type);
		castTopStackForVar(conf, popStack());
		switch(type) {
		case "I", "Z", "bool", "byte", "shrt", "int", "char":
			mv.visitVarInsn(Opcodes.ISTORE, varCount[varSwitch]);
			break;
		case "long", "J":
			mv.visitVarInsn(Opcodes.LSTORE, varCount[varSwitch]);
			break;
		case "doub", "D":
			mv.visitVarInsn(Opcodes.DSTORE, varCount[varSwitch]);
			break;
		case "flt":
			mv.visitVarInsn(Opcodes.FSTORE, varCount[varSwitch]);
			break;
		default:
			mv.visitVarInsn(Opcodes.ASTORE, varCount[varSwitch]);
			break;
		}
		
		if (generic == null) {
			varPos.get(varSwitch).put(name, new VarInfo(name, conf, type, varCount[varSwitch]++));
		}
		else {
			varPos.get(varSwitch).put(name, new GenVarInfo(name, conf, type, varCount[varSwitch]++).AddGenerics(generic, results, curName));
		}
		if (conf.equals("J") || conf.equals("D")) {
			varCount[varSwitch]++;
		}
		

	}
	
	public String loadVar(String name, ASTNode node) {
		
		VarInfo var;
		if (varPos.get(varSwitch).containsKey(name)) {
			var = varPos.get(varSwitch).get(name);
			System.out.println(var.name + var.type);
			if (var.type.contains("[")) {
				mv.visitVarInsn(Opcodes.ALOAD, var.pos);
				
				return var.toString();
			}
			switch(var.type) {
			case "Z":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return "Z";
			case "B":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return "B";
			case "S":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return "S";
			case "I":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return "I";
			case "C":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return "C";
			case "J":
				mv.visitVarInsn(Opcodes.LLOAD, var.pos);
				return "J";
			case "D":
				mv.visitVarInsn(Opcodes.DLOAD, var.pos);
				return "D";
			case "F":
				mv.visitVarInsn(Opcodes.FLOAD, var.pos);
				return "F";
			default:
				mv.visitVarInsn(Opcodes.ALOAD, var.pos);
				return var.toString();
				
			}
		}
		else {
			ClassInfo info;
			if (node.prev.type.equals("DOT")) {
				String prev = curStack.pop().type;
				if (prev.startsWith("[")) {
					mv.visitInsn(Opcodes.ARRAYLENGTH);
					return "I";
				}
				else {
					info = results.Classes.get(prev);
				}
			}
			else {
				info = results.Classes.get(curName);
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				
			}
			if (info.fields.containsKey(name)) {
				mv.visitFieldInsn(Opcodes.GETFIELD, info.name, name, info.fields.get(name).type);
				System.out.println("DOES THIS EVEN RESOLVE");
				return info.fields.get(name).type;
				
			}
			else {
				return "<ARRDEF>";
			}
			
			
			
		}
		
	
		
	}
	
	public void storeVar(String name, ASTNode node) {
		
		VarInfo var;
		if (varPos.get(varSwitch).containsKey(name)) {
			var = varPos.get(varSwitch).get(name);
			castTopStackForVar(var.type, popStack());
			switch(var.type) {
			case "I", "Z", "bool", "byte", "shrt", "int", "char":
				mv.visitVarInsn(Opcodes.ISTORE, var.pos);
				break;
			case "long", "J":
				mv.visitVarInsn(Opcodes.LSTORE, var.pos);
				break;
			case "doub", "D":
				mv.visitVarInsn(Opcodes.DSTORE, var.pos);
				break;
			case "flt":
				mv.visitVarInsn(Opcodes.FSTORE, var.pos);
				break;
			default:
				mv.visitVarInsn(Opcodes.ASTORE, var.pos);
				break;
			}
			
			
		}
		else {
			ClassInfo info;
			if (node.prev.type.equals("DOT")) {
				info = results.Classes.get(curStack.pop().type);
			}
			else {
				info = results.Classes.get(curName);
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitInsn(Opcodes.SWAP);
			}
			
			mv.visitFieldInsn(Opcodes.PUTFIELD, info.name, name, strToByte(info.fields.get(name).type));
			
		}
	}
	
	
	private void castTopStackForVar(String targetStack, String curStack) {
		switch(targetStack) {
		case "Z":
			switch(curStack) {
			case "Z":
				return;
			case "Ljava/lang/Boolean;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
				return;
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Boolean;":
			switch(curStack) {
			case "Z":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
				return;
			case "Ljava/lang/Boolean;":
				return;
			default:
				//error
				break;
			
			}
		case "I","S","B","C":
			switch(curStack) {
			case "B", "S", "I", "C":
				return;
			case "J":
				mv.visitInsn(Opcodes.L2I);
				return;
			case "F":
				mv.visitInsn(Opcodes.F2I);
				return;
			case "D":
				mv.visitInsn(Opcodes.D2I);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2I);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2I);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2I);
				return;
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Integer;":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "J":
				mv.visitInsn(Opcodes.L2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "F":
				mv.visitInsn(Opcodes.F2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "D":
				mv.visitInsn(Opcodes.D2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "Ljava/lang/Integer;":
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Character;":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "J":
				mv.visitInsn(Opcodes.L2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "F":
				mv.visitInsn(Opcodes.F2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "D":
				mv.visitInsn(Opcodes.D2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "Ljava/lang/Character;":
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Short;":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "J":
				mv.visitInsn(Opcodes.L2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "F":
				mv.visitInsn(Opcodes.F2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "D":
				mv.visitInsn(Opcodes.D2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "Ljava/lang/Short;":
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Byte;":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "J":
				mv.visitInsn(Opcodes.L2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "F":
				mv.visitInsn(Opcodes.F2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "D":
				mv.visitInsn(Opcodes.D2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "Ljava/lang/Byte;":
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2I);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			default:
				//error
				break;
			
			}

		case "J":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.I2L);
				return;
			case "J":
				return;
			case "F":
				mv.visitInsn(Opcodes.F2L);
				return;
			case "D":
				mv.visitInsn(Opcodes.D2L);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitInsn(Opcodes.I2L);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitInsn(Opcodes.I2L);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitInsn(Opcodes.I2L);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitInsn(Opcodes.I2L);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2L);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2L);
				return;
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Long;":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.I2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "J":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "F":
				mv.visitInsn(Opcodes.F2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "D":
				mv.visitInsn(Opcodes.D2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitInsn(Opcodes.I2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitInsn(Opcodes.I2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitInsn(Opcodes.I2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitInsn(Opcodes.I2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "Ljava/lang/Long;":
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2L);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			default:
				//error
				break;
			
			}
		case "F":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.I2F);
				return;
			case "J":
				mv.visitInsn(Opcodes.L2F);
				return;
			case "F":

				return;
			case "D":
				mv.visitInsn(Opcodes.D2F);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitInsn(Opcodes.I2F);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitInsn(Opcodes.I2F);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitInsn(Opcodes.I2F);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitInsn(Opcodes.I2F);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2F);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2F);
				return;
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Float;":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.I2F);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "J":
				mv.visitInsn(Opcodes.L2F);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "F":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "D":
				mv.visitInsn(Opcodes.D2F);				
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitInsn(Opcodes.I2F);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitInsn(Opcodes.I2F);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitInsn(Opcodes.I2F);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitInsn(Opcodes.I2F);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2F);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "Ljava/lang/Float;":
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				mv.visitInsn(Opcodes.D2F);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			default:
				//error
				break;
			
			}
		case "D":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.I2D);
				return;
			case "J":
				mv.visitInsn(Opcodes.L2D);
				return;
			case "F":
				mv.visitInsn(Opcodes.F2D);
				return;
			case "D":

				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitInsn(Opcodes.I2D);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitInsn(Opcodes.I2D);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitInsn(Opcodes.I2D);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitInsn(Opcodes.I2D);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2D);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2D);
				return;
			case "Ljava/lang/Double;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				return;
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Double;":
			switch(curStack) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.I2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "J":
				mv.visitInsn(Opcodes.L2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "F":
				mv.visitInsn(Opcodes.F2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "D":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "Ljava/lang/Byte;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				mv.visitInsn(Opcodes.I2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "Ljava/lang/Short;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				mv.visitInsn(Opcodes.I2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "Ljava/lang/Character;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				mv.visitInsn(Opcodes.I2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "Ljava/lang/Integer;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				mv.visitInsn(Opcodes.I2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "Ljava/lang/Long;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
				mv.visitInsn(Opcodes.L2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "Ljava/lang/Float;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				mv.visitInsn(Opcodes.F2D);
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			case "Ljava/lang/Double;":
				return;
			
			default:
				//error
				break;
			
			}
		case "Ljava/lang/Object;":
			switch(curStack) {
			case "B":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				return;
			case "S":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				return;
			case "I":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				return;
			case "C":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				return;
			case "J":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				return;
			case "F":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				return;
			case "D":
				mv.visitMethodInsn(Opcode.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				return;
			}
		default:
			return;
		}
		
		
		
	}

	public void conditionalE(ASTNode node, Label label) {
		switch(node.type) {
		case "BOOL":
			if (node.value.equals("true")) {
				mv.visitLdcInsn(1);
			}
			else {
				mv.visitLdcInsn(0);
			}
			mv.visitJumpInsn(Opcodes.IFNE, label);
			break;
		case "TRUEEQUALS":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			CTrueEquals(label);
			break;
		case "NOTEQUALS":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			CNotEquals(label);
			break;
		case "TRUEGREATERTHAN":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			CTrueGreaterThan(label);
			break;
		case "TRUELESSTHAN":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			CTrueLessThan(label);
			break;
		case "GREATERTHAN":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			CGreaterThan(label);
			break;
		case "LESSTHAN":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			CLessThan(label);
			break;
		}
	}
	
	private void CLessThan(Label l) {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Z":
				mv.visitJumpInsn(Opcodes.IF_ICMPLT, l);
				break;
			default:
				//throw error
				break;
			}
			return;
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPLT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			default:
				//error;
				break;
			}
			return;
		}
		
	}
		
	

	private void CTrueLessThan(Label l) {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Z":
				mv.visitJumpInsn(Opcodes.IF_ICMPLE, l);
				break;
			default:
				//throw error
				break;
			}
			return;
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPLE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		}
		
	
		
	}

	private void CTrueGreaterThan(Label l) {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Z":
				mv.visitJumpInsn(Opcodes.IF_ICMPGE, l);
				break;
			default:
				//throw error
				break;
			}
			return;
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPGE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		}
		
	
		
	}

	private void CGreaterThan(Label l) {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Z":
				mv.visitJumpInsn(Opcodes.IF_ICMPGT, l);
				break;
			default:
				//throw error
				break;
			}
			return;
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPGT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			default:
				//error;
				break;
			}
			return;
		}
		
	}

	private void CNotEquals(Label l) {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Z":
				mv.visitJumpInsn(Opcodes.IF_ICMPNE, l);
				break;
			default:
				//throw error
				break;
			}
			return;
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPNE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			default:
				//error;
				break;
			}
			return;
		default:
			if (s1.type.equals(s2.type)) {
				mv.visitJumpInsn(Opcodes.IF_ACMPNE, l);
			}
			else {
				//error
			}
			return;
		}
		
	}

	private void CTrueEquals(Label l) {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Z":
				mv.visitJumpInsn(Opcodes.IF_ICMPEQ, l);
				break;
			default:
				//throw error
				break;
			}
			return;
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPEQ, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			default:
				//error;
				break;
			}
			return;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			default:
				//error;
				break;
			}
			return;
		default:
			if (s1.type.equals(s2.type)) {
				mv.visitJumpInsn(Opcodes.IF_ACMPEQ, l);
			}
			else {
				//error
			}
			return;
		}
		
	}
	



	
	public String evalE(ASTNode node) {
		switch(node.type) {
		case "DOT":
			evalE(node.GetFirstNode());
			return evalE(node.GetNode(1));
		case "FUN":
			curStack.push(new StackInfo(invokeEasy(node), mv.size()));
			if (stackTop().equals("V")) {
				return popStack();
			}
			return stackTop();
		case "GENFUN":
			curStack.push(new StackInfo(constWithGen(node), mv.size()));
			return stackTop();
		case "VAR":
			curStack.push(new StackInfo(loadVar(node.value, node), mv.size()));
			return stackTop();
		case "ARR":
			return curStack.push(new StackInfo(LoadArrIndex(loadVar(node.value, node), node, 0), mv.size())).type;
		case "BRACE":
			return initArray(node);
		case "C":
			mv.visitLdcInsn(node.value.charAt(0));
			curStack.push(new StackInfo("C", mv.size()));
			return "C";
		case "Ljava/lang/String;":
			mv.visitLdcInsn(node.value);
			curStack.push(new StackInfo("Ljava/lang/String;", mv.size()));
			return "Ljava/lang/String;";
		case "I":
			mv.visitLdcInsn(Integer.parseInt(node.value));
			curStack.push(new StackInfo("I", mv.size()));
			return "I";
		case "D":
			mv.visitLdcInsn(Double.parseDouble(node.value));
			curStack.push(new StackInfo("D", mv.size()));

			return "D";
		case "Z":
			
			if (node.value.equals("true")) {
				mv.visitLdcInsn(1);
			}
			else {
				mv.visitLdcInsn(0);
			}
			curStack.push(new StackInfo("Z", mv.size()));
			return "Z";
		case "ADD":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(EAdd(), mv.size())).type;
		case "SUB":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(ESub(), mv.size())).type;
		case "MUL":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(EMul(), mv.size())).type;
		case "DIV":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(EDiv(), mv.size())).type;
		case "REM":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(ERem(), mv.size())).type;
		case "EXP":
			break;
			//to be done
		}
		
		return "NULL";
	}
	public VarInfo getVar(String name) {
		return varPos.get(varSwitch).get(name);
	}
	
	public String evalE(ASTNode node, String TypeExpected) {
		switch(node.type) {
		case "DOT":
			evalE(node.GetFirstNode());
			return evalE(node.GetNode(1));
		case "FUN":
			curStack.push(new StackInfo(invokeEasy(node), mv.size()));
			if (stackTop().equals("V")) {
				return popStack();
			}
			return stackTop();
		case "GENFUN":
			curStack.push(new StackInfo(constWithGen(node), mv.size()));
			return stackTop();
		case "VAR":
			curStack.push(new StackInfo(loadVar(node.value, node), mv.size()));
			return stackTop();
		case "ARR":
			return curStack.push(new StackInfo(LoadArrIndex(loadVar(node.value, node), node, 0), mv.size())).type;
		case "BRACE":
			return initArray(node);
		case "C":
			mv.visitLdcInsn(node.value.charAt(0));
			curStack.push(new StackInfo("C", mv.size()));
			return "C";
		case "Ljava/lang/String;":
			mv.visitLdcInsn(node.value);
			curStack.push(new StackInfo("Ljava/lang/String;", mv.size()));
			return "Ljava/lang/String;";
		case "I":
			if (TypeExpected.equals("J") || TypeExpected.equals("Ljava/lang/Long;")) {
				mv.visitLdcInsn(Long.parseLong(node.value));
				curStack.push(new StackInfo("J", mv.size()));
				return "J";
			}
			else {
				mv.visitLdcInsn(Integer.parseInt(node.value));
				curStack.push(new StackInfo("I", mv.size()));
				return "I";
			}
			
		case "D":
			if (TypeExpected.equals("F") || TypeExpected.equals("Ljava/lang/Float;")) {
				mv.visitLdcInsn(Float.parseFloat(node.value));
				curStack.push(new StackInfo("F", mv.size()));
				
				return "F";
				
			}
			else {
				mv.visitLdcInsn(Double.parseDouble(node.value));
				curStack.push(new StackInfo("D", mv.size()));

				return "D";
			}
			
		case "Z":
			
			if (node.value.equals("true")) {
				mv.visitLdcInsn(1);
			}
			else {
				mv.visitLdcInsn(0);
			}
			curStack.push(new StackInfo("Z", mv.size()));
			return "Z";
		case "ADD":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(EAdd(), mv.size())).type;
		case "SUB":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(ESub(), mv.size())).type;
		case "MUL":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(EMul(), mv.size())).type;
		case "DIV":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(EDiv(), mv.size())).type;
		case "REM":
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(ERem(), mv.size())).type;
		case "EXP":
			break;
			//to be done
		}
		
		return "NULL";
	}


	private String EAdd() {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(Z)Ljava/lang/String;", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//throw error
				break;
			}
			break;
		case "Ljava/lang/Boolean;":
			switch(s2.type) {
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Boolean", "toString", "()Ljava/lang/String;", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//throw error
				break;
			}
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(" + s1.type + ")Ljava/lang/String;", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/Object;)V", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/Object;)V", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/Object;)V", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/Object;)V", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(J)Ljava/lang/String;", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/Object;)V", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(F)Ljava/lang/String;", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);

				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(D)Ljava/lang/String;", false), s1.posInQueue-1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
		
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/String;":
			switch(s2.type) {
			case "B":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(B)Ljava/lang/String;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			case "S":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(S)Ljava/lang/String;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
				
			case "C":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(C)Ljava/lang/String;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
				
			case "I":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			case "J":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(J)Ljava/lang/String;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
				
			case "F":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(F)Ljava/lang/String;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
				
			case "D":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(D)Ljava/lang/String;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
				
			case "Ljava/lang/String":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);
				return "Ljava/lang/String;";
			}
			
		default:
			if (s2.type.equals("Ljava/lang/String;")) {
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+1);
				
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, s2.type.substring(1, s2.type.length()-1), "toString()", "()Ljava/lang/String;", false), s2.posInQueue-1);
				
				//one before other for efficiency as it pushes
				
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue-1);

				
				return "Ljava/lang/String;";
			}
			else {
				//error;
			}
		}
		return null;
		
	}
	private String ESub() {

		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				//error;
				break;
			}
			break;
		default:
				//error;
			
		}
		return null;
		
	}
	private String EMul() {

		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				//error;
				break;
			}
			break;
		default:
				//error;
			
		}
		return null;
		
	}
	private String EDiv() {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				//error;
				break;
			}
			break;
		default:
				//error;
			
		}
		return null;
		
	}
	private String ERem() {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue-1);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue-1);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				//error;
				break;
			}
			break;
		default:
				//error;
			
		}
		return null;
		
	}







	
	
	private String constWithGen(ASTNode tree) {
		boolean flag;
		mv.visitTypeInsn(Opcodes.NEW, IfImport(tree.value));
		mv.visitInsn(Opcodes.DUP);
		size = curStack.size();
		if (tree.GetNodeSize() > 0) evalE(tree.GetLastNode());
		MethodInfo info = results.Classes.get(tree.value).methods.get(tree.value);
		ArrayList<ArrayList<String>> stacks = getAllRangeStack(curStack.size()-size);
		if ((!Objects.isNull(info))) {
			for (int i = 0; i < info.args.size(); i++) {
				if (info.args.get(i).size() == stacks.size()) {
					flag = true;
					for (int j = 0; j < stacks.size(); j++) {
						if (!stacks.get(j).contains(info.args.get(i).get(j))) {
							flag = false;
						}
					}
					if (flag) {
						
						this.invokeSpecial("<init>", IfImport(tree.value), info.args.get(i).toArgs() + "V");
						return strToByte(tree.value);
					}
				}
			}
				
			
		}
		
		return null;
	}

	private String typedGeneric(ASTNode gen) {

		StringBuilder b = new StringBuilder("<");
		for (int i = 0; i < gen.GetNodeSize(); i++) {
			switch(gen.GetNode(i).type) {
			case "CLASSNAME":
				b.append("T" + IfImport(gen.GetNode(i).value) + ";");
				break;
			case "INFERRED":
				b.append('*');
				break;
			case "CLASSMODIFIER":
				if (gen.GetNode(i).value.equals("extends")) {
					b.append('+');
				}
				else {
					b.append('-');
				}
				b.append(strToByte(gen.GetNode(i).GetNode(1).value));
				break;
			case "GENERIC":
				b.append(typedGeneric(gen.GetNode(i)));
				break;
			}
			
		}
		return b.append(">").toString();
	}
	private String signatureWriterMethod(ASTNode methodCall) {
		ASTNode curGen;
		StringBuilder b = new StringBuilder("(");
		int i = 1;
		boolean flag = false;
		String resultString;
		while (methodCall.GetNodeSize() > i && !(curGen = methodCall.GetNode(i)).type.equals("START")) {
			b.append(resultString = strToByte(curGen.value));
			if (resultString.startsWith("T")) {
				flag = true;
			}
			if ((curGen = curGen.Grab("GENERIC")) != null) {
				b.deleteCharAt(b.length()-1);
				b.append(typedGeneric(curGen));
				b.append(";");
				flag = true;
			}
			i++;
		}
		b.append(")");
		
		
		if (flag) {
			b.append(strToByte(methodCall.value));
			if ((curGen = methodCall.Grab("GENERIC")) != null) {
				b.append(typedGeneric(curGen));
			}
			return b.toString();
		}
		return null;
	}
	
	
	
	private Object parseGeneric(ASTNode curGen) {
		StringBuilder r = new StringBuilder("<");
		for (int i = 0; i < curGen.GetNodeSize(); i++) {
			 switch(curGen.GetNode(i).type) {
			 case "CLASSNAME":
				 r.append(curGen.GetNode(i).value);
				 r.append(":");
				 r.append("Ljava/lang/Object;");
				 break;
			 case "CLASSMODIFIER":
				 r.append(curGen.GetNode(i).GetFirstNode().value);
				 r.append(":");
				 r.append(strToByte(curGen.GetNode(i).GetNode(1).value));
				 break;
			 case "GENERIC":
				 r.append(parseGeneric(curGen.GetNode(i)));
				 break;
			 }
		}
		return r.append(">").toString();
	}

	private String signatureWriterClass(ASTNode classCall) {
		ASTNode curGen;
		StringBuilder b = new StringBuilder();


		if ((curGen = classCall.Grab("GENERIC")) != null) {
			b.append(parseGeneric(curGen));
			if ((curGen = classCall.Grab("PARENT")) != null) {
				b.append(strToByte(curGen.value));
			}
			else {
				b.append("Ljava/lang/Object;");
			}
			return b.toString();
		}


		
		
		return null;
	}
	private String signatureWriterField(ASTNode fieldCall) {
		ASTNode curGen;
		boolean flag = false;
		StringBuilder b = new StringBuilder();

		b.append(strToByte(fieldCall.value));
		if ((curGen = fieldCall.Grab("GENERIC")) != null) {
			b.append(parseGeneric(curGen));
			flag = true;
		}
		if (flag) {
			return b.toString();
		}
		
		return null;
	}

	public String LoadArrIndex(String type, ASTNode node, int MatrixIndex) {
		if (type.equals("<ARRDEF>")) {
			if (node.GetNodeSize() > 1) {
				String valType;
				for (int i = 0; i < node.GetNodeSize(); i++) {
					evalE(node.GetNode(i), "I");
				}
				mv.visitMultiANewArrayInsn(valType = strToByte(node.value), node.GetNodeSize());
				return valType;
			}
			System.out.println(node.value + " jk");
			evalE(node.GetFirstNode(), "I");
			switch(node.value) {
			case "bool":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
				return "[Z";
			case "byte":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
				return "[B";
			case "shrt":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
				return "[S";
			case "int":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
				return "[I";
			case "char":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR);
				return "[C";
			case "long":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
				return "[J";
			case "doub":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
				return "[D";
			case "flt":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
				return "[F";
			default:
				String valType = strToByte(node.value);
				mv.visitTypeInsn(Opcodes.ANEWARRAY, valType.substring(1, valType.length()-1));
				return valType;
			}
		}
		evalE(node.GetNode(MatrixIndex));
		castTopStackForVar("I", popStack());
		if (node.GetNodeSize() - MatrixIndex > 1) {
			mv.visitInsn(Opcodes.AALOAD);
			return LoadArrIndex(type.replaceFirst("\\[", ""), node, MatrixIndex + 1);
		}
		else {
			switch(type) {
			case "[I":
				mv.visitInsn(Opcodes.IALOAD);
				return "I";
			case "[Z":
				mv.visitInsn(Opcodes.IALOAD);
				return "Z";
			case "[B":
				mv.visitInsn(Opcodes.BALOAD);
				return "B";
			case "[S":
				mv.visitInsn(Opcodes.SALOAD);
				return "S";
			case "[C":
				mv.visitInsn(Opcodes.CALOAD);
				return "C";
			case "[D":
				mv.visitInsn(Opcodes.DALOAD);
				return "D";
			case "[F":
				mv.visitInsn(Opcodes.FALOAD);
				return "F";
			case "[J":
				mv.visitInsn(Opcodes.LALOAD);
				return "J";
			default:
				System.out.println(type + "sup");
				mv.visitInsn(Opcodes.AALOAD);
				return type.replaceFirst("\\[", "");
			}
		}
		
	}

	private String initArray(ASTNode node) {
		ASTNode temp = node;
		int slack = -2;
		String valType;
		while (temp.type != "DECLARATION" && temp.type != "DESCRIPTION") {
			temp = temp.prev;
			slack += 2;
		}
		int enCapCode;
		mv.visitLdcInsn(node.GetNodeSize());
		switch(temp.value.substring(0, temp.value.length()-slack)) {
		case "bool[]":
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
			enCapCode = Opcodes.IASTORE;
			valType = "Z";
			break;
		case "byte[]":
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
			enCapCode = Opcodes.BASTORE;
			valType = "B";
			break;
		case "shrt[]":
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
			enCapCode = Opcodes.SASTORE;
			valType = "S";
			break;
		case "int[]":
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
			enCapCode = Opcodes.IASTORE;
			valType = "I";
			break;
		case "char[]":
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR);
			enCapCode = Opcodes.CASTORE;
			valType = "C";
			break;
		case "long[]":
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
			enCapCode = Opcodes.LASTORE;
			valType = "J";
			break;
		case "doub[]":
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
			enCapCode = Opcodes.DASTORE;
			valType = "D";
			break;
		case "flt[]":
			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
			enCapCode = Opcodes.FASTORE;
			valType = "F";
			break;
		default:
			valType = strToByte(temp.value.substring(0, temp.value.length()-slack-2));
			if (valType.contains("[")) {
				mv.visitTypeInsn(Opcodes.ANEWARRAY, valType);
			}
			else {
				mv.visitTypeInsn(Opcodes.ANEWARRAY, valType.substring(1, valType.length()-1));
			}
			
			enCapCode = Opcodes.AASTORE;
			
			break;
		}
		for (int i = 0; i < node.GetNodeSize(); i++) {
			mv.visitInsn(Opcodes.DUP);
			mv.visitLdcInsn(i);
			evalE(node.GetNode(i));
			castTopStackForVar(valType, popStack());
			mv.visitInsn(enCapCode);
		}
		
		return (strToByte(temp.value.substring(0, temp.value.length()-slack)));
		
	}

	public void If(ASTNode tree) {
		//deprecated
		labelList.add(new Label());
		labelList.add(new Label());
		labelList.add(new Label());
		conditionalE(tree, labelList.peek());
		mv.visitJumpInsn(Opcodes.GOTO, labelList.get(labelList.size()-2));
		mv.visitLabel(labelList.pop());
	}
	public void Else() {
		mv.visitLabel(labelList.pop());
	}
	public void EndOfIf() {
		mv.visitJumpInsn(Opcodes.GOTO, labelList.get(labelList.size()-2));
	}
	
	public void EndElse() {
		mv.visitJumpInsn(Opcodes.GOTO, labelList.peek());
		mv.visitLabel(labelList.pop());
	}
	
	public void While(ASTNode tree) {
		labelList.add(new Label());
		labelList.add(new Label());
		conditionalE(tree, labelList.peek());
		mv.visitLabel(labelList.peek());
	}
	public void EndWhile(ASTNode tree) {
		conditionalE(tree, labelList.pop());
		mv.visitJumpInsn(Opcodes.GOTO, labelList.peek());
		mv.visitLabel(labelList.pop());
	}
	public void For(ASTNode tree) {
		//keep in mind redoing parser needs redoing this
		labelList.add(new Label());
		labelList.add(new Label());
		evalE(tree.GetFirstNode().GetNode(1));
		newVar(tree.GetFirstNode().GetFirstNode().value, tree.GetFirstNode().value, null);
		conditionalE(tree.GetNode(1), labelList.peek());
		mv.visitLabel(labelList.peek());
	}
	public void EndFor(ASTNode tree) {
		evalE(tree.GetNode(2));
		storeVar(tree.GetFirstNode().GetFirstNode().value, null);
		conditionalE(tree.GetNode(1), labelList.pop());
		mv.visitJumpInsn(Opcodes.GOTO, labelList.peek());
		mv.visitLabel(labelList.pop());
	}
	
	public String invokeEasy(ASTNode tree) {
		String[] Methodinfo;
		if (tree.prev.type.equals("DOT")) {
			LinkedHashMap<String, String> genType = null;
			top = curStack.pop().type;
			if (top.startsWith("[")) {
				top = "[";
			}
			else {
				genType = sigToHash(top);
				top = stripObj(top);
			}
			size = curStack.size();
			if (tree.GetNodeSize() > 0) evalE(tree.GetLastNode());
			if (genType == null) {
				Methodinfo = checkMethodvStack(tree.value, top, curStack.size()-size);
			}
			else {
				Methodinfo = checkMethodvStack(tree.value, top, curStack.size()-size, genType);
			}
			if (Methodinfo[2].contains("static")) {
				invokeStatic(tree.value, IfImport(top), Methodinfo[0] + Methodinfo[1]);	
			}
			else {
				invokePublic(tree.value, IfImport(top), Methodinfo[0] + Methodinfo[1]);	
			}
			return Methodinfo[1];
		}
		else if (constructorCheck(tree.value)){
			Methodinfo = constructorDo(tree.value, tree);
			invokeSpecial("<init>", IfImport(tree.value), Methodinfo[0] + Methodinfo[1]);
			return strToByte(tree.value);
		}
		else {
			size = curStack.size();
			if (tree.GetNodeSize() > 0) evalE(tree.GetLastNode());
			Methodinfo = checkMethodvStack(tree.value, curName, curStack.size()-size);
			if (Methodinfo[2].contains("static")) {
				invokeStatic(tree.value, curName, Methodinfo[0] + Methodinfo[1]);
			}
			else {
				invokePublic(tree.value, curName, Methodinfo[0] + Methodinfo[1]);
			}
			
			return Methodinfo[1];
		}
	}
	private LinkedHashMap<String, String> sigToHash(String stack) {
		int splitpoint = stack.indexOf('<');
		if (splitpoint < 0) return null;
		int lastSlash = stack.substring(0, splitpoint).lastIndexOf('/');
		if (lastSlash < 0) lastSlash = 0;

		
		Iterator<String> gen = results.Classes.get(stack.substring(lastSlash+1, splitpoint)).genType.keySet().iterator();
		LinkedHashMap<String, String> returnee = new LinkedHashMap<>();
		StringBuilder build = new StringBuilder();
		int brackets = 0;
		for (int i = splitpoint+1; i < stack.length()-1; i++) {
			switch(stack.charAt(i)) {
			case ';':
				if (brackets == 0) {
					returnee.put(gen.next(), build.append(';').toString());
					build.setLength(0);
				}
				else {
					build.append(";");
				}
				
				break;
			case '>':
				if (brackets != 0 ) {
					build.append(">");
					brackets--;
				}
				break;
			case '<':
				build.append("<");
				brackets++;
				break;
			default:
				build.append(stack.charAt(i));
				break;
			}
		}
		
		return returnee;
	}

	private boolean constructorCheck(String Classname) {
		if (results.Classes.containsKey(Classname)) {
			return true;
		}
		return false;
	}

	public void invokeSpecial(String name, String owner, String args) {
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, args, false);
	}

	
	public void invokeStatic(String name, String owner, String args) {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, args, false);
	}
	
	public void invokePublic(String name, String owner, String args) {
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, args, false);
	}
	
	public void staticField(String name, String owner, String type) {
		mv.visitFieldInsn(Opcodes.GETSTATIC, owner, name, type);
	}
	
//	public void placeConst(ASTNode node) {
//		switch(node.type) {
//		case "STR":
//			mv.visitLdcInsn(node);
//		}
//		case "NUM":
//			mv.visitLdcInsn();
//	}
	
	public void saveMain() {
		FileOutputStream out;
		try {
			if (!Objects.isNull(MainClass)) {
				out = new FileOutputStream("Main.class");
				out.write(MainClass.toByteArray());
				out.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void saveClass() {
		FileOutputStream out;
		try {
			out = new FileOutputStream(curName + ".class");
			out.write(cw.toByteArray());
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		curName = null;
	}
	
	public void runClass(String filename) {
		String cmd = "java -cp \"C:\\Users\\Nolan Murray\\git\\cro\\crodot\" "  + filename;
		try {

			Process comm = Runtime.getRuntime().exec(cmd);
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(comm.getInputStream()));
			System.out.println("**********");
			while ((line = reader.readLine()) != null) {
				System.out.println(line);

			}
			reader.close();
			System.out.println("**********");
			reader = new BufferedReader(new InputStreamReader(comm.getErrorStream()));
			
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
			
			reader.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public int stackSize() {
		return curStack.size();
	}

	public void Return(ASTNode tree) {
		switch(returnType) {
		case "V":
			mv.visitInsn(Opcodes.RETURN);
			break;
		case "Z", "C", "S", "I":
			evalE(tree);
			curStack.pop();
			mv.visitInsn(Opcodes.IRETURN);
			break;
		case "J":
			evalE(tree);
			curStack.pop();
			mv.visitInsn(Opcodes.LRETURN);
			break;
		case "F":
			evalE(tree);
			curStack.pop();
			mv.visitInsn(Opcodes.FRETURN);
			break;
		case "D":
			evalE(tree);
			curStack.pop();
			mv.visitInsn(Opcodes.DRETURN);
			break;
		default:
			evalE(tree);
			curStack.pop();
			mv.visitInsn(Opcodes.ARETURN);
			break;
		}
		
	}

	public void storeArr(String name, ASTNode tree, ASTNode evalE) {
		ASTNode temp = tree.GetFirstNode();
		loadVar(name, tree);
		evalE(temp);
		
		while (temp.GetNodeSize() > 0) {
			popStack();
			mv.visitInsn(Opcodes.AALOAD);
			temp = temp.GetFirstNode();
			evalE(temp);
		}
		popStack();
		evalE(evalE);
		switch(popStack()) {
		case "I", "Z":
			mv.visitInsn(Opcodes.IASTORE);
			break;
		case "B":
			mv.visitInsn(Opcodes.BASTORE);
			break;
		case "S":
			mv.visitInsn(Opcodes.SASTORE);
			break;
		case "C":
			mv.visitInsn(Opcodes.CASTORE);
			break;
		case "D":
			mv.visitInsn(Opcodes.DASTORE);
			break;
		case "F":
			mv.visitInsn(Opcodes.FASTORE);
			break;
		default:
			mv.visitInsn(Opcodes.AASTORE);
			break;
		}
		
	}

	private String strToByteGeneric(String str, HashMap<String, String> methodG, LinkedHashMap<String, String> classG) {
		StringBuilder r = new StringBuilder();
		String temp;
		int last = 0;
		int bracks = 0;
		for (int i = 0; i < str.length(); i++) {
			switch(str.charAt(i)) {
			case ',':
				temp = str.substring(last, i-bracks);
				if (temp.isBlank()) {
					
				}
				else if (methodG != null && methodG.containsKey(temp)) {
					r.append(methodG.get(temp));
				}
				else if (classG != null && classG.containsKey("T" + temp + ";")) {
					r.append("T");
					r.append(temp);
					r.append(";");
				}
				else {
					r.append("L");
					r.append(IfImport(temp).replace('.', '/'));
					r.append(";");
				}
				bracks = 0;
				last = i+2;
				break;
			case '<':
				temp = str.substring(last, i-bracks);
				if (temp.isBlank()) {
					
				}
				else if (methodG != null && methodG.containsKey(temp)) {
					r.append("L");
					r.append(methodG.get(temp));
					r.append("<");
				}
				else if (classG != null && classG.containsKey("T" + temp + ";")) {
					r.append("T");
					r.append(temp);
					r.append('<');
				}
				else {
					r.append("L");
					r.append(IfImport(temp).replace('.', '/'));
					r.append("<");
				}
				bracks = 0;
				last = i+1;
				break;
			case '>':
				temp = str.substring(last, i-bracks);
				if (temp.isBlank()) {
					r.append(">");
					r.append(";");
				}
				else if (methodG != null && methodG.containsKey(temp)) {
					r.append("L");
					r.append(methodG.get(temp));
					r.append(";");
					r.append(">");
					r.append(";");
				}
				else if (classG != null && classG.containsKey("T" + temp + ";")) {
					r.append("T");
					r.append(temp);
					r.append(";");
					r.append(">");
					r.append(";");
				}
				else {
					r.append("L");
					r.append(IfImport(temp).replace('.', '/'));
					r.append(";");
					r.append(">");
					r.append(";");
				}
				bracks = 0;
				last = i+1;
				break;
			case '[':
				r.append('[');
				bracks += 2;
				break;
			case ']':
				break;
			case ' ':
				if (str.charAt(i-1) == '?') {
					if (str.charAt(i+1) == 'e') {
						r.append('+');
					}
					else {
						r.append('-');
					}
				}
				else if (str.charAt(i-1) == ','){
					break;
				}
				else {
					last = i+1;
				}
				break;
			case '?':
				if (str.charAt(i+1) == '>') {
					r.append('*');
					last = i+1;
				}
				break;
			}
		}
		
		return r.toString();
	}

}


