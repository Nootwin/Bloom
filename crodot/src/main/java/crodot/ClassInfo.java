package crodot;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;


public class ClassInfo {
	boolean extracted;
	String truename;
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
	
	
	
	ClassInfo(boolean extracted) {
		this.extracted = extracted;
		if (extracted) {
			construct = false;
			methods = new HashMap<>();
			fields = new HashMap<>();
			innerClasses = new HashMap<>();
			localInnerClassNames = new HashMap<>();
		}
	}


	public void extract() {
		extracted = true;
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
	
	public FieldInfo getField(String name) {
		ClassInfo c = this;
		while (c != null) {
			FieldInfo f = c.fields.get(name);
			if (f != null)
				return f;
			c = c.outerClass;
		}
		return null;
	}
	
	public MethodInfo getMethod(String name) {
		ClassInfo c = this;
		while (c != null) {
			MethodInfo m = c.methods.get(name);
			if (m != null)
				return m;
			c = c.outerClass;
		}
		return null;
	}
	

}
