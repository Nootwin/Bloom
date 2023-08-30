package crodot;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class caller {

	static void printoken (ArrayList<token> tcode) {
			for (token i : tcode) {
			System.out.print(i.type);
			System.out.print(' ');
			System.out.println(i.value);
			}
	}
	
	static void indtree(ASTNode tree, int n) {
		for (int z = 0; z < n; z++) {
			System.out.print('-');
		}
		System.out.println(tree.type);
		for (ASTNode i : tree.next) {
			indtree(i, n+1);
		}
	}
	
	static void printree(ArrayList<ASTNode> ncode) {
		for (ASTNode tree : ncode) {
			indtree(tree, 0);
		}
	}
	static void printresults(AnaResults r) {
		for (Map.Entry<String, ClassInfo> Class: r.Classes.entrySet()) {
			for (Map.Entry<String, MethodInfo> Method: Class.getValue().methods.entrySet()) {
				System.out.println("Class: " + Class.getValue().name + " Key: " + Class.getKey() + " Method: " + Method.getValue().name + " Key: " + Method.getKey() + " RETURN: " + Method.getValue().returnType + " First: " + Method.getValue().args.toString());
			}
			for (Map.Entry<String, FieldInfo> Field: Class.getValue().fields.entrySet()) {
				System.out.println("Class: " + Class.getValue().name + " Key: " + Class.getKey() + " Field: " + Field.getValue().name + " Key: " + Field.getKey() + " Type: " + Field.getValue().type);
			}
		}
	}
	static void printhelpfulresults(AnaResults r) {
		Set<Entry<String, String>> types;
		for (Map.Entry<String, ClassInfo> Class: r.Classes.entrySet()) {
			if (!Class.getValue().name.startsWith("java/lang")) {
				for (Map.Entry<String, MethodInfo> Method: Class.getValue().methods.entrySet()) {
					System.out.print("Class: " + Class.getValue().name + " Key: " + Class.getKey() + " Method: " + Method.getValue().name + " Key: " + Method.getKey() + " RETURN: " + Method.getValue().returnType + " First: ");
					System.out.print("[");
					for (int i = 0; i < Method.getValue().args.size(); i++) {
						System.out.print(Method.getValue().args.get(i).toString());
						System.out.print(", ");
					}
					System.out.println("]");
				}
				for (Map.Entry<String, FieldInfo> Field: Class.getValue().fields.entrySet()) {
					System.out.println("Class: " + Class.getValue().name + " Key: " + Class.getKey() + " Field: " + Field.getValue().name + " Key: " + Field.getKey() + " Type: " + Field.getValue().type);
				}
				
				if (Class.getValue().genType != null) {
					types = Class.getValue().Generics();
					System.out.print(Class.getValue().name);
					for (Entry<String, String> entry : types) {
						System.out.print("   " + entry.getKey());
						System.out.print(" " + entry.getValue());
					}
					System.out.println();
				}
				
			}
			
		}
	}
	
	public static void main(String[] args) {
		String file = fileReader.ReadFileToString("C:\\Users\\Nolan Murray\\git\\cro\\crodot\\src\\main\\java\\crodot\\main.cr");
		System.out.println(file);
		
		Lexer clone = new Lexer();
		ArrayList<token> lexed = clone.lex(file);
		printoken(lexed);
		
		clone = null;
		
		System.out.println("############");
		
		Parser newparse = new Parser(lexed);
		
		lexed = null;
		
		ASTNode parsed = newparse.parse();
		newparse = null;
		indtree(parsed, 0);
		
		Analyser analy = new Analyser(parsed);
		
		AnaResults results = analy.start();
		
		printhelpfulresults(results);
		Generator gen = new Generator(parsed, results );
		
		gen.createSys(null);
		

		
	}

}
