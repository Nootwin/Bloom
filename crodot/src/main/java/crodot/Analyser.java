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
import java.util.Map.Entry;

import org.objectweb.asm.Opcodes;

import crodotStates.TokenState;




public class Analyser {
	private AnaResults results = new AnaResults();
	private String curClass;
	private ASTNode trees;
	private String storage;
	private ASTNode temp;
	private int privacy = 0;
	private StringBuilder privacyString = new StringBuilder();
	private boolean accAlr;
	private ClassFetcher fetcher = new ClassFetcher();
	
	Analyser(ASTNode trees) {
		this.trees = trees;
	}
	
	public AnaResults start() {
		results.Classes.put("Main", new ClassInfo());
		results.Classes.get("Main").methods.put("main", new MethodInfo("main", "V"));
		results.Classes.get("Main").methods.get("main").args.add(new ArgsList<String>());
		results.Classes.get("Main").methods.get("main").args.getLast().add("[Ljava/lang/String;");
		curClass = "";
		accessPackage("java.lang");
		Import("java.io.PrintStream");
		
		File dir = new File(System.getProperty("user.dir") + "\\");
		File[] directoryListing = dir.listFiles();
		for (File child : directoryListing) {
			if (child.getName().endsWith(".class")) {
				Import(child.getName().substring(0, child.getName().indexOf('.')));
			}
		}
		for (int i = 0; i < trees.GetNodeSize(); i++) {
			analyse1(trees.GetNode(i));
		}

		  
		
		try {
			ArgsList<String> args = new ArgsList<>();
			String name;
			Class<?> id = Class.forName("[I");
			name = "[";
			
			
			results.Classes.put(name, new ClassInfo());
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
			info.fields.put("length", new FieldInfo("length", "I"));
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

	public String strToByte(String str) {
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
			case "flt":
				return "F";
			case "doub":
				return "D";
			default:
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
	
	

	public void analyse1(ASTNode tree) {
		switch(tree.type) {
		case TokenState.IMPORT:
			if (tree.GetFirstNode().value.charAt(tree.GetFirstNode().value.length()-1) == '*') {
				accessPackage(tree.GetFirstNode().value.substring(0, tree.GetFirstNode().value.length()-2));
				
			}
			else {
				Import(tree.GetFirstNode().value);
			}
			
			break;
		case TokenState.DEFINITION:
			curClass = tree.GetFirstNode().value;
			results.Classes.put(curClass, new ClassInfo());
			if (accAlr) {
				if (privacy == 0) {
					switch(tree.value) {
					case "abstract":
						results.Classes.get(curClass).AccessModifiers = "public abstract";
						results.Classes.get(curClass).AccessOpcode = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT;
						break;
					case "interface":
						results.Classes.get(curClass).AccessModifiers = "public interface";
						results.Classes.get(curClass).AccessOpcode = Opcodes.ACC_PUBLIC + Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
						break;
					default:
						results.Classes.get(curClass).AccessModifiers = "public";
						results.Classes.get(curClass).AccessOpcode = Opcodes.ACC_PUBLIC;
						break;
					}
					
				}
				else {
					switch(tree.value) {
					case "abstract":
						privacyString.append("abstract ");
						privacy += Opcodes.ACC_ABSTRACT;
						break;
					case "interface":
						privacyString.append("interface");
						privacy += Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
						break;
					}
					results.Classes.get(curClass).AccessModifiers = privacyString.append("public").toString();
					results.Classes.get(curClass).AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
			}
			else {
				switch(tree.value) {
				case "abstract":
					privacyString.append("abstract ");
					privacy += Opcodes.ACC_ABSTRACT;
					break;
				case "interface":
					privacyString.append("interface");
					privacy += Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT;
					break;
				}
				results.Classes.get(curClass).AccessModifiers = privacyString.toString();
				results.Classes.get(curClass).AccessOpcode = privacy;
				
				privacyString.setLength(0);
				privacy = 0;
				accAlr = true;
			}
			if ((temp = tree.Grab(TokenState.CLASSMODIFIER)) != null) {
				results.Classes.get(curClass).parent = IfImport(temp.value);
			}
			else {
				results.Classes.get(curClass).parent = "java/lang/Object";
			}
			results.Classes.get(curClass).methods.put(curClass, new MethodInfo(curClass, "V"));
			results.Classes.get(curClass).methods.get(curClass).args.add(new ArgsList<String>());
			if ((temp = tree.Grab(TokenState.GENERIC)) != null) {
				results.Classes.get(curClass).willGeneric();
				for (int i = 0; i < temp.GetNodeSize(); i++) {
					System.out.println(temp.GetNode(i).type);
					System.out.println(temp.GetNode(i).value);
					switch(temp.GetNode(i).type) {
					case TokenState.CLASSNAME:
						results.Classes.get(curClass).genType.put("T" + temp.GetNode(i).value + ";", "Ljava/lang/Object;");
						break;
					case TokenState.CLASSMODIFIER:
						results.Classes.get(curClass).genType.put(temp.GetNode(i).GetFirstNode().value, temp.GetNode(i).GetNode(1).value);
						break;
						
					}
				}
			}
			
			ASTNode start = tree.GetLastNode();
			for (int i = 0; i < start.GetNodeSize(); i++) {
				analyse1(start.GetNode(i));
			}

			break;
		case TokenState.ACCESS:
				System.out.println(tree.value);
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
						System.out.println("PRIVATE");
						privacy += Opcodes.ACC_PRIVATE;
						accAlr = false;
					}
					else {
						System.out.println("ERROR");
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
			if (curClass.equals("")) {
				results.Classes.get("Main").methods.put(storage, new MethodInfo(tree.GetFirstNode().value, strToByte(tree.value)));
				results.Classes.get("Main").methods.get(storage).args.add(fromNodetoArg(tree));
				
				results.Classes.get("Main").methods.get(storage).AccessModifiers = privacyString.append("static").toString();
				results.Classes.get("Main").methods.get(storage).AccessOpcode = privacy + Opcodes.ACC_STATIC;
				
				privacyString.setLength(0);
				privacy = 0;
				accAlr = true;
				
			}
			else if (curClass.equals(tree.GetFirstNode().value)) {
				results.Classes.get(curClass).methods.get(storage).args.add(fromNodetoArg(tree));
				if (accAlr) {
					if (privacy == 0) {
						results.Classes.get(curClass).methods.get(storage).AccessModifiers = "public";
						results.Classes.get(curClass).methods.get(storage).AccessOpcode = Opcodes.ACC_PUBLIC;
					}
					else {
						results.Classes.get(curClass).methods.get(storage).AccessModifiers = privacyString.append("public").toString();
						results.Classes.get(curClass).methods.get(storage).AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
						
						privacyString.setLength(0);
						privacy = 0;
						accAlr = true;
					}
				}
				else {
					results.Classes.get(curClass).methods.get(storage).AccessModifiers = privacyString.toString();
					results.Classes.get(curClass).methods.get(storage).AccessOpcode = privacy;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
				
			}
			else {
				results.Classes.get(curClass).methods.put(storage, new MethodInfo(tree.GetFirstNode().value, strToByte(tree.value)));
				results.Classes.get(curClass).methods.get(storage).args.add(fromNodetoArg(tree));
				
				if (accAlr) {
					
					if (privacy == 0) {
						results.Classes.get(curClass).methods.get(storage).AccessModifiers = "public";
						results.Classes.get(curClass).methods.get(storage).AccessOpcode = Opcodes.ACC_PUBLIC;
					}
					else {
						results.Classes.get(curClass).methods.get(storage).AccessModifiers = privacyString.append("public").toString();
						results.Classes.get(curClass).methods.get(storage).AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
						
						privacyString.setLength(0);
						privacy = 0;
						accAlr = true;
					}
				}
				else {
					System.out.println(privacyString.toString() + "    " + privacy + "    " + Opcodes.ACC_PRIVATE);
					results.Classes.get(curClass).methods.get(storage).AccessModifiers = privacyString.toString();
					results.Classes.get(curClass).methods.get(storage).AccessOpcode = privacy;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
			}
			break;
		case TokenState.DECLARATION:
			if (!curClass.isBlank()) {
				String name = tree.GetFirstNode().value;
				if (tree.GetNodeSize() > 1) {
					results.Classes.get(curClass).fields.put(name, new FieldInfo(tree.GetFirstNode().value, strToByte(tree.value), tree.GetNode(1)));
				}
				else {
					results.Classes.get(curClass).fields.put(name, new FieldInfo(tree.GetFirstNode().value, strToByte(tree.value), null));
				}
				if (accAlr) {
					if (privacy == 0) {
						results.Classes.get(curClass).fields.get(name).AccessModifiers = "public";
						results.Classes.get(curClass).fields.get(name).AccessOpcode = Opcodes.ACC_PUBLIC;
					}
					else {
						results.Classes.get(curClass).fields.get(name).AccessModifiers = privacyString.append("public").toString();
						results.Classes.get(curClass).fields.get(name).AccessOpcode = privacy + Opcodes.ACC_PUBLIC;
						
						privacyString.setLength(0);
						privacy = 0;
						accAlr = true;
					}
				}
				else {
					results.Classes.get(curClass).fields.get(name).AccessModifiers = privacyString.toString();
					results.Classes.get(curClass).fields.get(name).AccessOpcode = privacy;
					
					privacyString.setLength(0);
					privacy = 0;
					accAlr = true;
				}
			}
			break;
		case TokenState.END:
			if (tree.prev.prev.type == TokenState.DEFINITION) {
				curClass = "";
			}
			break;
		}
	
		
	}

	private void accessPackage(String packageName) {
		if (packageName.startsWith("java.")) {
			File jars = new File("javart\\");
			String shortname;
			if (jars.exists()) {
				try {
					for (File file : jars.listFiles()) {
						shortname = file.getName().substring(0, file.getName().length()-4);
						if (shortname.equals(packageName)) {
							BufferedReader read = new BufferedReader(new FileReader("javart\\" + file.getName()));
							String line;
							while((line = read.readLine()) != null) {
								Import(shortname + "." + line);
							}
							read.close();
						}
					}
				}
				catch(IOException fnf) {
					fnf.printStackTrace();
				}
			}
			else {
				System.out.println("error at import, Crodot lacks package data, Crodot java importer needs updating");
			}
		}
		else {
			//do stuff here
		}
	}
	private void Import(String classID) {
		String nameHolder;
		Class<?> id;
		String name = classID.replace('.', '/');
		if (results.Classes.containsKey(name)) {
			return;
		}
		try {
			id = Class.forName(classID);
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			id = fetcher.fetchNonJava(System.getProperty("user.dir") + "\\" + classID + ".class", classID);
		}
			ArgsList<String> args = new ArgsList<>();
			
			results.qNames.put(id.getSimpleName(), name);
			
			HashMap<String, String> genTypeMethod = null;
			ClassInfo info;
			
			
			results.Classes.put(name, info = new ClassInfo());
			
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
					info.methods.get(m.getName()).AccessModifiers = Modifier.toString(m.getModifiers());
				}
				
				for (Parameter p : m.getParameters()) {
					nameHolder = p.getParameterizedType().getTypeName();
					if (nameHolder.contains("<")) {
						args.add(strToByteGeneric(nameHolder, genTypeMethod, info.genType));
					}
					else if (genTypeMethod != null && genTypeMethod.containsKey(nameHolder)) {
						System.out.println("IAMTHEONE");
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
						System.out.println("IAMTHEONE");
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
				info.fields.put(f.getName(), new FieldInfo(f.getName(), strToByte(f.getGenericType().getTypeName())));
				info.fields.get(f.getName()).AccessModifiers = Modifier.toString(f.getModifiers());
			}	

		
		
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
					if (results.Classes.get(curClass).genType.containsKey("T" + parent.GetNode(i).value + ";")) {
						list.add("T" + parent.GetNode(i).value + ';');
					}
					else if (parent.GetNode(i).value.contains("<")){
						list.add(strToByteGeneric(parent.GetNode(i).value, null, results.Classes.get(curClass).genType));
					}
					else {
						list.add(strToByte(parent.GetNode(i).value));
					}
					
					// this is where you put the obj path when you build a obj finder
				}
				
			}
			
			
		}
		return list;
		
	}
		
	
}
