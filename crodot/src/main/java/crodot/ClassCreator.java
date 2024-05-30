package crodot;

import java.util.LinkedHashMap;

import org.objectweb.asm.ClassWriter;

public class ClassCreator {
	String simpleName;
	String internalName;
	
	ClassWriter cw;
	ClassCreator outer;
	
	ClassInfo classInfo;
	
	LinkedHashMap<String, String> curGenType;
	
	public ClassCreator(String simpleName, String internalName, ClassInfo classInfo) {
		this.simpleName = simpleName;
		this.internalName = internalName;
		this.classInfo = classInfo;
		this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
	}
	
	public ClassCreator(String simpleName, String internalName, ClassInfo classInfo, ClassCreator outer) {
		this.simpleName = simpleName;
		this.internalName = internalName;
		this.classInfo = classInfo;
		this.outer = outer;
		this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
	}
	
	public ClassCreator(String simpleName, String internalName, ClassInfo classInfo, ClassInfo outerClass, ClassCreator outer, LinkedHashMap<String, String> curGenType) {
        this.simpleName = simpleName;
        this.internalName = internalName;
        this.classInfo = classInfo;
        this.outer = outer;
        this.curGenType = curGenType;
		this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
	
	}
}
