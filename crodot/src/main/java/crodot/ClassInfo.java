package crodot;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;


public class ClassInfo {
	String parent;
	ClassInfo outerClass;
	boolean construct;
	int AccessOpcode;
	String AccessModifiers;
	HashMap<String, MethodInfo> methods;
	HashMap<String, FieldInfo> fields;
	HashMap<String, ClassInfo> innerClasses;
	HashMap<String, String> localInnerClassNames;
	LinkedHashMap<String, String> genType;
	
	
	
	ClassInfo() {
		construct = false;
		methods = new HashMap<>();
		fields = new HashMap<>();
		innerClasses = new HashMap<>();
		localInnerClassNames = new HashMap<>();
	}
	
	public void willGeneric() {
		genType = new LinkedHashMap<>();
	}
	
	public Set<Entry<String, String>> Generics() {
		return genType.entrySet();
	}
	
	public boolean canGeneric() {
		if (genType != null) return true;
		return false;
	}
}
