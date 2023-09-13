package crodot;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;


public class ClassInfo {
	String name;
	String parent;
	boolean construct;
	int AccessOpcode;
	String AccessModifiers;
	HashMap<String, MethodInfo> methods;
	HashMap<String, FieldInfo> fields;
	LinkedHashMap<String, String> genType;
	
	
	ClassInfo(String name) {
		this.name = name;
		construct = false;
		methods = new HashMap<>();
		fields = new HashMap<>();
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
