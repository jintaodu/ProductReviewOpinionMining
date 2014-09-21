import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

public class OpinionAspectRelation {

	private static HashSet<String> Stopwords = new HashSet<String>();// 普通常用的停止词
	private HashMap<String, Integer> around_opinionword_num = new HashMap<String, Integer>();// 计算每个单词周围的形容词数量
	private int scanspan = 5;// 从noun向前向后看的词的数量，用来寻找opinionwords
	private static String currentproductname = "";// 当前遍历的产品名字

	HashSet<String> opinioncandidates = new HashSet<String>();// opinion的候选词
	HashSet<String> aspectcandidates = new HashSet<String>();// aspect的候选词
	HashMap<String, Integer> opinioncandidatesubscript = new HashMap<String, Integer>();// opinioncandidates及其数值表示
	HashMap<String, Integer> aspectcandidatesubscript = new HashMap<String, Integer>();// aspectcandidates及其数值表示

	private static void ReadAspectStopwordFile(String fpath) throws IOException {
		File f = new File(fpath);
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {
			Stopwords.add(line.trim());
		}
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

	private int countopinionwords(String posresult, int position)// 计算position位置的前后有所少个形容词
	{
		int count = 0;
		String[] taggedwords = posresult.split(" ");
		int i = scanspan;
		if ((position - scanspan) >= 0) {
			while (i >= 0) {
				if (taggedwords[position - i].contains("_jj")
						|| taggedwords[position - i].contains("_jjr")
						|| taggedwords[position - i].contains("_jjs")) {
					opinioncandidates.add(taggedwords[position - i].substring(
							0, taggedwords[position - i].indexOf('_')));
					count++;
				}
				i--;
			}
		}
		if ((position + scanspan) < taggedwords.length) {
			i = scanspan;
			while (i > 0) {
				if (taggedwords[position + i].contains("_jj")
						|| taggedwords[position + i].contains("_jjr")
						|| taggedwords[position + i].contains("_jjs")) {
					opinioncandidates.add(taggedwords[position + i].substring(
							0, taggedwords[position + i].indexOf('_')));
					count++;
				}
				i--;
			}
		}
		if ((position - scanspan) < 0) {
			i = position;
			while (i > 0) {
				if (taggedwords[position - i].contains("_jj")
						|| taggedwords[position - i].contains("_jjr")
						|| taggedwords[position - i].contains("_jjs")) {
					opinioncandidates.add(taggedwords[position - i].substring(
							0, taggedwords[position - i].indexOf('_')));
					count++;
				}
				i--;
			}
		}
		if ((position + scanspan) >= taggedwords.length) {
			i = taggedwords.length - position - 1;
			while (i > 0) {
				if (taggedwords[position + i].contains("_jj")
						|| taggedwords[position + i].contains("_jjr")
						|| taggedwords[position + i].contains("_jjs")) {
					opinioncandidates.add(taggedwords[position + i].substring(
							0, taggedwords[position + i].indexOf('_')));
					count++;
				}
				i--;
			}

		}

		return count;
	}

	private int findopinionwords(String posresult, int position,
			Vector<String> opinion)// 计算position位置的前后有多少个形容词
	{
		int count = 0;
		String[] taggedwords = posresult.split(" ");
		int i = scanspan;
		if ((position - scanspan) >= 0) {
			while (i >= 0) {
				if (taggedwords[position - i].contains("_jj")
						|| taggedwords[position - i].contains("_jjr")
						|| taggedwords[position - i].contains("_jjs")) {
					opinion.add(taggedwords[position - i].substring(0,
							taggedwords[position - i].indexOf('_')));
					count++;
				}
				i--;
			}
		}
		if ((position + scanspan) < taggedwords.length) {
			i = scanspan;
			while (i > 0) {
				if (taggedwords[position + i].contains("_jj")
						|| taggedwords[position + i].contains("_jjr")
						|| taggedwords[position + i].contains("_jjs")) {
					opinion.add(taggedwords[position + i].substring(0,
							taggedwords[position + i].indexOf('_')));
					count++;
				}
				i--;
			}
		}
		if ((position - scanspan) < 0) {
			i = position;
			while (i > 0) {
				if (taggedwords[position - i].contains("_jj")
						|| taggedwords[position - i].contains("_jjr")
						|| taggedwords[position - i].contains("_jjs")) {
					opinion.add(taggedwords[position - i].substring(0,
							taggedwords[position - i].indexOf('_')));
					count++;
				}
				i--;
			}
		}
		if ((position + scanspan) >= taggedwords.length) {
			i = taggedwords.length - position - 1;
			while (i > 0) {
				if (taggedwords[position + i].contains("_jj")
						|| taggedwords[position + i].contains("_jjr")
						|| taggedwords[position + i].contains("_jjs")) {
					opinion.add(taggedwords[position + i].substring(0,
							taggedwords[position + i].indexOf('_')));
					count++;
				}
				i--;
			}

		}
		 // System.out.println(opinion.size()+" count = "+ count);
		return count;
	}

	private void insert(String aspect, int count)// 记录每一个候选aspect周围有多少形容词
	{
		if (around_opinionword_num.containsKey(aspect))
			around_opinionword_num.put(aspect,
					around_opinionword_num.get(aspect) + count);
		else {
			around_opinionword_num.put(aspect, count);
		}
	}

	public void PosTagAnalysis_Adjacent_Threshold(String fpath, int threshold)//使用adjacent的阈值方法
			throws IOException {
		around_opinionword_num.clear();
		File f = new File(fpath);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String posresult = null;
		while ((posresult = br.readLine()) != null) {
			String[] taggedwords = posresult.toLowerCase().split(" ");
			String nouns = "";
			for (int i = 0; i < taggedwords.length; ++i) {
				String taggedword = taggedwords[i];
				String[] word_tag = taggedword.split("_");
				if ((word_tag[1].equals("nn") || word_tag[1].equals("nnp")
						|| word_tag[1].equals("nns")
						|| word_tag[1].equals("np") || word_tag[1]
						.equals("nnps"))) {
					 insert(word_tag[0].toLowerCase(),countopinionwords(posresult.toLowerCase(),i));
					//nouns += word_tag[0] + " ";
				} /*else {
					if (nouns.length() > 0)
						insert(nouns.trim().toLowerCase(),
								countopinionwords(posresult.toLowerCase(), i - 1));
					nouns = "";
				}*/
			}// end of for
		}
		Saveresult(around_opinionword_num, threshold);

	}

	public void PosTagAnalysis_Adjacent_SPCA(String fpath )//使用adjacent和spca结合
			throws IOException {
		
		around_opinionword_num.clear();
		File f = new File(fpath);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String posresult = null;
		while ((posresult = br.readLine()) != null) {
			String[] taggedwords = posresult.toLowerCase().split(" ");
			String nouns = "";
			for (int i = 0; i < taggedwords.length; ++i) {
				String taggedword = taggedwords[i];
				String[] word_tag = taggedword.split("_");
				if ((word_tag[1].equals("nn") || word_tag[1].equals("nnp")
						|| word_tag[1].equals("nns")
						|| word_tag[1].equals("np") || word_tag[1]
						.equals("nnps"))) {
					insert(word_tag[0],
							countopinionwords(posresult.toLowerCase(), i));
					aspectcandidates.add(word_tag[0]);
				}
			}// end of for
		}
		File manualaspectfile = new File(".\\ManualAspects\\"+currentproductname);
		HashSet<String> manualaspects = new HashSet<String>();
		BufferedReader brmanualaspectfile = new BufferedReader(new FileReader(manualaspectfile));
		String line = null;
		while((line = brmanualaspectfile.readLine())!=null)
		{
			manualaspects.add(line.substring(0,line.indexOf(':')));
		}

		
        Vector<String> nounzeroopinion = new Vector<String>();//记录哪些名词没有被形容词修饰
        
        System.out.println("around_opinionword_num size before remove = " + around_opinionword_num.size());
        
        for(String s :around_opinionword_num.keySet())
        	if(around_opinionword_num.get(s) == 0)
        		nounzeroopinion.add(s);
        for(String s: nounzeroopinion)
        	around_opinionword_num.remove(s);
        
        System.out.println("around_opinionword_num size after remove = " + around_opinionword_num.size());
        
		System.out.println("周围没有形容词修饰的名词为手工标注的aspect：");
		for(String s:nounzeroopinion)
		{
			if(manualaspects.contains(s))
			System.out.println(s);
		}
        		
		//给每一个adj和noun付一个整数下标
		Integer num = 0;
		for (String s : opinioncandidates)//
			opinioncandidatesubscript.put(s, num++);
		for (String s : around_opinionword_num.keySet())
			aspectcandidatesubscript.put(s, num++);

		int aspectstartsubscript = opinioncandidatesubscript.size();
		Vector<Vector<String>> opinions = new Vector<Vector<String>>();
		for (int i = 0; i < aspectcandidatesubscript.size(); ++i)
			opinions.add(new Vector<String>());

		br = new BufferedReader(new FileReader(f));

		while ((posresult = br.readLine()) != null) {
			String[] taggedwords = posresult.toLowerCase().split(" ");
			String nouns = "";
			for (int i = 0; i < taggedwords.length; ++i) {
				String taggedword = taggedwords[i];
				String[] word_tag = taggedword.split("_");
				if ((word_tag[1].equals("nn") || word_tag[1].equals("nnp")
						|| word_tag[1].equals("nns")
						|| word_tag[1].equals("np") || word_tag[1]
						.equals("nnps"))) {
					if(aspectcandidatesubscript.containsKey(word_tag[0]))
					insert(word_tag[0],
							findopinionwords(posresult.toLowerCase(), i,
									opinions.get(aspectcandidatesubscript
											.get(word_tag[0])
											- aspectstartsubscript)));
				}
			}// end of for
		}

		File opinionwords = new File(".\\AroundOpinionwords\\"
				+ currentproductname);
		if (opinionwords.exists())
			opinionwords.delete();
		opinionwords.createNewFile();

		BufferedWriter bwopinionwords = new BufferedWriter(new FileWriter(
				opinionwords, true));

		HashMap<String, Integer> opinionnum = new HashMap<String, Integer>();

		for (String noun : aspectcandidatesubscript.keySet()) {
			opinionnum.clear();
			Vector<String> tmp = opinions.get(aspectcandidatesubscript
					.get(noun) - aspectstartsubscript);

			int k = tmp.size();
			
				for (int i = 0; i < k; ++i)
					if (!opinionnum.containsKey(tmp.get(i)))
						opinionnum.put(tmp.get(i), 1);
					else
						opinionnum.put(tmp.get(i),
								i + opinionnum.get(tmp.get(i)));
				bwopinionwords.write(noun + "::::");
				bwopinionwords.flush();

				ArrayList<Entry<String, Integer>> array = new ArrayList<Entry<String, Integer>>(
						opinionnum.entrySet());
				 //System.out.println("array " +
				 //array.size()+"  "+"opinionnum = "+opinionnum.size());
				Collections.sort(array,
						new Comparator<Map.Entry<String, Integer>>() {
							public int compare(Map.Entry<String, Integer> o1,
									Map.Entry<String, Integer> o2) {
								if (o2.getValue() - o1.getValue() > 0)
									return -1;
								else if (o2.getValue() - o1.getValue() == 0)
									return 0;
								else
									return 1;
							}
						});

				for (Entry<String, Integer> e : array) {
					// bw.write(e.getKey()+"  "+e.getValue()+"\r\n");
					bwopinionwords.write(e.getKey() + ":::" + e.getValue()
							+ "::::");
					bwopinionwords.flush();
				}
				bwopinionwords.write("\r\n");
				bwopinionwords.flush();

		}
		
		ArrayList<Entry<String, Integer>> array = new ArrayList<Entry<String, Integer>>(
				aspectcandidatesubscript.entrySet());
		 //System.out.println("array " +
		 //array.size()+"  "+"opinionnum = "+opinionnum.size());
		Collections.sort(array,
				new Comparator<Map.Entry<String, Integer>>() {
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
		File matrix = new File(".\\Stage1\\matrix.txt");
		if(matrix.exists()) matrix.delete(); matrix.createNewFile();
		BufferedWriter bwmatrix = new BufferedWriter(new FileWriter(matrix,true));
		
		File nouns = new File(".\\Stage1\\nouns.txt");
		if(nouns.exists()) nouns.delete(); nouns.createNewFile();
		BufferedWriter bwnouns = new BufferedWriter(new FileWriter(nouns,true));
        for(Entry<String,Integer> e:array)
        {
        	bwnouns.write(e.getKey()+"\r\n");
        	bwnouns.flush();
        	opinionnum.clear();
        	Vector<String> tmp = opinions.get(aspectcandidatesubscript.get(e.getKey()) - aspectstartsubscript);
        	int k = tmp.size();			
			for (int i = 0; i < k; ++i)
				if (!opinionnum.containsKey(tmp.get(i)))
					opinionnum.put(tmp.get(i), 1);
				else
					opinionnum.put(tmp.get(i),
							i + opinionnum.get(tmp.get(i)));
			for(String s: opinioncandidates)
			{
				if(opinionnum.containsKey(s))
				{
					bwmatrix.write(opinionnum.get(s) + " ");		
				} else bwmatrix.write("0.0 ");
				bwmatrix.flush();
			}
				
			
			bwmatrix.write("\r\n");
			bwmatrix.flush();
			
        }
		bwopinionwords.close();

	}

	public void Saveresult(HashMap<String, Integer> result, int threshold)
			throws IOException {// 将hashmap中的元素排序后打印输出,Adjacent方法用此函数
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

		File f = new File(".\\FinalSelectedAspects\\" + currentproductname);
		if (f.exists())
			f.delete();
		f.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));

		for (Entry<String, Integer> e : array) {
			if (e.getValue() >= threshold) {
				bw.write(e.getKey() + ":" + e.getValue() + "\r\n");
				bw.flush();
			}
		}
		bw.close();
	}

	private void finalselectedaspects(String productname,double threshold) throws IOException {//PCA方法用此函数
		File f = new File(".\\FinalSelectedAspects\\" + productname);
		if (f.exists())
			f.delete();
		f.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));

		File spca = new File(".\\Stage1\\SPCA.txt");
		File nouns = new File(".\\Stage1\\nouns.txt");

		BufferedReader brspca = new BufferedReader(new FileReader(spca));
		BufferedReader brnouns = new BufferedReader(new FileReader(nouns));
		String line1 = null;
		String line2 = null;
		while ((line1 = brspca.readLine()) != null
				&& (line2 = brnouns.readLine()) != null) {
			if (Math.abs(Double.parseDouble(line1)) >= threshold) {
				bw.write(line2 + ":1\r\n");
				bw.flush();
			}
		}
		bw.close();
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		ReadAspectStopwordFile(".\\Stopwords");

		String[] productname = new String[] { "Apple_iPod_Touch_2.txt",
				"Apple_MacBook_Pro.txt", "BlackBerry_Bold_9700.txt",
				"Canon_EOS.txt", "Fujifilm_Finepix.txt", "iPhone_3GS_16GB.txt","Nokia_5800_XpressMusic.txt","Nokia_N95.txt","Panasonic_Lumix_DMC.txt",
				"Samsung_NC10.txt", "Sony_NWZ_16GB.txt","Canon G3.txt"
				};
		int[] thresholdone = new int[] { 28, 9, 28, 5, 5, 72, 271, 92, 7, 18, 5 };// 每个商品对应一个阈值
		int[] thresholdmany = new int[] { 18, 7, 17, 5, 5, 43, 103, 56, 7, 17, 5 };// 每个商品对应一个阈值
		double[] thresholdSPCA = new double[] { 0.0, 0.0, 0.0, 1.40e-3, 0.15e-3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };// 每个商品对应一个阈值
		File f = new File(".\\TaggedResults");
		OpinionAspectRelation OAR = new OpinionAspectRelation();

            int productid = productname.length -1;
			currentproductname = productname[productid];
			System.out.println("Productname = "+ currentproductname);
			//OAR.PosTagAnalysis_Adjacent_Threshold(".\\TaggedResults\\" + currentproductname,thresholdone[productid]);
			
			OAR.PosTagAnalysis_Adjacent_SPCA(".\\TaggedResults\\" + currentproductname);
			//OAR.finalselectedaspects(currentproductname,thresholdSPCA[productid]);

	}

}
