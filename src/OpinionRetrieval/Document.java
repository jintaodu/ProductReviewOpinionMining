package OpinionRetrieval;

import java.util.HashMap;

public class Document {

	/**
	 * @param args
	 */
	
	public String documentname = null;
	public HashMap<String,Double> documentTF = null;
	public HashMap<String,Integer> documentWC = null;
	public String documentContent = null;
	public int documentlength = 0;
	public double ModelSimilarity = 0;
	public double manualAnnotationScore = 0.0;
	public double star = 0.0;
	public double helpfulness = 0.0;//SVR得到的helpfulness
	public double Scorehelpful = 0.0;//Shelpful公式得到的针对query的helpfulness
	public static double allhelpfulness = 0.0;//检索结果中所有文档的SVR helpfulness之和
	public static double maxhelpfulness = 0.0;//检索结果中最大Shelpful
	public static double minhelpfulness = 0.0;//检索结果中最小Shelpful
	
	public Document(String adocumentname){
		documentname = adocumentname;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
