package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotVar extends CrodotCode {
	int opcode;
	int varIndex;
	public CrodotVar(int opcode, int varIndex) {
		super();
		this.opcode = opcode;
		this.varIndex = varIndex;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitVarInsn(opcode, varIndex);
	}
	
	public String toString() {
		return "VAR" + opcode;
	}
	
	
}
