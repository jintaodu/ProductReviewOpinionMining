import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import opennlp.tools.util.InvalidFormatException;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

/**Phrase sentences based on stanford parser
 * @author yangliu
 * @blog http://blog.csdn.net/yangliuy
 * @mail yang.liu@pku.edu.cn
 */

public class StanfordParser {
	private static StanfordParser instance = null ;
	private static LexicalizedParser lp;
	
	//Singleton pattern
	public static StanfordParser getInstance(){
		if(StanfordParser.instance == null){
			LexicalizedParser lp = LexicalizedParser.loadModel("./englishPCFG.ser.gz","-retainTmpSubcategories");
			StanfordParser.instance = new StanfordParser(lp);
		}
		return StanfordParser.instance;
	}
	
	public StanfordParser(LexicalizedParser lp){
		StanfordParser.lp = lp;
	}
	 /**Parse sentences in a file
	 * @param SentFilename The input file
	 * @return  void
	 */
	  public void DPFromFile(String SentFilename) {
		    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		    
		    for (List<HasWord> sentence : new DocumentPreprocessor(SentFilename)) {
		      Tree parse = lp.apply(sentence);
		      parse.pennPrint();
		      System.out.println();
		      
		      GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		      List<TypedDependency> tdl = (List<TypedDependency>)gs.typedDependenciesCollapsedTree();
		      System.out.println(tdl);
		      System.out.println();
		    }
	  }

	 /**Parse sentences from a String
	 * @param sent The input sentence
	 * @return  List<TypedDependency> The list for type dependency
	 */
	  public List<TypedDependency> DPFromString(String sent) {
		    TokenizerFactory<CoreLabel> tokenizerFactory = 
		      PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		    List<CoreLabel> rawWords = 
		      tokenizerFactory.getTokenizer(new StringReader(sent)).tokenize();
		    Tree parse = lp.apply(rawWords);
	
		    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		    //Choose the type of dependenciesCollapseTree
		    //so that dependencies which do not 
		    //preserve the tree structure are omitted
		   return (List<TypedDependency>) gs.typedDependenciesCollapsedTree();   
	  }
	  /**Just for testing
		 * @param args
		 * @throws IOException 
		 * @throws InvalidFormatException 
		 */
		public static void main(String[] args) throws InvalidFormatException, IOException {
			// TODO Auto-generated method stub
			//Notice: There should be " " BEFORE and after ",", " ","(",")" etc.
			 String sent = "This phone has a colorful and even amazing screen ";
			 //String sent = "Bell , based in Los Angeles , makes and distributes electronic , computer and building products .";
			 //String sent = "It has an exterior design that combines form and function more elegantly than any point-and-shoot we've ever tested . "; 
			 //String sent = "A Digic II-powered image-processing system enables the SD500 to snap a limitless stream of 7-megapixel photos at a respectable clip , its start-up time is tops in its class , and it delivers decent photos when compared to its competition . "; 
			 //String sent = "I've had it for about a month and it is simply the best point-and-shoot your money can buy . "; 
			 
			 StanfordParser sdPaser = StanfordParser.getInstance();
			 
			 List<TypedDependency> tdl = sdPaser.DPFromString(sent);
			 for(TypedDependency oneTdl : tdl){
			    	System.out.println(oneTdl);
			  }
			 
			 String[] sent1 = new String[]{ "This", "is", "an", "easy", "sentence", "." };
			    List<CoreLabel> rawWords = Sentence.toCoreLabelList(sent1);
			    Tree parse = lp.apply(rawWords);
			    parse.pennPrint();
			    
			    
			    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
			    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
			    // You could also create a tokenizer here (as below) and pass it
			    // to DocumentPreprocessor
			    for (List<HasWord> sentence : new DocumentPreprocessor(".\\XMLReviewsYu\\Canon_EOS.txt")) {
			      Tree parse1 = lp.apply(sentence);
			      parse1.pennPrint();
			      System.out.println();

			      GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
			      Collection tdl1 = gs.typedDependenciesCCprocessed();
			      System.out.println(tdl1);
			      System.out.println();
			    }
			 /* ShallowParser swParser = ShallowParser.getInstance();
			  HashMap<Integer,String> phraseLablesMap = new HashMap<Integer, String>();
			  phraseLablesMap = swParser.chunk(sent);
			  WDTree wdtree = new WDTree();
			  WDTreeNode root = wdtree.bulidWDTreeFromList(tdl, phraseLablesMap);
			  wdtree.printWDTree(root);*/
		}
}
