import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class StanfordPosTagger {

	public static MaxentTagger tagger = null;
	private static HashMap<String, Integer> Aspects_POS = new HashMap<String, Integer>();// 最初经过pos后得到的aspect
	private static HashMap<String, Integer> manual_Aspects_Frequence = new HashMap<String, Integer>();

	public StanfordPosTagger() {
		tagger = new MaxentTagger(".\\english-bidirectional-distsim.tagger");
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

	private void WriteFile(File f, String content) throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(f, true));// 追加方式写入
		br.write(content);
		br.flush();
		br.close();
	}

	public String Postagger(String input)// 将词法标注结果返回
	{
		String tagged = tagger.tagString(input);
		System.out.println("Tagged = " + tagged);
		return tagged;
	}

	public void get_manual_aspects(String content, String productname)
			throws IOException {
		
		manual_Aspects_Frequence.clear();
		
		Document doc = Jsoup.parse(content);
		Elements aspects_es = doc.getElementsByTag("aspects");

		for (Element e : aspects_es) {
			String[] M_aspects = e.text().split(",");
			// System.out.println("F_aspects=" + Arrays.toString(F_aspects));
			//System.out.println("M_aspects=" + Arrays.toString(M_aspects));
			for (String s : M_aspects) {
				String S_lower = s.trim().toLowerCase();
				if (S_lower.length() >= 1) {
					if (manual_Aspects_Frequence.containsKey(S_lower))
						manual_Aspects_Frequence.put(s.trim(),
								manual_Aspects_Frequence.get(S_lower) + 1);
					else
						manual_Aspects_Frequence.put(S_lower, 1);
				}
			}
		}

		File manual_aspects = new File(".\\ManualAspectsOriginal\\"
				+ productname);// 手工标注得到的aspects记录到文件中
		if (manual_aspects.exists())
			manual_aspects.delete();
		manual_aspects.createNewFile();

		FileWriter manualaspectswriter = new FileWriter(manual_aspects, true);
		BufferedWriter manualaspectsbw = new BufferedWriter(manualaspectswriter);
		for (String aspect : manual_Aspects_Frequence.keySet()) {
			manualaspectsbw.write(aspect + ":"
					+ manual_Aspects_Frequence.get(aspect) + "\r\n");
			manualaspectsbw.flush();
		}
		manualaspectsbw.close();
	}

	public static void main(String[] args) throws Exception {

		StanfordPosTagger postagger = new StanfordPosTagger();
		/*
		 * String input =
		 * "if you are looking for an outstanding camera that can take you from simple to complex"
		 * ; String tagged = postagger.Postagger(input);
		 * System.out.println(tagged); Thread.sleep(10000);
		 */
        File f = new File(".\\XMLReviewsYu");
        String[] list = f.list();
        //for(int i=0;i<list.length;++i)
        for(int i=1;i<=1;++i)
        {
        	if(!(new File(".\\XMLReviewsYu\\"+list[i]).isDirectory()))
        	{
        		Aspects_POS.clear();
        		String productname = "Nokia_N95.txt";//list[i];// 调整productname
        		System.out.println("Current file = "+list[i]);
        		Thread.sleep(5000);
        		String content = postagger.ReadFile(".\\XMLReviewsYu\\ProAndCon\\" + productname);

        		//postagger.get_manual_aspects(content, productname);//手工标注的aspect

        		Document doc = Jsoup.parse(content);

        		Elements es = doc.getElementsByTag("sentence");

        		File transactionfile = new File(".\\TransactionFiles\\" + productname);// 事务文件存放目录
        	/*	if (transactionfile.exists())
        			transactionfile.delete();
        		transactionfile.createNewFile();*/

        		File tagresult = new File(".\\TaggedResults\\" + productname);// 对每个句子进行词性标注的结果
        		/*if (tagresult.exists())
        			tagresult.delete();
        		tagresult.createNewFile();*/

        		File aspects_POS = new File(".\\AspectsPos\\" + productname);// 词性标注得到的aspect记录到文件中
        		/*if (aspects_POS.exists())
        			aspects_POS.delete();
        		aspects_POS.createNewFile();*/

        		FileWriter fw = new FileWriter(tagresult, true);
        		BufferedWriter bw = new BufferedWriter(fw);

        		for (Element e : es) {// 遍历所有的句子

        			Document doc2 = Jsoup.parse(e.toString());
        			Elements es2 = doc2.getElementsByTag("sentcont");

        			for (Element ee : es2) {
        				// 使用stanfordpostagger
        				if(ee.text().trim().length() == 0)
        					continue;
        				String posresult = postagger.Postagger(ee.text());// added 20130718

        				bw.write(posresult + "\r\n");// 词性标注结果写入文件

        				String candidate_aspects = new String();
        				String[] taggedwords = posresult.split(" ");
        				String aspects = new String();
        				for (String taggedword : taggedwords) {
        					String[] word_tag = taggedword.split("_");
        					if (word_tag[1].equals("NN") || word_tag[1].equals("NNP")
        							|| word_tag[1].equals("NNS")
        							|| word_tag[1].equals("NP")
        							|| word_tag[1].equals("NNPS")) {
        						aspects += word_tag[0] + " ";
        					} else {
        						if (aspects.length() >= 1) {
        							candidate_aspects += aspects.trim() + "*";
        							aspects = new String();
        						}
        					}
        				}
        				if (aspects.length() > 0)
        					candidate_aspects += aspects.trim();
        				String[] Tag_aspects = candidate_aspects.split("\\*");// *在正则表达式里有特殊意义
        				
        				//for (String s : Tag_aspects)
        					//System.out.println("Tag_aspects=" + s);
        				
        				for (String aspect : Tag_aspects)// added dujintao 20130719
        				{
        					if (aspect.trim().length() >= 1)
        						if (Aspects_POS.containsKey(aspect.trim().toLowerCase()))
        							Aspects_POS.put(aspect.trim().toLowerCase(),
        									Aspects_POS.get(aspect.trim().toLowerCase()) + 1);
        						else
        							Aspects_POS.put(aspect.trim().toLowerCase(), 1);
        				}

        				if (candidate_aspects.trim().length() >= 1) {// 将得到的名词短语写入到transaction文件，是apriori算法的输入
        					//postagger.WriteFile(transactionfile, candidate_aspects.replace("*", " ").trim().toLowerCase()+ "\r\n");
        				}
        			}

        			//Elements es3 = doc2.getElementsByTag("aspects");
        			//String[] Manual_aspects = es3.text().split(",");

        			//System.out.println("Manual_aspects="+ Arrays.toString(Manual_aspects));
        		}//end of for elements es
        		/*for (String aspect : Aspects_POS.keySet()) {
        			postagger.WriteFile(aspects_POS,
        					aspect + "<---->" + Aspects_POS.get(aspect) + "\r\n");// 将aspects_POS写入文件，词法解析仅进行一次
        		}*/
        		bw.close();
	
        	}//end of if(isDirectory())
        }
        

	}//end of main

}
