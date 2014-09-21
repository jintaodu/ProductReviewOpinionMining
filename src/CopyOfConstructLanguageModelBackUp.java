import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CopyOfConstructLanguageModelBackUp {

	/**
	 * @param args
	 */
	private static HashSet<String> Stopwords = new HashSet<String>();// 普通常用的停止词
	private static HashMap<String, Integer> CollectionCount = new HashMap<String, Integer>();// 记录整个review的word及其词频count
	private static HashMap<String, Integer> DomainCount = new HashMap<String, Integer>();// 记录每个产品review的词频
	private static int CollectionAllCount = 0;// CollectionCount中的词频之和
	private static String[] filename = null;
	
	private static void ReadAspectStopwordFile(String fpath) throws IOException {
		File f = new File(fpath);
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {
			Stopwords.add(line);
		}
	}

	public CopyOfConstructLanguageModelBackUp(String reviewpath)
	{
		File f = new File(reviewpath);
		filename = f.list();
	}
	private boolean isnounphrase(String word) {
		if (word.contains("_nn") || word.contains("_nnp")
				|| word.contains("_nnps") || word.contains("_nns"))
			return true;
		return false;
	}
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

	private void WriteFile(File f, HashMap<String, Integer> corpuscount)
			throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(f, true));// 追加方式写入
		for (String key : corpuscount.keySet()) {
			br.write(key + ":" + corpuscount.get(key) + "\r\n");
			br.flush();
		}

		br.close();
	}

	private void insertdomaincount(String word)
	{
		String word_lower = word.toLowerCase();
		if (DomainCount.containsKey(word_lower))
			DomainCount.put(word_lower,
					DomainCount.get(word_lower) + 1);
		else
			DomainCount.put(word_lower, 1);
	}
	private void insertcollectioncount(String word)
	{
		String word_lower = word.toLowerCase();
		if (CollectionCount.containsKey(word_lower))
			CollectionCount.put(word_lower,
					CollectionCount.get(word_lower) + 1);
		else
			CollectionCount.put(word_lower, 1);
	}
	public void constructLanguageModelfromreview(String path) throws IOException {
		String xmlcorpusdir = path;
		
		for (int i = 0; i < filename.length; ++i)// 对XML格式的review循环处理
		{
			File f = new File("TaggedResults\\"+filename[i]);
			if(!(new File(xmlcorpusdir+"\\"+filename[i]).isDirectory()))
			{
				DomainCount.clear();
				System.out.println("Current File = " + filename[i]);
				
				/*
				 * 从taggedresults中抽取名词短语
				 */
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line = null;
				while((line = br.readLine()) != null)
				{
					String[] words = line.trim().toLowerCase().split(" ");
					String nouns = "", adjs = "";
					for (String word : words) {// 查找所有的名词和名词短语
						if (word.trim().length() != 0) {
							if (isnounphrase(word))
								nouns += word.substring(0, word.lastIndexOf('_')) + " ";
							else {
								if (nouns.length() > 0 && nouns.trim().indexOf(' ')>0)
								{
									insertdomaincount(nouns.trim());
									insertcollectioncount(nouns.trim());
								}								
								nouns = "";
							}
						}// end of if
					}
					if (nouns.trim().length() > 0 && nouns.trim().indexOf(' ')>0)// 处理最后一个词为名词的情况
					{
						insertdomaincount(nouns.trim());
						insertcollectioncount(nouns.trim());
					}
				}
						
				
				/*
				 * 从xml中抽取单个的词
				 */
				String content = ReadFile(xmlcorpusdir + "\\" + filename[i]);
				if(new File(xmlcorpusdir + "\\ProAndCon\\" + filename[i]).exists())
				   content += ReadFile(xmlcorpusdir + "\\ProAndCon\\" + filename[i]);//把Pro和Con的内容读入
				Document doc = Jsoup.parse(content);
				Elements es = doc.getElementsByTag("sentcont");
				System.out.println(filename[i] + " size=" + es.size());

				for (Element e : es)// 对collection进行词频统计
				{
					if (e.text().trim().length() >= 1) {
						
						String words[] = e.text().split("[^a-zA-Z0-9|$|\\-|']");
						// System.out.println(Arrays.toString(words));
						for (String word : words) {
							String word_lower = word.trim().toLowerCase();
							if (!Stopwords.contains(word_lower)
									&& word_lower.length() >= 1) {
								insertdomaincount(word_lower);
								insertcollectioncount(word_lower);
							}
						}
					}// end of if
				}//end of for(Element e:es)
				File lmmodel = new File(".\\LanguageModel\\" + filename[i]);
				if (lmmodel.exists())
					lmmodel.delete();
				lmmodel.createNewFile();
				WriteFile(lmmodel, DomainCount);
			}

		}//end of for(int i=0
		
		File lmmodel = new File(".\\LanguageModel\\" + "collection.txt");
		if (lmmodel.exists())
			lmmodel.delete();
		lmmodel.createNewFile();
		WriteFile(lmmodel, CollectionCount);
	}
	public void constructLanguageModelfromQA(String path) throws IOException {
		String xmlcorpusdir = path;
		File f = new File(xmlcorpusdir);
		// String[] filename = f.list();

		for (int i = 0; i < filename.length; ++i)// 对XML格式的QA循环处理
		{
			DomainCount.clear();
			System.out.println("Current File = " + filename[i]);
			String content = ReadFile(xmlcorpusdir + "\\" + filename[i]);
			Document doc = Jsoup.parse(content);
			Elements es_subject = doc.getElementsByTag("subject");
			System.out.println(filename[i] + " size=" + es_subject.size());
			
			Elements es_content = doc.getElementsByTag("content");
			System.out.println(filename[i] + " size=" + es_subject.size());
			
			Elements es_chosenanswer = doc.getElementsByTag("chosenanswer");			
			System.out.println(filename[i] + " size=" + es_subject.size());

			for (Element e : es_subject)// 对collection进行进行词频统计
			{
				if (e.text().trim().length() >= 1) {
					String words[] = e.text().split("[^a-zA-Z0-9|$|\\-|']");
					// System.out.println(Arrays.toString(words));
					for (String word : words) {
						String word_lower = word.trim().toLowerCase();
						if (!Stopwords.contains(word_lower)
								&& word_lower.length() >= 1) {
							if (CollectionCount.containsKey(word_lower))
								CollectionCount.put(word_lower,
										CollectionCount.get(word_lower) + 1);
							else
								CollectionCount.put(word_lower, 1);

							if (DomainCount.containsKey(word_lower))
								DomainCount.put(word_lower,
										DomainCount.get(word_lower) + 1);
							else
								DomainCount.put(word_lower, 1);
						}
					}
				}// end of if
			}//end of for(Element e:es)
			
			for (Element e : es_content)// 对collection进行进行词频统计
			{
				if (e.text().trim().length() >= 1) {
					String words[] = e.text().split("[^a-zA-Z0-9|$|-|']");
					// System.out.println(Arrays.toString(words));
					for (String word : words) {
						String word_lower = word.trim().toLowerCase();
						if (!Stopwords.contains(word_lower)
								&& word_lower.length() >= 1) {
							if (CollectionCount.containsKey(word_lower))
								CollectionCount.put(word_lower,
										CollectionCount.get(word_lower) + 1);
							else
								CollectionCount.put(word_lower, 1);

							if (DomainCount.containsKey(word_lower))
								DomainCount.put(word_lower,
										DomainCount.get(word_lower) + 1);
							else
								DomainCount.put(word_lower, 1);
						}
					}
				}// end of if
			}//end of for(Element e:es)
			
			for (Element e : es_chosenanswer)// 对collection进行进行词频统计
			{
				if (e.text().trim().length() >= 1) {
					String words[] = e.text().split("[^a-zA-Z0-9|$|-|']");
					// System.out.println(Arrays.toString(words));
					for (String word : words) {
						String word_lower = word.trim().toLowerCase();
						if (!Stopwords.contains(word_lower)
								&& word_lower.length() >= 1) {
							if (CollectionCount.containsKey(word_lower))
								CollectionCount.put(word_lower,
										CollectionCount.get(word_lower) + 1);
							else
								CollectionCount.put(word_lower, 1);

							if (DomainCount.containsKey(word_lower))
								DomainCount.put(word_lower,
										DomainCount.get(word_lower) + 1);
							else
								DomainCount.put(word_lower, 1);
						}
					}
				}// end of if
			}//end of for(Element e:es)
			
			File lmmodel = new File(".\\LanguageModel\\" + filename[i]);
			if (lmmodel.exists())
				lmmodel.delete();
			lmmodel.createNewFile();
			WriteFile(lmmodel, DomainCount);
		}//end of for(int i=0
		
		File lmmodel = new File(".\\LanguageModel\\" + "collection.txt");
		if (lmmodel.exists())
			lmmodel.delete();
		lmmodel.createNewFile();
		WriteFile(lmmodel, CollectionCount);
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		String reviewpath = ".\\XMLReviewsYu";
		CopyOfConstructLanguageModelBackUp ConLM = new CopyOfConstructLanguageModelBackUp(reviewpath);
		//ConLM.ReadAspectStopwordFile(".\\Stopwords");//读取停止词
		ConLM.constructLanguageModelfromreview(reviewpath);
		//ConLM.constructLanguageModelfromQA("C:/JAVA WorkSpace/eclipse 3.4.2 32bits/Yahoo Answers/QAGetbyQuestionSearch/Savedinonefile/Filtered");
	}

}
