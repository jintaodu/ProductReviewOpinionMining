import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

public class Evaluation {
	private static HashSet<String> product_id = new HashSet<String>();

	private static HashSet<String> recalledaspect= new HashSet<String>();
	private static HashSet<String> precisionfrequent= new HashSet<String>();
	private static HashSet<String> TransactionCont = new HashSet<String>();// 记录含有NP的句子
	private static IDictionary dict = null;
	private static HashSet<Synonym> synset_pair = new HashSet<Synonym>();
	
	public Evaluation() throws IOException
	{
		String wnhome = System.getenv("WNHOME");
		String path = wnhome + File.separator + "dict";
		URL url = new URL("file", null, path);
		//System.out.println(url.toString());
		//construct the dictionary object and open it
		dict = new Dictionary(url);
		dict.open();
		
		File f = new File(".\\synonymset.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		while ((line = br.readLine()) != null) {
			if(line.length()>=1)
			{
				String[] words = line.split("<-->");
				//System.out.println(line);
				synset_pair.add(new Synonym(words[0],words[1]));
			}

		}
		br.close();
	}
	private static boolean sentence_sup(String sentence,String itemset)//句子中是否包含某个itemset
	{
		String[] words = sentence.split(" ");
		HashSet<String> sentwords = new HashSet<String>();
		for(String s:words)
		{
			sentwords.add(s);
		}
		String[] items = itemset.split(" ");
		for(int j=0;j<items.length;j++)
		{
			if(!sentwords.contains(items[j])) return false;
		}
		return true;
	}
	public static String Stemer(String word) throws IOException {//利用wordnet对名词提取词根

		// look up first sense of the word "dog "
		WordnetStemmer ws = new WordnetStemmer(dict);
		POS pos = null;
		List<String> stem = ws.findStems(word, pos.NOUN);
		if(stem.size()==0||stem==null) return word;
		else return stem.get(0);
         
	}
	public void ReadFile(String fpath) throws IOException {
		File f = new File(fpath);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		int line_num = 0;
		int contain = 0;
		while ((line = br.readLine()) != null) {
			line_num++;
			System.out.println(line);
			//String[] word = line.split("\t");
			// System.out.println(word[0]+" "+word[1]+" "+word[2]);
			// System.out.println(line);
			//WriteFile(".\\OnlyReviews.txt", word[word.length - 1] + "\r\n");
			// product_id.add(word[1]);

			//if (line_num % 100000 == 0)
				//System.out.println("line_num = " + line_num + " " + word[1]);
		}
		System.out.println("line_num = " + line_num);
		System.out.println("product num = " + product_id.size());
		br.close();
	}

	public void WriteFile(String fpath, String content) throws IOException {
		File f = new File(fpath);
		FileWriter fw = new FileWriter(f, true);
		fw.write(content);
		fw.flush();
		fw.close();
	}

	private static boolean match(String frequent,String aspect) throws IOException//频繁项集是无序的
	{
		String[] frewords = frequent.split(" ");
		String[] aspectwords = aspect.split(" ");
		if(frewords.length==1&&aspectwords.length==1||frewords.length!=aspectwords.length) return false;

		if(frewords.length == 2)
		for(int i=0;i<frewords.length;++i)
			for(int j=0;j<frewords.length;++j)
			{
				if(i!=j)
				{
					String tmp = (frewords[i]+" "+frewords[j]);
					if(aspect.equals(tmp)||Stemer(aspect).equals(Stemer(tmp)))
						return true;
				}		
			}
		if(frewords.length == 3)
		for(int i=0;i<frewords.length;++i)
			for(int j=0;j<frewords.length;++j)
				for(int k=0;k<frewords.length;++k)
		     	{
					if(i!=j&&i!=k&&j!=k)
					{
						String tmp = (frewords[i]+" "+frewords[j]+" "+frewords[k]);
						if(aspect.equals(tmp)||Stemer(aspect).equals(Stemer(tmp)))
							return true;
					}
			    }
		return false;
	}
	public List<IWord> synonym(String word) throws IOException//得到同义词
	{
		IIndexWord idxWord = dict.getIndexWord(word,POS.NOUN);
		if(idxWord==null) return null;
		IWordID wordID = idxWord.getWordIDs().get(0); // 1st meaning
		IWord word1 = dict.getWord(wordID);
		ISynset synset = word1.getSynset();
		System.out.println("word = "+word);
        for(IWord w:synset.getWords())
        {
        	System.out.println(w.getLemma());
        }
		return synset.getWords();
		
	}
	public static void main(String[] arg) throws IOException, InterruptedException {

		Evaluation t = new Evaluation();
		
		String productname = "Nokia_5800_XpressMusic.txt";
		
		//String str1 = "earphone";
        //if(t.synonym(str1).contains(IWor)) System.out.println("1234567");
		//Thread.sleep(100000);
		//System.out.println(Stemer("digital cameras"));
		//Thread.sleep(10000);
		
		//t.ReadFile(".\\AllReviews.txt");
		HashMap<String, Integer> frequentitemset = new HashMap<String, Integer>();
		HashMap<String, Integer> manualaspects = new HashMap<String, Integer>();
		
		int precision_deno = 0;
		
		File f = new File(".\\FinalSelectedAspects\\"+productname);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		int item1num=0,item2num=0,item3num=0;
		while ((line = br.readLine()) != null) {//读取候选aspect
			String[] words = line.split(":");
			System.out.println(words[0]);
			frequentitemset.put(words[0], Integer.parseInt(words[1]));
		}
		br.close();
		HashSet<String> clusterselecteditems = new HashSet<String>();
		for(String s1:frequentitemset.keySet())
			for(String s2:frequentitemset.keySet())
			{
				if(synset_pair.contains(new Synonym(s1,s2)))
					clusterselecteditems.add(s2);
			}
		for(String str:clusterselecteditems)
		{
			//frequentitemset.remove(str);//将语义上接近的删除
		}
		
		precision_deno = frequentitemset.size();
		System.out.println("precision_deno = "+precision_deno+" item1num =" +item1num+" item2num = "+item2num+" item3num = "+item3num+"tatoal = "+frequentitemset.size());
		
		f = new File(".\\ManualAspects\\"+productname);
		br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {//读取手工标注的aspect
			String[] word = line.split(":");
			System.out.println(line);
			manualaspects.put(word[0], Integer.parseInt(word[1]));
		}
		br.close();
		f = new File(".\\TransactionFiles\\"+productname);
		br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {//读取transaction中的行
			TransactionCont.add(line);
		}
		br.close();

		int recall_deno = 0;
		int recall_nume = 0;
		
		HashSet<String> stemfrequentitemset = new HashSet<String>();
		for(String s:frequentitemset.keySet())
		{
			stemfrequentitemset.add(Stemer(s));
		}
		for (String s : manualaspects.keySet()) {//计算召回率
			recall_deno++;
			if(frequentitemset.containsKey(s)) {
				recall_nume++;
				recalledaspect.add(s);
			    System.out.println("manual= " + s );
			}else {
				String sStemer = Stemer(s);
				boolean matched = false;
				for(String ff:stemfrequentitemset)
					if(ff.equals(sStemer))
					{
						matched = true;
						recall_nume++;recalledaspect.add(s);
						System.out.println("manual= " + s +" frequent= "+ff+" stemer= "+sStemer);
						break;
					}
				if(matched == false)
				{
					for(String ff:frequentitemset.keySet())
						if(match(ff,s))
						{
							recall_nume++;recalledaspect.add(s);
							System.out.println("manual= " + s +" frequent="+ff+" stemer="+sStemer);
							matched = true;
							break;
						}
				}
				if(matched == false) {
					List<IWord> synsets = t.synonym(s); 
					if(synsets != null)
					for(String ff : frequentitemset.keySet())
						{
							boolean flag = false;
							for(IWord w:synsets)							
								if(w.getLemma().toString().equals(ff)){
									recall_nume++;recalledaspect.add(s);
									System.out.println("manual= " + s +" frequent="+ff+" stemer="+sStemer);
									flag = true;
									break;
							}
							if(flag == true) break;		
						}
				}
				if(matched == false)
				{
					for(String ff : frequentitemset.keySet())
					{
						if(synset_pair.contains(new Synonym(ff,s)))
							{recall_nume++;recalledaspect.add(s);
							System.out.println("manual= " + s +" frequent="+ff+" stemer="+sStemer);
							break;}
					}		
				}
			}

		}
		int precision_nume = 0;
		
		System.out.println("start computing precision");

		HashSet<String> stemmanualaspects = new HashSet<String>();
		for(String s:manualaspects.keySet())
		{
			stemmanualaspects.add(Stemer(s));
		}
		precision_deno = frequentitemset.size();//准确率的分母
		for (String s : frequentitemset.keySet()) {//计算准确率
			
			if(manualaspects.containsKey(s)){
				System.out.println("frequent= "+s);
				precisionfrequent.add(s);
			precision_nume++;
			}else {
				String sStemer = Stemer(s);
				
				boolean matched = false;
					for(String ff:stemmanualaspects)
						if(ff.equals(sStemer))
						{
							matched = true;
							precision_nume++;precisionfrequent.add(s);
							System.out.println("frequent= "+s+" manual= " + ff +" stemer= "+sStemer);
							break;
						}
					if(matched == false){
					for(String ff : manualaspects.keySet())
						if(match(s,ff))
						{
							precision_nume++;precisionfrequent.add(s);
							System.out.println("frequent= "+s+" manual=" + ff +" stemer="+sStemer);
							matched = true;
							break;
						}
					}
					if(matched == false) {
						List<IWord> synsets = t.synonym(s); 
						if(synsets != null)
						for(String ff : manualaspects.keySet())
							{
								boolean flag = false;
								for(IWord w:synsets)							
									if(w.getLemma().toString().equals(ff)){
										precision_nume++;precisionfrequent.add(s);
										System.out.println("frequent= " + s +" manual="+ff+" stemer="+sStemer);
										flag = true;
										break;
								}
								if(flag == true) break;		
							}
					}
					if(matched == false)
					{
						for(String ff : manualaspects.keySet())
						{
							if(synset_pair.contains(new Synonym(ff,s)))
								{precision_nume++;precisionfrequent.add(s);
								System.out.println("frequent= " + s +" manual="+ff+" stemer="+sStemer);
								break;}
						}		
					}
				}

			}




		for(String s:manualaspects.keySet())
		{
			if(!recalledaspect.contains(s))
			{			
				int fre =0;
				for(String str:TransactionCont)
				{					
					if(sentence_sup(str,s))
						fre++;
				}
				System.out.println("not recalled manual aspect = "+s+" frequency = "+fre);
				
			}		
		}//end of for(manualaspects)
		for(String s:frequentitemset.keySet())
		{
			if(!precisionfrequent.contains(s))
			{			
				int fre =0;
				for(String str:TransactionCont)
				{					
					if(sentence_sup(str,s))
						fre++;
				}
				System.out.println("not precision frequent itemset = "+s+" frequency = "+fre);	
			}
		}//end of for(manualaspects)
		//********************输出实验结果****************//
		System.out.println("\nEvaluation product name = "+ productname+"\n");
		System.out.println("recall_deno=" + recall_deno);
		System.out.println("recall_nume=" + recall_nume);
		double recall = (double) (recall_nume) / recall_deno;
		System.out.println("Recall=" + recall);
		double precision = (double) precision_nume / precision_deno;
		System.out.println("precision_nume = "+precision_nume+" precision_deno = "+precision_deno+"\nPrecision=" + precision);
		System.out.println("F1 = " + (2 * recall * precision)
				/ (recall + precision));
	}
	}
