package crodotInsn;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class CrodotLabel extends CrodotCode {
	Label label;

	public CrodotLabel(Label label) {
		super();
		this.label = label;
	}
	public void upload(MethodVisitor mv) {
		mv.visitLabel(label);
	}
	
}
