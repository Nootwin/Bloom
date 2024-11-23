package crodot;

import java.util.HashMap;
import java.util.LinkedList;

public class AnaResults {
	HashMap<String, ClassInfo> Classes = new HashMap<>();
	HashMap<String, String> qNames = new HashMap<>();
	LinkedList<String> packageImports = new LinkedList<>();
	
	Analyser analyser;
	
	AnaResults(Analyser analyser) {
        this.analyser = analyser;
	}
	
	ClassInfo addPotentialImportedClass(String simplename) {
		for (int i = 0; i < packageImports.size(); i++) {
			String pName = packageImports.get(i) + "." + simplename;
			System.out.println("Checking: " + pName);
			Class<?> mbClass = analyser.getClassObject(pName);
			if (mbClass != null) {
				return analyser.addToResults(mbClass);
			}
			
		}
		
		System.out.println("No class found for: " + simplename);
		
		return null;
	}
}
