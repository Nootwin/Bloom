package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotInt extends CrodotCode {
	int opcode;
	int operand;
	public CrodotInt(int opcode, int operand) {
		super();
		this.opcode = opcode;
		this.operand = operand;
	}
	public void upload(MethodVisitor mv) {
		mv.visitIntInsn(opcode, operand);
	}
	
	
}
