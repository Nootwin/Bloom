package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotIInc extends CrodotCode {
	int varIndex;
	int increment;
	public CrodotIInc(int varIndex, int increment) {
		super();
		this.varIndex = varIndex;
		this.increment = increment;
	}
	public void upload(MethodVisitor mv) {
		mv.visitIincInsn(varIndex, increment);
	}
	
	
}
