package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotType extends CrodotCode {
	int opcode;
	String type;
	public CrodotType(int opcode, String type) {
		super();
		this.opcode = opcode;
		this.type = type;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitTypeInsn(opcode, type);
	}
	
	
}
