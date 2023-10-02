package crodot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Queue;

public class ClassFetcher extends ClassLoader {
	public Class<?> fetchNonJava(String fN, String name){ //fileName in strToByteFormat	
		File file = new File(fN);
		System.out.println(file.exists());
		if (file.exists()) {
			System.out.println(file.toPath());
			byte[] b;
			try {
				b = Files.readAllBytes(file.toPath());
				return this.defineClass(name, b, 0, b.length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		else {
			//error?
			System.out.println("INSET ERRORS");
		}
		return null;
	}

	
	public Queue<File> loopUserDirectories(String dirFromUser) {
		File dir = new File(System.getProperty("user.dir") + dirFromUser + "\\");
		File[] directoryListing = dir.listFiles();
		Queue<File> q = new LinkedList<>();
		for (File child : directoryListing) {
			if (child.getName().endsWith(".class")) {
				q.add(child);
			}
		}
		
		return q;
	}


	private String strToByteToImp(String fN) {
		int gen = fN.indexOf("<");
		if (gen == -1) {
			return fN.substring(1, fN.length()-1).replace("/", ".");
		}
		else {
			return fN.substring(1, gen).replace("/", ".");
		}
	}
}
