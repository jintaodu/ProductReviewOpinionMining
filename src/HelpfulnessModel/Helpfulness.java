package HelpfulnessModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import OpinionRetrieval.Document;

public class Helpfulness {

	/**
	 * @param args
	 */
	class Doc{
		public HashSet<String> bigrams;
		public String[] unigrams;
		public double stars = 0;
		public String content;
		public double helpfulness = 0.0;
		public HashMap<String, Integer> WC = new HashMap<String, Integer>();
		
	}
	
	private final String regularExpression = "[^^a-zA-Z0-9|$|\\-|']";
	private HashSet<Doc> collection = new HashSet<Doc>();
	private HashSet<String> vocabulary = new HashSet<String>();
	private HashSet<String> StopWords = new HashSet<String>();
	private HashMap<String, Integer> dft = new HashMap<String,Integer>();
	private HashMap<String, Integer> wordindex = new HashMap<String,Integer>();//词典中每个单词一个下标
	private svm_model model;
	private int N;
	
	public void print(Object str){
		System.out.println(str);
	}
	
	private void readStopWords() throws IOException{
		File f = new File("Stopwords");
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(f));
		while ((line = br.readLine()) != null) {
			StopWords.add(line);
		}
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
	public void update_dft(Doc document){
		
		for(String word : document.WC.keySet()){
			if(dft.containsKey(word)){
				dft.put(word, dft.get(word)+1);
			}else{
				dft.put(word, 1);
			}
		}
	}
	public void save_rawdata(String domain) throws IOException{
		File f = new File("HelpfulnessModelData/"+domain+"/rawdata.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for(Doc doc : collection){
			bw.write(doc.content+"\r\n");
			bw.flush();
		}
		bw.close();
	}
	
	public HashSet<String> get_bigrams(String str){
		str = str.toLowerCase();
		HashSet<String> bigrams = new HashSet<String>();
		String words[] = str.split(regularExpression);
		for(int i = 0; i<= words.length - 2; i++){
			bigrams.add(words[i]+" "+words[i+1]);
		}
		if(1 == words.length){
			bigrams.add(str);
		}
		str = null;
		return bigrams;
	}
	/*
	 * 训练SVR@20150120
	 */
	public void initializeSVR(String domain) throws NumberFormatException, IOException{
		
		File f = new File("HelpfulnessModelData/"+domain+"/dft.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		N = Integer.parseInt(br.readLine());
		
		while((line = br.readLine()) != null){
			int index = line.indexOf("####");
			dft.put(line.substring(0,index), Integer.parseInt(line.substring(index+4)));
			
		}
		
		f = new File("HelpfulnessModelData/"+domain+"/wordindex.txt");
		br = new BufferedReader(new FileReader(f));
		while((line = br.readLine()) != null){
			int index = line.indexOf("####");
			wordindex.put(line.substring(0,index), Integer.parseInt(line.substring(index+4)));
			
		}
		br.close();
		
		model = svm.svm_load_model("HelpfulnessModelData/"+domain+"/model_r.txt");
	}
	public double predictHelpfulness(String domain, Document doc) throws IOException{
        
		File f = new File("HelpfulnessModelData/"+domain+"/SVR.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		
		
		//print(doc.documentlength);
		//print(doc.documentContent);
		ArrayList<svm_node> xx = new ArrayList<svm_node>();
		for(String word : doc.documentWC.keySet()){
			if(wordindex.containsKey(word)){
				
				svm_node node = new svm_node();
				node.index = wordindex.get(word);
				node.value = doc.documentWC.get(word)*Math.log10((double)N/dft.get(word))/doc.documentlength;
				xx.add(node);
			}
		}
		int vovabularysize = dft.size();
		svm_node node1 = new svm_node();
		node1.index = vovabularysize+1;
		node1.value = doc.documentlength; xx.add(node1);
		svm_node node2 = new svm_node();
		node2.index = vovabularysize +2;
		node2.value = doc.star; xx.add(node2);
		svm_node[] x = new svm_node[xx.size()];
		for(int i=0; i<xx.size(); ++i)
			x[i] = xx.get(i);
        double v = 0.0;
        try{
        	v = svm.svm_predict(model, x);
        }catch(Exception e){
        	e.printStackTrace();
        }
        return v;
    }
	public void SVRTrain(String domain)throws IOException{
		
		/*
		 * 生成训练文档
		 */
		File f = new File("HelpfulnessModelData/"+domain+"/SVR.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		int N = collection.size();
		for(Doc doc : collection){
			bw.write(doc.helpfulness+" ");
			for(String word : doc.WC.keySet()){
 				bw.write(wordindex.get(word)+":");
 				bw.write(doc.WC.get(word)*Math.log10((double)N/dft.get(word))/doc.unigrams.length+" ");
 				bw.flush();
			}
			bw.write((vocabulary.size()+1)+":"+doc.unigrams.length+" ");
			bw.write((vocabulary.size()+2)+":"+doc.stars+"\r\n");
			bw.flush();
		}
		
		/*
		 * 开始训练过程
		 */
		String[] arg = { "-c","5","-s", "3","-v","10",
						"HelpfulnessModelData/"+domain+"/SVR.txt", 		// 存放SVM训练模型用的数据的路径
						"HelpfulnessModelData/"+domain+"/model_r.txt" 	// 存放SVM通过训练数据训练出来的模型的路径
						};

		String[] parg = { "HelpfulnessModelData/"+domain+"/SVR.txt",     // 这个是存放测试数据
						  "HelpfulnessModelData/"+domain+"/model_r.txt", // 调用的是训练以后的模型
						  "HelpfulnessModelData/"+domain+"/out_r.txt"  	 // 生成的结果的文件的路径
						};
		
		
		svm_train t = new svm_train();	// 创建一个预测或者分类的对象
		//svm_predict p = new svm_predict();
		t.main(arg); // 调用
		
		svm_model model = svm.svm_load_model("HelpfulnessModelData/"+domain+"/model_r.txt");
		
		f = new File("HelpfulnessModelData/"+domain+"/SVR.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		while( (line = br.readLine())!=null ){
			String[] parts = line.trim().split(" ");
			svm_node[] x = new svm_node[parts.length-1];
			print("line="+line);
			for(int i=1;i<parts.length;++i){
				svm_node node = new svm_node();
				//print(parts[i]);
				int index = parts[i].indexOf(":");
				node.index = Integer.parseInt( parts[i].substring(0, index ) );
				node.value = Double.parseDouble( parts[i].substring( index+1 ));
				//print("node.index="+node.index+" node.value="+node.value);
				x[i-1] = node;
			}
			print("predict="+svm.svm_predict(model, x));
		}
		
		//p.main(parg); // 调用
	}
	
	
	public void process(String domain)throws IOException{
		
		File f = new File("HelpfulnessModelData/"+domain+"/rawdata.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		int count = 0;
		while( (line = br.readLine())!=null ){
			Doc document = new Doc();
			String[] parts = line.split("\t");
			double allvotes = Double.parseDouble(parts[4]);
			double helpfulvotes = Double.parseDouble(parts[3]);
			double stars = Double.parseDouble(parts[5]);
			
			document.content = line;
			document.unigrams = parts[7].split(regularExpression);
			document.stars = stars;
			document.WC = get_WordCount(parts[7]);
			document.helpfulness = helpfulvotes / allvotes;
			if(document.helpfulness < 0.6) count++;
			vocabulary.addAll(document.WC.keySet());
			collection.add(document);
			update_dft(document);
			
		}
		print("small helpfulness count="+count);
		f = new File("HelpfulnessModelData/"+domain+"/dft.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write(collection.size()+"\r\n");
		for(String word : dft.keySet()){
			bw.write(word+"####"+dft.get(word)+"\r\n");
			bw.flush();
		}
		bw.close();
		int index = 1;
		for(String word : vocabulary){
			wordindex.put(word, index++);
		}
		f = new File("HelpfulnessModelData/"+domain+"/wordindex.txt");
		bw = new BufferedWriter(new FileWriter(f));
		for(String word : wordindex.keySet()){
			bw.write(word+"####"+wordindex.get(word)+"\r\n");
			bw.flush();
		}
		bw.flush();
		SVRTrain(domain);
	}
	
	public Doc check_exist(String str){

		String[] parts = str.split("\t");
		HashSet<String> bigrams = get_bigrams( parts[7] );
		
		Doc document = new Doc();
		document.bigrams = bigrams;

		if(collection.size() == 0) return document;
		
		for(Doc doc : collection){
			
			int count=0;
			for(String bigram: doc.bigrams){
				if(bigrams.contains(bigram))
					count++;
			}
			double cover1 = (double)count/bigrams.size();
			count = 0;
			for(String bigram : bigrams){
				if(doc.bigrams.contains(bigram))
					count++;
			}
			double cover2 = (double)count/doc.bigrams.size();
			
			double cover = cover1 > cover2 ? cover2 : cover1;
			if(cover >= 0.8) return null;
		}
		return document;
	}
	
	public boolean check(ArrayList<String> oneproduct, String domain){
		
		HashSet<String> words = new HashSet<String>();
		for(int i = 1; i < oneproduct.size(); ++i){
			String[] splits = oneproduct.get(i).split(regularExpression);
			words.clear();
			for(String s : splits)
				words.add(s);
			if(words.contains(domain) || words.contains(domain+"s"))return true;
		}
		return false;
	
	}
	public HashSet<String> getproductids(String domain)throws IOException{
		File f = new File("H:/Paper Materials/AmazonReview（5.8million）/productinfo.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		HashSet<String> productids = new HashSet<String>();
		ArrayList<String> oneproduct = new ArrayList<String>();
		
		while((line = br.readLine()) != null){
			
			line = line.trim().toLowerCase();
			oneproduct.add(line);
			if(line.contains("break-reviewed")){
				
				if(check( oneproduct, domain ) ){
					productids.add( oneproduct.get(0).split("\t")[0] );
					System.out.println(oneproduct.get(0));
				}
				oneproduct.clear();
			}

			if(productids.size() > 2000) break;
		}
		print("抽取productID完毕  " + productids.size());
		return productids;
	}
	//public void
	public void extractDoc(String[] products , String domain)throws IOException{
		
		readStopWords();
		HashSet<String> getproductids = getproductids( domain );
		
		File f = new File("H:/Paper Materials/AmazonReview（5.8million）/reviewsNew.txt");
		BufferedReader br = null;
		String line = null;
		int count = 0;
		
		for(String product: products){
			
			br = new BufferedReader(new FileReader(f));
			
			while((line = br.readLine()) != null){
				line = line.toLowerCase().trim();
				String[] parts = line.split("\t");
				
				if(parts.length >= 2 && getproductids.contains(parts[1])){
					
					Doc document = check_exist(line);
					if( document != null){
						
						double allvotes = 0.0;
						double helpfulvotes = 0.0;
						double stars = 0.0;
						try{
							allvotes = Double.parseDouble(parts[4]);
							helpfulvotes = Double.parseDouble(parts[3]);
							stars = Double.parseDouble(parts[5]);
							
						}catch(Exception e){
							continue;
						}
						document.helpfulness = (double)helpfulvotes/allvotes;
						if( allvotes < 10.0 ) continue;//投票总数少于10被过滤掉
						if(document.helpfulness < 0.5){
							document.content = line;
							collection.add(document);
						}else if(Math.random() > 0.90 ){
							print("random value="+Math.random());
							print("helpfulness="+document.helpfulness);
							document.content = line;
							collection.add(document);
						}
					}
					count++;
				}
				if(collection.size() == 500) break;
			}//while
			
			print(collection.size());
			print(count);
		}

		print(collection.size());
		print(count);
		save_rawdata( domain );
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Helpfulness d = new Helpfulness();
		String[] products = new String[]{""/*"canon eos","fujifilm finepix","panasonic lumix","nikon coolpix"*/};
		String domain = "camera";
		
		//d.extractDoc(products, domain);//先运行此函数，因为数据量大，运行一次耗费时间
		d.process(domain);
		//d.print(Double.parseDouble(""));
	}

}
