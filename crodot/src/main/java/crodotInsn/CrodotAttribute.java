package crodotInsn;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.MethodVisitor;

public class CrodotAttribute extends CrodotCode {
	Attribute attribute;
	public CrodotAttribute(Attribute attribute) {
		super();
		this.attribute = attribute;
		// TODO Auto-generated constructor stub
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitAttribute(attribute);
	}

}
