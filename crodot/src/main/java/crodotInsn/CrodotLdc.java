package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public class CrodotLdc extends CrodotCode {
	Object value;

	public CrodotLdc(Object value) {
		super();
		this.value = value;
	}
	public void upload(MethodVisitor mv) {
		mv.visitLdcInsn(value);
	}

	public String toString() {
		return "LDC " + value;
	}
	
}
