package crodot;

import java.util.LinkedList;

public class MethodInfo {
	String name;
	String AccessModifiers;
	int AccessOpcode;
	LinkedList<ArgsList<String>> args;
	LinkedList<String> returnType;
	
	MethodInfo(String name) {
		this.name = name;
		this.returnType = new LinkedList<>();
		args = new LinkedList<>();
	}
	MethodInfo(String name, String returnType) {
		this.name = name;
		this.returnType = new LinkedList<>();
		this.returnType.add(returnType);
		args = new LinkedList<>();
	}
	
	
}
