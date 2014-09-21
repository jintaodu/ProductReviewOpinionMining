import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import WordAlignmentModel.Pair;

public class AspectIdentify {

	private static ShallowParser swParser;
	private static ArrayList<String> SentenceCont = new ArrayList<String>();// 记录含有NP的句子,不是transaction

	private static HashMap<String, Integer> Aspects_Cand_Apriori = new HashMap<String, Integer>();// 经过apriori算法后得到的aspects候选集
	private static HashMap<String, Integer> Aspects_Cand_Compact = new HashMap<String, Integer>();// 经过compactness
																									// prune筛选后的aspects

	private static HashMap<String, Integer> Aspects_Cand_pSup = new HashMap<String, Integer>();// 经过p-support筛选后的aspects
	private static HashMap<String, Integer> Aspects_Selected = new HashMap<String, Integer>();// 经过apriori,compactness,p-support筛选后的aspects
	private static HashSet<String> Aspects_Manual = new HashSet<String>();// 手工标注的的aspects
	private static HashMap<String, Integer> manual_Aspects_Frequence = new HashMap<String, Integer>();

	private static HashSet<String> Stopwords = new HashSet<String>();// 普通常用的停止词
	private static HashMap<String, Integer> CollectionCount = new HashMap<String, Integer>();// 记录整个review的word及其词频count
	private static HashMap<String, Integer> DomainCount = new HashMap<String, Integer>();// 记录每个产品review的词频
	private static HashMap<String, Double> AspectsLM = new HashMap<String, Double>();// 通过language model得到的aspect及其相似度
	private static HashMap<String,Double> NgramTFIDF = new HashMap<String,Double>();//通过google ngram得到的tfidf
	private static int CollectionAllCount = 0;// CollectionCount中的词频之和
	private static int DomainAllCount = 0;// CollectionCount中的词频之和
	private static HashMap<Pair, Double> T = new HashMap<Pair, Double>();// TranslationModel中的T表
    private static HashMap<Pair,Integer> alignfrequency = new HashMap<Pair,Integer>();
    
	private void ReadAspectStopwordFile(String fpath) throws IOException {
		File f = new File(fpath);
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {
			Stopwords.add(line);
		}
	}

	private boolean sentence_sup(String sentence, String itemset)// 句子中是否包含某个itemset
	{
		String[] words = sentence.split(" ");
		HashSet<String> sentwords = new HashSet<String>();
		for (String s : words) {
			sentwords.add(s);
		}
		String[] items = itemset.split(" ");
		for (int j = 0; j < items.length; j++) {
			if (!sentwords.contains(items[j]))
				return false;
		}
		return true;
	}

	private boolean containsingleword(String single, String aspect) {
		String[] words = aspect.split(" ");
		for (int i = 0; i < words.length; ++i)
			if (words[i].equals(single))
				return true;
		return false;
	}

	private void p_support() {
		for (String s : Aspects_Cand_Compact.keySet()) {
			if ((s.split(" ")).length >= 2) {
				int occurstime = 0;
				for (String sent : SentenceCont) {
					if (sentence_sup(sent, s))
						occurstime++;// 判断itemset在句子中是否出现
				}
				Aspects_Cand_Compact.put(s, occurstime);
			}
		}

		for (String s : Aspects_Cand_Compact.keySet()) {
			int s_psupport = Aspects_Cand_Compact.get(s);
			if (!s.trim().contains(" ")) {// singleword的aspect不包含空格
				for (String ss : Aspects_Cand_Compact.keySet()) {
					if (!ss.equals(s) && ss.trim().contains(" ")
							&& containsingleword(s, ss))
						s_psupport -= Aspects_Cand_Compact.get(ss);
				}// end of for
			} else {
				Aspects_Cand_pSup.put(s, s_psupport);
			}// end of if
			if (s_psupport >= 3)
				Aspects_Cand_pSup.put(s, s_psupport);
		}
		Aspects_Selected.clear();
		System.out
				.println("Aspects_Cand_pSup size=" + Aspects_Cand_pSup.size());
		Aspects_Selected = Aspects_Cand_pSup;
		System.out.println("Aspects_Selected size=" + Aspects_Selected.size());

	}

	private int indexofsentence(String sentence, String item) {
		String[] sentencewords = sentence.split(" ");
		for (int i = 0; i < sentencewords.length; ++i)
			if (sentencewords[i].equals(item))
				return i;
		return -1;
	}

	private boolean distanceless3(String sentence, String aspect) {//判断词与词之间的距离是否小于3
		String[] items = aspect.split(" ");

		for (int i = 0; i < items.length; ++i)
			for (int j = 0; j < items.length; ++j) {
				int indexi = indexofsentence(sentence, items[i]);
				int indexj = indexofsentence(sentence, items[j]);
				if (-1 == indexi || -1 == indexj
						|| Math.abs(indexi - indexj) > 3) {
					return false;
				}
			}
		return true;
	}

	public void compactphrase() throws InterruptedException {

		System.out.println("Aspects_Cand_Apriori size="
				+ Aspects_Cand_Apriori.size());
		for (String aspect : Aspects_Cand_Apriori.keySet()) {
			int occurstime = 0;
			for (String sentence : SentenceCont) {
				if (sentence.contains(aspect)) {
					occurstime++;// 作为sentence的子串出现时
				} else if (distanceless3(sentence, aspect))
					occurstime++;
			}

			if (occurstime >= 3) {// 至少在两句里出现

				Aspects_Cand_Compact.put(aspect,
						Aspects_Cand_Apriori.get(aspect));
			}
		}
		Aspects_Selected.clear();
		System.out.println("Aspects_Cand_Compact size="
				+ Aspects_Cand_Compact.size());
		Aspects_Selected = Aspects_Cand_Compact;
		System.out.println("Aspects_Selected size=" + Aspects_Selected.size());
	}

	private void WriteFile(File f, String content) throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(f, true));// 追加方式写入
		br.write(content);
		br.flush();
		br.close();
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

	public void printresult_Integer(HashMap<String, Integer> result) {// 将hashmap中的元素排序后打印输出
		ArrayList<Entry<String, Integer>> array = new ArrayList<Entry<String, Integer>>(
				result.entrySet());

		Collections.sort(array, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
					Map.Entry<String, Integer> o2) {
				if (o2.getValue() - o1.getValue() > 0)
					return 1;
				else if (o2.getValue() - o1.getValue() == 0)
					return 0;
				else
					return -1;
			}
		});

		for (Entry<String, Integer> e : array) {
			System.out.println(e.getKey() + ":" + e.getValue());
		}
	}

	public void printresult_Double(HashMap<String, Double> result) {// 将hashmap中的元素排序后打印输出
		ArrayList<Entry<String, Double>> array = new ArrayList<Entry<String, Double>>(
				result.entrySet());

		Collections.sort(array, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1,
					Map.Entry<String, Double> o2) {
				if (o2.getValue() - o1.getValue() > 0)
					return 1;
				else if (o2.getValue() - o1.getValue() == 0)
					return 0;
				else
					return -1;
			}
		});

		for (Entry<String, Double> e : array) {
			System.out.println(e.getKey() + ":" + e.getValue());
		}
	}
	public void Savesortedresults(HashMap<String, Double> result,BufferedWriter bw) throws IOException {// 将hashmap中的元素排序后打印输出
		ArrayList<Entry<String, Double>> array = new ArrayList<Entry<String, Double>>(
				result.entrySet());

		Collections.sort(array, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1,
					Map.Entry<String, Double> o2) {
				if (o2.getValue() - o1.getValue() > 0)
					return 1;
				else if (o2.getValue() - o1.getValue() == 0)
					return 0;
				else
					return -1;
			}
		});

		for (Entry<String, Double> e : array) {
			bw.write(e.getKey() + ":" + e.getValue()+"\r\n");
			bw.flush();
		}
	}
	public double LanguageModel(String phrase)// 通过language
												// model获得phrase与当前领域review的相似度
	{
		double similarity = 0.0;
		boolean overlap = false;
		String[] phrasewords = phrase.split(" ");
		double lamda = 0.2;
		double p = 0.0;
		for (String s : phrasewords) {
			if (DomainCount.containsKey(s)) {
				p = Math.log10(((1 - lamda) * (double) DomainCount.get(s)
						/ DomainAllCount + lamda
						* (double) CollectionCount.get(s) / CollectionAllCount));

			} else if (DomainCount.containsKey(s)) {
				p = Math.log10(lamda * (double) DomainCount.get(s)
						/ DomainAllCount);
			}
			if (p != 0.0)
				overlap = true;
			similarity += p;
		}
		if (similarity == 0.0)
			return -100.0;
		return similarity;
	}

	public double TR_LanguageModel(String phrase)// 通过Transaltionbasedlanguage
													// model获得phrase与当前领域review的相似度
	{
		double similarity = 0.0;
		boolean overlap = false;
		String[] phrasewords = new String[]{phrase};//phrase.split(" ");
		double lamda = 0.01;
		double beita = 0.2;
		for (String s : phrasewords) {
			//System.out.println(" s = "+s);
			double sum = 0.0;
			double Pmltd = 0.0;
			double p = 0.0, PmltColl = 0.0;
			if (DomainCount.containsKey(s+"_nn"))
				Pmltd = (double) DomainCount.get(s+"_nn") / DomainAllCount;
			for (String w : DomainCount.keySet()) {
				if (w.equals(s))
					sum += (double) DomainCount.get(s+"_nn") / DomainAllCount;
				else if (T.containsKey(new Pair(s, w)))
					sum += T.get(new Pair(s, w))
							* (double) DomainCount.get(w) / DomainAllCount;
			}// end of for(DomainCount.keySet())
			if (CollectionCount.containsKey(s+"_nn"))
				PmltColl = (double) CollectionCount.get(s+"_nn") / CollectionAllCount;
			p = Math.log10(((1 - lamda) * (beita * sum + (1 - beita) * Pmltd) + lamda
					* PmltColl));
           if(s.equals("gusto")) System.err.println("pmltd = "+Pmltd+" PmltColl="+PmltColl+"  "+CollectionCount.get(s+"_nn")+"  "+" sum="+sum+" p = "+p);
			if (p != 0.0)
				overlap = true;
			similarity += p;
		}// end of for1
		if (similarity == 0.0)
			return -100.0;
		return similarity;
	}

	public double TR_LanguageModelwithTFIDF(String phrase)// 通过Transaltionbasedlanguage
	// model获得phrase与当前领域review的相似度
     {
		double similarity = 0.0;
		boolean overlap = false;
		String[] phrasewords = phrase.split(" ");//new String[]{phrase};
		double lamda = 0.2;
		double beita = 0.2;
		double delta = 0.4;
		for (String s : phrasewords) {
			System.out.println("phrasewords length = "+phrasewords.length);
			double sum = 0.0;
			double Pmltd = 0.0;
			double p = 0.0, PmltColl = 0.0;
			if (DomainCount.containsKey(s))
			    Pmltd = (double) DomainCount.get(s) / DomainAllCount;
			for (String w : DomainCount.keySet()) {
				if (w.equals(s))
				sum += (double) DomainCount.get(s) / DomainAllCount;
				else if (T.containsKey(new Pair(s, w)))
				sum += T.get(new Pair(s, w))
				/** (double) DomainCount.get(w) / DomainAllCount*/;
			}// end of for(DomainCount.keySet())
			if (CollectionCount.containsKey(s))
			   PmltColl = (double) CollectionCount.get(s) / CollectionAllCount;
			double ngramtfidf = 0.0;
			if(NgramTFIDF.containsKey(s))  ngramtfidf = NgramTFIDF.get(s);
			   p = Math.log10((1-delta)*((1 - lamda) * (beita * sum + (1 - beita) * Pmltd) + lamda * PmltColl)+delta*ngramtfidf);
			
			if (p != 0.0)
			   overlap = true;
			similarity += p;
		}// end of for1
		if (similarity == 0.0)
		   return -100.0;
		return similarity;
      }
	
	public void initializeLanguageModel(String productname) throws IOException {// 初始化language
																				// model，就是读取两个词频文件
		File f = new File(".\\LanguageModel\\" + productname);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		while ((line = br.readLine()) != null) {
			String word = line.substring(0, line.lastIndexOf(':'));
			String count = line.substring(line.lastIndexOf(':')+1);
			DomainCount.put(word, Integer.parseInt(count));
			DomainAllCount += Integer.parseInt(count);
		}
		br.close();

		f = new File(".\\LanguageModel\\collection.txt");
		br = new BufferedReader(new FileReader(f));
		line = null;
		while ((line = br.readLine()) != null) {
			String word = line.substring(0, line.lastIndexOf(':'));
			String count = line.substring(line.lastIndexOf(':')+1);
			CollectionCount.put(word, Integer.parseInt(count));
			CollectionAllCount += Integer.parseInt(count);
		}
		br.close();
	}

	public void initializeTFIDF(String productname) throws NumberFormatException, IOException
	{
		File tfidfcomputed = new File(".\\GoogleNgramTFIDF\\tfidfcomputed.txt");
		BufferedReader br = new BufferedReader(new FileReader(tfidfcomputed));
		HashMap<String,Double> tmp = new HashMap<String,Double>();
		String line = null;
		while((line = br.readLine())!=null)
		{
			String[] info = line.split("::::");
			tmp.put(info[0], Double.parseDouble(info[1]));
		}
		
		File aspectpos = new File(".\\AspectsPos\\"+productname);
		BufferedReader aspectposbr = new BufferedReader(new FileReader(aspectpos));
		double sum = 0.0;
		while((line = aspectposbr.readLine())!=null)
		{
			String[] info = line.split("<---->");
			sum += tmp.get(info[0]);
			NgramTFIDF.put(info[0], tmp.get(info[0]));
		}
		for(String noun:NgramTFIDF.keySet())
		{
			System.out.println("noun = "+noun+" ngram normalized tfidf = "+NgramTFIDF.get(noun)/sum);
			NgramTFIDF.put(noun, NgramTFIDF.get(noun)/sum);//归一化处理
		}
		
	}
	public void GetSentenceCont(String content) throws IOException// 读文件，记录入SentenceCont中,apriori算法使用
			, InterruptedException {

		Document doc = Jsoup.parse(content);
		Elements es = doc.getElementsByTag("sentence");


		for (Element e : es) {// 遍历所有的句子

			Document doc2 = Jsoup.parse(e.toString());
			Elements es2 = doc2.getElementsByTag("sentcont");

			if(es2.text().trim().length()>= 1) {
				SentenceCont.add(es2.text().toLowerCase());
			}
		}

	}
	private void Comp_Ttable(String fpath, String flag) throws IOException {
		File f = new File(fpath);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		while ((line = br.readLine()) != null) {
			String word[] = line.replace(":::::", "::::").split("::::");
			//System.out.println("line = "+line);	
			double sim = Double.parseDouble(word[2]);
			if(!(word[0].equals("NULL"))&&!(word[1].equals("NULL")))
			{
				Pair co;
				if(flag.equals("e2c"))//名词到形容词
				{
					co = new Pair(word[0], word[1]);
					T.put(co, sim);
				}else{
					co = new Pair(word[1], word[0]);
					if(T.containsKey(co))
						T.put(co, 1.0 / ( 0.5 / sim + 0.5 / T.get(co) ));
					else
						T.put(co, sim*0.5);
					}
						
				}// end of if
			}//end of while
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {

		// swParser = ShallowParser.getInstance();//使用opennlp进行词法解析

		AspectIdentify AI = new AspectIdentify();

		AI.ReadAspectStopwordFile(".\\Stopwords");// 读入停止词

		String BaselineMethod = "HITS";// 可选参数Apriori、(Tr)LanguageModel、Translation Based LM with TFIDF
		String productname = "Nokia_5800_XpressMusic.txt";
		
		AspectsLM.clear();
		Aspects_Selected.clear();
		
		String content = AI.ReadFile(".\\XMLReviewsYu\\" + productname);

		File f2 = new File(".\\FinalSelectedAspects\\"+productname);// 经过trLM/LM/apriori后得到的最终aspect候选集
		if (f2.exists())
			f2.delete();
		f2.createNewFile();
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(f2, true));// 追加写入，经过下面的aspect候选集筛选方法，将选择的候选集写入./FinalSelectedAspects

		File aspects_POS = new File(".\\AspectsPos\\"+productname);
		FileReader aspects_POSreader = new FileReader(aspects_POS);
		BufferedReader aspects_POSbr = new BufferedReader(aspects_POSreader);
		String line = null;
		while((line = aspects_POSbr.readLine())!=null)
		{
			String[] words = line.split("<---->");
			Aspects_Selected.put(words[0],Integer.parseInt(words[1]));
		}
		
		File alignfile = new File(".\\Stage1\\VisualAlignFrequency.txt");
		BufferedReader alignbr = new BufferedReader(new FileReader(alignfile));
	    while((line=alignbr.readLine())!=null)
		{
			String[] words = line.split("::::");
			alignfrequency.put(new Pair(words[0],words[1]), Integer.parseInt(words[2]));
		}
		HashMap<String,Double> results = new HashMap<String,Double>();//记录每一个候选词的相似度,并排序
		// 可选参数Apriori、(Tr)LanguageModel、Translation Based LM with TFIDF
        System.out.println("Selected Method is " + BaselineMethod);
        
		if (BaselineMethod.equals("Apriori")) {
			Aspects_Cand_Apriori.clear();
			Aspects_Cand_Compact.clear();
			Aspects_Cand_pSup.clear();

			AI.GetSentenceCont(content);//
			System.out.println("Initialize Apriori Starting...");
			AprioriAlgorithm.main(new String[] {productname});// 调用apriori算法 note 20130719
			System.out.println("Initialize Apriori Ending...");

			File f = new File(".\\FrequentItemset.txt");
			BufferedReader br1 = new BufferedReader(new FileReader(f));
			String aspect_item = null;
			while ((aspect_item = br1.readLine()) != null) {
				String[] words = aspect_item.split(":");
				Aspects_Cand_Apriori.put(words[0], Integer.parseInt(words[1]));
			}
			br1.close();
			Aspects_Selected.clear();
			Aspects_Selected = Aspects_Cand_Apriori; // 将Apriori算法最初得到的candidate
														// item付给aspect_selected容器，noted
														// 20130719
			AI.compactphrase();
			AI.p_support();
			for (String s : Aspects_Selected.keySet()) {
				if (s.trim().length() >= 1) {
					bw2.write(s + ":" + Aspects_Selected.get(s) + "\r\n");
					bw2.flush();
				}
			}

		}else if (BaselineMethod.equals("LanguageModel"))// 利用languagemodel筛选aspect
		{
			System.out.println("Initialize LanguageModel Starting...");
			AI.initializeLanguageModel(productname);
			System.out.println("Initialize LanguageModel Ending...");
			for (String s : Aspects_Selected.keySet()) {
				if(!Stopwords.contains(s))
				{
					double similarity = AI.LanguageModel(s);
					System.out.println("similarity=" + similarity
							+ " candidate aspect = " + s);
					if (s.trim().length() >= 1 && similarity >= -3.460) {
						AspectsLM.put(s, similarity);
						bw2.write(s + ":" + Aspects_Selected.get(s) + "\r\n");
						bw2.flush();
					}
				}
			}//end of for
		} else if (BaselineMethod.equals("Translation Based LM")) {
			
			AI.initializeLanguageModel(productname);
			System.out.println("1). Initialize Ttable Starting...");
			AI.Comp_Ttable(".\\Ttable\\e2c.actual.t1.final", "e2c");//计算转移概率表
			AI.Comp_Ttable(".\\Ttable\\c2e.actual.t1.final", "c2e");//计算转移概率表			
			System.out.println("2). Initialize Ttable Ending...");
			System.out.println("3). Computing penalize function...");
			double b = 20;
			for(Pair p:T.keySet())
			{
				if(alignfrequency.containsKey(p))
					T.put(p, T.get(p)*Math.exp(-b/Math.log10(alignfrequency.get(p)+0.001)));
				else {
					T.put(p, T.get(p)*Math.exp(-b/Math.log10(1.0+0.001)));
				}
			}
			for (String s : Aspects_Selected.keySet()) {
				if(!Stopwords.contains(s))
				{
					double similarity = AI.TR_LanguageModel(s);
					//System.out.println("similarity=" + similarity+ " candidate aspect = " + s);
					results.put(s,similarity);
					if (s.trim().length() >= 1 && similarity >= -3.50) {//在Canon G3数据集上，不考虑opinion频率-0.60；考虑opinion频率-3.15
						AspectsLM.put(s, similarity);
						bw2.write(s + ":" + Aspects_Selected.get(s) + "\r\n");
						bw2.flush();
					}
				}
			}//end of for
		}//end of if(TrLM)
		else if (BaselineMethod.equals("Translation Based LM with TFIDF")) {
		
	    AI.initializeTFIDF(productname);
		System.out.println("Initialize Ttable Starting...");
		AI.initializeLanguageModel(productname);
		AI.Comp_Ttable(".\\Ttable\\e2c.actual.t1.final", "e2c");//计算转移概率表
		AI.Comp_Ttable(".\\Ttable\\c2e.actual.t1.final", "c2e");//计算转移概率表			
		System.out.println("Initialize Ttable Ending...");
		
		for (String s : Aspects_Selected.keySet()) {
			if(!Stopwords.contains(s))
			{
				double similarity = AI.TR_LanguageModelwithTFIDF(s);
				System.out.println("similarity=" + similarity
						+ " candidate aspect = " + s);
				if (s.trim().length() >= 1 && similarity >= -1.02) {//在Canon G3数据集上，不考虑opinion频率-0.89；考虑opinion频率-2.80
					AspectsLM.put(s, similarity);
					bw2.write(s + ":" + Aspects_Selected.get(s) + "\r\n");
					bw2.flush();
				}
			}
		}//end of for
	}//end of if(TrLM)
	else if(BaselineMethod.equals("HITS"))//实现Lei zhang的HITS算法
	{
		System.out.println("Initializing Language Model Start...");
		AI.initializeLanguageModel(productname);
		System.out.println("Initializing Language Model End...");
		File spca = new File(".\\Stage1\\SPCA.txt");
		File nouns = new File(".\\Stage1\\nouns.txt");

		BufferedReader brspca = new BufferedReader(new FileReader(spca));
		BufferedReader brnouns = new BufferedReader(new FileReader(nouns));
		HashMap<String,Double> authority = new HashMap<String,Double>();
		String line1 = null;
		String line2 = null;
		while ((line1 = brspca.readLine()) != null
				&& (line2 = brnouns.readLine()) != null) {
			authority.put(line2, Double.parseDouble(line1));
		}

		for (String s : Aspects_Selected.keySet()) {
			if(!Stopwords.contains(s))
			{				
				double logfreq = 0.0;
				if(DomainCount.containsKey(s+"_nn")) 
					logfreq = Math.log10((double)DomainCount.get(s+"_nn")/DomainAllCount);
				else logfreq=-100;
				//System.out.println("similarity=" + similarity+ " candidate aspect = " + s);
				double autho = 0.0;
				if(authority.containsKey(s.trim())) 
					autho = authority.get(s.trim());
				else autho = 1.0;
				double similarity = logfreq*Math.exp(autho);
				results.put(s,similarity);
				if (s.trim().length() >= 1 && similarity >= -3.45) {//在Canon G3数据集上，不考虑opinion频率-0.60；考虑opinion频率-3.15
					AspectsLM.put(s, similarity);
					bw2.write(s + ":" + Aspects_Selected.get(s) + "\r\n");
					bw2.flush();
				}
			}
		}//end of for	
	}

		System.out.println("所有aspect candidate的相似度排序结果：");
		AI.printresult_Double(results);

		File sortedTrLMWeight = new File(".\\SortedTrLMWeight\\"+productname);
		if(sortedTrLMWeight.exists()) sortedTrLMWeight.delete();sortedTrLMWeight.createNewFile();
		BufferedWriter sortedTrLMWeightbw = new BufferedWriter(new FileWriter(sortedTrLMWeight,true));
		AI.Savesortedresults(AspectsLM, sortedTrLMWeightbw);
		sortedTrLMWeightbw.close();
		
/*		File manualaspects = new File(".\\ManualAspects\\"+productname);// 记录手工标注的aspect,Evaluation时会读文件使用
        
		BufferedReader br = new BufferedReader(new FileReader(manualaspects));

		while((line = br.readLine())!=null)
		{
			String [] words = line.split(":");
			manual_Aspects_Frequence.put(words[0], Integer.parseInt(words[1]));
		}*/
		
		/*File TW = new File(".\\TrLMWeight\\"+productname);
		if(TW.exists()) TW.delete();   TW.createNewFile();
		BufferedWriter TWbw = new BufferedWriter(new FileWriter(TW,true));
		for(String aspect:manual_Aspects_Frequence.keySet())
		{
			TWbw.write(aspect+":"+AI.TR_LanguageModel(aspect)+"\r\n");
			TWbw.flush();
		}*/
	}

}
