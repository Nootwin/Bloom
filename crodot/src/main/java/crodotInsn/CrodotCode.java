package crodotInsn;

import org.objectweb.asm.MethodVisitor;

public abstract class CrodotCode {
	public abstract void upload(MethodVisitor mv);
}
