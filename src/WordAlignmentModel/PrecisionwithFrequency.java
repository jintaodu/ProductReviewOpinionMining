package WordAlignmentModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class PrecisionwithFrequency {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub

		String productname = "Nokia_5800_XpressMusic.txt";
		File f1 = new File(".\\Stage1\\VisualAlignFrequency.txt");
		BufferedReader br1 = new BufferedReader(new FileReader(f1));
		String line = null;
		HashMap<String,Integer> alignfrequency = new HashMap<String,Integer>();
		while((line =  br1.readLine())!=null)
		{
			String[] words = line.split("::::");
			if(alignfrequency.containsKey(words[0]))
			alignfrequency.put(words[0], alignfrequency.get(words[0])+Integer.parseInt(words[2]));
			else alignfrequency.put(words[0],Integer.parseInt(words[2]));
		}
		HashMap<String,Double> alignprobability = new HashMap<String,Double>(); 
		f1 = new File(".\\Stage1\\TranslationVisualResult.txt");
		br1 = new BufferedReader(new FileReader(f1));
		boolean flag = false;
		while((line=br1.readLine())!=null)
		{
			if(line.equals("::::"))
			{
				flag = true; continue;
			}
			String [] words = line.split("::::");
			if(false == flag)
			{
				if(alignprobability.containsKey(words[0]))
					alignprobability.put(words[0], alignprobability.get(words[0])+Double.parseDouble(words[2]));
				else alignprobability.put(words[0], Double.parseDouble(words[2]));
			}else
			{
				if(alignprobability.containsKey(words[1]))
					alignprobability.put(words[1], alignprobability.get(words[1])+Double.parseDouble(words[2]));
				else alignprobability.put(words[1], Double.parseDouble(words[2]));
			}
		}
		
		HashSet<String> manualaspects = new HashSet<String>();
		
		f1 = new File(".\\ManualAspects\\"+productname);
		br1 = new BufferedReader(new FileReader(f1));
		while((line = br1.readLine())!=null)
		{
		 	manualaspects.add(line.substring(0, line.indexOf(":")));
		}
		
		int fre_sum = 0;
		int fre_count = 0;
		double alignprobability_sum = 0.0;
		HashSet<String> set = new HashSet<String>();
		for(int i=1;i<=100;++i)
		{
			set.clear();
			fre_sum = 0;
			alignprobability_sum=0.0;
			for(String s : alignfrequency.keySet())
			{
				if(alignfrequency.get(s)==i)
				{
					fre_sum+=i;
					set.add(s);
					if(alignprobability.containsKey(s))
					alignprobability_sum += alignprobability.get(s);
				}
			}
			int precision_num = 0;
			for(String s: set)
				if(manualaspects.contains(s))
					precision_num++;
			//System.out.println("fre = "+i+" "+(double)precision_num/set.size()+" alignprobility = "+alignprobability_sum);
			System.out.println(i+" "+(double)precision_num/set.size()+" "+alignprobability_sum);
		}
		
		
	}

}
