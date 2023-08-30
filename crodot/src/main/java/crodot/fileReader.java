package crodot;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class fileReader {

    static String ReadFileToString(String fileName) {

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
                		case ';', '{', '}':
                			fileContent += line;
                			break;
                		default:
                			fileContent += line + ";";
                			break;
                		}
                	}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileContent += " ";
        return fileContent;
    }
}