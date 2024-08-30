package crodot;

public class MethodCreator {
	
	public MethodCreator(String name, String returnType, CrodotMethodVisitor prev) {
		super();
		this.name = name;
		this.returnType = returnType;
		this.prev = prev;
	}
	
	String name;
	String returnType;
	
	
	CrodotMethodVisitor prev;
	CrodotMethodVisitor mv;
	
}
