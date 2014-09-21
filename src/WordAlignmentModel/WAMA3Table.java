package WordAlignmentModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class WAMA3Table {

	/**
	 * @param args
	 * @throws IOException
	 */
	
	private HashMap<Pair, Integer> alignfrequency = new HashMap<Pair, Integer>();
	public void insertMap(Pair p)
	{
		if(alignfrequency.containsKey(p))
			alignfrequency.put(p, alignfrequency.get(p)+1);
		else alignfrequency.put(p, 1);
	}
	
	public HashMap<Pair,Integer> getMap()
	{
		return alignfrequency;
	}
	
	public void printMap()
	{
		for(Pair p :alignfrequency.keySet())
			System.out.println(p+"::::"+alignfrequency.get(p));
	}
	
	public void processA3Table() throws IOException
	{
		File f = new File(".\\Stage1\\e2c.A3.final");
		BufferedReader br = new BufferedReader(new FileReader(f));

		int line_num = 0;
		
		String line1 = null;
		while ((line1 = br.readLine()) != null) {
			line_num++;
		}
		br = new BufferedReader(new FileReader(f));
		String line2 = null;
		String line3 = null;
		while (line_num > 0) {
			line1 = br.readLine();
			line_num--;
			line2 = br.readLine();
			line_num--;
			String[] sourcewords = line2.trim().split(" ");
			line3 = br.readLine();
			String[] targetwords = line3.split("\\(\\{|\\}\\)");
			for (int i = 0; i < targetwords.length-1; i += 2) {
              
				System.out.println(targetwords[i]+"          "+targetwords[i+1]);
				String[] counterparts = targetwords[i+1].trim().split(" ");

				for(int j=0;j<counterparts.length;++j)
				{
					if(counterparts[j].trim().length() > 0 )
					insertMap(new Pair(targetwords[i].trim(),sourcewords[Integer.parseInt(counterparts[j].trim())-1]));
				}
			}
			line_num--;
		}
		
		f = new File(".\\Stage1\\c2e.A3.final");
		br = new BufferedReader(new FileReader(f));

		line_num = 0;
		
		while ((line1 = br.readLine()) != null) {
			line_num++;
		}
		br = new BufferedReader(new FileReader(f));
		while (line_num > 0) {
			line1 = br.readLine();
			line_num--;
			line2 = br.readLine();
			line_num--;
			String[] sourcewords = line2.trim().split(" ");
			line3 = br.readLine();
			String[] targetwords = line3.split("\\(\\{|\\}\\)");
			for (int i = 0; i < targetwords.length-1; i += 2) {
              
				System.out.println(targetwords[i]+"          "+targetwords[i+1]);
				String[] counterparts = targetwords[i+1].trim().split(" ");

				for(int j=0;j<counterparts.length;++j)
				{
					if(counterparts[j].trim().length() > 0 )
					insertMap(new Pair(sourcewords[Integer.parseInt(counterparts[j].trim())-1],targetwords[i].trim()));
				}
			}
			line_num--;
		}
		
		printMap();
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		WAMA3Table A3T = new WAMA3Table();
		A3T.processA3Table();

	}

}
