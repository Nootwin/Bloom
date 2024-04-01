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
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Stack;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import crodotInsn.CrodotIInc;
import crodotInsn.CrodotInsn;
import crodotInsn.CrodotInt;
import crodotInsn.CrodotMethod;
import crodotInsn.CrodotType;
import crodotInsn.CrodotVar;
import crodotStates.TokenState;
import javassist.bytecode.Opcode;


public class CodeCreator {
	private String sourceFile;
	private String curName;
	private String returnType;
	private ClassWriter cw;
	public CrodotMethodVisitor mv;
	private ClassWriter OtherClass;
	private ClassWriter MainClass;
	private CrodotMethodVisitor MainMethod;
	private Stack<Label> labelList;
	private String top;
	private int size;
	private Stack<StackInfo> curStack;
	private ErrorThrower err;
	private ASTNode storeEvalE;
	AnaResults results;
	private ArrayList<Integer> getAllRangeStackPos;
	private VariableManager vars;
	LinkedHashMap<String, String> curGenType;
	private Analyser analy;
	
	
	CodeCreator(AnaResults results, ErrorThrower err, Analyser analy, String sourceFile) {
		vars = new VariableManager();
		labelList = new Stack<>();
		curStack = new Stack<>();
//		varSwitch = 1;
//		varPos = new ArrayList<>();
//		varPos.add(null);
//		varPos.add(null);
		this.results = results;
		this.err = err;
		this.sourceFile = sourceFile;
		this.analy = analy;
		
		MainClass = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		MainClass.visit(Opcodes.V19, Opcodes.ACC_PUBLIC, "Main", null, "java/lang/Object", null);
		MainClass.visitSource(sourceFile, null);
		
		MainMethod = new CrodotMethodVisitor(MainClass.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null));
//		varPos.set(0, new HashMap<>());
//		varPos.get(0).put("args", new VarInfo("args", "str", 0));
		vars.addMain("args",  new VarInfo("args", "str", 0));
		vars.setMainLength(1);
//		varCount[0] = 1;
		labelList = new Stack<>();
	}
	
	public void setCurGenType(ASTNode gen, String name) {
		if (gen == null) {
			curGenType = null;
			return;
		}
		curGenType = createGenType(gen, name);
	}
	
	public LinkedHashMap<String, String> createGenType(ASTNode gen, String name) {
		String type = IfImport(name);
		System.out.println(name);
		LinkedHashMap<String, String> map = new LinkedHashMap<>();;
		ClassInfo info = results.Classes.get(type);
		Iterator<String> keys = info.genType.keySet().iterator();
		StringBuilder sig = new StringBuilder();
		//why getting called
		for (int i = 0; i < info.genType.size(); i++) {
			map.put(keys.next(), nodeToString(gen.GetNode(i), info));
		}
		
		return map;
	}
	
	public String nodeToString(ASTNode node, ClassInfo info) {
		StringBuilder sig = new StringBuilder();
		if (info.canGeneric() && info.genType.containsKey(("T" + node.value + ";"))) {
			sig.append("T");
			sig.append(node.value);
			sig.append(";");
		}
		else {
			sig.append("L");
			if (results.qNames.containsKey(node.value)) {
				sig.append(results.qNames.get(node.value));
			}
			else {
				sig.append(node.value);
			}
			if (node.GetNodeSize() > 0) {
				sig.append("<");
				for (int i = 0; i < node.GetNodeSize(); i++) {
					sig.append(nodeToString(node.GetNode(i), info));
				}
				sig.append(">");
			}
			sig.append(";");
		}
		return sig.toString();
	}
	
	public void ClearStack() {
		if (mv != null ) {
			while (!curStack.isEmpty()) {
				curStack.pop();
				mv.visitInsn(Opcodes.POP);
			}
		}
	}
	public String[] checkMethodvStack(String Methodname, String Classname, int size, int LineNum) {
		int[] priority = null;
		int[] tempPrio;
		boolean flag;
		int indexOf;
		MethodInfo info = results.Classes.get(Classname).methods.get(Methodname);
		if (info == null) {
			err.UnknownMethodException(LineNum, Methodname, Classname);
		}
		ArrayList<ArrayList<String>> stacks = getAllRangeStack(size);
		//System.out.println("INVOKE" + stacks);
		if ((!Objects.isNull(info))) {
			for (int i = 0; i < info.args.size(); i++) {
				if (info.args.get(i).size() == stacks.size()) {
					tempPrio = new int[size+1];
					tempPrio[0] = i;
					flag = true;
					for (int j = 0; j < stacks.size(); j++) {
						System.out.println("TYPETYPE" + stacks.get(j).toString());
						if ((indexOf = stacks.get(j).indexOf(info.args.get(i).get(j))) != -1) {
						
							tempPrio[j+1] = indexOf;
						}
						else if (stacks.get(j).get(0).equals("null") && info.args.get(i).get(j).length() > 1) {
							tempPrio[j+1] = 0;
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
								err.AmbiguousMethodCallExecption(LineNum, Methodname);
							}
						}
					}	
				}
			}
			if (priority != null) {
				addCastings(info.args.get(priority[0]), stacks);
				String ret;
				if (results.Classes.get(curName).canGeneric()) {
					ret = replaceReturn(info.returnType.get(priority[0]), results.Classes.get(curName).genType);
					if (ret.length() > 1 && !results.Classes.containsKey(stripToImport(ret))) {
						analy.Import(ImportFormat(ret));
					}
					
					return new String[] {replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(curName).genType), ret, info.AccessModifiers};
				}
				else {
					ret = info.returnType.get(priority[0]);
					if (ret.length() > 1 && !results.Classes.containsKey(stripToImport(ret))) {
						analy.Import(ImportFormat(ret));
					}
					return new String[] {info.args.get(priority[0]).toArgs(), ret, info.AccessModifiers};
				}
			}
				
			
		}
		System.out.println("damn daniel " + Methodname + "   " + curName + "   "+ Classname +"   "+ stacks.toString());
		return null;
	}
	
	public void storeField(ASTNode eval, StackInfo parent) {
		String name;
		if (eval.prev.prev.type == TokenState.DOT) {
			ClassInfo info = results.Classes.get(name = stripToImport(parent.type));
			mv.visitFieldInsn(Opcodes.PUTFIELD, name, eval.value, info.fields.get(eval.value).type);
		}
		else {
			ClassInfo info = results.Classes.get(curName);
			mv.visitFieldInsn(Opcodes.PUTFIELD, curName, eval.value, info.fields.get(eval.value).type);
		}
	}
	
	public String stripToImport(String type) {
		int i;
		if ((i = type.indexOf("<")) == -1) {
			return type.substring(1, type.length()-1);
		}
		else {
			return type.substring(1, i);
		}
	}
	
	private void addCastings(ArgsList<String> argsList, ArrayList<ArrayList<String>> stacks) {

		for (int i = 0; i < argsList.size(); i++) {
			if (stacks.get(i).get(0).length() < 2 && argsList.get(i).length() > 1 && stacks.get(i).get(0) != argsList.get(i) ) {
				castingsPrimitive(stacks.get(i).get(0), getAllRangeStackPos.get(i));

			}
		}
		
	}
	
	private void castingsPrimitive(String type, int posInQueue) {
		switch(type) {
		case "Z":
			mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false), posInQueue);
			break;
		case "B":
			mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false), posInQueue);
			break;
		case "S":
			mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false), posInQueue);
			break;
		case "I":
			mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false), posInQueue);
			break;
		case "C":
			mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false), posInQueue);
			break;
		case "F":
			mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false), posInQueue);
			break;
		case "D":
			mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false), posInQueue);
			break;
		case "J":
			mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false), posInQueue);
			break;
		}
	}

	
	

	public String[] checkMethodvStack(String Methodname, String Classname, int size, LinkedHashMap<String, String> genTypeInfo, int lineNum) {
		MethodInfo info = results.Classes.get(Classname).methods.get(Methodname);
		if (info == null) {
			err.UnknownMethodException(lineNum, Methodname, Classname);
		}
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
								err.AmbiguousMethodCallExecption(lineNum, Methodname);
							}
						}
						
					}
				}
			}
			if (priority != null) {
				String ret;
				addCastings(info.args.get(priority[0]), stacks);
				if (results.Classes.get(curName).canGeneric()) {
					ret = replaceReturn(replaceReturn(info.returnType.get(priority[0]), results.Classes.get(curName).genType), results.Classes.get(Classname).genType);
					if (ret.length() > 1 && !results.Classes.containsKey(stripToImport(ret))) {
						analy.Import(ImportFormat(ret));
					}
					return new String[] {replaceAll(replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(curName).genType) , results.Classes.get(Classname).genType), ret, info.AccessModifiers, genTypeInfo.get(info.returnType.get(priority[0])), };
				}
				else {
					ret = replaceReturn(info.returnType.get(priority[0]), results.Classes.get(Classname).genType);
					if (ret.length() > 1 && !results.Classes.containsKey(stripToImport(ret))) {
						analy.Import(ImportFormat(ret));
					}
					return new String[] {replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(Classname).genType), ret, info.AccessModifiers, genTypeInfo.get(info.returnType.get(priority[0]))};
				}
			}
				
			
		}
		System.out.println("damn daniel");
		return null;
	}
	
	private String replaceReturn(String returner, LinkedHashMap<String, String> genPairs) {
		for (Entry<String, String> entry : genPairs.entrySet()) {
			if (returner.equals(entry.getKey())) {
				return entry.getValue();
			}	
		}
		return returner;
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
	
	private ArrayList<String> getAllPossibleTypes(String str) {
		String arrayPrefix;
		String baseType;
		if (str.startsWith("[")) {
			int lastsqar = str.lastIndexOf('[');
			arrayPrefix = str.substring(0, lastsqar);
			baseType = str.substring(lastsqar);
		}
		else {
			arrayPrefix = "";
			baseType = str;
		}
		ArrayList<String> list = new ArrayList<>();
		list.add(str);
		switch(baseType) {
		case "Z":
			list.add(arrayPrefix + "Ljava/lang/Boolean;");
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "C":
			list.add(arrayPrefix + "I");
			list.add(arrayPrefix + "Ljava/lang/Character;");
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "B":
			list.add(arrayPrefix + "I");
			list.add(arrayPrefix + "Ljava/lang/Byte;");
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "S":
			list.add(arrayPrefix + "I");
			list.add(arrayPrefix + "Ljava/lang/Short;");
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "I":
			list.add(arrayPrefix + "Ljava/lang/Integer;");
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "J":
			list.add(arrayPrefix + "Ljava/lang/Long;");
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "F":
			list.add(arrayPrefix + "Ljava/lang/Float;");
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "D":
			list.add(arrayPrefix + "Ljava/lang/Double;");
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "Ljava/lang/String;", "null":
			list.add(arrayPrefix + "Ljava/lang/Object;");
			break;
		case "Ljava/lang/Object;":
			break;
		default:
			if (results.Classes.get(curName).canGeneric() && results.Classes.get(curName).genType.containsKey(str)) {
				list.add(arrayPrefix + "Ljava/lang/Object;");
				break;
			}
			try {
				Class <?> C = Class.forName(ImportFormat(str));
				while ((C = C.getSuperclass()) != null) {
					list.add(arrayPrefix + "L" + C.getName().replace(".", "/") + ";");
				}
				break;
			}
			catch (ClassNotFoundException e){
				list.add("Ljava/lang/Object;");
				e.printStackTrace();
			}
			
		}
		int x = arrayPrefix.length();
		for (int i = 0; i < x; i++) {
			arrayPrefix = arrayPrefix.substring(1);
			list.add(arrayPrefix + "Ljava/lang/Object;");
		}
		
		return list;
	}
	
	private ArrayList<ArrayList<String>> getAllRangeStack(int size){
		ArrayList<ArrayList<String>> returnList = new ArrayList<>();
		getAllRangeStackPos = new ArrayList<>();
		StackInfo str;
		for (int i = 0; i < size; i++) {
			str = curStack.pop();
			getAllRangeStackPos.add(str.posInQueue);
			returnList.add(getAllPossibleTypes(str.type));
		}
		return returnList;
	}
	
	
	private String ImportFormat(String ObjectName) {
		int e;
		if ((e = ObjectName.indexOf('<')) < 0) {
			return ObjectName.substring(1, ObjectName.length()-1).replace("/", ".");
		}
		else {
			return ObjectName.substring(1, e).replace("/", ".");
		}
		
	}
	private String[] constructorDo(String Classname, ASTNode tree) {
		int[] priority = null;
		int[] tempPrio;
		boolean flag;
		int indexOf;
		String type = IfImport(Classname);
		mv.visitTypeInsn(Opcodes.NEW, type);
		mv.visitInsn(Opcodes.DUP);
		if (tree.GetNodeSize() > 0) evalE(tree.GetLastNode());
		size = curStack.size();
		MethodInfo info = results.Classes.get(type).methods.get(type);
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
								//err
							}
						}
					}	
				}
			}
			if (priority != null) {
				addCastings(info.args.get(priority[0]), stacks);
				if (results.Classes.get(curName).canGeneric()) {
					return new String[] {replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(curName).genType), "V", "public"};
				}
				else {
					return new String[] {info.args.get(priority[0]).toArgs(), "V", "public"};
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
		cw = OtherClass;
		ASTNode temp = classnode;
		if ((temp = classnode.Grab(TokenState.CLASSMODIFIER)) != null) {
			cw.visit(Opcodes.V19, results.Classes.get(curName).AccessOpcode, curName, signatureWriterClass(classnode), IfImport(temp.value), null);
		}
		else {
			cw.visit(Opcodes.V19, results.Classes.get(curName).AccessOpcode, curName, signatureWriterClass(classnode), "java/lang/Object", null);
		}
		
		cw.visitSource(sourceFile, null);
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
		mv = null;
		return true;
	}
	
	public boolean newMethod(String methodName, ASTNode tree) {
		vars.setSub();
		vars.resetSub();
		String returnType = GenSuperClass(strToByte(tree.value));
		this.returnType = returnType;
		String signature = signatureWriterMethod(tree);
		
		if (!results.Classes.get(curName).methods.get(methodName).AccessModifiers.contains("static")) {
			vars.add("this", new VarInfo("this" , curName, 0));
		}
		ArgsList<String> args = fromNodetoArg(tree);
		if (methodName.equals(curName)) {
			mv = new CrodotMethodVisitor(cw.visitMethod(results.Classes.get(curName).methods.get(methodName).AccessOpcode, "<init>", args.toArgs() + "V", signature, null));
			addDefaultstoConst(curName);
			results.Classes.get(curName).construct = true;
			this.returnType = "V";
		}
		else {
			System.out.println("RIRIRI" + args.toArgs());
			mv = new CrodotMethodVisitor(cw.visitMethod(results.Classes.get(curName).methods.get(methodName).AccessOpcode, methodName,  args.toArgs() + returnType, signature, null));
		}
		
		labelList = new Stack<>();
		return false;
	}
	
	private String GenSuperClass(String strToByte) {
		ClassInfo info = results.Classes.get(curName);
		if (info.canGeneric() && info.genType.containsKey(strToByte)) {
			return info.genType.get(strToByte);
		}
		return strToByte;
	}

	private void addDefaultstoConst(String Classname) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
	    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, results.Classes.get(Classname).parent, "<init>", "()V", false);
	    for (Map.Entry<String, FieldInfo> entry : results.Classes.get(Classname).fields.entrySet()) {
	    	if (entry.getValue().HasCroValue()) {
	    		mv.visitVarInsn(Opcodes.ALOAD, 0);
	    		evalE(entry.getValue().OwnerValue);
	    		popStack();
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

		if (results.qNames.containsKey(type)) {
			return results.qNames.get(type);
		}

		return type;
	}
	
	public boolean accMain(boolean classM, boolean method, boolean If) {
		if (classM) {
			OtherClass = cw;
			cw = MainClass;
			curName = "Main";
		
			if (method) {
				mv =  MainMethod;
				vars.setMain();
				returnType = "V";
			}
		}
		if (If) {
			EndOfIf();
			
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
				vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "str", vars.getLength()));
				break;
			case "char":
				vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "char", vars.getLength()));
				break;
			case "long":
				vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "long", vars.getLength()));
				break;
			case "int":
				vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "int", vars.getLength()));
				break;
			case "shrt":
				vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "shrt", vars.getLength()));
				break;
			case "byte":
				vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "byte", vars.getLength()));
				break;
			case "bool":
				vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, "bool", vars.getLength()));
				break;
			default:
				vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, parent.GetNode(i).value, vars.getLength()));
				}
				
			}
		
	}
	
	public ArgsList<String> fromNodetoArg(ASTNode parent) {
		ArgsList<String> build = new ArgsList<>();
		String temp;
		for (int i = 1; i < parent.GetNodeSize()-1; i++) {
			if (parent.GetNode(i).type == TokenState.GENERIC) {
				break;
			}
			else if (results.Classes.get(curName).canGeneric() && results.Classes.get(curName).genType.containsKey("T" + parent.GetNode(i).value + ";")) {
				build.add(temp = results.Classes.get(curName).genType.get("T" + parent.GetNode(i).value + ";"));
			}
			else {
				build.add(temp = strToByte(parent.GetNode(i).value));
			}
			
			
			vars.add(parent.GetNode(i).GetFirstNode().value, new VarInfo(parent.GetNode(i).GetFirstNode().value, temp, vars.getLength()));
			
			

			
			
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
		cw.visitField(Access, name, strToByte(type), signatureWriterField(node), null).visitEnd();
		
	}
	public void newFieldUnkType(String name, int Access, ASTNode node) {
		err.UnknownFieldTypeException(node.line, name, curName);
		
		
	}
	public void uninitnewVar(String name, String type, int line) {
		String t = strToByte(type);
		if (t.length() > 1 && (!results.Classes.containsKey(IfImport(type)))) {
			err.UnknownClassException(line, name, type);
		}
		vars.add(name, new VarInfo(name, t, vars.getLength()));
	}
	
	public void newVar(String name, String type, ASTNode generic, int lineNum) {
		String conf = strToByte(type);
		castTopStackForVar(conf, popStack(), lineNum);
		switch(conf) {
		case "I", "Z", "B", "S", "C":
			mv.visitVarInsn(Opcodes.ISTORE, vars.getLength());
			break;
		case "J":
			mv.visitVarInsn(Opcodes.LSTORE, vars.getLength());
			break; 
		case "D":
			mv.visitVarInsn(Opcodes.DSTORE, vars.getLength());
			break;
		case "F":
			mv.visitVarInsn(Opcodes.FSTORE, vars.getLength());
			break;
		default:
			mv.visitVarInsn(Opcodes.ASTORE, vars.getLength());
			break;
		}
		
		if (generic == null) {
			vars.add(name, new VarInfo(name, conf, type, vars.getLength()));
		}
		else {
			vars.add(name, new GenVarInfo(name, conf, type, vars.getLength()).AddGenerics(generic, results, IfImport(type)));
		}
		if (conf.equals("J") || conf.equals("D")) {
			vars.Inc();
		}
	}
	
	public void newUnknownVar(String name) {
		
		String type = popStack();
		LinkedHashMap<String, String> generic = sigToHash(type);
		switch(type) {
		case "I", "Z", "S", "B", "C":
			mv.visitVarInsn(Opcodes.ISTORE, vars.getLength());
			break;
		case "J":
			mv.visitVarInsn(Opcodes.LSTORE, vars.getLength());
			break;
		case "D":
			mv.visitVarInsn(Opcodes.DSTORE, vars.getLength());
			break;
		case "F":
			mv.visitVarInsn(Opcodes.FSTORE, vars.getLength());
			break;
		default:
			mv.visitVarInsn(Opcodes.ASTORE, vars.getLength());
			break;
		}
		
		if (generic == null) {
			vars.add(name, new VarInfo(name, type, vars.getLength()));
		}
		else {
			vars.add(name, new GenVarInfo(name, type.substring(0, type.indexOf('<')+1), vars.getLength()).AddGenerics(generic));
		}
		if (type.equals("J") || type.equals("D")) {
			vars.Inc();
		}
	}
	public void CastElementAnywhere(StackInfo info, String wantedType) {
		
	}
	
	public String peekStack() {
		return curStack.peek().type;
	}
	
	public String removeGenerics(String type) {
		if (type.contains("<")) {
			return type.substring(0, type.indexOf('<')) + ";";
		}
		return type;
	}
	
	public String loadVar(String name, ASTNode bode) {
		String add;
		VarInfo var;
		ASTNode node = bode;
		//might need work
		if (vars.contains(name)) {
			if (node.prev.type == TokenState.INCREMENT || node.prev.type == TokenState.DECREMENT) { 
				add = "VAR";
			}
			else {
				add = "";
			}
			var = vars.get(name);
			if (var.type.contains("[")) {
				mv.visitVarInsn(Opcodes.ALOAD, var.pos);
				
				return var.toString();
			}
			switch(var.type) {
			case "Z":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return add + "Z";
			case "B":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return add + "B";
			case "S":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return add + "S";
			case "I":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return add + "I";
			case "C":
				mv.visitVarInsn(Opcodes.ILOAD, var.pos);
				return add + "C";
			case "J":
				mv.visitVarInsn(Opcodes.LLOAD, var.pos);
				return add + "J";
			case "D":
				mv.visitVarInsn(Opcodes.DLOAD, var.pos);
				return add + "D";
			case "F":
				mv.visitVarInsn(Opcodes.FLOAD, var.pos);
				return add + "F";
			default:
				mv.visitVarInsn(Opcodes.ALOAD, var.pos);
				return add + var.toString();
				
			}
		}
		else {
			if (node.prev.type == TokenState.INCREMENT || node.prev.type == TokenState.DECREMENT) { 
				add = "FEL";
				node = node.prev;
			}
			else {
				add = "";
			}
			ClassInfo info;
			String prev;
			if (node.prev.type == TokenState.DOT) {
				System.out.println(name);
				prev = stripToImport(curStack.pop().type);
				if (prev.startsWith("[")) {
					mv.visitInsn(Opcodes.ARRAYLENGTH);
					return "I";
				}
				else {
					info = results.Classes.get(prev);
				}
			}
			else {
				info = results.Classes.get(prev = curName);
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				
			}

			if (info.fields.containsKey(name)) {

				mv.visitFieldInsn(Opcodes.GETFIELD, prev, name, info.fields.get(name).type);
				return add + info.fields.get(name).type;
				
			}
			else {
				mv.pop();
				return "<ARRDEF>";
			}
		}
	}
	
	void addLineNumber(int line) {
		Label l = new Label();
		mv.visitLabel(l);
		mv.visitLineInsn(line, l);
	}
	
	public void storeVar(String name, ASTNode node) {
		
		VarInfo var;
		if (vars.contains(name) && (node.prev.type != TokenState.DOT || (node.prev.prev.type != TokenState.DOT && node == node.prev.GetFirstNode()))) {
			var = vars.get(name);
			castTopStackForVar(var.type, popStack(), node.line);
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
			StackInfo s = curStack.pop();
			ClassInfo info;
			String typeName;
			if (node.prev.type == TokenState.DOT) {
				info = results.Classes.get(typeName = stripToImport(curStack.pop().type));
			}
			else {
				info = results.Classes.get(typeName = curName);
				mv.insert(new CrodotVar(Opcodes.ALOAD, s.posInQueue), size);
			}
			
			if (info.fields.containsKey(name)) {
				castTopStackForVar(info.fields.get(name).type ,s.type, node.line);
				mv.visitFieldInsn(Opcodes.PUTFIELD, typeName, name, info.fields.get(name).type);
			}
			else {
				//err
			}
			
			
		}
	}
	
	
	private void castTopStackForVar(String targetStack, String curStack, int lineNum) {

		switch(targetStack) {
		case "Z":
			switch(curStack) {
			case "Z":
				return;
			case "Ljava/lang/Boolean;":
				mv.visitMethodInsn(Opcode.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
				return;
			default:
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
				err.IncompatibleTypeException(lineNum, targetStack, curStack);
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
			if (!targetStack.equals(curStack)) {
				mv.visitTypeInsn(Opcodes.CHECKCAST, stripToImport(targetStack));
			}
			break;
		}
		
		
		
	}
	
	public void conditionalE(ASTNode node, Label label) {
		evalE(node.GetFirstNode());
		StackInfo s1 = curStack.pop();
		evalE(node.GetNode(1));
		StackInfo s2 = curStack.pop();
		
		String fType;
		if(!s1.type.equals(s2.type)) {
			fType = castBothTG(s1, s2);
		}
		else {
			fType = s1.type;
		}
		switch(node.type) {
		case TokenState.BOOLEAN:
			if (node.value.equals("true")) {
				mv.visitLdcInsn(1);
			}
			else {
				mv.visitLdcInsn(0);
			}
			mv.visitJumpInsn(Opcodes.IFEQ, label);
			break;
		case TokenState.TRUEEQUALS:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPNE, label);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, label);
				break;
			default:
				mv.visitJumpInsn(Opcodes.IF_ACMPNE, label);
				break;
			}
			break;
		case TokenState.NOTEQUALS:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, label);
				break;
			default:
				mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
				break;
			}
			break;
		case TokenState.TRUEGREATERTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPLT, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, label);
				break;
			default:
				//err
			}
			break;
		case TokenState.TRUELESSTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPGT, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, label);
				break;
			default:
				//err
			}
			break;
		case TokenState.GREATERTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPLE, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, label);
				break;
			default:
				//err
			}
			break;
		case TokenState.LESSTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPGE, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, label);
				break;
			default:
				//err
			}
			break;
		}
	}

//	public void conditionalE(ASTNode node, Label label) {
//		switch(node.type) {
//		case TokenState.BOOLEAN:
//			if (node.value.equals("true")) {
//				mv.visitLdcInsn(1);
//			}
//			else {
//				mv.visitLdcInsn(0);
//			}
//			mv.visitJumpInsn(Opcodes.IFNE, label);
//			break;
//		case TokenState.TRUEEQUALS:
//
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CTrueEquals(label);
//			break;
//		case TokenState.NOTEQUALS:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CNotEquals(label);
//			break;
//		case TokenState.TRUEGREATERTHAN:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CTrueGreaterThan(label);
//			break;
//		case TokenState.TRUELESSTHAN:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CTrueLessThan(label);
//			break;
//		case TokenState.GREATERTHAN:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CGreaterThan(label);
//			break;
//		case TokenState.LESSTHAN:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CLessThan(label);
//			break;
//		}
//	}
	
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
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
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l);
				break;
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
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
		case TokenState.NULLVALUE:
			mv.visitInsn(Opcodes.ACONST_NULL);
			return curStack.push(new StackInfo("null", mv.size())).type;
		case TokenState.IS:
			System.out.println("IS" + node.GetFirstNode());
			String val = evalE(node.GetFirstNode());
			curStack.pop();
			return Is(val, node.GetNode(1).value);
		case TokenState.SEPERATOR:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			return null;
		case TokenState.CAST:
			return Cast(node);
		case TokenState.EQUIVALENCY:
			if (node.value.equals("=") || node.prev.type != TokenState.CODE || node.prev.type != TokenState.START) {
				return curStack.push(new StackInfo(equalsInStatement(node, true), mv.size())).type;
			}
			return equalsInStatement(node, false);
			
			
		case TokenState.TRUEEQUALS, TokenState.NOTEQUALS, TokenState.TRUEGREATERTHAN, TokenState.TRUELESSTHAN, TokenState.GREATERTHAN, TokenState.LESSTHAN:
			return booleanOperator(node);
		case TokenState.DOT:
			if (node.GetNodeSize() < 3) {
				evalE(node.GetFirstNode());
				return evalE(node.GetNode(1));
			}
			else {

				this.storeEvalE = node.GetNode(2);
				evalE(node.GetFirstNode());
				return evalE(node.GetNode(1), "!E");
			}
			
			//hey future nolan, you gotta switch the s1 and s2 arguements 
			
		case TokenState.INCREMENT:
			return EIncrement(node, 1);
		case TokenState.DECREMENT:
			return EIncrement(node, -1);
		case TokenState.FUN:
			curStack.push(new StackInfo(invokeEasy(node), mv.size()));
			if (stackTop().equals("V")) {
				return popStack();
			}
			return stackTop();
		case TokenState.GENFUN:
			if (curGenType != null && node.Grab(TokenState.GENERIC).GetNodeSize() < 1) {
				curStack.push(new StackInfo(constWithGen(node, curGenType), mv.size()));
			}
			else {
				curStack.push(new StackInfo(constWithGen(node,createGenType(node.Grab(TokenState.GENERIC), node.value)), mv.size()));
			}
			return stackTop();
		case TokenState.IDENTIFIER:
			curStack.push(new StackInfo(loadVar(node.value, node), mv.size()));
			if (curStack.peek().type.equals("<ARRDEF>")) {
				err.UnknownIdentifierException(node.line, node.value);
			}
			return stackTop();
		case TokenState.NOT:
			mv.visitLdcInsn(false);
			evalE(node.GetFirstNode());
			mv.visitInsn(Opcodes.IAND);
			return "Z";
		case TokenState.ARR:
			if (node.prev.type == TokenState.INCREMENT || node.prev.type == TokenState.DECREMENT) {

				curStack.push(new StackInfo(loadVar(node.value, node), mv.size()));
				if (curStack.peek().type.startsWith("FEL")) {
					curStack.peek().type = curStack.peek().type.substring(3);
				}
				curStack.push(new StackInfo(LoadArrIndex(curStack.peek().type, node, 0), mv.size()));
				curStack.peek().type = "ARR" + curStack.peek().type;

				return curStack.peek().type;
			}
			return curStack.push(new StackInfo(LoadArrIndex(loadVar(node.value, node), node, 0), mv.size())).type;
		case TokenState.RIGHTBRACE:
			return curStack.push(new StackInfo(initArray(node, null), mv.size())).type;
		case TokenState.CHAR:
			mv.visitLdcInsn(node.value.charAt(0));
			curStack.push(new StackInfo("C", mv.size()));
			return "C";
		case TokenState.STRING:
			mv.visitLdcInsn(node.value);
			curStack.push(new StackInfo("Ljava/lang/String;", mv.size()));
			return "Ljava/lang/String;";
		case TokenState.INTEGER:
			mv.visitLdcInsn(Integer.parseInt(node.value));
			curStack.push(new StackInfo("I", mv.size()));
			return "I";
		case TokenState.LONG:
			mv.visitLdcInsn(Long.parseLong(node.value));
			curStack.push(new StackInfo("J", mv.size()));
			return "J";
		case TokenState.DOUBLE:
			mv.visitLdcInsn(Double.parseDouble(node.value));
			curStack.push(new StackInfo("D", mv.size()));

			return "D";
		case TokenState.BOOLEAN:
			
			if (node.value.equals("true")) {
				mv.visitLdcInsn(1);
			}
			else {
				mv.visitLdcInsn(0);
			}
			curStack.push(new StackInfo("Z", mv.size()));
			return "Z";
		case TokenState.ADD:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Add(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.SUB:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Sub(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.MUL:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Mul(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.DIV:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Div(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.REM:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Rem(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.EXP:
			break;
			//to be done
		}
		
		return "NULL";
	}
	
//	private LinkedHashMap<String, String> GenToHash(ClassInfo info, ASTNode gen) {
//		LinkedHashMap<String, String> genType = new LinkedHashMap<String, String>();
//		Iterator<String> keys = info.genType.keySet().iterator();
//		StringBuilder sig = new StringBuilder();
//		for (int i = 0; i < info.genType.size(); i++) {
//			nodeToString(genNode.GetNode(i), results, parentClass);
//			genType.put(keys.next(), sig.toString());
//			sig.setLength(0);
//		}// TODO Auto-generated method stub
//		return genType;
//	}

	private String Cast(ASTNode node) {
		
		String type = IfImport(node.value);
		ClassInfo info;
		if (type.equals("str")) {
			type = "java/lang/String";
			info = results.Classes.get("java/lang/String");
		}
		else {
			info = results.Classes.get(type);
		}
		
		evalE(node.GetFirstNode());
		popStack();
		mv.visitTypeInsn(Opcodes.CHECKCAST, type);
		return type;
	}

	private String equalsInStatement(ASTNode node, boolean leave) {
		String parent;
		String setTo;
		ASTNode left = node.GetFirstNode();
		ASTNode right = node.GetNode(1);
		switch(left.type) {
		case TokenState.IDENTIFIER:
			switch(node.value) {
			case "+=":
				setTo = Add(new StackInfo(loadVar(left.value, left), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			case "-=":
				setTo = Sub(new StackInfo(loadVar(left.value, left), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			case "*=":
				setTo = Mul(new StackInfo(loadVar(left.value, left), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			case "/=":
				setTo = Div(new StackInfo(loadVar(left.value, left), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			case "%=":
				setTo = Rem(new StackInfo(loadVar(left.value, left), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			default:
				setTo = evalE(right);
				break;
			}
			
			
			if (leave) {
				if (setTo.equals("J") || setTo.equals("D")) {
					mv.visitInsn(Opcodes.DUP2);
				} else {
					mv.visitInsn(Opcodes.DUP);
				}
			}
			storeVar(left.value, left);
			
			return vars.get(left.value).type;
		case TokenState.DOT:
			parent = evalE(left.GetFirstNode());
			curStack.pop();
			String partype;
			ClassInfo info = results.Classes.get(partype = stripToImport(parent));
			if (left.GetNode(1).type == TokenState.ARR) {
				mv.visitFieldInsn(Opcodes.GETFIELD, partype, left.GetNode(1).value, info.fields.get(left.GetNode(1).value).type);
				
				String parent2 = info.fields.get(left.GetNode(1).value).type;
				for (int i = 0; i < left.GetNode(1).GetNodeSize()-1; i++) {
					evalE(left.GetNode(1).GetNode(i));
					mv.visitInsn(Opcodes.AALOAD);
					parent2 = parent2.substring(1);
					curStack.pop();
				}
				evalE(left.GetNode(1).GetLastNode());
				curStack.pop();
				String subtype = parent2.substring(1);
				switch(node.value) {
				case "+=":
					mv.visitInsn(Opcodes.DUP2);
					loadArr(parent2);
					setTo = Add(new StackInfo(subtype, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				case "-=":
					mv.visitInsn(Opcodes.DUP2);
					loadArr(parent2);
					setTo = Sub(new StackInfo(subtype, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				case "*=":
					mv.visitInsn(Opcodes.DUP2);
					loadArr(parent2);
					setTo = Div(new StackInfo(subtype, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				case "/=":
					mv.visitInsn(Opcodes.DUP2);
					loadArr(parent2);
					setTo = Mul(new StackInfo(subtype, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				case "%=":
					mv.visitInsn(Opcodes.DUP2);
					loadArr(parent2);
					setTo = Rem(new StackInfo(subtype, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				default:
					setTo = evalE(right);
					break;
				}
				castTopStackForVar(subtype, setTo, node.line);
				if (leave) {
					if (parent2.equals("[J") || parent2.equals("[D")) {
						mv.visitInsn(Opcodes.DUP2_X2);
					} else {
						mv.visitInsn(Opcodes.DUP_X2);
					}
				}
				curStack.pop();
				switch(parent2) {
				case "[I":
					mv.visitInsn(Opcodes.IASTORE);
					return "I";
				case "[S":
					mv.visitInsn(Opcodes.SASTORE);
					return "S";
				case "[B":
					mv.visitInsn(Opcodes.BASTORE);
					return "B";
				case "[C":
					mv.visitInsn(Opcodes.CASTORE);
					return "C";
				case "[J":
					mv.visitInsn(Opcodes.LASTORE);
					return "J";
				case "[F":
					mv.visitInsn(Opcodes.FASTORE);
					return "F";
				case "[D":
					mv.visitInsn(Opcodes.DASTORE);
					return "D";
				case "[Z":
					mv.visitInsn(Opcodes.BASTORE);
					return "Z";
				default:
					mv.visitInsn(Opcodes.AASTORE);
					return parent2.substring(1);
				}
			}
			else {
				String type = info.fields.get(left.GetNode(1).value).type;
				switch(node.value) {
				case "+=":
					mv.visitInsn(Opcodes.DUP);
					mv.visitFieldInsn(Opcodes.GETFIELD, partype, left.GetNode(1).value, type);
					setTo = Add(new StackInfo(type, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				case "-=":
					mv.visitInsn(Opcodes.DUP);
					mv.visitFieldInsn(Opcodes.GETFIELD, partype, left.GetNode(1).value, type);
					setTo = Sub(new StackInfo(type, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				case "*=":
					mv.visitInsn(Opcodes.DUP);
					mv.visitFieldInsn(Opcodes.GETFIELD, partype, left.GetNode(1).value, type);
					setTo = Div(new StackInfo(type, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				case "/=":
					mv.visitInsn(Opcodes.DUP);
					mv.visitFieldInsn(Opcodes.GETFIELD, partype, left.GetNode(1).value, type);
					setTo = Mul(new StackInfo(type, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				case "%=":
					mv.visitInsn(Opcodes.DUP);
					mv.visitFieldInsn(Opcodes.GETFIELD, partype, left.GetNode(1).value, type);
					setTo = Rem(new StackInfo(type, mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
					break;
				default:
					setTo = evalE(right);
					break;
				}
				castTopStackForVar(type, setTo, node.line);
				if (leave) {
					if (type.equals("J") || type.equals("D")) {
						mv.visitInsn(Opcodes.DUP2_X1);
					} else {
						mv.visitInsn(Opcodes.DUP_X1);
					}
				}
				mv.visitFieldInsn(Opcodes.PUTFIELD,  partype, left.GetNode(1).value, type);
				curStack.pop();
				return type;
			}
			
		case TokenState.ARR:
			parent = loadVar(left.value, left);
			for (int i = 0; i < left.GetNodeSize()-1; i++) {
				evalE(left.GetNode(i));
				mv.visitInsn(Opcodes.AALOAD);
				parent = parent.substring(1);
				curStack.pop();
			}
			evalE(left.GetLastNode());
			switch(node.value) {
			case "+=":
				mv.visitInsn(Opcodes.DUP2);
				loadArr(parent);
				setTo = Add(new StackInfo(parent.substring(1), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			case "-=":
				mv.visitInsn(Opcodes.DUP2);
				loadArr(parent);
				setTo = Sub(new StackInfo(parent.substring(1), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			case "*=":
				mv.visitInsn(Opcodes.DUP2);
				loadArr(parent);
				setTo = Div(new StackInfo(parent.substring(1), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			case "/=":
				mv.visitInsn(Opcodes.DUP2);
				loadArr(parent);
				setTo = Mul(new StackInfo(parent.substring(1), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			case "%=":
				mv.visitInsn(Opcodes.DUP2);
				loadArr(parent);
				setTo = Rem(new StackInfo(parent.substring(1), mv.size()), new StackInfo(evalE(right), mv.size()), node.line);
				break;
			default:
				setTo = evalE(right);
				break;
			}
			castTopStackForVar(parent.substring(1), setTo, node.line);
			if (leave) {
				if (parent.equals("[J") || parent.equals("[D")) {
					mv.visitInsn(Opcodes.DUP2_X2);
				} else {
					mv.visitInsn(Opcodes.DUP_X2);
				}
			}
			curStack.pop();
			curStack.pop();
			switch(parent) {
			case "[I":
				mv.visitInsn(Opcodes.IASTORE);
				return "I";
			case "[S":
				mv.visitInsn(Opcodes.SASTORE);
				return "S";
			case "[B":
				mv.visitInsn(Opcodes.BASTORE);
				return "B";
			case "[C":
				mv.visitInsn(Opcodes.CASTORE);
				return "C";
			case "[J":
				mv.visitInsn(Opcodes.LASTORE);
				return "J";
			case "[F":
				mv.visitInsn(Opcodes.FASTORE);
				return "F";
			case "[D":
				mv.visitInsn(Opcodes.DASTORE);
				return "D";
			case "[Z":
				mv.visitInsn(Opcodes.BASTORE);
				return "Z";
			default:
				mv.visitInsn(Opcodes.AASTORE);
				return parent.substring(1);
			}
		}
		return null;
	}

	private String castBothTG(StackInfo s1, StackInfo s2) {
		System.out.println(s1.type + s2.type);
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Z":
				return "Z";
			case "Ljava/lang/Boolean;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false),s2.posInQueue);
				return "Z";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(Z)Ljava/lang/String;", false), s1.posInQueue);
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(" + s1.type + ")Ljava/lang/String;", false), s1.posInQueue);
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				return "J";
			case "J":
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(J)Ljava/lang/String;", false), s1.posInQueue);
				return "Ljava/lang/String;";
			default:
				//err
			}
		
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				return "F";
			case "F":
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf","(F)Ljava/lang/String;", false), s1.posInQueue);
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				return "D";
			case "D":
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(D)Ljava/lang/String;", false), s1.posInQueue);
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/Boolean;":
			switch(s2.type) {
			case "Z":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false),s1.posInQueue);
				return "Z";
			case "Ljava/lang/Boolean;":
				return "Ljava/lang/Boolean;";
			case "Ljava/lang/String;":
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				return "Ljava/lang/Byte;";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Short;":
				return "Ljava/lang/Short;";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Character;":
				return "Ljava/lang/Character;";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "I";
			case "Ljava/lang/Integer;":
				return "Ljava/lang/Integer;";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "J";
			case "Ljava/lang/Long;":
				return "Ljava/lang/Long;";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "F";
			case "Ljava/lang/Float;":
				return "Ljava/lang/Float;";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/String;":
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				return "D";
			case "Ljava/lang/Double;":
				return "Ljava/lang/Double;";
			case "Ljava/lang/String;":
				return "Ljava/lang/String;";
			default:
				//err
			}
		case "Ljava/lang/String;":
			switch (s2.type) {
			case "B":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(B)Ljava/lang/String;", false), s2.posInQueue);
				return "Ljava/lang/String;";
			case "S":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(S)Ljava/lang/String;", false), s2.posInQueue);
				return "Ljava/lang/String;";
			case "I":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;",
						false), s2.posInQueue);
				return "Ljava/lang/String;";
			case "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(C)Ljava/lang/String;",
						false), s2.posInQueue);
				return "Ljava/lang/String;";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(J)Ljava/lang/String;",
						false), s2.posInQueue);
				return "Ljava/lang/String;";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(F)Ljava/lang/String;",
						false), s2.posInQueue);
				return "Ljava/lang/String;";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(D)Ljava/lang/String;",
                        false), s2.posInQueue);
                return "Ljava/lang/String;";
			default:
				return "Ljava/lang/String;";
			}
			
			
			
		}
		return null;
	}
	
	private String booleanOperator(ASTNode node) {
		evalE(node.GetFirstNode());
		StackInfo s1 = curStack.pop();
		evalE(node.GetNode(1));
		StackInfo s2 = curStack.pop();

		Label l1 = new Label();
		Label l2 = new Label();
		String fType;
		if(s1.type != s2.type) {
			fType = castBothTG(s1, s2);
		}
		else {
			fType = s1.type;
		}
		switch(node.type) {
		case TokenState.TRUEEQUALS:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPNE, l1);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, l1);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l1);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, l1);
				break;
			default:
				mv.visitJumpInsn(Opcodes.IF_ACMPNE, l1);
				break;
			}
			break;
		case TokenState.NOTEQUALS:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPEQ, l1);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, l1);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l1);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, l1);
				break;
			default:
				mv.visitJumpInsn(Opcodes.IF_ACMPEQ, l1);
				break;
			}
			break;
		case TokenState.TRUEGREATERTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPLT, l1);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, l1);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l1);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, l1);
				break;
			default:
				//err
			}
			break;
		case TokenState.TRUELESSTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPGT, l1);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, l1);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l1);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, l1);
				break;
			default:
				//err
			}
			break;
		case TokenState.GREATERTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPLE, l1);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, l1);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l1);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, l1);
				break;
			default:
				//err
			}
			break;
		case TokenState.LESSTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPGE, l1);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, l1);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l1);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, l1);
				break;
			default:
				//err
			}
			break;
		}
		
		mv.visitLdcInsn(true);
		mv.visitJumpInsn(Opcodes.GOTO, l2);
		mv.visitLabel(l1);
		mv.visitLdcInsn(false);
		mv.visitLabel(l2);
		
		
		// TODO Auto-generated method stub
		curStack.push(new StackInfo("Z", mv.size()));
		return null;
	}

	private String EIncrement(ASTNode node, int mod) {
		StackInfo parent;
		if (node.prev.type == TokenState.DOT) {
			parent = curStack.peek();
		}
		else {
			parent = null;
		}
		evalE(node.GetFirstNode());
		StackInfo s = curStack.pop();

		String lils = s.type.substring(3);

		switch(s.type.substring(0, 3)) {
		case "VAR":
			if (node.value.equals("E")) {
				switch(lils) {
				case "B", "C", "S", "I":
					mv.visitIincInsn(vars.get(node.GetFirstNode().value).pos, mod);
					break;
				case "J":
					mv.visitInsn(Opcodes.DUP2);
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "F":
					mv.visitInsn(Opcodes.DUP);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "D":
					mv.visitInsn(Opcodes.DUP2);
					mv.visitLdcInsn((double) mod);
					mv.visitInsn(Opcodes.DADD);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Byte;":
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Short;":
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Integer;":
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Character;":
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Long;":
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "valueOf", "(L)Ljava/lang/Long;", false);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Float;":
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Double;":
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.DADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				}
				curStack.add(new StackInfo(lils, mv.size()));
			}
			else {
				switch(lils) {
				case "B", "C", "S", "I":
					mv.insert(new CrodotIInc(vars.get(node.GetFirstNode().value).pos, mod), s.posInQueue);
					break;
				case "J":
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					mv.visitInsn(Opcodes.DUP2);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "F":
					
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					mv.visitInsn(Opcodes.DUP);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "D":
					mv.visitLdcInsn((double) mod);
					mv.visitInsn(Opcodes.DADD);
					mv.visitInsn(Opcodes.DUP2);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Byte;":
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
					mv.visitInsn(Opcodes.DUP);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Short;":
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
					mv.visitInsn(Opcodes.DUP);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Integer;":
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
					mv.visitInsn(Opcodes.DUP);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Character;":
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
					mv.visitInsn(Opcodes.DUP);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Long;":
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false);
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "valueOf", "(L)Ljava/lang/Long;", false);
					mv.visitInsn(Opcodes.DUP);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Float;":
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
					mv.visitInsn(Opcodes.DUP);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				case "Ljava/lang/Double;":
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.DADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
					mv.visitInsn(Opcodes.DUP);
					curStack.add(new StackInfo(lils, mv.size()));
					storeVar(node.GetFirstNode().value, node.prev);
					break;
				}
				curStack.add(new StackInfo(lils, mv.size()));
			}
			return lils;
		case "FEL": 
			if (node.value.equals("E")) {
				switch(lils) {
				case "B", "C", "S", "I":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					storeField(node.GetFirstNode(), parent);
					
					break;
				case "J":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP2_X1);
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					storeField(node.GetFirstNode(), parent);
					
					break;
				case "F":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					storeField(node.GetFirstNode(), parent);
					
					break;
				case "D":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP2_X1);
					mv.visitLdcInsn((double) mod);
					mv.visitInsn(Opcodes.DADD);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Byte;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Short;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Integer;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Character;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Long;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Float;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Double;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitInsn(Opcodes.DUP_X1);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
					mv.visitLdcInsn((double) mod);
					mv.visitInsn(Opcodes.DADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
					storeField(node.GetFirstNode(), parent);
					break;
				}
				
			}
			else {
				switch(lils) {
				case "B", "C", "S", "I":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				case "J":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					mv.visitInsn(Opcodes.DUP2_X1);
					storeField(node.GetFirstNode(), parent);
					
					break;
				case "F":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					
					break;
				case "D":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitLdcInsn((double) mod);
					mv.visitInsn(Opcodes.DADD);
					mv.visitInsn(Opcodes.DUP2_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Byte;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Short;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Integer;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Character;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Long;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Float;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				case "Ljava/lang/Double;":
					mv.insert(new CrodotInsn(Opcodes.DUP),  s.posInQueue-1);
					
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
					mv.visitLdcInsn((double) mod);
					mv.visitInsn(Opcodes.DADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
					mv.visitInsn(Opcodes.DUP_X1);
					storeField(node.GetFirstNode(), parent);
					break;
				}
			}
			curStack.add(new StackInfo(lils, mv.size()));
			return lils;
		case "ARR":

			if (node.value.equals("E")) {
				StackInfo arr;
				switch(lils) {
				case "B", "C", "S", "I":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					storeArrforInc(node.GetFirstNode().value, lils);
					
					break;
				case "J":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP2_X2);
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					storeArrforInc(node.GetFirstNode().value, lils);
					
					break;
				case "F":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					storeArrforInc(node.GetFirstNode().value, lils);
					
					break;
				case "D":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP2_X2);
					mv.visitLdcInsn((double) mod);
					mv.visitInsn(Opcodes.DADD);
					storeArrforInc(node.GetFirstNode().value, lils);
					
					break;
				case "Ljava/lang/Byte;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Short;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Integer;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Character;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
					mv.visitLdcInsn(mod);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Long;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
					mv.visitLdcInsn((long) mod);
					mv.visitInsn(Opcodes.LADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Float;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
					mv.visitLdcInsn((float) mod);
					mv.visitInsn(Opcodes.FADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Double;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitInsn(Opcodes.DUP_X2);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
					mv.visitLdcInsn((double) mod);
					mv.visitInsn(Opcodes.DADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				}
			}
			else {
				StackInfo arr;
				switch(lils) {
				case "B", "C", "S", "I":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitLdcInsn(1);
					mv.visitInsn(Opcodes.IADD);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					
					break;
				case "J":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitLdcInsn((long) 1);
					mv.visitInsn(Opcodes.LADD);
					mv.visitInsn(Opcodes.DUP2_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					
					break;
				case "F":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitLdcInsn((float) 1);
					mv.visitInsn(Opcodes.FADD);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					
					break;
				case "D":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitLdcInsn((double) 1);
					mv.visitInsn(Opcodes.DADD);
					mv.visitInsn(Opcodes.DUP2_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					
					break;
				case "Ljava/lang/Byte;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
					mv.visitLdcInsn(1);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Short;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
					mv.visitLdcInsn(1);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Integer;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
					mv.visitLdcInsn(1);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Character;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
					mv.visitLdcInsn(1);
					mv.visitInsn(Opcodes.IADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Long;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
					mv.visitLdcInsn((long) 1);
					mv.visitInsn(Opcodes.LADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Float;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
					mv.visitLdcInsn((float) 1);
					mv.visitInsn(Opcodes.FADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				case "Ljava/lang/Double;":
					arr = curStack.pop();
					mv.insert(new CrodotInsn(Opcodes.DUP),  arr.posInQueue);
					mv.insert(new CrodotInsn(Opcodes.DUP_X1),  s.posInQueue);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
					mv.visitLdcInsn((double) 1);
					mv.visitInsn(Opcodes.DADD);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
					mv.visitInsn(Opcodes.DUP_X2);
					storeArrforInc(node.GetFirstNode().value, lils);
					break;
				}
			
			}
			break;
		default:
			break;
			//error
		}
		return null;
	}

	public VarInfo getVar(String name) {
		return vars.get(name);
	}
	
	public String evalE(ASTNode node, String TypeExpected) {

		switch(node.type) {
		case TokenState.IS:
			System.out.println("IS" + node.GetFirstNode());
			String val = evalE(node.GetFirstNode());
			curStack.pop();
			return Is(val, node.GetNode(1).value);
		case TokenState.SEPERATOR:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			return null;
		case TokenState.CAST:
			return Cast(node);
		case TokenState.EQUIVALENCY:
			if (node.value.equals("=") || node.prev.type != TokenState.CODE || node.prev.type != TokenState.START) {
				return curStack.push(new StackInfo(equalsInStatement(node, true), mv.size())).type;
			}
			return equalsInStatement(node, false);
		case TokenState.TRUEEQUALS, TokenState.NOTEQUALS, TokenState.TRUEGREATERTHAN, TokenState.TRUELESSTHAN, TokenState.GREATERTHAN, TokenState.LESSTHAN:
			return booleanOperator(node);
		case TokenState.DOT:
			if (node.GetNodeSize() < 3) {
				evalE(node.GetFirstNode());
				return evalE(node.GetNode(1));
			}
			else {
				
				this.storeEvalE = node.GetNode(2);
				evalE(node.GetFirstNode());
				return evalE(node.GetNode(1), "!E");
			}
		case TokenState.FUN:
			curStack.push(new StackInfo(invokeEasy(node), mv.size()));

			if (stackTop().equals("V")) {
				return popStack();
			}
			return stackTop();
		case TokenState.INCREMENT:
			return EIncrement(node, 1);
		case TokenState.DECREMENT:
			return EIncrement(node, -1);
		case TokenState.GENFUN:
			//err
			System.out.println("GENFUN");
			if (curGenType != null && node.Grab(TokenState.GENERIC).GetNodeSize() < 1) {
				curStack.push(new StackInfo(constWithGen(node, curGenType), mv.size()));
			}
			else {
				curStack.push(new StackInfo(constWithGen(node,createGenType(node.Grab(TokenState.GENERIC), node.value)), mv.size()));
			}
			
			return stackTop();
		case TokenState.IDENTIFIER:
			
			
			if (TypeExpected.equals("!E")) {
				evalE(storeEvalE);
				storeVar(node.value, node);
				storeEvalE = null;
				return null;
			}
			else {
				curStack.push(new StackInfo(loadVar(node.value, node), mv.size()));
				if (curStack.peek().type.equals("<ARRDEF>")) {
					err.UnknownIdentifierException(node.line, node.value);
				}
				return stackTop();
			}
			
		case TokenState.ARR:
			if (node.prev.type == TokenState.INCREMENT || node.prev.type == TokenState.DECREMENT) {

				curStack.push(new StackInfo(loadVar(node.value, node), mv.size()));
				if (curStack.peek().type.startsWith("FEL")) {
					curStack.peek().type = curStack.peek().type.substring(3);
				}
				curStack.push(new StackInfo(LoadArrIndex(curStack.peek().type, node, 0), mv.size()));
				curStack.peek().type = "ARR" + curStack.peek().type;
				return curStack.peek().type;
			}
			return curStack.push(new StackInfo(LoadArrIndex(loadVar(node.value, node), node, 0), mv.size())).type;
		case TokenState.NOT:
			mv.visitLdcInsn(false);
			evalE(node.GetFirstNode());
			mv.visitInsn(Opcodes.IAND);
			return "Z";
		case TokenState.RIGHTBRACE:
			
			if (TypeExpected.startsWith("[")) {
				return curStack.push(new StackInfo(initArray(node, TypeExpected.substring(1)), mv.size())).type;
			}
			else {
				err.IncompatibleTypeException(node.line, TypeExpected, "Array");
				return null;
			}
			
		case TokenState.CHAR:
			mv.visitLdcInsn(node.value.charAt(0));
			curStack.push(new StackInfo("C", mv.size()));
			return "C";
		case TokenState.STRING:
			mv.visitLdcInsn(node.value);
			curStack.push(new StackInfo("Ljava/lang/String;", mv.size()));
			return "Ljava/lang/String;";
		case TokenState.INTEGER:
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
		case TokenState.LONG:
			if (TypeExpected.equals("I") || TypeExpected.equals("Ljava/lang/Integer;")) {
				//error
			}
			else {
				mv.visitLdcInsn(Long.parseLong(node.value));
				curStack.push(new StackInfo("J", mv.size()));
				return "J";
			}
			
		case TokenState.DOUBLE:
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
			
		case TokenState.BOOLEAN:
			
			if (node.value.equals("true")) {
				mv.visitLdcInsn(1);
			}
			else {
				mv.visitLdcInsn(0);
			}
			curStack.push(new StackInfo("Z", mv.size()));
			return "Z";
		case TokenState.ADD:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Add(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.SUB:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Sub(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.MUL:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Mul(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.DIV:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Div(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.REM:
			evalE(node.GetFirstNode());
			evalE(node.GetNode(1));
			
			return curStack.push(new StackInfo(Rem(curStack.pop(), curStack.pop(), node.line), mv.size())).type;
		case TokenState.EXP:
			break;
			//to be done
		}
		
		return "NULL";
	}


	private String EAdd(int line) {
		StackInfo s2 = curStack.pop();
		StackInfo s1 = curStack.pop();
		
		
		switch(s1.type) {
		case "Z":
			switch(s2.type) {
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(Z)Ljava/lang/String;", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Boolean;":
			switch(s2.type) {
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IADD);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(J)Ljava/lang/String;", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LADD);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue+2);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue+2);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/Object;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(F)Ljava/lang/String;", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(D)Ljava/lang/String;", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FADD);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DADD);
				return "D";
			case "Ljava/lang/String;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/String;":
			switch(s2.type) {
			case "B":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(B)Ljava/lang/String;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			case "S":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(S)Ljava/lang/String;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
				
			case "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(C)Ljava/lang/String;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
				
			case "I":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				
				return "Ljava/lang/String;";
			case "J":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(J)Ljava/lang/String;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
				
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(F)Ljava/lang/String;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				

				return "Ljava/lang/String;";
				
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(D)Ljava/lang/String;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				
			
				return "Ljava/lang/String;";
				
			case "Ljava/lang/String;":
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			default:
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				return "Ljava/lang/String;";
			}
			
		default:
			if (s2.type.equals("Ljava/lang/String;")) {
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
				
				//one before other for efficiency as it pushes
				mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
				//one before other for efficiency as it pushes
				//one before other for efficiency as it pushes
				mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
				mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				
				
				return "Ljava/lang/String;";
			}
			else {
				err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
			}
		}
		return null;
		
	}
	private String ESub(int line) {

		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.ISUB);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LSUB);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FSUB);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DSUB);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
				break;
			}
			break;
		default:
				err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
			
		}
		return null;
		
	}
	private String EMul(int line) {

		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IMUL);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LMUL);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FMUL);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DMUL);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
				break;
			}
			break;
		default:
				err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
			
		}
		return null;
		
	}
	private String EDiv(int line) {
		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IDIV);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LDIV);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FDIV);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DDIV);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
				break;
			}
			break;
		default:
				err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
			
		}
		return null;
		
	}
	private String ERem(int line) {

		StackInfo s1 = curStack.pop();
		StackInfo s2 = curStack.pop();
		
		switch(s1.type) {
		case "B", "S", "I", "C":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Byte;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Short;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Character;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Integer;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.IREM);
				return "I";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "J":
			switch(s2.type) {
			case "B", "S", "I", "C":

				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "J":

				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Long;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2L), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.LREM);
				return "J";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "F":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "F":
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Float;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "D":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2F), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "F";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "D":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "J":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "F":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "D":
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		case "Ljava/lang/Double;":
			switch(s2.type) {
			case "B", "S", "I", "C":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "J":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "F":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "D":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			case "Ljava/lang/Byte;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Short;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Character;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Integer;":
				mv.insert(new CrodotInsn(Opcodes.I2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Long;":
				mv.insert(new CrodotInsn(Opcodes.L2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Float;":
				mv.insert(new CrodotInsn(Opcodes.F2D), s2.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s2.posInQueue);
				mv.visitInsn(Opcodes.FREM);
				return "D";
			case "Ljava/lang/Double;":
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
				mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
				mv.visitInsn(Opcodes.DREM);
				return "D";
			default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
				break;
			}
			break;
		default:
				err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
			
		}
		return null;
		
	}


	private String Add(StackInfo s2, StackInfo s1, int line) {
		String type = castBothTG(s1, s2);
		switch(type) {
		case "B", "S", "I", "C":
			mv.visitInsn(Opcodes.IADD);
			return "I";
		case "J":
			mv.visitInsn(Opcodes.LADD);
			return "J";
		case "F":
			mv.visitInsn(Opcodes.FADD);
			return "F";
		case "D":
			mv.visitInsn(Opcodes.DADD);
			return "D";
		case "Ljava/lang/Byte;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IADD);
			return "I";
		case "Ljava/lang/Short;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IADD);
			return "I";
		case "Ljava/lang/Character;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IADD);
			return "I";
		case "Ljava/lang/Integer;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s1.posInQueue);
			mv.visitInsn(Opcodes.IADD);
			return "I";
		case "Ljava/lang/Long;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
			mv.visitInsn(Opcodes.LADD);
			return "J";
		case "Ljava/lang/Float;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false),	s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
			mv.visitInsn(Opcodes.FADD);
			return "F";
		case "Ljava/lang/Double;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
			mv.visitInsn(Opcodes.DADD);	
			return "D";
		case "Ljava/lang/String;":
			if (s1.type.equals("Ljava/lang/String;")) {
				switch(s2.type) { 
				case "B", "S", "I", "C", "J", "D", "F":
					
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue+1);
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue+1);
					
				
					//one before other for efficiency as it pushes
					mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
					//one before other for efficiency as it pushes
					//one before other for efficiency as it pushes
					mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
					mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				    return "Ljava/lang/String;";
				case "Ljava/lang/String;":
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
					
					//one before other for efficiency as it pushes
					mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
					//one before other for efficiency as it pushes
					//one before other for efficiency as it pushes
					mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
					mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
					return "Ljava/lang/String;";
				default:
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
					
					//one before other for efficiency as it pushes
					mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue);
					//one before other for efficiency as it pushes
					//one before other for efficiency as it pushes
					mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
					mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
					return "Ljava/lang/String;";
				}
			}
			else {
				switch(s1.type) { 
				case "B", "S", "I", "C", "J", "D", "F":
					System.out.println("here");
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue+1);
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue+1);

				
					//one before other for efficiency as it pushes
					mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false), s1.posInQueue+1);
					//one before other for efficiency as it pushes
					//one before other for efficiency as it pushes
					mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
					mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
				
			
				
				    return "Ljava/lang/String;";
				default:
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false), s2.posInQueue);
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false), s2.posInQueue);
					
					//one before other for efficiency as it pushes
					mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false), s1.posInQueue);
					mv.insert(new CrodotMethod(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false), s1.posInQueue-1);
					//one before other for efficiency as it pushes
					//one before other for efficiency as it pushes
					mv.insert(new CrodotInsn(Opcodes.DUP), s1.posInQueue-1);
					mv.insert(new CrodotType(Opcodes.NEW, "java/lang/StringBuilder"), s1.posInQueue-1);
					
					return "Ljava/lang/String;";
				}
			}
		default:
			err.UnknownArithmeticInputException(line, TokenState.ADD, s1.type, s2.type);
			return null;
		}
		
		
	}
	
	private String Is(String valType, String type) {
		String sureType;
		if (type.equals("str")) {
			sureType = "java/lang/String";
		}
		else {
			sureType = IfImport(type);
		}
		switch (valType) {
		case "B", "S", "I", "Z", "C", "F":
			mv.visitInsn(Opcodes.POP);
			if (valType.equals(strToByte(type))) {
				mv.visitLdcInsn(1);
			}
			else {
				mv.visitLdcInsn(0);
			}
			return "Z";
		case "D", "J":
			mv.visitInsn(Opcodes.POP2);
			if (valType.equals(strToByte(type))) {
				mv.visitLdcInsn(1);
			}
			else {
				mv.visitLdcInsn(0);
			}
			return "Z";	
		}

		mv.visitTypeInsn(Opcodes.INSTANCEOF, sureType);
		return "Z";
	}
	
	private String Rem(StackInfo s2, StackInfo s1, int line) {
		String type = castBothTG(s1, s2);
		switch(type) {
		case "B", "S", "I", "C":
			mv.visitInsn(Opcodes.IREM);
			return "I";
		case "J":
			mv.visitInsn(Opcodes.LREM);
			return "J";
		case "F":
			mv.visitInsn(Opcodes.FREM);
			return "F";
		case "D":
			mv.visitInsn(Opcodes.DREM);
			return "D";
		case "Ljava/lang/Byte;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IREM);
			return "I";
		case "Ljava/lang/Short;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IREM);
			return "I";
		case "Ljava/lang/Character;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IREM);
			return "I";
		case "Ljava/lang/Integer;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s1.posInQueue);
			mv.visitInsn(Opcodes.IREM);
			return "I";
		case "Ljava/lang/Long;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
			mv.visitInsn(Opcodes.LREM);
			return "J";
		case "Ljava/lang/Float;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false),	s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
			mv.visitInsn(Opcodes.FREM);
			return "F";
		case "Ljava/lang/Double;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
			mv.visitInsn(Opcodes.DREM);	
			return "D";
		default:
			err.UnknownArithmeticInputException(line, TokenState.REM, s1.type, s2.type);
			return null;
		}
		
		
	}
	
	private String Div(StackInfo s2, StackInfo s1, int line) {
		String type = castBothTG(s1, s2);
		switch(type) {
		case "B", "S", "I", "C":
			mv.visitInsn(Opcodes.IDIV);
			return "I";
		case "J":
			mv.visitInsn(Opcodes.LDIV);
			return "J";
		case "F":
			mv.visitInsn(Opcodes.FDIV);
			return "F";
		case "D":
			mv.visitInsn(Opcodes.DDIV);
			return "D";
		case "Ljava/lang/Byte;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IDIV);
			return "I";
		case "Ljava/lang/Short;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IDIV);
			return "I";
		case "Ljava/lang/Character;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IDIV);
			return "I";
		case "Ljava/lang/Integer;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s1.posInQueue);
			mv.visitInsn(Opcodes.IDIV);
			return "I";
		case "Ljava/lang/Long;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
			mv.visitInsn(Opcodes.LDIV);
			return "J";
		case "Ljava/lang/Float;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false),	s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
			mv.visitInsn(Opcodes.FDIV);
			return "F";
		case "Ljava/lang/Double;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
			mv.visitInsn(Opcodes.DDIV);	
			return "D";
		default:
			err.UnknownArithmeticInputException(line, TokenState.DIV, s1.type, s2.type);
			return null;
		}
		
		
	}

	private String Mul(StackInfo s2, StackInfo s1, int line) {
		String type = castBothTG(s1, s2);
		switch(type) {
		case "B", "S", "I", "C":
			mv.visitInsn(Opcodes.IMUL);
			return "I";
		case "J":
			mv.visitInsn(Opcodes.LMUL);
			return "J";
		case "F":
			mv.visitInsn(Opcodes.FMUL);
			return "F";
		case "D":
			mv.visitInsn(Opcodes.DMUL);
			return "D";
		case "Ljava/lang/Byte;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IMUL);
			return "I";
		case "Ljava/lang/Short;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IMUL);
			return "I";
		case "Ljava/lang/Character;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
			mv.visitInsn(Opcodes.IMUL);
			return "I";
		case "Ljava/lang/Integer;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s1.posInQueue);
			mv.visitInsn(Opcodes.IMUL);
			return "I";
		case "Ljava/lang/Long;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
			mv.visitInsn(Opcodes.LMUL);
			return "J";
		case "Ljava/lang/Float;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false),	s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
			mv.visitInsn(Opcodes.FMUL);
			return "F";
		case "Ljava/lang/Double;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
			mv.visitInsn(Opcodes.DMUL);	
			return "D";
		default:
			err.UnknownArithmeticInputException(line, TokenState.MUL, s1.type, s2.type);
			return null;
		}
		
		
	}


	private String Sub(StackInfo s2, StackInfo s1, int line) {
		String type = castBothTG(s1, s2);
		switch(type) {
		case "B", "S", "I", "C":
			mv.visitInsn(Opcodes.ISUB);
			return "I";
		case "J":
			mv.visitInsn(Opcodes.LSUB);
			return "J";
		case "F":
			mv.visitInsn(Opcodes.FSUB);
			return "F";
		case "D":
			mv.visitInsn(Opcodes.DSUB);
			return "D";
		case "Ljava/lang/Byte;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false), s1.posInQueue);
			mv.visitInsn(Opcodes.ISUB);
			return "I";
		case "Ljava/lang/Short;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false), s1.posInQueue);
			mv.visitInsn(Opcodes.ISUB);
			return "I";
		case "Ljava/lang/Character;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false), s1.posInQueue);
			mv.visitInsn(Opcodes.ISUB);
			return "I";
		case "Ljava/lang/Integer;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false),s1.posInQueue);
			mv.visitInsn(Opcodes.ISUB);
			return "I";
		case "Ljava/lang/Long;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false), s1.posInQueue);
			mv.visitInsn(Opcodes.LSUB);
			return "J";
		case "Ljava/lang/Float;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false),	s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false), s1.posInQueue);
			mv.visitInsn(Opcodes.FSUB);
			return "F";
		case "Ljava/lang/Double;":
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s2.posInQueue);
			mv.insert(new CrodotMethod(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false), s1.posInQueue);
			mv.visitInsn(Opcodes.DSUB);	
			return "D";
		default:
			err.UnknownArithmeticInputException(line, TokenState.SUB, s1.type, s2.type);
			return null;
		}
		
		
	}
	
	
	
	private String constWithGen(ASTNode tree, LinkedHashMap<String, String> genTypeInfo) {
		String type = IfImport(tree.value);
		System.out.println(type);
		int[] priority = null;
		int[] tempPrio;
		boolean flag;
		int indexOf;
		mv.visitTypeInsn(Opcodes.NEW, type);
		mv.visitInsn(Opcodes.DUP);
		if (tree.GetNodeSize() > 0) evalE(tree.GetLastNode());
		size = curStack.size();
		MethodInfo info = results.Classes.get(type).methods.get(type);
		ArrayList<ArrayList<String>> stacks = getAllRangeStack(size);
		if ((!Objects.isNull(info))) {
			for (int i = 0; i < info.args.size(); i++) {
				if (info.args.get(i).size() == stacks.size()) {
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
									//err
								}
							}
						}	
					}
				}
				if (priority != null) {
					addCastings(info.args.get(priority[0]), stacks);
					if (results.Classes.get(curName).canGeneric()) {
						this.invokeSpecial("<init>", IfImport(tree.value), replaceAll(replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(curName).genType) , results.Classes.get(type).genType) + "V");
						return "L" + IfImport(tree.value) + genToString(tree.Grab(TokenState.GENERIC));
					
					}
					else {
						this.invokeSpecial("<init>", IfImport(tree.value), replaceAll(info.args.get(priority[0]).toArgs(), results.Classes.get(type).genType) + "V");
						return "L" + IfImport(tree.value) + genToString(tree.Grab(TokenState.GENERIC));
					}
					
				}
			}
			
			
			
		}
		
		err.UnknownConstructorException(tree.line, tree.value, stacks);
		return null;
	}

	private String genToString(ASTNode gen) {
		StringBuilder b = new StringBuilder("<");
		for (int i = 0; i < gen.GetNodeSize(); i++) {
			switch(gen.GetNode(i).type) {
			case TokenState.CLASSNAME:
				b.append(strToByte(gen.GetNode(i).value));
				break;
			case TokenState.INFERRED:
				b.append('*');
				break;
			case TokenState.CLASSMODIFIER:
				if (gen.GetNode(i).value.equals("extends")) {
					b.append('+');
				}
				else {
					b.append('-');
				}
				b.append(strToByte(gen.GetNode(i).GetNode(1).value));
				break;
			case TokenState.GENERIC:
				b.append(genToString(gen.GetNode(i)));
				break;
			}
			
		}
		return b.append(">;").toString();
	}
	


	private String typedGeneric(ASTNode gen, LinkedHashMap<String, String> genTypeInfo) {
		
		StringBuilder b = new StringBuilder("<");
		for (int i = 0; i < gen.GetNodeSize(); i++) {
			switch(gen.GetNode(i).type) {
			case TokenState.CLASSNAME:
				String name;
				if (genTypeInfo.containsKey(gen.GetNode(i).value)) {
					b.append(name = genTypeInfo.get("T" + gen.GetNode(i).value + ";"));
				} else {
					b.append(name = strToByte(gen.GetNode(i).value));
				}
				if (gen.GetNode(i).GetNodeSize() > 0) {
					System.out.println(stripToImport(name));
					b.insert(b.length()-1, typedGeneric(gen.GetNode(i).GetNode(0), results.Classes.get(stripToImport(name)).genType));
				}
				break;
			case TokenState.INFERRED:
				b.append('*');
				break;
			case TokenState.CLASSMODIFIER:
				if (gen.GetNode(i).value.equals("extends")) {
					b.append('+');
				}
				else {
					b.append('-');
				}
				b.append(strToByte(gen.GetNode(i).GetNode(1).value));
				break;
			case TokenState.GENERIC:
				b.append(typedGeneric(gen.GetNode(i), genTypeInfo)); //hjelp
				break;
			}
			
		}
		return b.append(">").toString();
	}
	private String signatureWriterMethod(ASTNode methodCall) {
		ASTNode curGen = null;
		StringBuilder b = new StringBuilder("(");
		int i = 1;
		boolean flag = false;
		String resultString;
		while (methodCall.GetNodeSize() > i && (curGen = methodCall.GetNode(i)).type != TokenState.START) {
			System.out.println("GENGEN" + curGen.value);
			if (curGen.type == TokenState.GENERIC) {
				System.out.println("GENGEBEN");
				break;
			}
			b.append(resultString = strToByte(curGen.value));

			if (resultString.startsWith("T")) {
				flag = true;
			}
			if ((curGen = curGen.Grab(TokenState.GENERIC)) != null) {
				b.deleteCharAt(b.length()-1);
				b.append(typedGeneric(curGen, results.Classes.get(resultString).genType));
				b.append(";");
				flag = true;
			}
			i++;
		}
		b.append(")");
		
		
		if (flag || curGen.type == TokenState.GENERIC) {
			b.append(resultString = strToByte(methodCall.value));

			if ((curGen = methodCall.Grab(TokenState.GENERIC)) != null) {
				b.setLength(b.length()-1);
				b.append(typedGeneric(curGen, results.Classes.get(IfImport(methodCall.value)).genType));
				b.append(";");
			}
			System.out.println(b.toString());
			return b.toString();
		}
		return null;
	}
	
	
	
	private Object parseGeneric(ASTNode curGen) {
		StringBuilder r = new StringBuilder("<");
		for (int i = 0; i < curGen.GetNodeSize(); i++) {
			 switch(curGen.GetNode(i).type) {
			 case TokenState.CLASSNAME:
				 r.append(curGen.GetNode(i).value);
				 r.append(":");
				 r.append("Ljava/lang/Object;");
				 break;
			 case TokenState.CLASSMODIFIER:
				 r.append(curGen.GetNode(i).GetFirstNode().value);
				 r.append(":");
				 r.append(strToByte(curGen.GetNode(i).GetNode(1).value));
				 break;
			 case TokenState.GENERIC:
				 r.append(parseGeneric(curGen.GetNode(i)));
				 break;
			 }
		}
		return r.append(">").toString();
	}

	private String signatureWriterClass(ASTNode classCall) {
		ASTNode curGen;
		StringBuilder b = new StringBuilder();


		if ((curGen = classCall.Grab(TokenState.GENERIC)) != null) {
			b.append(parseGeneric(curGen));
			if ((curGen = classCall.Grab(TokenState.CLASSMODIFIER)) != null) {
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
		if ((curGen = fieldCall.Grab(TokenState.GENERIC)) != null) {
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
					//check comment
					popStack();
				}
				mv.visitMultiANewArrayInsn(valType = strToByte(node.value), node.GetNodeSize());
				return valType;
			}
			evalE(node.GetFirstNode(), "I");
			popStack();
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
		castTopStackForVar("I", popStack(), node.line);
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
				mv.visitInsn(Opcodes.BALOAD);
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
				mv.visitInsn(Opcodes.AALOAD);
				return type.replaceFirst("\\[", "");
			}
		}
		
	}
	
	private String loadArr(String type) {
		switch(type) {
		case "[I":
			mv.visitInsn(Opcodes.IALOAD);
			return "I";
		case "[Z":
			mv.visitInsn(Opcodes.BALOAD);
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
			mv.visitInsn(Opcodes.AALOAD);
			return type.replaceFirst("\\[", "");
		}
	}
	
	private String initArray(ASTNode node, String type) { //type is the type of the elements in the array, strToByte format
		String retType;
		int enCapCode;
		mv.visitLdcInsn(node.GetNodeSize());
		if (!(type == null)) {
			
			
			retType = "[" + type;
			switch(type) {
			case "Z":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
				enCapCode = Opcodes.IASTORE;
				break;
			case "B":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
				enCapCode = Opcodes.BASTORE;
				break;
			case "S":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
				enCapCode = Opcodes.SASTORE;
				break;
			case "I":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
				enCapCode = Opcodes.IASTORE;
				break;
			case "C":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR);
				enCapCode = Opcodes.CASTORE;
				break;
			case "J":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
				enCapCode = Opcodes.LASTORE;
				break;
			case "D":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
				enCapCode = Opcodes.DASTORE;
				break;
			case "F":
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
				enCapCode = Opcodes.FASTORE;
				break;
			default:
				System.out.println("hello?" + type);
				mv.visitTypeInsn(Opcodes.ANEWARRAY, type);
				enCapCode = Opcodes.AASTORE;
				break;
			}
			
			for (int i = 0; i < node.GetNodeSize(); i++) {
				mv.visitInsn(Opcodes.DUP);
				mv.visitLdcInsn(i);
				evalE(node.GetNode(i));
				castTopStackForVar(type, popStack(), node.line);
				mv.visitInsn(enCapCode);
			}
		}
		else {
			LinkedList<String> posType;
			ArrayList<String> nextType;
			LinkedList<StackInfo> elements = new LinkedList<>();
			
			
			mv.add(null);
			int arrayDec = mv.size()-1;
			if (node.GetNodeSize() > 0) {
				mv.visitInsn(Opcodes.DUP);
				mv.visitLdcInsn(0);
				evalE(node.GetFirstNode());
				elements.add(curStack.pop());
				posType = new LinkedList<String>(getAllPossibleTypes(elements.getFirst().type));
				for (int i = 1; i < node.GetNodeSize(); i++) {
					mv.visitInsn(Opcodes.DUP);
					mv.visitLdcInsn(i);
					evalE(node.GetNode(i));
					elements.add(curStack.pop());
					nextType = getAllPossibleTypes(elements.getLast().type);
					while (!nextType.contains(posType.getFirst())) {
						posType.removeFirst();
						if (posType.isEmpty()) {
							err.AmbiguousAutoArrayTypeException(node.value, node.line);
						}
					}
				}
				retType = "[" + posType.getFirst();
				switch(posType.getFirst()) {
				case "Z":
					mv.set(new CrodotInt(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN), arrayDec);
					enCapCode = Opcodes.IASTORE;
					break;
				case "B":
					mv.set(new CrodotInt(Opcodes.NEWARRAY, Opcodes.T_BYTE), arrayDec);
					enCapCode = Opcodes.BASTORE;
					break;
				case "S":
					mv.set(new CrodotInt(Opcodes.NEWARRAY, Opcodes.T_SHORT), arrayDec);
					enCapCode = Opcodes.SASTORE;
					break;
				case "I":
					mv.set(new CrodotInt(Opcodes.NEWARRAY, Opcodes.T_INT), arrayDec);
					enCapCode = Opcodes.IASTORE;
					break;
				case "J":
					mv.set(new CrodotInt(Opcodes.NEWARRAY, Opcodes.T_LONG), arrayDec);
					enCapCode = Opcodes.LASTORE;
					break;
				case "F":
					mv.set(new CrodotInt(Opcodes.NEWARRAY, Opcodes.T_FLOAT), arrayDec);
					enCapCode = Opcodes.FASTORE;
					break;
				case "D":
					mv.set(new CrodotInt(Opcodes.NEWARRAY, Opcodes.T_DOUBLE), arrayDec);
					enCapCode = Opcodes.DASTORE;
					break;
				default:
					if (posType.getFirst().startsWith("[")) {
						mv.set(new CrodotType(Opcodes.ANEWARRAY, posType.getFirst()), arrayDec);
					}
					else {
						mv.set(new CrodotType(Opcodes.ANEWARRAY, stripToImport(posType.getFirst())), arrayDec);
					}
					enCapCode = Opcodes.AASTORE;
					break;
				}
				StackInfo curEle;
				while (!elements.isEmpty()) {
					curEle = elements.removeLast();
					mv.insert(new CrodotInsn(enCapCode), curEle.posInQueue);
					if (curEle.type.length() < 2 && !curEle.type.equals(posType.getFirst())) {
						castingsPrimitive(curEle.type, curEle.posInQueue);
					}
					
				}
				
				
			}
			else {
				retType = "[Ljava/lang/Object;";
				mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
				
			}
			
			
		}
		return retType;
	}

//	private String initArray(ASTNode node) {
//		ASTNode temp = node;
//		int slack = -2;
//		String valType;
//		while (temp.type != TokenState.DECLARATION && temp.type != TokenState.DESCRIPTION)  {
//			temp = temp.prev;
//			slack += 2;
//		}
//		int enCapCode;
//		mv.visitLdcInsn(node.GetNodeSize());
//		switch(temp.value.substring(0, temp.value.length()-slack)) {
//		case "bool[]":
//			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
//			enCapCode = Opcodes.IASTORE;
//			valType = "Z";
//			break;
//		case "byte[]":
//			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
//			enCapCode = Opcodes.BASTORE;
//			valType = "B";
//			break;
//		case "shrt[]":
//			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
//			enCapCode = Opcodes.SASTORE;
//			valType = "S";
//			break;
//		case "int[]":
//			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
//			enCapCode = Opcodes.IASTORE;
//			valType = "I";
//			break;
//		case "char[]":
//			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR);
//			enCapCode = Opcodes.CASTORE;
//			valType = "C";
//			break;
//		case "long[]":
//			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
//			enCapCode = Opcodes.LASTORE;
//			valType = "J";
//			break;
//		case "doub[]":
//			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
//			enCapCode = Opcodes.DASTORE;
//			valType = "D";
//			break;
//		case "flt[]":
//			mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
//			enCapCode = Opcodes.FASTORE;
//			valType = "F";
//			break;
//		default:
//			valType = strToByte(temp.value.substring(0, temp.value.length()-slack-2));
//			if (valType.contains("[")) {
//				mv.visitTypeInsn(Opcodes.ANEWARRAY, valType);
//			}
//			else {
//				mv.visitTypeInsn(Opcodes.ANEWARRAY, valType.substring(1, valType.length()-1));
//			}
//			
//			enCapCode = Opcodes.AASTORE;
//			
//			break;
//		}
//		for (int i = 0; i < node.GetNodeSize(); i++) {
//			mv.visitInsn(Opcodes.DUP);
//			mv.visitLdcInsn(i);
//			evalE(node.GetNode(i));
//			castTopStackForVar(valType, popStack(), node.line);
//			mv.visitInsn(enCapCode);
//		}
//		
//		return (strToByte(temp.value.substring(0, temp.value.length()-slack)));
//		
//	}
	
	private void IfconditionalE(ASTNode node, Label label) {
		if (node.type == TokenState.BOOLEAN) {
			if (node.value.equals("true")) {
				mv.visitLdcInsn(1);
			} else {
				mv.visitLdcInsn(0);
			}
			mv.visitJumpInsn(Opcodes.IFEQ, label);
			return;
		}
		else if (node.type == TokenState.IS) {
			evalE(node);
			mv.visitJumpInsn(Opcodes.IFEQ, label);
			return;
		}
		
		evalE(node.GetFirstNode());
		StackInfo s1 = curStack.pop();
		evalE(node.GetNode(1));
		StackInfo s2 = curStack.pop();
		String fType;
		if (s1.type.equals(s2.type)) {
			fType = s1.type;
		}
		else {
			fType = castBothTG(s1, s2);
		}
		switch(node.type) {
		case TokenState.TRUEEQUALS:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPNE, label);
				break;
			case "J":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFNE, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFNE, label);
				break;
			default:
				mv.visitJumpInsn(Opcodes.IF_ACMPNE, label);
				break;
			}
			break;
		case TokenState.NOTEQUALS:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFEQ, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFEQ, label);
				break;
			default:
				mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
				break;
			}
			break;
		case TokenState.TRUEGREATERTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPLT, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLT, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLT, label);
				break;
			default:
				//err
			}
			break;
		case TokenState.TRUELESSTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPGT, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGT, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGT, label);
				break;
			default:
				//err
			}
			break;
		case TokenState.GREATERTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPLE, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFLE, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFLE, label);
				break;
			default:
				//err
			}
			break;
		case TokenState.LESSTHAN:
			switch(fType) {
			case "B", "S", "I", "C":
				mv.visitJumpInsn(Opcodes.IF_ICMPGE, label);
				break;
			case "L":
				mv.visitInsn(Opcodes.LCMP);
				mv.visitJumpInsn(Opcodes.IFGE, label);
				break;
			case "F":
				mv.visitInsn(Opcodes.FCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, label);
				break;
			case "D":
				mv.visitInsn(Opcodes.DCMPL);
				mv.visitJumpInsn(Opcodes.IFGE, label);
				break;
			default:
				//err
			}
			break;
		}
		
//		switch(node.type) {
//		case TokenState.BOOLEAN:
//			if (node.value.equals("true")) {
//				mv.visitLdcInsn(1);
//			}
//			else {
//				mv.visitLdcInsn(0);
//			}
//			mv.visitJumpInsn(Opcodes.IFNE, label);
//			break;
//		case TokenState.TRUEEQUALS:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CNotEquals(label);
//			break;
//		case TokenState.NOTEQUALS:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CTrueEquals(label);
//			break;
//		case TokenState.TRUEGREATERTHAN:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CLessThan(label);
//			break;
//		case TokenState.TRUELESSTHAN:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CGreaterThan(label);
//			break;
//		case TokenState.GREATERTHAN:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CTrueLessThan(label);
//			break;
//		case TokenState.LESSTHAN:
//			evalE(node.GetFirstNode());
//			evalE(node.GetNode(1));
//			CTrueGreaterThan(label);
//			break;
//		}
		
	}

	public void If(ASTNode tree) {
		//deprecated
		//its the labelList
		labelList.add(new Label());
		labelList.add(new Label());
		IfconditionalE(tree, labelList.peek());
	}
	
	public void Elif(ASTNode tree) {
		Label l = labelList.pop();
		mv.visitJumpInsn(Opcodes.GOTO, labelList.peek());
		labelList.add(new Label());
		mv.visitLabel(l); 
		IfconditionalE(tree, labelList.peek());
	}

	public void Else() {
		Label l = labelList.pop();
		mv.visitJumpInsn(Opcodes.GOTO, labelList.peek());
		mv.visitLabel(l);
	}
	public void EndElse() {
		mv.visitLabel(labelList.pop());
	}
	public void EndOfIf() {
		mv.visitLabel(labelList.pop());
		labelList.pop();
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

		labelList.add(new Label());
		labelList.add(new Label());
		
		if (tree.GetFirstNode().type == TokenState.DECLARATION) {
			evalE(tree.GetFirstNode().GetNode(1));
			newVar(tree.GetFirstNode().GetFirstNode().value, tree.GetFirstNode().value, null, tree.line);
		}
		else {
			evalE(tree.GetFirstNode().GetFirstNode());
			newUnknownVar(tree.GetFirstNode().value);
		}
		conditionalE(tree.GetNode(1), labelList.peek());
		mv.visitLabel(labelList.peek());
	}
	public void EndFor(ASTNode tree) {
		evalE(tree.GetNode(2));
		if (tree.GetFirstNode().type == TokenState.DECLARATION) {
			storeVar(tree.GetFirstNode().GetFirstNode().value, tree);
		}
		else {
			storeVar(tree.GetFirstNode().value, tree);
		}
		conditionalE(tree.GetNode(1), labelList.pop());
		mv.visitLabel(labelList.pop());
	}
	
	public String invokeEasy(ASTNode tree) {
		String[] Methodinfo;
		String longname = IfImport(tree.value);
		if (tree.prev.type == TokenState.DOT) {
			LinkedHashMap<String, String> genType = null;
			top = curStack.pop().type;
			if (top.startsWith("[")) {
				top = "[";
			}
			else {
				genType = sigToHash(top);
				top = stripToImport(top);
			}
			size = curStack.size();
			if (tree.GetNodeSize() > 0) evalE(tree.GetLastNode());
			if (genType == null) {
				Methodinfo = checkMethodvStack(tree.value, top, curStack.size()-size, tree.line);
			}
			else {
				Methodinfo = checkMethodvStack(tree.value, top, curStack.size()-size, genType, tree.line);
			}
			String desc = removeGenerics(Methodinfo[0]) + removeGenerics(Methodinfo[1]);
			if (Methodinfo[2].contains("static")) {
				invokeStatic(tree.value, IfImport(top), Methodinfo[0] + Methodinfo[1]);	
			}
			else {
				System.out.println(desc);
				invokePublic(tree.value, IfImport(top), desc);	
			}

			if (Methodinfo.length > 3 && Methodinfo[3] != null) {
				mv.visitTypeInsn(Opcodes.CHECKCAST, Methodinfo[3].substring(1, Methodinfo[3].length()-1));
				return  Methodinfo[3];
			}
			return Methodinfo[1];
		}
		else if (constructorCheck(longname)){

			Methodinfo = constructorDo(longname, tree);
			invokeSpecial("<init>", longname, Methodinfo[0] + Methodinfo[1]);

			
			return strToByte(tree.value);
		}
		else {
			size = curStack.size();
			if (tree.GetNodeSize() > 0) evalE(tree.GetLastNode());
			Methodinfo = checkMethodvStack(tree.value, curName, curStack.size()-size, tree.line);
			String desc = removeGenerics(Methodinfo[0]) + removeGenerics(Methodinfo[1]);
			if (Methodinfo[2].contains("static")) {
				invokeStatic(tree.value, curName, desc);
			}
			else {
				invokePublic(tree.value, curName, desc);
			}

			return Methodinfo[1];
		}
	}
	private LinkedHashMap<String, String> sigToHash(String stack) {
		int splitpoint = stack.indexOf('<');
		if (splitpoint < 0) return null;

		
		Iterator<String> gen = results.Classes.get(stack.substring(1, splitpoint)).genType.keySet().iterator();
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
		String cmd = "java -cp \"" + filename;
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
		System.out.println("THISRET" + returnType);
		if (returnType.equals("V")) {
			System.out.println("?");
			mv.visitInsn(Opcodes.RETURN);
			return;
		}
		
		
		evalE(tree);
		castTopStackForVar(returnType, popStack(), tree.line);
		switch(returnType) {
		case "Z", "C", "S", "I":
			mv.visitInsn(Opcodes.IRETURN);
			break;
		case "J":
			mv.visitInsn(Opcodes.LRETURN);
			break;
		case "F":
			mv.visitInsn(Opcodes.FRETURN);
			break;
		case "D":
			mv.visitInsn(Opcodes.DRETURN);
			break;
		default:
			mv.visitInsn(Opcodes.ARETURN);
			break;
		}
		
	}
	void storeArrforInc(String name, String type) {
		switch(type) {
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
	
	VariableManager getVarManager() {
		return vars;
	}

}


