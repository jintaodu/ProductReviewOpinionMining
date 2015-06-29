package OpinionRetrieval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Statistic {

	/**
	 * @param args
	 */
	private final String regularExpression = "[^^a-zA-Z0-9|$|\\-|']";
	
	public void queryStatistic()throws IOException{
		File f = new File("Query/queries.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		long totallength = 0;
		long totalaspectnumber = 0;
		while((line = br.readLine())!=null){
			if(line.length() > 0){
				String[] info = line.split("####");
				
				totalaspectnumber += info[3].split("#").length;
				String domain = info[1];
				String productname = info[2];
				String query = info[0].toLowerCase();
				String[] aspects = info[3].toLowerCase().split("#");//aspect之间用#隔开
				
				QueryExpansion qe = new QueryExpansion(domain, productname);
				
				for(String aspect : aspects){
					
					query += " "+qe.queryexpansion(aspect);
					String expandedpart = qe.queryexpansion(aspect);
					String[] words = query.split(regularExpression);
					HashSet<String> qwords = new HashSet();
					for(String w : words) qwords.add(w);
					if( ! qwords.contains(aspect) ) query += " "+aspect;//此时的aspect不在query中，很可能两个词是同义词
					
				}
				
				String[] words = query.split(regularExpression);
				totallength += words.length;
				
				totalaspectnumber += query.split("\\|\\|").length+1;
				
			}
		}
		System.out.println((double)totallength/55);
		System.out.println((double)totalaspectnumber/55);
		long totalresults = 0;
		f = new File("C:/Users/mmdb/Desktop/TopORResult/1");
		String[] list = f.list();
		for(String fn : list){
			if(fn.endsWith(".txt")){
				System.out.println(fn);
				br = new BufferedReader(new FileReader(f.getAbsolutePath()+"/"+fn));
				while((line = br.readLine()) != null){
					if(line.contains("query=")) totalresults++;
				}
			}
		}
		
		System.out.println(" totalresults ="+totalresults);
	}
	
	public boolean check(ArrayList<String> oneproduct, String domain){
		
		HashSet<String> words = new HashSet<String>();
		for(int i = 1; i < oneproduct.size(); ++i){
			String[] splits = oneproduct.get(i).split(regularExpression);
			words.clear();
			for(String s : splits)
				words.add(s);
			if(words.contains(domain))return true;
		}
		return false;
	
	}
	
	public void helpfulnessData(String domain) throws IOException{
		File f = new File("HelpfulnessModelData/"+domain+"/rawdata.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line  = null;
		long totallength = 0;
		int minlength = 100000;
		int maxlength = 0;
		HashMap<String,Integer> products = new HashMap<String,Integer>();
		while((line = br.readLine())!=null){
			String[] info = line.toLowerCase().split("\t");
			if(products.containsKey(info[1])){
				products.put(info[1] , products.get(info[1])+1);
			}else{
				products.put(info[1] , 1 );
			}
			
			int length = info[7].split(regularExpression).length;
			if(length > maxlength) maxlength = length;
			if(length < minlength) minlength = length;
			totallength += length;
		}
		
		int Minreviews = 1000;
		int Maxreviews = 0;
		for(String p : products.keySet()){
			if(products.get(p) > Maxreviews) Maxreviews = products.get(p);
			if(products.get(p) < Minreviews) Minreviews = products.get(p);
		}
		
		System.out.println("Ave Reviews/Produc ="+500.00/products.size());
		System.out.println("totalproducts="+products.size());
		System.out.println("Minreviews="+Minreviews);
		System.out.println("Maxreviews="+Maxreviews);

	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		Statistic statistic = new Statistic();
		//statistic.helpfulnessData("camera");
		statistic.queryStatistic();
	}

}
