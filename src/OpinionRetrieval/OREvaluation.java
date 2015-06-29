package OpinionRetrieval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class OREvaluation {

	/**
	 * @param args
	 * @20150111
	 */
	private List<Document> idealList = null;
	private List<Document> retrievalList = null;
	
	private static double ndcg5 = 0.0;
	private static double ndcg10 = 0.0;
	private static double ndcg15 = 0.0;
	private static double ndcg20 = 0.0;
	private static double map = 0.0;
	private static double p1 = 0.0;
	private static double p5 = 0.0;
	private static double p10 = 0.0;
	private static double p20 = 0.0;
	
	private double logab(int a, int b){
		return Math.log10(b)/Math.log10(a);
	}
	private double IDCG(List<Document> list, int k){
		if( k <= 0){ 
			System.err.println("IDCG 参数k错误");
			return Double.MAX_VALUE;
		}
		double result = 0.0;
		for (int i=0; i < k; ++i){
			result += (Math.pow(2.0, list.get(i).manualAnnotationScore) - 1) / logab(2, 2+i);
		}
		//System.out.println("IDCG="+result);
		return result;
	}
	
	public double DCG(List<Document> list, int k){
		
		double result = 0.0;
		for (int i=0; i < k; ++i){
			result += (Math.pow(2.0, list.get(i).manualAnnotationScore) - 1) / logab(2, 2+i);
		}
		return result;
	}
	private double get_ManualAnnotationScore(List<Document> idealList, String documentname) throws Exception{
		for(Document doc : idealList){
			if(doc.documentname.equals(documentname))
				return doc.manualAnnotationScore;
		}
		//return 1.0;
		throw new Exception("检索结果在手工标注结果中找不到doc: "+documentname);//找不到时可以看出找不到的信息。
	}
	
	public void getidealList(String query) throws IOException{
		idealList = readTopORResult(query.split("####")[0]+".txt");
	}
	public void getretrievalList(String query, boolean queryexpansion, String ModelType)throws Exception{
		OpinionRetrieval or = new OpinionRetrieval();
		
		if(ModelType.equals("Rerank")){
			
			String filename = URLEncoder.encode(query.split("####")[0].toLowerCase(), "UTF-8");
			HashMap<String,Double> Scores = new HashMap<String,Double>();
			File f = new File("ORResults/"+filename+".txt");
			BufferedReader br = new BufferedReader(new FileReader(f));
			retrievalList  = new ArrayList<Document>();
			String line = null;
			while((line = br.readLine()) != null){
				Document doc = new Document(line);
				retrievalList.add(doc);
			}

		}else{
			retrievalList = or.retrieveNow(query, queryexpansion, ModelType);
		}
		
	}
	
	public void NDCG() throws Exception{
		
		for(Document doc : retrievalList)
			doc.manualAnnotationScore = get_ManualAnnotationScore(idealList, doc.documentname);
		for(int i=0; i< 25; i++)
			System.out.print(idealList.get(i).manualAnnotationScore+"   ");
		System.out.println();
		for(Document doc : retrievalList)
			System.out.print(doc.manualAnnotationScore+"   ");
		System.out.println();
		
		//System.out.println("NDCG@1="+DCG( retrievalList, 1 )/IDCG( idealList, 1 ));
		double ndcg5local = DCG( retrievalList, 5 )/IDCG( idealList, 5 );
		double ndcg10local = DCG( retrievalList, 10 )/IDCG( idealList, 10 );
		double ndcg15local = DCG( retrievalList, 15 )/IDCG( idealList, 15 );
		double ndcg20local = DCG( retrievalList, 20 )/IDCG( idealList, 20 );
		ndcg5  += ndcg5local;
		ndcg10 += ndcg10local;
		ndcg15 += ndcg15local;
		ndcg20 += ndcg20local;
		System.out.println("NDCG@5="  + ndcg5local);
		System.out.println("NDCG@10=" + ndcg10local);
		System.out.println("NDCG@15=" + ndcg15local);
		System.out.println("NDCG@20=" + ndcg20local);
		
		//return DCG( retrievalList, k )/IDCG( idealList, k );
	}
	public void MAP(){
		int count = 0;
		double maplocal=0.0;
		for(int i = 0; i < retrievalList.size(); ++i){
			Document doc = retrievalList.get(i);
			if(doc.manualAnnotationScore >= 2.0){
				count++;
				maplocal += (double)count/(i+1);
			}
		}
		if(0 == count){
			System.out.println("MAP="+maplocal);
		}else{
			maplocal /= count;
			System.out.println("MAP="+maplocal);
		}
		map += maplocal;
			
		
	}
	private double Precision(int k){
		double count = 0.0;
		for(int i=0; i<k; i++)
			if(retrievalList.get(i).manualAnnotationScore >= 2.0)
				count += 1.0;
		return count/k;
	}
	public void Precision(){
		
		
		double p1local = Precision( 1 );
		double p5local = Precision( 5 );
		double p10local = Precision( 10 );
		double p20local = Precision( 20 );
		p1 += p1local;
		p5 += p5local;
		p10 += p10local;
		p20 += p20local;
		System.out.println("P@1=" + p1local);
		System.out.println("P@5=" + p5local);
		System.out.println("P@10=" + p10local);
		System.out.println("P@20=" + p20local);
	}
	public List<Document> readTopORResult(String filename) throws IOException{
		filename = URLEncoder.encode(filename, "UTF-8");
		HashMap<String,Double> Scores = new HashMap<String,Double>();
		File f = new File("C:/Users/mmdb/Desktop/TopORResult/1/"+filename);
		BufferedReader br = new BufferedReader(new FileReader(f));
		
		String line = null;
		while((line = br.readLine()) != null){
			if(line.contains("####")){
				System.out.println(line);
				Double score = Double.parseDouble(line.substring( line.indexOf("=") + 1) );
				String documentname = line.substring(0, line.indexOf("####"));
				Scores.put(documentname, score);
			}
		}
		
		f = new File("C:/Users/mmdb/Desktop/TopORResult/2/"+filename);
		br = new BufferedReader(new FileReader(f));
		while((line = br.readLine()) != null){
			if(line.contains("####")){
				System.out.println(line);
				Double score = Double.parseDouble(line.substring( line.indexOf("=") +1));
				String documentname = line.substring(0, line.indexOf("####"));
				if(Scores.get(documentname) == null) System.out.println("123 "+documentname);
				Scores.put( documentname , (score+Scores.get(documentname)) / 2.0 );
			}
		}
		br.close();
		
		ArrayList<Entry<String, Double>> array = new ArrayList<Entry<String, Double>>(
				Scores.entrySet());
		
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
			
		List<Document> idealList = new ArrayList<Document>();

		for (Entry<String, Double> e : array) {
			Document doc = new Document(e.getKey());
			doc.manualAnnotationScore = e.getValue();
			idealList.add(doc);
		}
		return idealList;
		
	}
	
	public List<String> get_Queries()throws IOException{
		File f = new File("Query/queries.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line=null;
		List<String> queries = new ArrayList<String>();
		while((line = br.readLine())!=null){
			queries.add( line );
		}
		return queries;
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		OREvaluation eva = new OREvaluation();
		List<String> queries = eva.get_Queries();
		
		String ModelType = "Rerank";
		//String ModelType = "Helpfulness";
		//String ModelType = "OkapiBM25";
		//String ModelType = "Baseline";
		
		int queryindex = 0;
		for(queryindex =0; queryindex < queries.size(); ++queryindex){
			
			eva.getidealList(queries.get(queryindex));
			eva.getretrievalList( queries.get(queryindex), true , ModelType );
			
			eva.NDCG();
			eva.Precision();
			eva.MAP();
			System.out.println( queries.get(queryindex) );
		}
		System.out.println("\r\n总评：");
		System.out.println("\nMAP="+map/queries.size());
		System.out.println("P@1="+p1/queries.size());
		System.out.println("P@5="+p5/queries.size());
		System.out.println("P@10="+p10/queries.size());
		System.out.println("P@20="+p20/queries.size());
		
		System.out.println("NDCG@5="+ndcg5/queries.size());
		System.out.println("NDCG@10="+ndcg10/queries.size());
		System.out.println("NDCG@15="+ndcg15/queries.size());
		System.out.println("NDCG@20="+ndcg20/queries.size());
		
		
	}

}
