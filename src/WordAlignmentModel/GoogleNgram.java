package WordAlignmentModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

public class GoogleNgram {

	/**
	 * @param args
	 * @throws IOException
	 */
	private static int totalvolumes = 0;
	private static double base = 0.0;//取对数的基数
	
	private double logorithm(double a, double b)
	{
		return Math.log10(b)/Math.log10(a);
	}
	
	public void searchNgram(HashMap<String,Double> tfidfcomputed,String noun,BufferedWriter bwtfidfcomputedfile) throws IOException
	{
		int ngram = noun.split(" ").length;
		File googlengram = new File("");
		//if(1 == ngram)
		     googlengram = new File("H:\\Google Ngram\\1gram\\googlebooks-eng-all-1gram-20120701-"	+ noun.charAt(0));
/*		else if(2 == ngram)
			googlengram = new File("H:\\Google Ngram\\2gram\\googlebooks-eng-all-2gram-20120701-"	+ noun.charAt(0));
		else if(3 == ngram)
			googlengram = new File("H:\\Google Ngram\\3gram\\googlebooks-eng-all-3gram-20120701-"	+ noun.charAt(0));*/
		
		if(googlengram.exists())//判断需要的文件存在不存在
		{
			BufferedReader br = new BufferedReader(new FileReader(googlengram));
			
			System.out.println("googlefile = "	+ googlengram.getName());
	        String line = null;
			double tfidf = 0.0;
			double tfsum = 0.0;
			double dfsum = 0.0;
			int count = 0;
			while ((line = br.readLine()) != null) {
				
				String[] words = line.split("\t");

				String words0lowercase = words[0].toLowerCase();
				if (words0lowercase.equals(noun)) {
					tfsum += Double.parseDouble(words[2]);
					dfsum += Double.parseDouble(words[3]);
					count++;
				}
				if(count != 0 && !words0lowercase.equals(noun) )//循环结束条件
				 {				
				 	break;
				  }
				
			}//end of while
			
			if(0 == tfsum || 0== dfsum)
			{
				tfidfcomputed.put(noun, 0.0);
				bwtfidfcomputedfile.write(noun+"::::"+tfidf+"\r\n");
				bwtfidfcomputedfile.flush();
			}else
			{
				System.out.println("totalcolumes = "+totalvolumes+" tfsum = "+tfsum+" dfsum = "+dfsum);
		        tfidf = (tfsum)*logorithm(base,totalvolumes/dfsum);//计算tfidf
				tfidfcomputed.put(noun, tfidf);
				bwtfidfcomputedfile.write(noun+"::::"+tfidf+"\r\n");
				bwtfidfcomputedfile.flush();
			}

			System.out.println("aspect candidate = " + noun
					+ " tfidf = " + tfidf);
		}else{
			System.err.println("Can not find file :"+googlengram.getName());
			tfidfcomputed.put(noun, 0.0);
			bwtfidfcomputedfile.write(noun+"::::"+0.0+"\r\n");
			bwtfidfcomputedfile.flush();
		}
		
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
      
		
		GoogleNgram GNgram = new GoogleNgram();
		base = 10000.0;
		String line = null;
		
		File totalcount = new File("H:\\Google Ngram\\googlebooks-eng-all-totalcounts-20120701.txt");
		BufferedReader totalcountbr = new BufferedReader(new FileReader(totalcount));
		line = totalcountbr.readLine();
		
		String[] items = line.split("\t");
		for(int i=1;i<=items.length-1;++i)
		{
			String[] numbers = items[i].split(",");
			totalvolumes += Integer.parseInt(numbers[3]);
			System.out.println(Integer.parseInt(numbers[3]));
		}
		System.out.println("totalvolumes = " +totalvolumes +"\n");
		File tfidffile = new File(".\\Stage1\\tfidf.txt");//本次计算得到的tfidf列表
		if (tfidffile.exists())
			tfidffile.delete();
		tfidffile.createNewFile();
		BufferedWriter bwtfidffile = new BufferedWriter(new FileWriter(
				tfidffile, true));

		
		HashMap<String, Double> tfidfcomputed = new HashMap<String, Double>();
		
		File tfidfcomputedfile = new File(".\\GoogleNgramTFIDF\\tfidfcomputed.txt");//已经计算过tfidf的aspect candidate
		BufferedReader brtfidfcomputedfile = new BufferedReader(
				new FileReader(tfidfcomputedfile));
		
		BufferedWriter bwtfidfcomputedfile = new BufferedWriter(
				new FileWriter(tfidfcomputedfile,true));
		
		while((line = brtfidfcomputedfile.readLine())!=null)
		{
			String[] info = line.split("::::");
			tfidfcomputed.put(info[0], Double.parseDouble(info[1]));
		}
		
		File aspectcandidatefile = new File(".\\Stage1\\nouns.txt");
		BufferedReader braspectcandidatefile = new BufferedReader(
				new FileReader(aspectcandidatefile));
			
		while ((line = braspectcandidatefile.readLine()) != null) {
			
			String noun = line;
			System.out.println("\naspect candidate = " + noun);
			if(!tfidfcomputed.containsKey(noun))
			{
				GNgram.searchNgram(tfidfcomputed,noun,bwtfidfcomputedfile);
			}else
			{
				System.out.println("\t"+ noun + "  already exist!");
			}	
	   }//end of while
		
		braspectcandidatefile = new BufferedReader(
				new FileReader(aspectcandidatefile));
		while((line = braspectcandidatefile.readLine())!= null)
		{
			bwtfidffile.write(tfidfcomputed.get(line).toString()+"\r\n");
			bwtfidffile.flush();
		}		
	}

}
