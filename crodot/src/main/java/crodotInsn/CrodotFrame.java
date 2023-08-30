package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotFrame extends CrodotCode {
	int type;
	int numLocal;
	Object[] local;
	int numStack;
	Object[] stack;
	public CrodotFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		super();
		this.type = type;
		this.numLocal = numLocal;
		this.local = local;
		this.numStack = numStack;
		this.stack = stack;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitFrame(type, numLocal, local, numStack, stack);
	}

	



}
