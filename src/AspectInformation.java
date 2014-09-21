import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class AspectInformation {

	/**
	 * @param args
	 */
	private String ReadFile(String fpath) throws IOException {
		File f = new File(fpath);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		StringBuffer content = new StringBuffer();
		while ((line = br.readLine()) != null) {
			content.append(line);
		}
		br.close();
		return content.toString();
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		AspectInformation AL = new AspectInformation();
		File f = new File(".\\XMLReviewsYu");
		String[] fname = f.list();
		HashSet<String> aspects = new HashSet<String>();
		for(String file: fname)
		{
			aspects.clear();
			String content = AL.ReadFile(".\\XMLReviewsYu\\"+file);		
			Document doc = Jsoup.parse(content);
			Elements es = doc.getElementsByTag("aspects");

			for (Element e : es) {//该循环得到了manual aspect的集合,存储在manual_Aspects_Frequence中

				String[] M_aspects = e.text().split(",");
				//System.out.println(e.text());
                for(String s : M_aspects)
                	if(s.trim().length()>=1) aspects.add(s.trim());


	        }// end of for sentence
			System.out.println("filename = " + file+" aspectnum = "+aspects.size());
		}
		
	}

}
