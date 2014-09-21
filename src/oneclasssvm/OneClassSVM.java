package oneclasssvm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class OneClassSVM {

	/**
	 * @param args
	 */
	
	private static String[] manual_aspects;
	private static HashSet<String> Aspects_Manual = new HashSet<String>();// 手工标注的的aspects
	
	public String ReadFile(String fpath) throws IOException {
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
	
	public void initializemanualaspects() throws IOException
	{
		File f = new File(".\\manual_Aspects_Frequence.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		HashSet<String> manualaspects = new HashSet<String>();
		while((line=br.readLine())!=null)
		{
			String[] words = line.split(":");
			manualaspects.add(words[0]);
		}
		
		br.close();
		int i=0;manual_aspects = new String[manualaspects.size()];
		for(String s:manualaspects)
		{
			manual_aspects[i++] = s;
		}
		
	}
	public void WriteTrainFile() throws IOException
	{
		File f = new File(".\\data_train.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		
	}
	public static void main(String[] args)throws IOException {
		// TODO Auto-generated method stub
 
		/*OneClassSVM onesvm = new OneClassSVM();
		String content = onesvm.ReadFile(".\\XMLReviews\\Apex AD2600 Progressive-scan DVD player.txt");
		Document doc = Jsoup.parse(content);
		Elements es = doc.getElementsByTag("sentence");

		int precision_nume = 0, precision_deno = 0;
		int recall_nume = 0, recall_deno = 0;

		ShallowParser swParser = ShallowParser.getInstance();
		for (Element e : es) {

			Document doc2 = Jsoup.parse(e.toString());
			Elements es2 = doc2.getElementsByTag("sentcont");

			Elements es3 = doc2.getElementsByTag("aspects");
			String[] M_aspects = es3.text().split(",");
			if(M_aspects[0].length()>=1)
			for(String s:M_aspects)
			{
				Aspects_Manual.add(s);
			}
		}
		
		String[] trainArgs = {"UCI-breast-cancer-tra"};//directory of training file
		String modelFile = svm_train.main(trainArgs);
		String[] testArgs = {"UCI-breast-cancer-test", modelFile, "UCI-breast-cancer-result"};//directory of test file, model file, result file
		Double accuracy = svm_predict.main(testArgs);
		System.out.println("SVM Classification is done! The accuracy is " + accuracy);*/
		String[] trainArgs = {".\\svmdata\\data_train.txt",".\\svmdata\\model_r.txt","-s 2"};
		String[] testArgs ={".\\svmdata\\data_test.txt",".\\svmdata\\model_r.txt",".\\svmdata\\testtesult.txt"};
		System.out.println("....SVM运行开始....");
		svm_train.main(trainArgs);
		svm_predict.main(testArgs);
		
		
	}

}
