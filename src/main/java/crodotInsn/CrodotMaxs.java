package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotMaxs extends CrodotCode {
	int maxStack;
	int maxLocals;
	public CrodotMaxs(int maxStack, int maxLocals) {
		super();
		this.maxStack = maxStack;
		this.maxLocals = maxLocals;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitMaxs(maxStack, maxLocals);
	}
	
	
}
