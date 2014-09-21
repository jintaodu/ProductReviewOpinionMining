import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReviewFormatTransferYu {// Yujianxing数据集预处理成XML

	public void WriteFile(File f, String line) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
		bw.write(line);
		bw.flush();
		bw.close();
	}


	public void FormatTransfer(String fpath, String fname) throws IOException {// 读原始review文件

		File f = new File(fpath);

/*		File writefreereview = new File(".\\XMLReviewsYu\\" + fname);
		if (writefreereview.exists())
			writefreereview.delete();
		writefreereview.createNewFile();*/
		
		File writeproandcon = new File(".\\XMLReviewsYu\\ProAndCon\\" + fname);
		if (writeproandcon.exists())
			writeproandcon.delete();
		writeproandcon.createNewFile();
		

		InputStreamReader isr = new InputStreamReader(
				new FileInputStream(fpath), "UTF-8");
		BufferedReader br = new BufferedReader(isr);

		String line = null;

		String freereviewcontent = null;
		String proandconreviewcontent = null;
		int reviewid = 1;
		int sentencenum = 0;
		int proandconnum = 0;
		while ((line = br.readLine()) != null) {//每行是一个review document
			freereviewcontent = new String();
			proandconreviewcontent = new String();
			if (line.length() >= 1) {

				String[] reviewparts = line.split("::::");
				freereviewcontent += "<review><reviewid>" + (reviewid++)
						+ "</reviewid>\r\n";
				freereviewcontent += "<title>" + reviewparts[2].trim() + "</title>\r\n";
				freereviewcontent += "<rating>" + reviewparts[5].trim() + "</rating>\r\n";

				proandconreviewcontent += freereviewcontent;
				
				String pro_aspects = new String();
				String pro_subjectivity = new String();
				
				/*
				 * 把pro和con的review content写到文件里，和free review content分开存放
				 */
				if (reviewparts[6].trim().length() >= 1	&& !reviewparts[6].trim().equals("Reviewer left no comment")) {

					String [] prosentences = reviewparts[6].split("\\. |! |\\? ");
					sentencenum += prosentences.length;// 把pro看做一句话是不对的，需要分句
					proandconnum += prosentences.length;
					if (reviewparts[9].trim().length() >= 1)
						for (String str : reviewparts[9].split(";"))// pro part aspect and sentiment subjectivity
						{
							String[] aspect_subjectivity = str.split(":");
							pro_aspects += aspect_subjectivity[0].trim() + ",";
							pro_subjectivity += "["
									+ aspect_subjectivity[1].trim() + "] ";
						}
                    proandconreviewcontent += "<sentence>\r\n";
					for(String s:prosentences)
					   proandconreviewcontent += "<sentcont>" + s + "</sentcont>\r\n";
					proandconreviewcontent += "<aspects>" + pro_aspects
							+ "</aspects><subjectivity>" + pro_subjectivity
							+ "</subjectivity></sentence>\r\n";
				}

				if (reviewparts[7].trim().length() >= 1 && !reviewparts[6].trim().toLowerCase().equals("reviewer left no comment")) {

					String [] consentences = reviewparts[7].split("\\. |! |\\? ");
					sentencenum += consentences.length;// 把con看做一句话是不对的，需要分句
					proandconnum += consentences.length;
					String con_aspects = new String();
					String con_subjectivity = new String();
					if (reviewparts[10].trim().length() >= 1)
						for (String str : reviewparts[10].split(";"))// con part aspect and sentiment subjectivity
						{
							String[] aspect_subjectivity = str.split(":");
							con_aspects += aspect_subjectivity[0].trim() + ",";
							con_subjectivity += "["
									+ aspect_subjectivity[1].trim() + "] ";
						}
					proandconreviewcontent += "<sentence>\r\n";
					for(String s:consentences)
					    proandconreviewcontent += "<sentcont>" + s + "</sentcont>\r\n";
					proandconreviewcontent += "<aspects>" + con_aspects
							+ "</aspects><subjectivity>" + con_subjectivity
							+ "</subjectivity></sentence>\r\n";
				}

				WriteFile(writeproandcon, proandconreviewcontent + "</review>\r\n");
				
				/*
				 * 把free review content写到文件里，和pro con review content分开存放
				 */	
				
				if (reviewparts[11].length() > 2) {

					String summaryreviews = null;
					if (-1 == reviewparts[11].indexOf("<-->"))
						summaryreviews = reviewparts[11];
					else
						summaryreviews = reviewparts[11]
								.substring(reviewparts[11].indexOf("<-->") + 5);
					if (summaryreviews.contains("/:/:")) {
						String[] summarysentence = summaryreviews.split("/:/:");

						for (String s : summarysentence) {
							String aspect = new String();
							String subjectivity = new String();
							sentencenum++;// review sentence num++
							//System.out.println("summarysentence=" + s);
							String[] sentence_aspects = s.split(">:>:");
							if (sentence_aspects[0].trim().length() >= 1) {
								freereviewcontent += "<sentence><sentcont>"
										+ sentence_aspects[0].trim() + "</sentcont>";
								if (sentence_aspects[1].trim().length() >= 1)
									for (String a : sentence_aspects[1]
											.split(";")) {
										if(a.trim().length()>=1)
										{
											String[] aspect_subjectivity = a
											.split(":");
									aspect += aspect_subjectivity[0].trim()
											+ ",";
									subjectivity += "["
											+ aspect_subjectivity[1].trim()
											+ "] ";
										}
									}
								freereviewcontent += "<aspects>" + aspect.trim()
										+ "</aspects><subjectivity>"
										+ subjectivity.trim()
										+ "</subjectivity></sentence>\r\n";
							}
						}
					}// end of if(contain /:/:)

				}
			}//end of if (line.length() >= 1)

			//WriteFile(writefreereview,  freereviewcontent +proandconreviewcontent + "</review>\r\n");
		}//end of while
        br.close();
		System.out.println(fname + " review size is = " + (reviewid - 1)
				+ " allsentencenum = " + sentencenum+" ProAndCon sentencenum = "+proandconnum);
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub

		ReviewFormatTransferYu rft = new ReviewFormatTransferYu();

		String corpushomedir = ".\\Product_ReviewsYu";
		String[] list1 = new File(corpushomedir).list();

		for (String dir1 : list1) {//遍历productreview目录下的所有review文件
			
			if ((new File(corpushomedir+"\\"+dir1)).isDirectory()) {
				String[] list2 = new File(corpushomedir + "\\" + dir1).list();
				for(String productname: list2)
				{

					System.out.println("Current productname = "+ productname);
					Thread.sleep(5000);
					rft.FormatTransfer(corpushomedir + "\\"+dir1+"\\" + productname
							+ "\\corpus.txt", productname + ".txt");
				}
			}
		}

		String line = "picture quality[+2][p]123654[+2]picture [cc]quality[+2][p]book[-6][-3]";
		Pattern p = Pattern.compile("\\[(\\+|-)\\d\\]");
		Matcher m = p.matcher(line);
		while (m.find()) {
			System.out.println(m.group());
		}

	}

}
