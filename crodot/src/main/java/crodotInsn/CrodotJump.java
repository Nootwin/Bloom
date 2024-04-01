package crodotInsn;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class CrodotJump extends CrodotCode {
	int opcode;
	Label label;
	public CrodotJump(int opcode, Label label) {
		super();
		this.opcode = opcode;
		this.label = label;
	}
	public void upload(MethodVisitor mv) {
		mv.visitJumpInsn(opcode, label);
	}

	public String toString() {
		return "visitJumpInsn(" + opcode + ", " + label + ")";
	}
	
	
}
