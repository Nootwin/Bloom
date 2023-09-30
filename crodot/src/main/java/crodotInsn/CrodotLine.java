package crodotInsn;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class CrodotLine extends CrodotCode {
	int line;
	Label label;
	public CrodotLine(int l, Label label) {
		super();
		this.line = l;
		this.label = label;
	}
	public void upload(MethodVisitor mv) {
		mv.visitLineNumber(line, label);
	}

}
