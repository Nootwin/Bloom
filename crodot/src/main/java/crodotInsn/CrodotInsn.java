package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotInsn extends CrodotCode {
	public int opcode;

	public CrodotInsn(int opcode) {
		super();
		this.opcode = opcode;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitInsn(opcode);
	}

	public String toString() {
		return "mv.visitInsn(" + opcode + ");";
	}
}
