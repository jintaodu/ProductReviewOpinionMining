package OpinionRetrieval;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashSet;

public class MergeResults {

	/**
	 * @param args
	 */
	
	public static void main(String[] args)throws IOException {
		// TODO Auto-generated method stub
		File f = new File("TopORResult");
		String[] files = f.list();
		HashSet<String> newResult = new HashSet<String>();
		
		/*for(String filename : files){
			BufferedReader br = new BufferedReader
		}
		URLEncoder.encode(annotationFile,"utf-8")+".txt"*/
	}

}
