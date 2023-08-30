package crodotInsn;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class CrodotTableSwitch extends CrodotCode {
	int min;
	int max;
	Label dflt;
	Label[] labels;
	public CrodotTableSwitch(int min, int max, Label dflt, Label... labels) {
		super();
		this.min = min;
		this.max = max;
		this.dflt = dflt;
		this.labels = labels;
	}
	
	public void upload(MethodVisitor mv) {
		mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	
}
