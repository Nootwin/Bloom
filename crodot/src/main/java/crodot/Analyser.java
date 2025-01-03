package crodot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.Map.Entry;

import org.objectweb.asm.Opcodes;

import crodotStates.TokenState;




public class Analyser {
	private AnaResults results = new AnaResults(this);
	private Stack<String> curClass = new Stack<String>();
	private ASTNode trees;
	private String storage;
	private ASTNode temp;
	private int privacy = 0;
	private StringBuilder privacyString = new StringBuilder();
	private boolean accAlr;
	private ClassFetcher fetcher = new ClassFetcher();
	private ClassInfo curClassInfo;
	private int subcounter = 0;
	
	Analyser(ASTNode trees) {
		this.trees = trees;
	}
	
	String setCurClass(String name) {
		return curClass.push(name);
	}

	String getCurClass() {
		return curClass.peek();
	}

	String popCurClass() {
		return curClass.pop();
	}
	
	public AnaResults start() {
		results.Classes.put("Main", new ClassInfo(true));
		results.Classes.get("Main").methods.put("main", new MethodInfo("main", "V"));
		results.Classes.get("Main").methods.get("main").args.add(new ArgsList<String>());
		results.Classes.get("Main").methods.get("main").args.getLast().add("[Ljava/lang/String;");
		setCurClass("Main");
		accessPackage("java.lang");
		addToResults("java.io.PrintStream");
		curClassInfo = results.Classes.get("Main");
		curClassInfo.truename = "Main";
		
		File dir = new File(System.getProperty("user.dir") + "\\");
		File[] directoryListing = dir.listFiles();
		Queue<Class<?>> ids = new LinkedList<>();
		for (File child : directoryListing) {
			if (child.getName().endsWith(".class")) {
				ids.add(getClassObject(child.getName().substring(0, child.getName().indexOf('.'))));
			}
		}
		while (!ids.isEmpty()) {
			addToResults(ids.poll());
		}
		for (int i = 0; i < trees.GetNodeSize(); i++) {
			analyse1(trees.GetNode(i));
		}

		  
		
		try {
			ArgsList<String> args = new ArgsList<>();
			String name;
			Class<?> id = Class.forName("[I");
			name = "[";
			
			
			results.Classes.put(name, new ClassInfo(true));
			ClassInfo info = results.Classes.get(name);
			for (Method m : id.getMethods()) {
				if (!info.methods.containsKey(m.getName())) {
					info.methods.put(m.getName(), new MethodInfo(m.getName(), strToBytePlus(m.getReturnType().getName().replace('.', '/'))));
					info.methods.get(m.getName()).AccessModifiers = Modifier.toString(m.getModifiers());
				}	
				for (Parameter p : m.getParameters()) {
					args.add(strToBytePlus(p.getType().getName().replace('.', '/')));
				}
				info.methods.get(m.getName()).args.add(args);
				args = new ArgsList<>();
					
			}
			for (Constructor<?> c : id.getConstructors()) {
				if (!info.methods.containsKey(name)) {
					info.methods.put(name, new MethodInfo(c.getName(), "V"));
					info.methods.get(name).AccessModifiers = Modifier.toString(c.getModifiers());
				}	
				for (Parameter p : c.getParameters()) {
					args.add(strToBytePlus(p.getType().getName().replace('.', '/')));
				}
				info.methods.get(name).args.add(args);
			}
			info.fields.put("length", new FieldInfo("length", "I", name));
			info.fields.get("length").AccessModifiers = "public";
				
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return results;
	}
	
	private String strToBytePlus(String str) {
		if (str.contains("[")) {
			StringBuilder build = new StringBuilder();
			for (int i = 0; i < str.chars().filter(ch -> ch == '[').count(); i++) {
				build.append("[");
			}
			switch(str.replace("[]", "")) {
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
			case "shrt", "short":
				build.append("S");
				break;
			case "byte":
				build.append("B");
				break;
			case "bool", "boolean":
				build.append("Z");
				break;
			case "flt", "float":
				build.append("F");
				break;
			case "doub", "double":
				build.append("D");
				break;
			default:
				build.append('L');
				build.append(IfImport(str));
				build.append(';');
				break;
			}
			return build.toString();
		}
		else {
			switch(str) {
			case "void":
				return "V";
			case "str", "java.lang.String":
				return "Ljava/lang/String;";
			case "char":
				return "C";
			case "long":
				return "J";
			case "int":
				return "I";
			case "shrt", "short":
				return "S";
			case "byte":
				return "B";
			case "bool", "boolean":
				return "Z";
			case "flt", "float":
				return "F";
			case "doub", "double":
				return "D";
			default:
				return 'L' + IfImport(str) + ';';
			}
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
					results.addPotentialImportedClass(temp);
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
					results.addPotentialImportedClass(temp);
					System.out.println(temp + "WHJFHNBWUE");
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
					results.addPotentialImportedClass(temp);
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

	private String strToByte(String str) {
		if (str.contains("[")) {
			StringBuilder build = new StringBuilder();
			for (int i = 0; i < str.chars().filter(ch -> ch == '[').count(); i++) {
				build.append("[");
			}
			switch(str.replace("[]", "")) {
			case "void":
				build.append("V");
				break;
			case "str", "String":
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
			case "flt":
				build.append("F");
				break;
			case "doub":
				build.append("D");
				break;
			default:
				build.append('L');
				results.addPotentialImportedClass(str);
				build.append(IfImport(str));
				build.append(';');
				break;
			}
			return build.toString();
		}
		else {
			switch(str) {
			case "void":
				return "V";
			case "str", "String":
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
			case "flt":
				return "F";
			case "doub":
				return "D";
			default:
				results.addPotentialImportedClass(str);
				return 'L' + IfImport(str) + ';';
			}
		}
		

	}
	
	public String IfImport(String type) {
		if (results.qNames.containsKey(type)) {
			return results.qNames.get(type);
		}
		return type;
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
	
	

	public void analyse1(ASTNode tree) {
		switch(tree.type) {
		case TokenState.IMPORT:
			if (tree.GetFirstNode().value.charAt(tree.GetFirstNode().value.length()-1) == '*') {
				accessPackage(tree.GetFirstNode().value.substring(0, tree.GetFirstNode().value.length()-2));
				
			}
			else {
				addToResults(tree.GetFirstNode().value);
			}
			
			break;
		case TokenState.SUBDEFINITION:
			ClassInfo prev = curClassInfo;
			ClassInfo ininfo = new ClassInfo(true);
			setCurClass(getCurClass() + "$" + tree.GetFirstNode().value);
			results.Classes.put(getCurClass(), ininfo);
			curClassInfo.innerClasses.put(getCurClass(), ininfo);
			curClassInfo.localInnerClassNames.put(tree.GetFirstNode().value, getCurClass());
			curClassInfo = ininfo;
			curClassInfo.outerClass = prev;
			curClassInfo.truename = getCurClass();
			
			//might not need strtobyte
			FieldInfo outeraccess = curClassInfo.fields.put("this$" + subcounter, new FieldInfo("this$" + subcounter, strToByte(prev.truename), null));
			outeraccess = curClassInfo.fields.get("this$" + subcounter);
			curClassInfo.fields.put("<outer>", outeraccess);
			outeraccess.AccessModifiers = "final synthetic";
			outeraccess.AccessOpcode = Opcodes.ACC_FINAL + Opcodes.ACC_SYNTHETIC;
			
			
			subcounter++;
			
			if (accAlr) {
				if (privacy == 0) {
					switch(tree.value) {
					case "abstract":
						ininfo.AccessModifiers = "public abstract";
						ininfo.AccessOpcode = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT;
						break;
					case "interface":
						ininfo.AccessModifiers = "public interface";
						ininfo.AccessOpcode = Opcodes.ACC_PUBLIC + Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
						break;
					default:
						ininfo.AccessModifiers = "public";
						ininfo.AccessOpcode = Opcodes.ACC_PUBLIC;
						break;
					}
					
				}
				else {
					switch(tree.value) {
					case "abstract":
						privacyString.append(" abstract");
						privacy += Opcodes.ACC_ABSTRACT;
						break;
					case "interface":
						privacyString.append(" interface");
						privacy += Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
						break;
					}
					ininfo.AccessModifiers = privacyString.append("public").toString();
					ininfo.AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
			}
			else {
				switch(tree.value) {
				case "abstract":
					privacyString.append(" abstract");
					privacy += Opcodes.ACC_ABSTRACT;
					break;
				case "interface":
					privacyString.append(" interface");
					privacy += Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
					break;
				}
				ininfo.AccessModifiers = privacyString.toString();
				ininfo.AccessOpcode = privacy;
				
				privacyString.setLength(0);
				privacy = 0;
				accAlr = true;
			}
			if ((temp = tree.Grab(TokenState.CLASSMODIFIER)) != null) {
				ininfo.parent = IfImport(temp.value);
			}
			else {
				ininfo.parent = "java/lang/Object";
			}
			
			ininfo.methods.put(getCurClass(), new MethodInfo(getCurClass(), "V"));
			ininfo.methods.get(getCurClass()).args.add(new ArgsList<String>());
			ininfo.methods.get(getCurClass()).args.getFirst().add("L" + prev.truename + ";");

			if ((temp = tree.Grab(TokenState.GENERIC)) != null) {
				ininfo.willGeneric();
				for (int i = 0; i < temp.GetNodeSize(); i++) {
					switch(temp.GetNode(i).type) {
					case TokenState.CLASSNAME:
						ininfo.genType.put("T" + temp.GetNode(i).value + ";", "Ljava/lang/Object;");
						break;
					case TokenState.CLASSMODIFIER:
						ininfo.genType.put(temp.GetNode(i).GetFirstNode().value, temp.GetNode(i).GetNode(1).value);
						break;
						
					}
				}
			}
			
			ASTNode start2 = tree.GetLastNode();
			for (int i = 0; i < start2.GetNodeSize(); i++) {
				analyse1(start2.GetNode(i));
			}
			popCurClass();
			curClassInfo = prev;
			subcounter--;

			
			break;
			
		case TokenState.DEFINITION:
			popCurClass();
			setCurClass(tree.GetFirstNode().value);
			results.Classes.put(getCurClass(), curClassInfo = new ClassInfo(true));
			curClassInfo.truename = getCurClass();
			if (accAlr) {
				if (privacy == 0) {
					switch(tree.value) {
					case "abstract":
						curClassInfo.AccessModifiers = "public abstract";
						curClassInfo.AccessOpcode = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT;
						break;
					case "interface":
						curClassInfo.AccessModifiers = "public interface";
						curClassInfo.AccessOpcode = Opcodes.ACC_PUBLIC + Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
						break;
					default:
						curClassInfo.AccessModifiers = "public";
						curClassInfo.AccessOpcode = Opcodes.ACC_PUBLIC;
						break;
					}
					
				}
				else {
					switch(tree.value) {
					case "abstract":
						privacyString.append(" abstract");
						privacy += Opcodes.ACC_ABSTRACT;
						break;
					case "interface":
						privacyString.append(" interface");
						privacy += Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
						break;
					}
					curClassInfo.AccessModifiers = privacyString.append("public").toString();
					curClassInfo.AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
			}
			else {
				switch(tree.value) {
				case "abstract":
					privacyString.append(" abstract");
					privacy += Opcodes.ACC_ABSTRACT;
					break;
				case "interface":
					privacyString.append(" interface");
					privacy += Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
					break;
				}
				curClassInfo.AccessModifiers = privacyString.toString();
				curClassInfo.AccessOpcode = privacy;
				
				privacyString.setLength(0);
				privacy = 0;
				accAlr = true;
			}
			if ((temp = tree.Grab(TokenState.CLASSMODIFIER)) != null) {
				curClassInfo.parent = IfImport(temp.value);
			}
			else {
				curClassInfo.parent = "java/lang/Object";
			}
			curClassInfo.methods.put(getCurClass(), new MethodInfo(getCurClass(), "V"));
			curClassInfo.methods.get(getCurClass()).args.add(new ArgsList<String>());
			
			if ((temp = tree.Grab(TokenState.GENERIC)) != null) {
				curClassInfo.willGeneric();
				for (int i = 0; i < temp.GetNodeSize(); i++) {
					switch(temp.GetNode(i).type) {
					case TokenState.CLASSNAME:
						curClassInfo.genType.put("T" + temp.GetNode(i).value + ";", "Ljava/lang/Object;");
						break;
					case TokenState.CLASSMODIFIER:
						curClassInfo.genType.put(temp.GetNode(i).GetFirstNode().value, temp.GetNode(i).GetNode(1).value);
						break;
						
					}
				}
			}
			
			ASTNode start = tree.GetLastNode();
			for (int i = 0; i < start.GetNodeSize(); i++) {
				analyse1(start.GetNode(i));
			}

			popCurClass();
			setCurClass("Main");
			curClassInfo = results.Classes.get("Main");

			break;
		case TokenState.ACCESS:
				switch(tree.value) {
				case "local":
					if (accAlr) {
						privacy += Opcodes.ACC_OPEN;
						accAlr = false;
					}
					else {
						//error
					}
					break;
				case "priv":
					if (accAlr) {
						privacyString.append("private");
						privacy += Opcodes.ACC_PRIVATE;
						accAlr = false;
					}
					else {
						//error
					}
					break;
				case "proc":
					if (accAlr) {
						privacyString.append("protected");
						privacy += Opcodes.ACC_PROTECTED;
						accAlr = false;
					}
					else {
						//error
					}
					break;
				case "final":
					privacyString.append("final");
					privacy += Opcodes.ACC_FINAL;
					break;
				case "static":
					privacyString.append("static");
					privacy += Opcodes.ACC_STATIC;
					break;
				case "abstract":
					privacyString.append("abstract");
					privacy += Opcodes.ACC_ABSTRACT;
					break;
				}
				analyse1(tree.GetFirstNode());
				break;
		case TokenState.DESCRIPTION:
			
			storage = tree.GetFirstNode().value;
			if (getCurClass().equals("Main")) {
				curClassInfo.methods.put(storage, new MethodInfo(tree.GetFirstNode().value, strToByte(tree.value)));
				curClassInfo.methods.get(storage).args.add(fromNodetoArg(tree));
				curClassInfo.methods.get(storage).ownername = "Main";
				
				curClassInfo.methods.get(storage).AccessModifiers = privacyString.append("static").toString();
				curClassInfo.methods.get(storage).AccessOpcode = privacy + Opcodes.ACC_STATIC;
				
				privacyString.setLength(0);
				privacy = 0;
				accAlr = true;
				
			}
			else if (getCurClass().equals(tree.GetFirstNode().value)) {
				curClassInfo.methods.get(storage).args.add(fromNodetoArg(tree));
				curClassInfo.methods.get(storage).ownername = getCurClass();
				if (accAlr) {
					if (privacy == 0) {
						curClassInfo.methods.get(storage).AccessModifiers = "public";
						curClassInfo.methods.get(storage).AccessOpcode = Opcodes.ACC_PUBLIC;
					}
					else {
						curClassInfo.methods.get(storage).AccessModifiers = privacyString.append("public").toString();
						curClassInfo.methods.get(storage).AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
						
						privacyString.setLength(0);
						privacy = 0;
						accAlr = true;
					}
				}
				else {
					curClassInfo.methods.get(storage).AccessModifiers = privacyString.toString();
					curClassInfo.methods.get(storage).AccessOpcode = privacy;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
				
			}
			else {
				curClassInfo.methods.put(storage, new MethodInfo(tree.GetFirstNode().value, strToByte(tree.value)));
				curClassInfo.methods.get(storage).args.add(fromNodetoArg(tree));
				curClassInfo.methods.get(storage).ownername = getCurClass();
				
				if (accAlr) {
					
					if (privacy == 0) {
						curClassInfo.methods.get(storage).AccessModifiers = "public";
						curClassInfo.methods.get(storage).AccessOpcode = Opcodes.ACC_PUBLIC;
					}
					else {
						curClassInfo.methods.get(storage).AccessModifiers = privacyString.append("public").toString();
						curClassInfo.methods.get(storage).AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
						
						privacyString.setLength(0);
						privacy = 0;
						accAlr = true;
					}
				}
				else {
					curClassInfo.methods.get(storage).AccessModifiers = privacyString.toString();
					curClassInfo.methods.get(storage).AccessOpcode = privacy;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
			}
			break;
		case TokenState.DECLARATION:
				String name = tree.GetFirstNode().value;
				if (tree.GetNodeSize() > 1) {
					curClassInfo.fields.put(name, new FieldInfo(tree.GetFirstNode().value, strToByte(tree.value), tree.GetNode(1), getCurClass()));
				}
				else {
					curClassInfo.fields.put(name, new FieldInfo(tree.GetFirstNode().value, strToByte(tree.value), null));
				}
				if (accAlr) {
					if (privacy == 0) {
						curClassInfo.fields.get(name).AccessModifiers = "public";
						curClassInfo.fields.get(name).AccessOpcode = Opcodes.ACC_PUBLIC;
					}
					else {
						curClassInfo.fields.get(name).AccessModifiers = privacyString.append("public").toString();
						curClassInfo.fields.get(name).AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
						
						privacyString.setLength(0);
						privacy = 0;
						accAlr = true;
					}
				}
				else {
					curClassInfo.fields.get(name).AccessModifiers = privacyString.toString();
					curClassInfo.fields.get(name).AccessOpcode = privacy;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
			break;
		}
		
	}
	
	private void accessPackage(String packageName) {
		results.packageImports.add(packageName);
	}

//	private void accessPackage(String packageName) {
//		if (packageName.startsWith("java.")) {
//			File jars = new File("javart\\");
//			String shortname;
//			if (jars.exists()) {
//				try {
//					for (File file : jars.listFiles()) {
//						shortname = file.getName().substring(0, file.getName().length()-4);
//						if (shortname.equals(packageName)) {
//							BufferedReader read = new BufferedReader(new FileReader("javart\\" + file.getName()));
//							String line;
//							while((line = read.readLine()) != null) {
//								addToResults(shortname + "." + line);
//							}
//							read.close();
//						}
//					}
//				}
//				catch(IOException fnf) {
//					fnf.printStackTrace();
//				}
//			}
//			else {
//				System.out.println("error at import, Crodot lacks package data, Crodot java importer needs updating");
//			}
//		}
//		else {
//			//do stuff here
//		}
//	}
	
	public void addToResults(String classID) {
		String name = classID.replace('.', '/');
		Class<?> id = getClassObject(classID);
		
		if (id != null) {
			results.qNames.put(id.getSimpleName(), name);
			if (results.Classes.containsKey(name)) {
				return;
			}
		
			results.Classes.put(name, Import(name, id));
		}
		return;
		
	}
	
	public ClassInfo addToResults(Class<?> id) {
		String name = id.getCanonicalName().replace('.', '/');
		ClassInfo info = new ClassInfo(true);
		
		if (id != null) {
			results.qNames.put(id.getSimpleName(), name);
			if (results.Classes.containsKey(name)) {
				return null;
			}
		
			results.Classes.put(name, info = Import(name, id));
			return info;
		}
		return null;
		
	}
	

	public Class<?> getClassObject(String classID) {
		try {
			return Class.forName(classID);
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			return fetcher.fetchNonJava(System.getProperty("user.dir") + "\\" + classID + ".class", classID);
		}
	}
	
	public ClassInfo Import(String name, Class<?> id) {
		String nameHolder;
		ArgsList<String> args = new ArgsList<>();
			
		HashMap<String, String> genTypeMethod = null;
		ClassInfo info = new ClassInfo(true);
		
		info.truename = name;
		if (id.getEnclosingClass() != null) {
			info.parent = id.getEnclosingClass().getCanonicalName().replace('.', '/');
		}
			
		for (TypeVariable<?> type : id.getTypeParameters()) {
			if (!info.canGeneric()) {
				info.willGeneric();
			}
			info.genType.put("T" + type.getName() + ";", "L" + type.getBounds()[0].getTypeName().replace(".", "/") + ";");

		}
		for (Method m : id.getMethods()) {
			
			for (TypeVariable<?> type : m.getTypeParameters()) {
				if (genTypeMethod == null) {
					genTypeMethod = new HashMap<>();
				}
				genTypeMethod.put(type.getName(), type.getBounds()[0].getTypeName().replace(".", "/"));
			}
				
			if (!info.methods.containsKey(m.getName())) {
				info.methods.put(m.getName(), new MethodInfo(m.getName()));
				info.methods.get(m.getName()).ownername = name;
				info.methods.get(m.getName()).AccessModifiers = Modifier.toString(m.getModifiers());
			}
				
			for (Parameter p : m.getParameters()) {
				nameHolder = p.getParameterizedType().getTypeName();
				if (nameHolder.contains("<")) {
					args.add(strToByteGeneric(nameHolder, genTypeMethod, info.genType));
				}
				else if (genTypeMethod != null && genTypeMethod.containsKey(nameHolder)) {
					args.add(genTypeMethod.get("L" + nameHolder + ";"));
				}
				else if (info.canGeneric() && info.genType.containsKey("T" + nameHolder + ";")) {
					args.add("T" + nameHolder + ";");
				}
				else {
					args.add(strToBytePlus(nameHolder.replace('.', '/')));
				}
			}
				
			if (!info.methods.get(m.getName()).args.contains(args)) {
				info.methods.get(m.getName()).args.add(args);
				nameHolder = m.getGenericReturnType().getTypeName();
				if (nameHolder.contains("<")) {
					info.methods.get(m.getName()).returnType.add(strToByteGeneric(nameHolder, genTypeMethod, info.genType));
				}
				else if (genTypeMethod != null && genTypeMethod.containsKey(nameHolder.replace("[]", ""))) {
					info.methods.get(m.getName()).returnType.add(strToByte(genTypeMethod.get(nameHolder.replace("[]", ""))));
				}
				else if (info.canGeneric() && info.genType.containsKey("T" + nameHolder.replace("[]", "") + ";")) {
					info.methods.get(m.getName()).returnType.add("T" + moveBrackets(nameHolder) + ";");
				}
				else {
					info.methods.get(m.getName()).returnType.add(strToBytePlus(nameHolder.replace('.', '/')));
				}
			}

			args = new ArgsList<>();
			genTypeMethod = null;
		}
		for (Constructor<?> c : id.getConstructors()) {
			if (!info.methods.containsKey(name)) {
				info.methods.put(name, new MethodInfo(c.getName(), "V"));
				info.methods.get(name).AccessModifiers = Modifier.toString(c.getModifiers());
			}	
			for (Parameter p : c.getParameters()) {
				nameHolder = p.getParameterizedType().getTypeName();
				if (nameHolder.contains("<")) {
					args.add(strToByteGeneric(nameHolder, genTypeMethod, info.genType));
				}
				else if (genTypeMethod != null && genTypeMethod.containsKey(nameHolder)) {
					args.add(genTypeMethod.get("L" + nameHolder + ";"));
				}
				else if (info.canGeneric() && info.genType.containsKey("T" + nameHolder + ";")) {
					args.add("T" + nameHolder + ";");
				}
				else {
					args.add(strToBytePlus(nameHolder.replace('.', '/')));
				}
					
			}
			info.methods.get(name).args.add(args);
			args = new ArgsList<>();
		}
		for (Field f : id.getFields()) {
			info.fields.put(f.getName(), new FieldInfo(f.getName(), strToByte(f.getGenericType().getTypeName()), name));
			info.fields.get(f.getName()).AccessModifiers = Modifier.toString(f.getModifiers());
		}
		for (Class<?> c : id.getClasses()) {
			//touchup
			String name2 = c.getName().replace('.', '/');
			info.localInnerClassNames.put(c.getSimpleName(), name2);
			info.innerClasses.put(name2, Import(name2, c));
		}
			
		return info;

		
		
	}

	private String moveBrackets(String nameHolder) {
		StringBuilder r = new StringBuilder(nameHolder);
		for (int i = nameHolder.length()-1; i > 0; i--) {
			if (r.charAt(i) == ']') {
				r.deleteCharAt(i);
				r.deleteCharAt(i-1);
				r.insert(0, '[');
			}
			else {
				break;
			}
		}
		return r.toString();
	}
	
	public ArgsList<String> fromNodetoArg(ASTNode parent) {
		ArgsList<String> list = new ArgsList<>();
		boolean skip = false;
		for (int i = 1; i < parent.GetNodeSize()-1; i++) {
			switch(parent.GetNode(i).value) {
			case ",", "<>":
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
					if (!getCurClass().equals("Main") && curClassInfo.canGeneric() && curClassInfo.genType.containsKey("T" + parent.GetNode(i).value + ";")) {
						list.add("T" + parent.GetNode(i).value + ';');
					}
					else if (parent.GetNode(i).value.contains("<")){
						if (parent.GetNode(i).Grab(TokenState.GENERIC) != null) {
							String temp = strToByteGeneric(parent.GetNode(i).value, null, curClassInfo.genType);
							list.add(temp.substring(0, temp.length()-1) + genToString(parent.GetNode(i).Grab(TokenState.GENERIC)));
						} 
						else {
							list.add(strToByteGeneric(parent.GetNode(i).value, null, curClassInfo.genType));
						}
						
					}
					else {
						if (parent.GetNode(i).Grab(TokenState.GENERIC) != null) {
							String temp = strToByte(parent.GetNode(i).value);
							list.add(temp.substring(0, temp.length()-1) + genToString(parent.GetNode(i).Grab(TokenState.GENERIC)));
						} 
						else {
							list.add(strToByte(parent.GetNode(i).value));
						}
					}
					
					// this is where you put the obj path when you build a obj finder
				}
				break;
				
			}
			
			
		}
		return list;
		
	}
		
	
}
