import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AmazonReviewProcess {

	/**
	 * @throws IOException
	 * @2013年11月19日
	 */
	private HashSet<String> productids = new HashSet<String>();
	private ArrayList<String> selectreview = new ArrayList<String>();

	public void selectereview(String domain) throws IOException, InterruptedException {
		File productinfo = new File(
				"H:\\Paper Materials\\AmazonReview（5.8million）\\productinfo.txt");
		BufferedReader proinfobr = new BufferedReader(new FileReader(
				productinfo));
		int line_num=0;
		while(proinfobr.readLine()!=null)
		{
			line_num++;
		}
		proinfobr = new BufferedReader(new FileReader(productinfo));
		String line = proinfobr.readLine();

		while (line_num-- >1) {
			//System.out.println(line);
			if (line.contains("BREAK-REVIEWED")) {
				 line = proinfobr.readLine();
				 line_num--;
				String[] info = line.split("\t");
				line = proinfobr.readLine();
				line_num--;
				while(!line.contains("BREAK-REVIEWED")||line.equals("BREAK"))
				{
					if(proinfobr.readLine().toLowerCase().contains(domain))
					 {
						productids.add(info[0].trim());
						System.out.println("productid="+info[0].trim());
					 }
					line = proinfobr.readLine();
					line_num--;
				}
				
			}//end of if
			else {
				line = proinfobr.readLine();
				line_num--;
			}
            if(productids.size()==20) break;
		}
		
		System.out.println("product id count = " + productids.size());
		File selectedreviews = new File(".\\SelectedReviews\\" + domain
				+ "_reviews.txt");
		if (selectedreviews.exists())
			selectedreviews.delete();
		selectedreviews.createNewFile();
		BufferedWriter selectedreviewsbw = new BufferedWriter(new FileWriter(
				selectedreviews));

		File reviews = new File(
				"H:\\Paper Materials\\AmazonReview（5.8million）\\reviewsNew.txt");
		BufferedReader reviewbr = new BufferedReader(new FileReader(reviews));
		while ((line = reviewbr.readLine()) != null) {
			String[] info = line.split("\t");
			if (productids.contains(info[1].trim())) {
				//System.out.println("helpful = " + info[3] + " unhelpful = "
					//	+ info[4]);
				selectedreviewsbw.write(line + "\r\n");
				selectedreviewsbw.flush();
				selectreview.add(line);
			}
		}

		System.out.println(domain + " 领域选定的review document数量为："
				+ selectreview.size());
	}

	public void generatefeaturevector(String domain) throws IOException {
		HashMap<String, Integer> tf = new HashMap<String, Integer>();
		HashMap<String, Integer> df = new HashMap<String, Integer>();
		int N = 0;
		int dn = selectreview.size();// review document number
		HashSet<String> tokens = new HashSet<String>();
		HashSet<String> allunigram = new HashSet<String>();
		for (String r : selectreview) {
			String[] info = r.split("\t");
			String words[] = info[7].toLowerCase().split(
					"[^a-zA-Z0-9|$|\\-|,|']");
			tokens.clear();
			for (String t : words) {
				if (t.trim().length() > 0) {
					tokens.add(t);
					allunigram.add(t);
				}

			}
			for (String t : tokens) {
				if (t.trim().length() >0)
				 if(df.containsKey(t))
					df.put(t, df.get(t) + 1);
				else
					df.put(t, 1);
			}
		}

		File featurevector = new File(".\\SelectedReviews\\featurevector.arff");
		if (featurevector.exists())
			featurevector.delete();
		featurevector.createNewFile();

		BufferedWriter bw = new BufferedWriter(new FileWriter(featurevector,
				true));
		bw.write("@relation \'" + domain + "\'\r\n");
		bw.flush(); 
		Integer i=0;
		for (String t : allunigram) {
			if(t.trim().length() ==0) continue;
			bw.write("@attribute " + i++ + " real\r\n");
			//bw.write("@attribute " + t + " real\r\n");
			bw.flush();
		}
		bw.write("@attribute rating real\r\n");
		bw.flush();
		bw.write("@attribute length real\r\n");
		bw.flush();
		bw.write("@attribute class real\r\n");
		bw.flush();
		bw.write("@data\r\n");
		bw.flush();
		for (String r : selectreview) {
			tf.clear();
			N=0;
			String[] info = r.split("\t");
			if (Integer.parseInt(info[3].trim())
					+ Integer.parseInt(info[4].trim()) == 0
					|| info[5].trim().length() == 0)
				continue;
			String words[] = info[7].toLowerCase().split(
					"[^a-zA-Z0-9|$|\\-|,|']");
			tokens.clear();

			for (String t : words) {
				if(t.trim().length() > 0)
				if (tf.containsKey(t))
					tf.put(t, tf.get(t));
				else
					tf.put(t, 1);
				N++;
			}
			String fv = "";// feature vector
			for (String t : allunigram) {
				if(t.length() > 0)
				if (tf.containsKey(t))
					fv += tf.get(t) * Math.log10(dn / df.get(t)) / N + ",";
				else
					fv += "0,";
			}
			
			fv += info[5].trim() + ",";//rating 维度
			fv += N + ",";//length 维度
			
			fv += (double) Integer.parseInt(info[3].trim())//class 维度
					/ ( Integer
							.parseInt(info[4].trim()));

			bw.write(fv + "\r\n");
			System.out.println("特征数量 = "+fv.split(",").length);
			bw.flush();
		}
		/*
		 * 生成测试arff文件
		 */
		featurevector = new File(".\\SelectedReviews\\test.arff");
		if (featurevector.exists())
			featurevector.delete();
		featurevector.createNewFile();

		bw = new BufferedWriter(new FileWriter(featurevector,
				true));
		bw.write("@relation \'" + domain + "_test\'\r\n");
		bw.flush(); 
		i=0;
		for (String t : allunigram) {
			if(t.trim().length() ==0) continue;
			bw.write("@attribute " + i++ + " real\r\n");
			//bw.write("@attribute " + t + " real\r\n");
			bw.flush();
		}
		bw.write("@attribute rating real\r\n");
		bw.flush();
		bw.write("@attribute length real\r\n");
		bw.flush();
		bw.write("@attribute class real\r\n");
		bw.flush();
		bw.write("@data\r\n");
		bw.flush();
		File test = new File(".\\SelectedReviews\\test.txt");
		BufferedReader brtest = new BufferedReader(new FileReader(test));
		String line = null;
		while((line = brtest.readLine())!=null)
		{
			String[] words = line.toLowerCase().split("[^a-zA-Z0-9|$|\\-|,|']");
			tf.clear();N=0;
			for (String t : words) {
				if(t.trim().length() > 0)
				if (tf.containsKey(t))
					tf.put(t, tf.get(t));
				else
					tf.put(t, 1);
				N++;
			}
			String fv = "";// feature vector
			for (String t : allunigram) {
				if(t.length() > 0)
				if (tf.containsKey(t))
					fv += tf.get(t) * Math.log10(dn / df.get(t)) / N + ",";
				else
					fv += "0,";
			}
			
			fv += "5.0,";//rating 维度
			fv += N + ",";//length 维度
			fv += "0.4";//class 维度即predication

			bw.write(fv + "\r\n");
			bw.flush();
		}
		bw.close();

	}

	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub

		String domain = "mp3 players";
		AmazonReviewProcess ARP = new AmazonReviewProcess();
		ARP.selectereview(domain);
		ARP.generatefeaturevector(domain);

	}

}
