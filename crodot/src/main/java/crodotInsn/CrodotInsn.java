package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotInsn extends CrodotCode {
	int opcode;

	public CrodotInsn(int opcode) {
		super();
		this.opcode = opcode;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitInsn(opcode);
	}
}
