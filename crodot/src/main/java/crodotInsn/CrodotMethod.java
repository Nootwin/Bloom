package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotMethod extends CrodotCode {
	int opcode;
	String owner;
	String name;
	String descriptor;
	boolean isInterface;
	
	public CrodotMethod(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		super();
		this.opcode = opcode;
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
		this.isInterface = isInterface;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
	}
	
	public String toString() {
		return "mv.visitMethodInsn(" + opcode + ", " + owner + ", " + name + ", " + descriptor + ", " + isInterface + ");";
	}
	
	
}
