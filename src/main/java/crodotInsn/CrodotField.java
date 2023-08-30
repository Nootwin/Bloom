package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotField extends CrodotCode {
	int opcode;
	String owner;
	String name;
	String descriptor;
	
	public CrodotField(int opcode, String owner, String name, String descriptor) {
		super();
		this.opcode = opcode;
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitFieldInsn(opcode, owner, name, descriptor);
	}

}
