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
	
	
	
	public GenVarInfo AddGenerics(ASTNode genNode, AnaResults results, String parentClass) {
		ClassInfo info = results.Classes.get(simpleType);
		Iterator<String> keys = info.genType.keySet().iterator();
		for (int i = 0; i < info.genType.size(); i++) {
			sig.setLength(0);
			nodeToString(genNode.GetNode(i), results, parentClass);
			InferredTypes.put(keys.next(), sig.toString());
		}
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
		if (results.Classes.get(parentClass).canGeneric() && results.Classes.get(parentClass).genType.containsKey(("T" + className.value + ";"))) {
			sig.append("T");
			sig.append(className.value);
			sig.append(";");
		}
		else {
			sig.append("L");
			if (results.Classes.containsKey(className.value)) {
				sig.append(results.Classes.get(className.value).name);
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
