import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReviewFormatTransferLiuBing {

	/**
	 * @param args
	 * @throws IOException
	 */
	public void WriteFile(File f, String line) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(f,true));
		bw.write(line);
		bw.flush();
		bw.close();
	}

	public void ReadFile(String fpath,String fname) throws IOException {
		File f = new File(fpath);
		
		File writefile = new File(".\\XMLReviews\\"+fname);
		if(writefile.exists()) writefile.delete();
		writefile.createNewFile();
		
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;

		String reviewcontent = new String();
		int reviewid = 1;
		int sentencenum=0;
		while ((line = br.readLine()) != null) {
			if (line.length() >= 1) {
				if ((line.charAt(0)) == '*')
					continue;
				if (line.contains("[t]")) {
					if (reviewid != 1)
						reviewcontent += "</review>";
					WriteFile(writefile,reviewcontent);
					//System.out.println(reviewcontent);
					reviewcontent = new String();
					String title = line.substring(line.indexOf("[t]") + 3,
							line.length());

					reviewcontent += "<review>" + "<reviewid>" + (reviewid++)
							+ "</reviewid>\r\n" + "<title>" + title
							+ "</title>\r\n";
				} else if (line.contains("##")) {
					sentencenum++;//¾ä×ÓÊý++
					String sentence = line.substring(line.indexOf("##") + 2,
							line.length());
					reviewcontent += "<sentence><sentcont>" + sentence + "</sentcont><aspects>";
					if (line.indexOf("##") > 0) {
						String aspects = line.substring(0, line.indexOf("##"));
						String[] aspectwords = aspects
								.split("\\[([a-z]+|((\\+|-)\\d))\\]");
						for (String s : aspectwords)
							if (s.length() >= 1) {
								reviewcontent += s;
								//reviewcontent += ",";
							}
						reviewcontent +="</aspects><subjectivity>";
						Pattern p = Pattern.compile("\\[(\\+|-)\\d\\]");
						Matcher m = p.matcher(aspects);
						while(m.find())
						{
							reviewcontent += m.group();
							reviewcontent +=" ";
						}
					}//end of if ##
					else {
						reviewcontent +="</aspects><subjectivity>";
					}
					reviewcontent += "</subjectivity></sentence>\r\n";

				}//end of if ##
			 
			}
			
		}//end of while(readline())
		
		System.out.println(fname+" review size is = "+(reviewid-1)+" sentencenum = "+sentencenum);

		WriteFile(writefile,reviewcontent+"</review>");
		br.close();

	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String fname = ".\\customer review data";
		String[] list = new File(fname).list();
		ReviewFormatTransferLiuBing rft = new ReviewFormatTransferLiuBing();
		for (String f : list) {
			rft.ReadFile(fname + "\\" + f,f);
		}

		
		 String line = "picture quality[+2][p]123654[+2]picture [cc]quality[+2][p]book[-6][-3]";
		 Pattern p = Pattern.compile("\\[(\\+|-)\\d\\]");
		 Matcher m = p.matcher(line);
		 while(m.find())
		 {
			 System.out.println(m.group());
		 }
		
		 

	}

}
