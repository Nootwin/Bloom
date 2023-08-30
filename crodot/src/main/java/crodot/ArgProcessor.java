package crodot;

import java.util.LinkedList;
import java.util.Queue;

public class ArgProcessor {
	ArgsList<String> stackPreview;
	Queue<Object[]> opCodes;
	
	ArgProcessor() {
		stackPreview = new ArgsList<>();
		opCodes = new LinkedList<>();
		
	}
	
	
}
