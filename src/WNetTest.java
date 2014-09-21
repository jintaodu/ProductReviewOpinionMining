import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

public class WNetTest {

	public static void testDitctionary() throws IOException {

		String wnhome = System.getenv("WNHOME");
		String path = wnhome + File.separator + "dict";
		URL url = new URL("file", null, path);
		System.out.println(url.toString());
		// construct the dictionary object and open it
		IDictionary dict = new Dictionary(url);
		dict.open();

		// look up first sense of the word "dog "
		WordnetStemmer ws = new WordnetStemmer(dict);
		POS pos = null;
		List<String> stem = ws.findStems("this", pos);
		if(stem.size()==0||stem==null) System.out.println("dvd-r");
		else System.out.println(stem.get(0));
         
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		testDitctionary();
		String s = "This is my book.if";
		String word[] = s.split("[^^a-zA-Z]+");
		//for(String e :word)
			//System.out.println(e);
		
	}

}
