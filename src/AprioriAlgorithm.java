import java.io.*;
import java.util.*;

public class AprioriAlgorithm {
	static String trans_set[];
	double minsup = 0.003;
	double minconf = 0.1;
	int item_counts = 0;
	int freq_top;

	TreeSet max_freq = new TreeSet();
	TreeSet item1_cand = new TreeSet();
	static TreeSet cand_set[];
	HashMap Associate = new HashMap();
	static TreeSet[] freq_set;
	private static HashSet<String> items = new HashSet<String>();
    private static HashSet<String> aspectsStopwords = new HashSet<String>();
    private static HashMap<String,Integer> FrequentItemset = new HashMap<String,Integer>();
    
    private static void ReadAspectStopwordFile(String fpath) throws IOException
    {
    	File f = new File(fpath);
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {
			aspectsStopwords.add(line);
		}
    }
	public static void main(String[] args) throws IOException, InterruptedException {
		
		//File f = new File(".\\TransactionFiles\\"+args[1]);//args[1]为productname
		File f = new File("H:/阿里大数据竞赛/shoppinggoods.txt");//args[1]为productname
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(f));
		int line_num = 0;
		int item_count = 0;
		while ((line = br.readLine()) != null) {
			String[] item = line.split(" ");
			item_count += item.length;
			for (String i : item)
				items.add(i);//将transaction中的item均为小写
			line_num++;
		}
		System.out
				.println("line_num=" + line_num + " item_count=" + item_count);
		br.close();
		br = new BufferedReader(new FileReader(f));
		trans_set = new String[line_num];
		cand_set = new TreeSet[item_count];
		freq_set = new TreeSet[item_count];
		line_num = 0;
		while ((line = br.readLine()) != null) {
			trans_set[line_num++] = line;
		}
		ReadAspectStopwordFile(".\\Stopwords");//读取停止词 transaction文件中一般不含有停止词了
		AprioriAlgorithm AP = new AprioriAlgorithm();	
		
		AP.run();
	}

	public AprioriAlgorithm() {
		max_freq = new TreeSet();
		item_counts = counts();
		for (int i = 0; i < item_counts; i++) {
			freq_set[i] = new TreeSet();
			cand_set[i] = new TreeSet();
		}
		cand_set[0] = item1_cand;
	}

	public void run() throws IOException, InterruptedException {
		int k = 1;
		G1_item();// 满足支持度的k=1的item
		do {
			k++;
			Gk_item(k);
			Gk_freq(k);
		} while (k <= 2);
		freq_top = k - 1;
		print_tran();
		P_freq();
		String fpath = ".\\FrequentItemset.txt";
		File f = new File(fpath);
		if(f.exists()) f.delete();
		f.createNewFile();
		for(String s:FrequentItemset.keySet())
		{
			WriteFile(f,s+":"+FrequentItemset.get(s)+"\r\n");
		}
		// Max_associate();
		// P_Max_freq();
		// G_associate();
		// P_associate();
	}

	void print_tran()
	// 输出事务数据库事务集和最小支持度、最小置信度
	{
		System.out.println("事务数据库中的事务集如下:");
		for (int i = 0; i < trans_set.length; i++)
			System.out.println(trans_set[i]);
		System.out.println("\n");
		System.out.print("最小支持度minsup=");
		System.out.println(minsup);
		System.out.print("最小置信度minconf=" + minconf);
		System.out.println("\n");
	}
	private  boolean isNumeric(String str){
		  for (int i = str.length();--i>=0;){  
		   if (!Character.isDigit(str.charAt(i))){
		    return false;
		   }
		  }
		  return true;
		}
	private int counts() {// 计算1-候选集个数,不包括重复的
		String c1 = new String("a");
		String[] c2 = null;
		int i, j, m;
		for (i = 0; i < trans_set.length; i++) {
			c1 = trans_set[i];
			c2 = c1.split(" ");
			for (j = 0; j < c2.length; j++) {
				//if(!aspectsStopwords.contains(c2[j].toLowerCase())&&!isNumeric(c2[j]))
				item1_cand.add(c2[j]);
			}
		}
		System.out.println("item1_cand size=" + item1_cand.size());
		return item1_cand.size();
	}

	double count_sup(String x) throws InterruptedException {
		// 计算项目集支持度
		int temp = 0;
		for (int i = 0; i < trans_set.length; i++) {
			String[] items = x.split(" ");
			String[] transactionitems = trans_set[i].split(" ");
			HashSet<String> tmp = new HashSet<String>();
			for(String ti:transactionitems)
			   tmp.add(ti);
			for (int j = 0; j < items.length; j++) {
				if (!tmp.contains(items[j]))//
					break;
				else if (j == (items.length - 1))
					{temp++;//if(x.contains("mac osx")) {System.out.println("temp="+temp+" "+trans_set[i]);Thread.sleep(5000);}
					}
			}
		}
		//if(x.equals("ive")) {System.out.println("temp="+temp);Thread.sleep(10000);}
		return temp;
	}

	public void G1_item() throws InterruptedException, IOException {// 产生1候选集合和频繁1-项集
		double m = 0;
		String t = "";
		Iterator temp = cand_set[0].iterator();
		while (temp.hasNext()) {
			t = (String) temp.next();
			m = count_sup(t);// 计算item t的支持度
			if (m >= minsup * trans_set.length) {
				freq_set[0].add(t);
				if(FrequentItemset.containsKey(t))
				FrequentItemset.put(t, FrequentItemset.get(t)+(int)m);
				else FrequentItemset.put(t, (int)m);
			}
		}
	}

	public void Gk_item(int k) throws InterruptedException// 由k-1频繁集生成k候选集
	{
		String[] y, z;
		TreeSet T = new TreeSet();
		Iterator t1 = freq_set[k - 2].iterator();// k-1候选集迭代器
		for (int i = 0; i < freq_set[k - 2].size() - 1; i++) {
			String str = (String) t1.next();
			y = str.split(" ");
			int j = k - 1;
			//t1 = freq_set[k - 2].iterator();
			while (t1.hasNext() || (j == k - 1)){
				z = ((String) t1.next()).split(" ");
				for (j = 0; j < y.length - 1; j++) {
					if (!y[j].equals(z[j]))
						break;
				}
				if ((j == y.length - 1) && (!y[j].equals(z[j]))) {
					T.add(str + " " + z[j]);
					if(str.equals("digital")&&z[j].equals("camera")||str.equals("camera")&&z[j].equals("digital")) 
					{System.out.println("digital camera="+str + " " + z[j]);Thread.sleep(5000);}
				}
			}// end of while
			t1 = freq_set[k - 2].iterator();
			for (j = i; j >= 0; j--)
				t1.next();
		}
		System.out.println("cand_set " + k + " size=" + T.size());
		cand_set[k - 1] = T;// 生成的k候选集
	}

	public void Gk_freq(int k) throws InterruptedException, IOException {// 由k候选集产生频繁k项集
		String s = "";
		Iterator ix = cand_set[k - 1].iterator();
		while (ix.hasNext()) {
			s = (String) ix.next();
			double m = count_sup(s);
			if ( m>= (minsup * trans_set.length)) {
				freq_set[k - 1].add(s);
				if(FrequentItemset.containsKey(s))
					FrequentItemset.put(s, FrequentItemset.get(s)+(int)m);
					else FrequentItemset.put(s, (int)m);
			}
		}//end of while
	}

	public boolean Freq_is_empty(int k) {
		if (freq_set[k - 1].isEmpty())
			return true;
		else
			return false;
	}

	public boolean included(String s1, String s2) {
		for (int i = 0; i < s1.length(); i++) {
			if (s2.indexOf(s1.charAt(i)) == -1)
				return false;
			else if (i == s1.length() - 1)
				return true;
		}
		return true;
	}

	public void Max_associate() {// 求最大频繁项集
		int i, j;
		String temp = "", temp1 = "", temp2 = "";
		Iterator iterator, iterator1, iterator2;
		for (i = 0; i < freq_top; i++) {
			max_freq.addAll(freq_set[i]);
		}
		for (i = 0; i < freq_top; i++) {
			iterator1 = freq_set[i].iterator();
			while (iterator1.hasNext()) {
				temp1 = (String) iterator1.next();
				for (j = i + 1; j < freq_top; j++) {
					iterator2 = freq_set[j].iterator();
					while (iterator2.hasNext()) {
						temp2 = (String) iterator2.next();
						if (included(temp1, temp2))
							max_freq.remove(temp1);
					}
				}
			}
		}
	}

	public void P_Max_freq() {
		Iterator iterator = max_freq.iterator();
		System.out.print("最大频繁项集:");
		while (iterator.hasNext()) {
			System.out.print((String) iterator.next() + "*");
		}
		System.out.println("\n");
	}

	private void WriteFile(File f,String content) throws IOException {
	
		BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
		bw.write(content);
		bw.flush();
		bw.close();
	}

	void P_freq() throws IOException {
		
		String fpath = ".\\apriori-output.txt";
		File f = new File(fpath);
		if(f.exists()) f.delete();
		f.createNewFile();
		System.out.println("产生的频繁项集为:" + freq_set[0].size());
		//for (int i = 0; i < freq_set[0].size(); i++) {
		for (int i = 0; i < 3; i++) {
			Iterator iy = freq_set[i].iterator();
			if (iy.hasNext())
				System.out.println("频繁" + (i + 1) + "项集" + ";");
			//WriteFile("频繁" + (i + 1) + "项集" + ":\r\n");
			while (iy.hasNext()) {
				String item = (String) iy.next();
				System.out.println(item);
				WriteFile(f,item+"\r\n");
			}
			System.out.println("\n-----" + (i + 1) + "---------------");
		}
	}

	public void G_sub(String s) throws InterruptedException {
		String x = "", y = "";
		for (int i = 1; i < (1 << s.length()) - 1; i++) {
			for (int j = 0; j < s.length(); j++) {
				if (((1 << j) & i) != 0) {
					x += s.charAt(j);
					y = s.replaceAll(x, "");
				}
			}
			if (count_sup(s) / count_sup(x) >= minconf) {
				Associate.put(x, s);
			}
			x = "";
			y = "";
		}
	}

	public void G_associate() throws InterruptedException { // 从给定的频繁项目集中生成强关联规则
		String s;
		Iterator iterator = max_freq.iterator();
		while (iterator.hasNext()) {
			s = (String) iterator.next();
			G_sub(s);
		}
	}

	public void P_associate() {
		String x, s, y;
		double temp = 0;
		Set hs = Associate.keySet();
		Iterator iterator = hs.iterator();
		System.out.println("产生的关联规则为:");
		while (iterator.hasNext()) {
			x = (String) iterator.next();
			s = (String) Associate.get(x);
			y = s;
			for (int i = 0; i < x.length(); i++) {
				y = y.replace(x.charAt(i), ' ');
			}
			System.out.println(x + "==>" + y + "\t");
		}
	}
}
