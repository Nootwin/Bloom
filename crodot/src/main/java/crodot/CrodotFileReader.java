package crodot;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;


public class CrodotFileReader {
	private String fileName;
	private Queue<Integer> lineNum;
	private LinkedList<String> fileContents;
	
	CrodotFileReader(String fileName) {
		this.fileName = fileName;
		this.lineNum = new LinkedList<>();
		this.fileContents = new LinkedList<>();
	}

    String ReadFileToString() {

        String fileContent = "";

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            String line2;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("[^\\p{Graph}\n\r\t ]", ""); // remove all invisible characters from line
                if (!line.isBlank()) {
                	if (line.length() > 0)	 {
                		line2 = line.replaceAll("\\s+", "");
                		switch(line2.charAt(line2.length()-1)) {
                		case ';', '{':
                			fileContent += line;
                			break;
                		default:
                			fileContent += line + ";";
                			break;
                		}
                		
                	}
                	
                }
                fileContents.add(line);
        		lineNum.add(fileContent.length());
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileContent += " ";
        return fileContent;
    }
    
    Queue<Integer> getLineNumbers() {
    	return lineNum;
    }
    
    LinkedList<String> getContentList() {
    	return fileContents;
    }
}