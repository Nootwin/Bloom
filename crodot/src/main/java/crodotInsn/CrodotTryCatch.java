package crodotInsn;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class CrodotTryCatch extends CrodotCode {
	Label start;
	Label end;
	Label handler;
	String type;
	public CrodotTryCatch(Label start, Label end, Label handler, String type) {
		super();
		this.start = start;
		this.end = end;
		this.handler = handler;
		this.type = type;
	}
	public void upload(MethodVisitor mv) {
		mv.visitTryCatchBlock(start, end, handler, type);
	}
	
	
}
