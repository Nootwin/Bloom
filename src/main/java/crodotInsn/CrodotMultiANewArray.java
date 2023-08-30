package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotMultiANewArray extends CrodotCode {
	String descriptor;
	int numDimensions;
	public CrodotMultiANewArray(String descriptor, int numDimensions) {
		super();
		this.descriptor = descriptor;
		this.numDimensions = numDimensions;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitMultiANewArrayInsn(descriptor, numDimensions);
	}
	
	
}
