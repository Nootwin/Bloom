package crodot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class GenVarInfo extends VarInfo {
	LinkedHashMap<String, String> InferredTypes = new LinkedHashMap<>();
	StringBuilder sig = new StringBuilder();
	
	
	public GenVarInfo() {
		this.name = null;
		this.type = null;
		this.simpleType = null;
		this.pos = -1;
	}
	
	public GenVarInfo(String name, String type, int pos) {
		this.name = name;
		this.type = type;
		this.simpleType = type;
		this.pos = pos;
	}
	
	public GenVarInfo(String name, String type, String simpleType, int pos) {
		this.name = name;
		this.type = type;
		this.simpleType = simpleType;
		this.pos = pos;
	}
	
	public GenVarInfo(String name, String type, String simpleType, int pos, LinkedHashMap<String, String> m) {
		this.name = name;
		this.type = type;
		this.simpleType = simpleType;
		this.pos = pos;
		this.InferredTypes = m;
	}
	
	
	public GenVarInfo AddGenerics(ASTNode genNode, AnaResults results, String parentClass) {
		String classname = parentClass.replace("[]", ""); 

		if (results.qNames.containsKey(classname)) {
			//System.out.println(classname + "PEWMNMMMM");
			classname = results.qNames.get(classname);
		}
		//System.out.println(classname);
		ClassInfo info = results.Classes.get(classname);
		Iterator<String> keys = info.genType.keySet().iterator();
		for (int i = 0; i < info.genType.size(); i++) {
			sig.setLength(0);
			nodeToString(genNode.GetNode(i), results, parentClass);
			InferredTypes.put(keys.next(), sig.toString());
		}
		return this;
	}
	
	public GenVarInfo AddGenerics(LinkedHashMap<String, String> m) {
		InferredTypes = m;
		return this;
	}
	
	public String toString() {
		
		StringBuilder b = new StringBuilder();
		b.append(type);
		b.deleteCharAt(b.length()-1);
		b.append("<");
		for (Entry<String, String> entry: InferredTypes.entrySet()) {
			b.append(entry.getValue());
		}
		b.append(">;");
		return b.toString();
		
	}
	
	public void nodeToString(ASTNode className, AnaResults results, String parentClass) {
		String pClass = parentClass.replace("[]", ""); 

		if (results.qNames.containsKey(pClass)) {
			//System.out.println(pClass + "KFIOEJFEI");
			pClass = results.qNames.get(pClass);
		}
		//System.out.println("HEREHREREHER" + pClass);
		if (results.Classes.get(pClass).canGeneric() && results.Classes.get(pClass).genType.containsKey(("T" + className.value + ";"))) {
			sig.append("T");
			sig.append(className.value);
			sig.append(";");
		}
		else {
			sig.append("L");
			if (results.qNames.containsKey(className.value)) {
				sig.append(results.qNames.get(className.value));
			}
			else {
				sig.append(className.value);
			}
			if (className.GetNodeSize() > 0) {
				sig.append("<");
				for (int i = 0; i < className.GetNodeSize(); i++) {
					nodeToString(className.GetNode(i), results, parentClass);
				}
				sig.append(">");
			}
			sig.append(";");
		}
		
	}

}
