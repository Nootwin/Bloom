package crodot;

import java.util.LinkedList;

public class MethodCreator {
	
	public MethodCreator(String name, String returnType, CrodotMethodVisitor mv) {
		super();
		this.name = name;
		this.returnType = returnType;
		this.mv = mv;
		this.prev = null;
	}
	
	
	public MethodCreator(String name, String returnType, CrodotMethodVisitor mv, CrodotMethodVisitor prev) {
		super();
		this.name = name;
		this.returnType = returnType;
		this.mv = mv;
		this.prev = prev;
	}
	
	String name;
	String returnType;
	
	
	CrodotMethodVisitor prev;
	CrodotMethodVisitor mv;
	
	LinkedList<ClassInfo> innerclasses = new LinkedList<ClassInfo>();
	
}
