package crodot;

import java.util.LinkedList;
import java.util.Queue;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import crodotInsn.CrodotAttribute;
import crodotInsn.CrodotCode;
import crodotInsn.CrodotField;
import crodotInsn.CrodotFrame;
import crodotInsn.CrodotIInc;
import crodotInsn.CrodotInsn;
import crodotInsn.CrodotInt;
import crodotInsn.CrodotJump;
import crodotInsn.CrodotLabel;
import crodotInsn.CrodotLdc;
import crodotInsn.CrodotLine;
import crodotInsn.CrodotLookupSwitch;
import crodotInsn.CrodotMaxs;
import crodotInsn.CrodotMethod;
import crodotInsn.CrodotMultiANewArray;
import crodotInsn.CrodotTableSwitch;
import crodotInsn.CrodotTryCatch;
import crodotInsn.CrodotType;
import crodotInsn.CrodotVar;


public class CrodotMethodVisitor{
	LinkedList<CrodotCode> codes;
	Queue<CrodotCode> err = new LinkedList<>();
	MethodVisitor mv;
	
	CrodotMethodVisitor(MethodVisitor mv) {
		this.mv = mv;
		codes = new LinkedList<CrodotCode>();
		// TODO Auto-generated constructor stub
	}
	public void visitCode() {
		mv.visitCode();
	}
	public void visitAttribute(Attribute a) {
		codes.add(new CrodotAttribute(a));
	}
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		codes.add(new CrodotField(opcode, owner, name, descriptor));
	}
	public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		codes.add(new CrodotFrame(type, numLocal, local, numStack, stack));
	}
	public void visitIincInsn(int varIndex, int increment) {
		codes.add(new CrodotIInc(varIndex, increment));
	}
	public void visitInsn(int opcode) {
		codes.add(new CrodotInsn(opcode));
	}
	public void visitIntInsn(int opcode, int operand) {
		codes.add(new CrodotInt(opcode, operand));
	}
	public void visitJumpInsn(int opcode, Label label) {
		codes.add(new CrodotJump(opcode, label));
	}
	public void visitLabel(Label label) {
		codes.add(new CrodotLabel(label));
	}
	public void visitLdcInsn(Object value) {
		codes.add(new CrodotLdc(value));
	}
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		codes.add(new CrodotLookupSwitch(dflt, keys, labels));
	}
	public void visitMaxs(int maxStack, int maxLocals) {
		codes.add(new CrodotMaxs(maxStack, maxLocals));
	}
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		codes.add(new CrodotMethod(opcode, owner, name, descriptor, isInterface));
	}
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		codes.add(new CrodotMultiANewArray(descriptor, numDimensions));
	}
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		codes.add(new CrodotTableSwitch(min, max, dflt, labels));
	}
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		codes.add(new CrodotTryCatch(start, end, handler, type));
	}
	public void visitTypeInsn(int opcode, String type) {
		codes.add(new CrodotType(opcode, type));
	}
	public void visitVarInsn(int opcode, int varIndex) {
		codes.add(new CrodotVar(opcode, varIndex));
	}
	public void visitLineInsn(int line, Label l) {
		codes.add(new CrodotLine(line, l));
	}
	public void visitEnd() {
		this.apply();
		mv.visitEnd();
	}

	public void insert(CrodotCode code, int space) {
		codes.add(space, code);
	}
	public CrodotCode pop() {
		return codes.removeLast();
	}
	
	
	public void apply() {
		CrodotCode c;
		debug();
		while ((c = codes.poll()) != null) {

			c.upload(mv);
			
		}
		
	}
	
	public void debug() {
		System.out.println("START DEBUG");
		for (CrodotCode c : codes) {
			System.out.println(c.toString());
		}
		System.out.println("END DEBUG");
	}
	
	public int size() {
		return codes.size();
	}
	
	
	

}
