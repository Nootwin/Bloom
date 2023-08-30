package crodotInsn;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class CrodotLookupSwitch extends CrodotCode {
	Label dflt;
	int[] keys;
	Label[] labels;
	
	public CrodotLookupSwitch(Label dflt, int[] keys, Label[] labels) {
		super();
		this.dflt = dflt;
		this.keys = keys;
		this.labels = labels;
	}
	public void upload(MethodVisitor mv) {
		mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	
}
