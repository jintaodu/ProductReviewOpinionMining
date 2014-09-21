import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

/**
 * a Shallow Parser based on opennlp
 * 
 * @author yangliu
 * @blog http://blog.csdn.net/yangliuy
 * @mail yang.liu@pku.edu.cn
 */

public class ShallowParser {

	private static ShallowParser instance = null;
	private static POSModel model;
	private static ChunkerModel cModel;
	private static HashSet<String> aspectStopwords = new HashSet<String>();// aspects的停止词
   
	public static String Stemer(String word) throws IOException {//利用wordnet对名词提取词根

		String wnhome = System.getenv("WNHOME");
		String path = wnhome + File.separator + "dict";
		URL url = new URL("file", null, path);
		//System.out.println(url.toString());
		// construct the dictionary object and open it
		IDictionary dict = new Dictionary(url);
		dict.open();

		// look up first sense of the word "dog "
		WordnetStemmer ws = new WordnetStemmer(dict);
		POS pos = null;
		List<String> stem = ws.findStems(word, pos.NOUN);
		if(stem.size()==0||stem==null) return word;
		else return stem.get(0);
         
	}
	// Singleton pattern
	public static ShallowParser getInstance() throws InvalidFormatException,
			IOException {
		if (ShallowParser.instance == null) {
			POSModel model = new POSModelLoader().load(new File(
					".\\en-pos-maxent.bin"));
			InputStream is = new FileInputStream(".\\en-chunker.bin");
			ChunkerModel cModel = new ChunkerModel(is);
			ShallowParser.instance = new ShallowParser(model, cModel);
		}
		return ShallowParser.instance;
	}

	public ShallowParser(POSModel model, ChunkerModel cModel) throws IOException {
		ShallowParser.model = model;
		ShallowParser.cModel = cModel;
		File asw = new File(".\\Stopwords");
		BufferedReader br = new BufferedReader(new FileReader(asw));
		String line = null;
		while ((line = br.readLine()) != null) {
			aspectStopwords.add(line);
		}

	}

	public HashMap<Integer, String> chunk(String input) throws IOException {
		PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
		POSTaggerME tagger = new POSTaggerME(model);
		ObjectStream<String> lineStream = new PlainTextByLineStream(
				new StringReader(input));
		// perfMon.start();
		String line;
		String whitespaceTokenizerLine[] = null;
		String[] tags = null;
		while ((line = lineStream.read()) != null) {
			whitespaceTokenizerLine = WhitespaceTokenizer.INSTANCE
					.tokenize(line);
			tags = tagger.tag(whitespaceTokenizerLine);
			POSSample posTags = new POSSample(whitespaceTokenizerLine, tags);
			// System.out.println(posTags.toString());
			// perfMon.incrementCounter();
		}
		// perfMon.stopAndPrintFinalResult();

		// chunker
		ChunkerME chunkerME = new ChunkerME(cModel);
		String result[] = chunkerME.chunk(whitespaceTokenizerLine, tags);
		System.out.println("result=" + Arrays.toString(result));
		HashMap<Integer, String> phraseLablesMap = new HashMap<Integer, String>();
		Integer wordCount = 1;
		Integer phLableCount = 0;
		for (String phLable : result) {
			if (phLable.equals("O"))
				phLable += "-Punctuation"; // The phLable of the last word is OP
			if (phLable.split("-")[0].equals("B"))
				phLableCount++;
			phLable = phLable.split("-")[1] + phLableCount;
			// if(phLable.equals("ADJP")) phLable = "NP"; //Notice: ADJP
			// included in NP
			// if(phLable.equals("ADVP")) phLable = "VP"; //Notice: ADVP
			// included in VP
			// System.out.println(wordCount + ":" + phLable);
			phraseLablesMap.put(wordCount, phLable);
			wordCount++;
		}

		// Span[] span = chunkerME.chunkAsSpans(whitespaceTokenizerLine, tags);
		// for (Span phLable : span)
		// System.out.println(phLable.toString());
		return phraseLablesMap;
	}

	public String getchunk(String sentence, HashMap<Integer, String> result)
			throws InterruptedException, IOException {
		String aspects = new String();// 记录结果
		String[] words = sentence.split(" ");
		System.out.println(Arrays.toString(words));
		String start = "NP1";
		String NP = new String();
		for (int i = 0; i < words.length; ++i) {
			String phLable = result.get(i + 1);

			if (phLable.contains("NP") && start.equals(phLable)) {
				NP += (" " + words[i]);
			} else if (phLable.contains("NP") && !start.equals(phLable)) {
				// System.out.println(NP);
				if (!aspectStopwords.contains(NP.trim())) {
					aspects += (NP + "*").trim();
				}

				NP = new String();
				NP += (" " + words[i]);
				start = phLable;
			} else if (!phLable.contains("NP")) {
				if (NP.length() >= 1) {
					// System.out.println(NP);
					if (!aspectStopwords.contains(NP.trim())) {
						aspects += (NP + "*").trim();
					}
					NP = new String();
				}
				if (i + 2 <= words.length)
					start = result.get(i + 2);
			}

		}// end of for
/*		start = "VP1";String VP = new String();
		for (int i = 0; i < words.length; ++i) {
			String phLable = result.get(i + 1);

			if (phLable.contains("VP") && start.equals(phLable)) {
				VP += (" " + words[i]);
			} else if (phLable.contains("VP") && !start.equals(phLable)) {
				// System.out.println(NP);
				if (!aspectStopwords.contains(VP.trim())) {
					aspects += (VP + "*").trim();
				}

				VP = new String();
				VP += (" " + words[i]);
				start = phLable;
			} else if (!phLable.contains("VP")) {
				if (VP.length() >= 1) {
					// System.out.println(NP);
					if (!aspectStopwords.contains(VP.trim())) {
						aspects += (VP + "*").trim();
					}

					VP = new String();
				}
				if (i + 2 <= words.length)
					start = result.get(i + 2);
			}

		}// end of for
*/
		return aspects.trim();
	}


	private void WriteFile(File f,String content) throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(f,true));
		br.write(content);
		br.flush();
		br.close();
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {		

		ShallowParser swParser = ShallowParser.getInstance();     
		
		//String input = "You can take it anywhere withyou unlike other bulky cameras and capture every moment of yourlife .";
		String input = "the g3 is loaded with many useful features , ";
		System.out.println("swParser chunk = "+swParser.chunk(input));
		String[] F = swParser.getchunk(input, swParser.chunk(input)).split("\\*");//*在正则表达式里有特殊意义
				  
		for(String s : F) System.out.println("F= "+s);
				 
		System.out.println("123 456 ".trim());
		Thread.sleep(10000000);
		// swParser.printresult(Aspects_Manual);
		// System.err.println("---------------------------------");
		//swParser.printresult(Aspects_Frequent);

		// }

	}

}
