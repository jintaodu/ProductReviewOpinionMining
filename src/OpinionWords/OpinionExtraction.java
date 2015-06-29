package OpinionWords;

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
import java.util.Map;
import java.util.Map.Entry;

public class OpinionExtraction {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public OpinionExtraction(){}
	public void extract_original_opinions(String fpath, String productname) throws IOException
	{
		HashMap<String,Integer> opinions = new HashMap<String,Integer>();
		
		File f = new File(fpath);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		
		while ((line = br.readLine()) != null) {
			//System.out.println(line);
			String[] words = line.trim().toLowerCase().split(" ");
			for(String w : words){
				if(w.endsWith("_jj")||w.endsWith("_jjr")||w.endsWith("_jjs")){
					String word = w.substring(0, w.indexOf("_"));
					if(opinions.containsKey(word)){
						opinions.put(word, opinions.get(word)+1);
					}else{
						opinions.put(word, new Integer(1));
					}
				}//end if
			}
		}
		br.close();
		ArrayList<Entry<String, Integer>> array = new ArrayList<Entry<String, Integer>>(
				opinions.entrySet());

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
		BufferedWriter bw1 = new BufferedWriter(new FileWriter("./ManualOpinionsOriginal/"+productname));//作为情感词的手工标注初步结果，先统计词频较高的，然后再手工挑选
		BufferedWriter bw2 = new BufferedWriter(new FileWriter("./OpinionsPos/"+productname));
		for (Entry<String, Integer> e : array) {
			System.out.println(e.getKey() + ":" + e.getValue()+"\r\n");
			bw1.write( e.getKey() + ":" + e.getValue()+"\r\n" ); bw1.flush();
			bw2.write( e.getKey() + "<---->" + e.getValue()+"\r\n" );  bw2.flush();
		}
		bw1.close(); 
		bw2.close();
	}
	
	public void Hu_method_opinion_extract(String fpath, String productname) throws IOException{
		
		
		HashMap<String,Integer> opinions = new HashMap<String, Integer>();
		HashSet<String> aspects = new HashSet<String>();
		BufferedReader aspectsreader = new BufferedReader(new FileReader("./FinalSelectedAspects/"+productname));
		String line = null;
		while ((line = aspectsreader.readLine()) != null){
			aspects.add(line.substring(0, line.indexOf(":")));
		}
		
		File f = new File(fpath);
		BufferedReader br = new BufferedReader(new FileReader(f));

		while ((line = br.readLine()) != null) {
			//System.out.println(line);
			String[] words = line.trim().toLowerCase().split("::::");
			System.out.println(line);
			for(int i = 1; i < words.length; ++i ){
				String[] splits = words[i].split(":::");
				if(aspects.contains(words[0])){
					String word = splits[0];
					int count = Integer.parseInt(splits[1]);
					if(opinions.containsKey(word)){
						opinions.put(word, opinions.get(word)+count);
					}else{
						opinions.put(word, count);
					}
				}
				
			}//for
		}
		br.close();
		ArrayList<Entry<String, Integer>> array = new ArrayList<Entry<String, Integer>>(
				opinions.entrySet());

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
		
		BufferedWriter bw1 = new BufferedWriter(new FileWriter("./FinalSelectedOpinions/"+productname));
		for (Entry<String, Integer> e : array) {
			System.out.println(e.getKey() + ":" + e.getValue()+"\r\n");
			if(e.getValue() > 60000){
				bw1.write( e.getKey() + ":" + e.getValue()+"\r\n" );
			}
			
		}
		bw1.close();
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		OpinionExtraction oe = new OpinionExtraction();
		String productname = "Phone.txt";
		File dir = new File("./TaggedResultsMerge");
		//oe.extract_original_opinions("./TaggedResults/"+productname, productname);
		oe.Hu_method_opinion_extract("./AroundOpinionwords/"+productname, productname);
		
	}

}
