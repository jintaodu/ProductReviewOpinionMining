package OpinionRetrieval;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;

import HelpfulnessModel.Helpfulness;
import WordAlignmentModel.Pair;

public class OpinionRetrieval {

	/**
	 * @param args
	 */
	private HashMap<String, Double> CollectionTF = new HashMap<String, Double>();
	private HashSet<String> StopWords = new HashSet<String>();
	private HashSet<String> OpinionLexicon = new HashSet<String>();
	private HashMap<String,String> ReviewsDocs = new HashMap<String,String>();
	private HashMap<Pair, Double> T = new HashMap<Pair, Double>();// TranslationModel中的T表
	private HashMap<String,Double> Results = new HashMap<String,Double>();
	private HashMap<String, Document> ReviewsDocsList = new HashMap<String,Document>();//评论文档对象列表
	private static HashMap<Pair,Integer> alignfrequency = new HashMap<Pair,Integer>();//词对齐次数
	private HashSet<String> AllModelResults = new HashSet<String>();//不同模型得到的所有result的key的集合
	private String annotationFile = null;
	private String expandedQuery = null;
	private final String regularExpression = "[^^a-zA-Z0-9|$|\\-|']";
	private Helpfulness helpfulness = new Helpfulness();
	private HashSet<String> aspects = new HashSet<String>();//ICPR论文中得到的aspects
	private HashSet<String> opinions = new HashSet<String>();//ICPR论文中得到的opinions
	/*
	 * Okapimodel
	 */
	private HashMap<String, Integer> dfw = new HashMap<String,Integer>();
	private HashMap<Integer, String> wordindex = new HashMap<Integer, String>();//给每个词以小标
	private double Wa = 0.0, N=0.0;
	
	
	private void readStopWords() throws IOException{
		File f = new File("Stopwords");
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {
			StopWords.add(line);
		}
	}
	public void print(Object str){
		System.out.println(str);
	}
	public void readAspects(String domain) throws IOException{
		File f = new File("FinalSelectedAspects/"+domain+".txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		while((line = br.readLine()) != null){
			aspects.add(line.substring(0,line.lastIndexOf(":")));
		}
		print("finalaspects size="+aspects.size());
		br.close();
	}
	public void readOpoinions(String domain) throws IOException{
		File f = new File("FinalSelectedOpinions/"+domain+".txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		while((line = br.readLine()) != null){
			opinions.add(line.substring(0,line.lastIndexOf(":")));
		}
		print("finalopinions size="+opinions.size());
		br.close();
	}
	
	private void readOpinionLexicon() throws IOException{
		File f = new File("OpinionLexicon.txt");
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {
			OpinionLexicon.add(line);
		}
		System.out.println("OpinionLexicion size="+OpinionLexicon.size());
	}
	
	private void reviewFormatParsing(String fpath, String fname) throws IOException{
		
		InputStreamReader isr = new InputStreamReader(
				new FileInputStream(fpath), "UTF-8");
		BufferedReader br = new BufferedReader(isr);

		String line = null;

		String freereviewcontent = null;
		String proandconreviewcontent = null;
		String reviewmeta = null;//记录review的元数据信息：id，网站，作者，overallrating

		int sentencenum = 0;
		int proandconnum = 0;
		int line_num = 1;
		while ((line = br.readLine()) != null) {//每行是一个review document
			freereviewcontent = "";
			proandconreviewcontent = "";
			reviewmeta = "";
			if (line.length() >= 1) {
				
				String[] reviewparts = line.split("::::");
				
				reviewmeta += "<websource>"+reviewparts[0].trim()+"</websource>\r\n";
				reviewmeta += "<reviewid>"+reviewparts[1].trim()+"</reviewid>\r\n";
				reviewmeta += "<reviewtitle>"+reviewparts[2].trim()+"</reviewtitle>\r\n";
				reviewmeta += "<rating>" + reviewparts[5].trim() + "</rating>\r\n";
				
				/*
				 * 把pro和con的review content写到文件里，和free review content分开存放
				 */
				if (reviewparts[6].trim().length() >= 1	&& !reviewparts[6].trim().equals("Reviewer left no comment")) {

					String [] prosentences = reviewparts[6].split("\\. |! |\\? ");
					sentencenum += prosentences.length;// 把pro看做一句话是不对的，需要分句
					proandconnum += prosentences.length;
					
                    proandconreviewcontent += "<pro><sentence>\r\n";
					for(String s:prosentences)
					   proandconreviewcontent += "<sentcont>" + s + "</sentcont>\r\n";
					proandconreviewcontent += "</sentence></pro>\r\n";
				}

				if (reviewparts[7].trim().length() >= 1 && !reviewparts[6].trim().equalsIgnoreCase("reviewer left no comment")) {

					String [] consentences = reviewparts[7].split("\\. |! |\\? ");
					sentencenum += consentences.length;// 把con看做一句话是不对的，需要分句
					proandconnum += consentences.length;
					
					proandconreviewcontent += "<con><sentence>\r\n";
					for(String s:consentences)
					    proandconreviewcontent += "<sentcont>" + s + "</sentcont>\r\n";
					proandconreviewcontent += "</sentence></con>\r\n";
				}
				
				if (reviewparts[11].length() > 2) {

					String summaryreviews = null;
					if (-1 == reviewparts[11].indexOf("<-->"))
						summaryreviews = reviewparts[11];
					else
						summaryreviews = reviewparts[11]
								.substring(reviewparts[11].indexOf("<-->") + 5);
					//if (summaryreviews.contains("/:/:")) {
						String[] summarysentence = summaryreviews.split("/:/:");

						for (String s : summarysentence) {

							String[] sentence_aspects = s.split(">:>:");
							if (sentence_aspects[0].trim().length() >= 1) {
								freereviewcontent += "<sentence><sentcont>"
										+ sentence_aspects[0].trim() + "</sentcont>";
								
								freereviewcontent += "</sentence>\r\n";
							}
						}
						freereviewcontent = "<freereview>" + freereviewcontent + "</freereview>\r\n";
					//}// end of if(contain /:/:)
				}
				
				if(ReviewsDocs.containsKey(reviewparts[1].trim() +"#"+ String.valueOf(line_num)+"#"+ fname )){
					System.out.println("true="+reviewparts[1].trim()+"\r\n"+ReviewsDocs.get(reviewparts[1].trim()+"#"+ String.valueOf(line_num) +"#"+ fname));
					System.out.println("--------------\r\n"+reviewmeta+proandconreviewcontent+freereviewcontent);					
				}
				ReviewsDocs.put(reviewparts[1].trim()+"#"+ String.valueOf(line_num) +"#"+ fname, reviewmeta+proandconreviewcontent+freereviewcontent);

				//System.out.println( reviewmeta+proandconreviewcontent+freereviewcontent );
				
			}//end if line
			line_num++;
			
		}//end while
		
		
	}
	private void ReadDocs(String domain ) throws InterruptedException, IOException
	{
		ReviewsDocs.clear();
		
		String corpushomedir = ".\\Product_ReviewsYu";
		String[] list = new File(corpushomedir+"/"+domain).list();
		for(String productname: list){
			reviewFormatParsing(corpushomedir + "/"+domain+"/" + productname
					+ "/corpus.txt", productname + ".txt");
		}

		
		/*String[] list1 = new File(corpushomedir).list();

		for (String dir1 : list1) {//遍历productreview目录下的所有review文件
			
			if ((new File(corpushomedir+"\\"+dir1)).isDirectory() && dir1.equals("phone")) {
				String[] list2 = new File(corpushomedir + "\\" + dir1).list();
				for(String productname: list2)
				{
					System.out.println("Current productname = "+ productname);
					Thread.sleep(1000);
					reviewFormatParsing(corpushomedir + "\\"+dir1+"\\" + productname
							+ "\\corpus.txt", productname.toLowerCase() + ".txt");
				}
			}//end if
		}*/
		
		System.out.println("Doc number is ： "+ReviewsDocs.size());
		
	}
	
	private double refineAlignProbability(Pair p, double sim ){
		double b = 20;
		if(alignfrequency.containsKey(p))
			return sim*Math.exp(-b/Math.log10(alignfrequency.get(p)+0.001));
		else {
			return sim*Math.exp(-b/Math.log10(1.0+0.001));
		}
	}
	
	private void Comp_Ttable(String fpath, String flag) throws NumberFormatException, IOException{
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
						T.put(co, refineAlignProbability(co, 1.0 / ( 0.5 / sim + 0.5 / T.get(co) )));
					else
						//T.put(co, sim*0.5);
						T.put(co, refineAlignProbability(co,sim*0.5) );
					}
						
				}// end of if
			}//end of while
	}
	
	public HashMap<String, Integer> get_WordCount(String content){
			content = content.toLowerCase();
			HashMap<String, Integer> WC = new HashMap<String, Integer>();
			String words[] = content.split(regularExpression);
			for (String w : words) {
				if ((w.length() >= 3) && !StopWords.contains(w)) {
			
					if (WC.containsKey(w)) {
						int num = WC.get(w) + 1;
						WC.put(w, num);
					} else {
						WC.put(w, 1);
					}
				}
			}
			return WC;
	}
	
	public HashMap<String,Double> get_TermFrquency(String content){
		
		HashMap<String,Double> DocVec = new HashMap<String,Double>();
		HashMap<String, Integer> WC = new HashMap<String, Integer>();
		content = content.toLowerCase();
		String words[] = content.split(regularExpression);
		for (String w : words) {
			
			if ((w.length() >= 3) && !StopWords.contains(w)) {

				if (WC.containsKey(w)) {
					int num = WC.get(w) + 1;
					WC.put(w, num);
				} else {
					WC.put(w, 1);
				}
			}
		}

		long Totalwordcount = 0;
		for (String s : WC.keySet()) {
			Totalwordcount += WC.get(s);
		}
		// System.out.println("totalwordcount = "+ Totalwordcount);
		for (String s : WC.keySet()) {
			DocVec.put(s, (double) WC.get(s) / Totalwordcount);
		}
		return DocVec;
		
	}
	
	private void constructOkapiBM25(String domain) throws IOException{
		
		dfw.clear();
		ReviewsDocsList.clear();
		long doctotallength = 0;
		
		for(String key : ReviewsDocs.keySet()){
			
			Document doc = new Document(key);
			doc.documentContent = ReviewsDocs.get(key);
			try{
				doc.star = Double.parseDouble(Jsoup.parse(doc.documentContent).getElementsByTag("rating").text());
			}catch(Exception e){
				doc.star = 0.0;
			}
			
			String filecontent = Jsoup.parse(doc.documentContent).text().toLowerCase();
			doc.documentTF = get_TermFrquency(filecontent);
			doc.documentWC = get_WordCount(filecontent);
			String words[] = filecontent.split(regularExpression);
			doctotallength += words.length;
			doc.documentlength = words.length;
			
			doc.helpfulness = helpfulness.predictHelpfulness(domain, doc);
			Document.allhelpfulness += doc.helpfulness;
			
			//print("helpfulness="+doc.helpfulness);
			
			//print("doc.documentWC="+doc.documentWC);
			
			for(String word : doc.documentTF.keySet()){
				if(dfw.containsKey(word)){
					dfw.put(word, dfw.get(word)+1);
				}else{
					dfw.put(word,1);
				}
			}
			if(words.length >0 ){
				ReviewsDocsList.put(key, doc);
			}
		}
		int index = 0;
		for(String word : dfw.keySet()){
			wordindex.put(index,word);
			index++;
		}
		System.out.println("wordindex size = "+wordindex.size());
		System.out.println("dfw size = "+dfw.size());
		N = ReviewsDocs.size();
		Wa = (double)doctotallength/N;
	
	}
	
	public void construct_LM() throws IOException{
		File dir = new File("./LanguageModel");
		String[] filenames = dir.list();
		
		HashMap<String,Integer> WordCount = new HashMap<String,Integer>();
		long Totalwordcount = 0;
		
		for(String fn : filenames){
			if(fn.contains("Phone")){
				
				BufferedReader br = new BufferedReader(new FileReader(dir.getAbsolutePath()+"/"+fn));
				String line = null;
				while((line = br.readLine())!=null){
					String word = line.substring(0, line.lastIndexOf(":")).replace("_nn", "").replace("_jj", "");
					
					int count = Integer.parseInt( line.substring(line.lastIndexOf(":")+1) );
					if(WordCount.containsKey(word)){
						
						WordCount.put(word, WordCount.get(word)+count);
					}else{
						WordCount.put(word,count);
					}
					Totalwordcount += count;
				}
				br.close();
			}
		}
		
		for (String s : WordCount.keySet()) {
			CollectionTF.put(s, (double) WordCount.get(s) / Totalwordcount);
		}
	}
	
	private double getsimilarity(Document doc, HashMap<String,Integer> QueryWC, String ModelType){
		
		double similarity = 0.0;
		
		if (ModelType.equals("Translation Based LM")) {
			
			double lamda = 0.2;
			double beita = 0.2;
			for (String s : QueryWC.keySet()) {
				double sum = 0.0;
				double Pmltd = 0.0;
				double p = 0.0, PmltColl = 0.0;
				if (doc.documentTF.containsKey(s))
					Pmltd = doc.documentTF.get(s);
				for (String w : doc.documentTF.keySet()) {
					if (w.equals(s))
						sum += doc.documentTF.get(s);
					else if (T.containsKey(new Pair(s, w))){
						sum += T.get(new Pair(s, w)) * doc.documentTF.get(w);
						//System.out.println("s= "+s+" w= "+w+" sim="+T.get(new Pair(s, w)));
					}
				}// end of for2
				//System.err.println("sum="+sum+" pmltd="+Pmltd+" divide= "+Pmltd/sum);
				if (CollectionTF.containsKey(s))
					PmltColl = CollectionTF.get(s);
				if (sum != 0.0 || Pmltd != 0.0 || PmltColl != 0.0) {
					p = Math.log10(((1 - lamda)
							* (beita * sum + (1 - beita) * Pmltd) + lamda
							* PmltColl));
					similarity += p;
				}

			}// end of for1
		}else if (ModelType.equals("Language Model")) {

			double lamda = 0.2;
			double p = 0.0;
			for (String s : QueryWC.keySet()) {
				double Pmltd = 0.0, PmltColl = 0.0;
				if (doc.documentTF.containsKey(s))
					Pmltd = doc.documentTF.get(s);
				if (CollectionTF.containsKey(s))
					PmltColl = CollectionTF.get(s);
				if (Pmltd!=0.0 || PmltColl!=0.0) {
					p = Math.log10(((1 - lamda) * Pmltd + lamda
							* PmltColl));

					similarity += p;
				}
			}
		} else if(ModelType.equals("NewGenerationModel")){
		
			double Iop = 0.0;
			double Irel = 0.0;
			for (String s : QueryWC.keySet()) {
				double sum = 0.0;
				double Pmltd = 0.0;
				double p = 0.0, PmltColl = 0.0;
				if (doc.documentTF.containsKey(s))
					Irel += doc.documentTF.get(s);
				for (String w : doc.documentTF.keySet()) {
					if (T.containsKey(new Pair(s, w))){
						Iop += T.get(new Pair(s, w));
						//System.out.println("s= "+s+" w= "+w+" sim="+T.get(new Pair(s, w)));
					}
						
				}// end of for2
				
				//System.err.println("sum="+sum+" pmltd="+Pmltd+" divide= "+Pmltd/sum);

			}// end of for1
			similarity = Iop*Irel;
			
		}else if(ModelType.equals("Baseline")){
			
			//System.out.println("QueryWordCount = "+QueryWC);
			double ScoreOp = 0.0;
			double ScoreTopic = 0.0;
			double Kd = 1.2 * (0.25 + 0.75
					* (double) doc.documentlength / Wa);
			
			for (String s : QueryWC.keySet()) {
				if (doc.documentWC.containsKey(s)) {//求交集
					double p = Math.log((N - dfw.get(s) + 0.5)
							/ (dfw.get(s) + 0.5))
							* QueryWC.get(s)
							* (1.2 + 1)
							* doc.documentWC.get(s)
							/ (Kd + doc.documentWC.get(s));
					
					ScoreTopic += p;
				}
			}
			
			double opinionPart = 0.0;
			for(String qi : QueryWC.keySet()){
				double denominator=0.0, numerator = 0.0;//分母和分子
				if(doc.documentTF.containsKey(qi)){
					for(String opinion : OpinionLexicon){
						if(doc.documentTF.keySet().contains(opinion)){
							numerator += doc.documentTF.get(opinion) < doc.documentTF.get(qi)? doc.documentTF.get(opinion):doc.documentTF.get(qi);
						}
					}
					denominator += doc.documentTF.get(qi)*doc.documentlength;
					opinionPart += numerator/denominator;
				}
			}
			double lambda = 0.7;
			ScoreOp = (1-lambda)*Math.log10(opinionPart + 1) + lambda;
			System.out.println("opinionPart="+opinionPart+" ScoreOp="+ScoreOp+" ScoreTopic="+ScoreTopic);
			similarity = ScoreOp * ScoreTopic;
			
		}else if(ModelType.equals("OkapiBM25")){
			
			//System.out.println("QueryWordCount = "+QueryWC);
			double ScoreOp = 0.0;
			double ScoreTopic = 0.0;
			double Kd = 1.2 * (0.25 + 0.75
					* (double) doc.documentlength / Wa);
			
			double aspectfromdoc = 0;
			double aspectfromquery = 0;
			double helpfulnessweight = 0.0;
			for(String word : doc.documentWC.keySet()){
				if(aspects.contains(word))
					aspectfromdoc += doc.documentWC.get(word);
			}
			//aspectfromdoc /= doc.documentlength;
			
			for(String word : QueryWC.keySet()){
				if(doc.documentWC.containsKey(word) && aspects.contains(word))
					aspectfromquery += doc.documentWC.get(word);
			}
			//aspectfromquery /= QueryWC.size();
			
			print("aspectfromdoc="+aspectfromdoc+"  aspectfromquery="+aspectfromquery);
			
			for (String s : QueryWC.keySet()) {
				if (doc.documentWC.containsKey(s)) {//求交集
					double p = Math.log((N - dfw.get(s) + 0.5)
							/ (dfw.get(s) + 0.5))
							* QueryWC.get(s)
							* (1.2 + 1)
							* doc.documentWC.get(s)
							/ (Kd + doc.documentWC.get(s));
					
					ScoreTopic += p;
				}
				for (String w : doc.documentTF.keySet()) {
					if ( OpinionLexicon.contains(w) && T.containsKey(new Pair(s, w))){
						ScoreOp += T.get(new Pair(s, w));
					}
				}
			}
			
			double helpfulnesspart = doc.helpfulness*Math.log10(aspectfromquery/aspectfromdoc +1 );
			print("helpfulnesspart="+helpfulnesspart);
			print("helpfulness="+doc.helpfulness);
			print("document id="+doc.documentname);
			
			if(0 == aspectfromquery|| 0 == aspectfromdoc) helpfulnesspart = 0;
			doc.Scorehelpful      = helpfulnesspart;
			
			if(doc.maxhelpfulness < helpfulnesspart) doc.maxhelpfulness = helpfulnesspart;
			if(doc.minhelpfulness > helpfulnesspart) doc.minhelpfulness = helpfulnesspart;
			
			double lambda = 0.8;
			System.out.println("ScoreOp="+(( 1-lambda )*Math.log10(1 + ScoreOp) + lambda)+" ScoreTopic="+ScoreTopic);

			if( 0 == ScoreOp){
				similarity = 0;
			}else{
				similarity = (( 1-lambda )*Math.log10(1 + ScoreOp) + lambda)*ScoreTopic;
			}
		}else if(ModelType.equals("Helpfulness")){
			
			//System.out.println("QueryWordCount = "+QueryWC);
			double ScoreOp = 0.0;
			double ScoreTopic = 0.0;
			double Kd = 1.2 * (0.25 + 0.75
					* (double) doc.documentlength / Wa);
			
			
			double aspectfromdoc = 0;
			double aspectfromquery = 0;
			double helpfulnessweight = 0.0;
			
			for(String word : doc.documentWC.keySet()){
				if(aspects.contains(word))
					aspectfromdoc += doc.documentWC.get(word);
			}
			//aspectfromdoc /= doc.documentlength;
			
			for(String word : QueryWC.keySet()){
				if(doc.documentWC.containsKey(word) && aspects.contains(word))
					aspectfromquery += doc.documentWC.get(word);
			}
			//aspectfromquery /= QueryWC.size();
			
			print("aspectfromdoc="+aspectfromdoc+"  aspectfromquery="+aspectfromquery);
			for (String s : QueryWC.keySet()) {
				if (doc.documentWC.containsKey(s)) {//求交集
					double p = Math.log((N - dfw.get(s) + 0.5)
							/ (dfw.get(s) + 0.5))
							* QueryWC.get(s)
							* (1.2 + 1)
							* doc.documentWC.get(s)
							/ (Kd + doc.documentWC.get(s));
					
					ScoreTopic += p;
				}
				for (String w : doc.documentTF.keySet()) {
					if ( OpinionLexicon.contains(w) && T.containsKey(new Pair(s, w)) ){
						ScoreOp += T.get(new Pair(s, w));
					}
				}
			}
			
			double helpfulnesspart = doc.helpfulness*Math.log10(aspectfromquery/aspectfromdoc +1 );
			double lambda = 0.7;
			double beita = 0.1;
			if(0 == aspectfromquery|| 0 == aspectfromdoc) helpfulnesspart = 0;
			System.out.println("ScoreOp="+(( 1-lambda )*Math.log10(1 + ScoreOp) + lambda)+" ScoreTopic="+ScoreTopic+" helpfulnesspart="+helpfulnesspart);

			similarity = (beita)*(( 1-lambda )*Math.log10(1 + ScoreOp) + lambda)*ScoreTopic +(1-beita)*helpfulnesspart;
		}

		return similarity;
	}
	private double ComputeCosine(double[] vectori, double[] vectorj){
		double nominator = 0.0;
		double d1 = 0.0, d2 = 0.0;
		for(int i=0; i< vectori.length; ++i){
			nominator += vectori[i]*vectorj[i];
			d1 += Math.pow(vectori[i], 2.0);
			d2 += Math.pow(vectorj[i], 2.0);
		}
		return nominator/(Math.sqrt(d1)*Math.sqrt(d2));
	}
	private double ComputeRelatedness(String ri, String rj){
		//此函数用计算W矩阵的元素
		double wij = 0.0;
		Document doci = ReviewsDocsList.get(ri);
		Document docj = ReviewsDocsList.get(rj);
		double[] vectori = new double[dfw.size()];
		double[] vectorj = new double[dfw.size()];
		
		double eita = 0.5;//参数eita
		
		for(Integer index=0; index < dfw.size(); ++index){
			String word = wordindex.get(index);
			if(StopWords.contains(word)) continue;
			if( doci.documentTF.containsKey(word)){
				vectori[index] = doci.documentTF.get(word)*Math.log10(N/dfw.get(word));
			}
			if(docj.documentTF.containsKey(word)){
				vectorj[index] = docj.documentTF.get(word)*Math.log10(N/dfw.get(word));
			}
		}
		
		//double helpfulsim = Math.abs(ReviewsDocsList.get(ri).Scorehelpful - ReviewsDocsList.get(rj).Scorehelpful)/(Document.maxhelpfulness-Document.minhelpfulness);
		double helpfulsim = Math.exp( -1+(-1)*Math.abs(ReviewsDocsList.get(ri).Scorehelpful - ReviewsDocsList.get(rj).Scorehelpful) );
		double vecsim = ComputeCosine( vectori, vectorj );
		print("vecsim="+vecsim+" helpfulsim="+helpfulsim);
		//print("ReviewsDocsList.get(ri).Scorehelpful"+ReviewsDocsList.get(ri).Scorehelpful);
		//print("ReviewsDocsList.get(rj).Scorehelpful"+ReviewsDocsList.get(rj).Scorehelpful);
		wij = (1-eita)*vecsim + eita*helpfulsim;
		
		return wij;
	}
	private void sparseGraphW(double[][] W, int aNN){
		//本函数稀疏W表示的图
		int NN = 100 - aNN;
		for(int i=0; i<100;++i){
			double[] Wi = W[i].clone();
			Arrays.sort(Wi);
			System.out.println(Arrays.toString(Wi));
			for(int j = 0; j<100;++j){
				if(W[i][j] < Wi[NN])
					W[i][j] = 0.0;
			}
		}
	}
	private void ComputeDandWforRerank(ArrayList<Entry<String, Double>> array, int aNN)throws IOException{
		//此函数用来计算Rerank中的矩阵D和W，将结果写文件 20150203
		double[][] D = new double[100][100];
		double[][] W = new double[100][100];
		
		for(int i=0; i< 100; i++)
			for(int j=0; j<100; j++){
				if( i == j ) W[i][j] = 0.0;
				else W[i][j] = ComputeRelatedness(array.get(i).getKey(),array.get(j).getKey());
			}
		sparseGraphW( W , aNN );//sparse graph of W
		for(int i=0; i< 100; i++){
			double tmp = 0.0;
			for(int j=0; j<100; j++){
				tmp += W[i][j];
			}
			D[i][i] = tmp;
		}
		
		File f = new File("ORResults/"+URLEncoder.encode(annotationFile,"utf-8")+"_W.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for(int i=0; i< 100; i++){
			for(int j=0; j<100; j++){
				bw.write(W[i][j]+" ");
			}
			bw.write("\r\n");
			bw.flush();
		}
		f = new File("ORResults/"+URLEncoder.encode(annotationFile,"utf-8")+"_D.txt");
		bw = new BufferedWriter(new FileWriter(f));
		for(int i=0; i< 100; i++){
			bw.write(D[i][i]+"\r\n");
			bw.flush();
		}
		
		f = new File("ORResults/"+URLEncoder.encode(annotationFile,"utf-8")+"_Y.txt");
		bw = new BufferedWriter(new FileWriter(f));
		for(int i=0; i< 100; i++){
			bw.write(ReviewsDocsList.get( array.get(i).getKey() ).ModelSimilarity+"\r\n");
			bw.flush();
		}
		bw.close();
	}
	private void Rerank(ArrayList<Entry<String, Double>> array)throws IOException{
		File f = new File("ORResults/"+URLEncoder.encode(annotationFile,"utf-8")+".txt");//读取matlab得到的f向量
		BufferedReader br = new BufferedReader(new FileReader(f));
		HashMap<String,Double> rerankresult = new HashMap<String,Double>();
		String line = null;
		int rank = 0;
		while((line = br.readLine()) != null){
			rerankresult.put(array.get(rank).getKey(), Double.parseDouble(line));
			rank++;
		}
		br.close();
		
		ArrayList<Entry<String, Double>> farray = new ArrayList<Entry<String, Double>>(rerankresult.entrySet());
		
		Collections.sort(farray, new Comparator<Map.Entry<String, Double>>() {
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
				
		f = new File("ORResults/"+URLEncoder.encode(annotationFile,"utf-8")+".txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for(int i=0; i<20; i++){
			AllModelResults.add(farray.get(i).getKey());
			bw.write(farray.get(i).getKey()+"\r\n");
			bw.flush();
		}
		
		bw.close();
		
	}
	public List<Document> PrintTopResultes(int topnum) throws IOException{

		ArrayList<Entry<String, Double>> array = new ArrayList<Entry<String, Double>>(
				Results.entrySet());
		//TopResults.clear();
		long start = System.currentTimeMillis();
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
		System.out.println("Sort similatiries time = "
				+ (System.currentTimeMillis() - start) + "ms");
		//先执行语句1），然后执行matlab代码，最后执行2）代码
		//ComputeDandWforRerank(array, 50 );//1)此函数生成Rerank需要的两个矩阵
		//Rerank( array );//2)此函数生成Rerank后得到的前20个结果
		
		List<Document> retrievalList = new ArrayList<Document>();
		Integer top = 0;
		
		for (Entry<String, Double> e : array) {
			if (top++ < topnum) {
				print(top.toString() + ". " + e.getKey() + "####"+ e.getValue());
				print("Helpfulness="+ReviewsDocsList.get(e.getKey()).helpfulness);
				print("doc.Scorehelpful ="+ReviewsDocsList.get(e.getKey()).Scorehelpful );
				print(ReviewsDocs.get(e.getKey()));
				AllModelResults.add(e.getKey());
				
				Document doc = new Document(e.getKey());
				doc.ModelSimilarity = e.getValue();
				retrievalList.add(doc);
			} else
				break;
		}
		
		return retrievalList;
	}
	
	private void WirteResults()throws IOException{
		
		File f1 = new File(("C:/Users/mmdb/Desktop/TopORResult/1/"+URLEncoder.encode(annotationFile,"utf-8")+".txt"));
		File f2 = new File(("C:/Users/mmdb/Desktop/TopORResult/2/"+URLEncoder.encode(annotationFile,"utf-8")+".txt"));
		if(!f1.exists()) f1.createNewFile();
		if(!f2.exists()) f2.createNewFile();
		BufferedReader br1 = new BufferedReader(new FileReader(f1));
		HashSet<String> existfiles = new HashSet<String>();
		String line = null;
		while((line = br1.readLine()) != null){
			if(line.contains("####")){
				existfiles.add(line.substring(0,line.indexOf("####")));
			}
		}
		BufferedWriter bw1 = new BufferedWriter(new FileWriter(f1,true));
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(f2,true));
		for(String key : AllModelResults){
			if(!existfiles.contains(key)){
				bw1.write("query="+expandedQuery+"\r\n\r\n");
				bw1.write(key + "####Manual Annotation Score =\r\n\r\n");
				bw1.write(ReviewsDocs.get( key )+"\r\n\r\n");
				bw1.flush();
				
				bw2.write("query="+expandedQuery+"\r\n\r\n");
				bw2.write(key + "####Manual Annotation Score =\r\n\r\n");
				bw2.write(ReviewsDocs.get( key )+"\r\n\r\n");
				bw2.flush();
			}
		}
		bw1.close();bw2.close();
		//以上内容为了合并已标注和未标注问题20150119
		File f = new File("TopORResult/" + URLEncoder.encode(annotationFile,"utf-8")+".txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f,true));
		bw.write("Result size="+AllModelResults.size()+"\r\n");
		for(String key : AllModelResults){
			bw.write("query="+expandedQuery+"\r\n\r\n");
			bw.write(key + "####Manual Annotation Score =\r\n\r\n");
			bw.write(ReviewsDocs.get( key )+"\r\n\r\n");
			bw.flush();
		}

		bw.close();

	}
	public List<Document> OpinionSearch(String[] aspects, String domain, String productname, String query, boolean queryexpansion, String ModelType) throws IOException{
		
		QueryExpansion qe = new QueryExpansion(domain, productname);
		System.out.println("original query="+query);
		Results.clear();//每次进行检索都对上次的检索结果清空
		annotationFile = query.length() > 100 ? query.substring(0,100) : query;//用原始query作为文件名保存
		Document.minhelpfulness = 2.0;
		Document.maxhelpfulness = 0.0;
		if(queryexpansion){
			for(String aspect : aspects){
				query += " "+qe.queryexpansion(aspect);
				String[] words = query.split(regularExpression);
				HashSet<String> qwords = new HashSet();
				for(String w : words) qwords.add(w);
				if( ! qwords.contains(aspect) ) query += " "+aspect;//此时的aspect不在query中，很可能两个词是同义词
			}
			expandedQuery = query;
		}
		print("expansion query = " + query);
		query = query.replace("||", " , ");
		HashMap<String,Integer> QueryWC = get_WordCount(query);
		System.out.println("QueryWC--->"+QueryWC);
		
		for(String key :ReviewsDocs.keySet()){
			if( !key.contains(productname) ) continue;
			//System.out.println("content="+ReviewsDocs.get(key));
			double similarity = getsimilarity( ReviewsDocsList.get(key), QueryWC, ModelType );
			ReviewsDocsList.get(key).ModelSimilarity = similarity;
			Results.put(key, similarity);
		}
		
		return PrintTopResultes(20);
	}
	public void initialize(String domain, String productname) throws IOException, InterruptedException{
		
		print("initialize SVR!");
		helpfulness.initializeSVR(domain);
		print("read final aspects");
		readAspects(domain);
		print("read final opinions");
		readOpoinions(domain);
		print("Read StopWords");
		readStopWords();
		print("Read Opinion Lexicon");
		readOpinionLexicon();
		//System.out.println("Construct Language Model");
		//construct_LM();
		print("Read Documents");
		ReadDocs( domain );
		print("construct OkapiBM25");
		constructOkapiBM25(domain);
		print("Read AlignFrequency");
		
		alignfrequency.clear();
		File alignfile = new File(".\\TTableforOpinionRetrieval/"+domain+"/VisualAlignFrequency.txt");
		BufferedReader alignbr = new BufferedReader(new FileReader(alignfile));
		String line = null;
	    while((line=alignbr.readLine())!=null)
		{
			String[] words = line.split("::::");
			alignfrequency.put(new Pair(words[0],words[1]), Integer.parseInt(words[2]));
		}
	    
	    print("Construct Ttable");
	    
		T.clear();
		Comp_Ttable(".\\TTableforOpinionRetrieval/"+domain+"/e2c.actual.t1.final", "e2c");//计算转移概率表
		Comp_Ttable(".\\TTableforOpinionRetrieval/"+domain+"/c2e.actual.t1.final", "c2e");//计算转移概率表
	}
	public List<String> get_Queries()throws IOException{
		File f = new File("Query/queries.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line=null;
		List<String> queries = new ArrayList<String>();
		while((line = br.readLine())!=null){
			queries.add(line);
		}
		return queries;
	}
	
	public void start()throws IOException, InterruptedException{
		
		List<String> queries = get_Queries();
		String ModelType = "Baseline";//"OkapiBM25";//"NewGenerationModel";//"Translation Based LM";//"Language Model";//
		
		String predomain="";
		for(String line : queries){
			
			AllModelResults.clear();
			String[] info = line.split("####");
			String query = info[0].toLowerCase();
			String domain = info[1];
			String productname = info[2];//windows文件名不区分大小写
			String[] aspects = info[3].toLowerCase().split("#");//aspect之间用#隔开
			
			System.out.println("domain="+domain+" productname = "+productname +" aspects= "+Arrays.toString(aspects));
			if( !predomain.equals(domain) ){
				initialize(domain, productname);
			}
			predomain = domain;
			
			OpinionSearch( aspects, domain, productname, query , true,  "Helpfulness" );////GM+MWA+QE+Linear
			
			//OpinionSearch( aspects, domain, productname, query , true,  "OkapiBM25" );//GM+MWA+QE
			//OpinionSearch( aspects, domain, productname, query , false, "OkapiBM25" );//GM+MWA
			//OpinionSearch( aspects, domain, productname, query , true,  "Baseline" );//GM+QE
			//OpinionSearch( aspects, domain, productname, query , false, "Baseline" );//GM
			
			WirteResults();
		}
	}
	public List<Document> retrieveNow(String line, boolean queryexpansion, String ModelType) throws IOException, InterruptedException{
		
		String[] info = line.split("####");
		String query = info[0].toLowerCase();
		String domain = info[1];
		String productname = info[2];//windows文件名不区分大小写
		String[] aspects = info[3].toLowerCase().split("#");
		initialize(domain, productname);
		
		return OpinionSearch( aspects, domain, productname, query , queryexpansion,  ModelType );
		
	}
	public void queryURLencode()throws IOException{
		File f = new File("Query/queries.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		ArrayList<String> queries = new ArrayList<String>();
		String line = null;
		while((line = br.readLine()) != null){
			queries.add(URLEncoder.encode(line.split("####")[0],"UTF-8"));
		}
		br.close();
		f = new File("Query/queriesurlencode.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for(String q: queries){
			bw.write(q+"\r\n");
			bw.flush();
		}
		bw.close();
		
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
		OpinionRetrieval or = new OpinionRetrieval();
		or.queryURLencode();//这个文件供matlab使用，matlab进行urlencode不方便
		or.start();
		
		/*String ModelType = "OkapiBM25";//"NewGenerationModel";//"Translation Based LM";//"Language Model";//
		String query = "Nokia N95 Built-In bluetooth?";
		String productname = "nokia_n95";//windows文件名不区分大小写
		String domain = "phone";
		String[] aspects = new String[]{"bluetooth"};
		
		or.initialize(domain, productname);
		
		boolean queryexpansion = false;                                                                                                                                                                                                                                nsion = true;
		or.OpinionSearch( aspects, domain, productname, query , queryexpansion, ModelType );*/
	}

}
