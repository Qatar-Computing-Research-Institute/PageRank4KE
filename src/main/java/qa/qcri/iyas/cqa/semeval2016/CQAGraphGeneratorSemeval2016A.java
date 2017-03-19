package qa.qcri.iyas.cqa.semeval2016;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import qa.qcri.iyas.dataReader.xml.Semeval2016XmlParser;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAcomment2016;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAinstance2016;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAquestion2016;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAquestionThreadList;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAsimpleQuestion;
import qa.qcri.iyas.ds.BooleanInvertedIndex;
import qa.qcri.iyas.features.computer.core.FeatureComputer;
import qa.qcri.iyas.representationsToString.outputFormatter.KelpFormatter;
import qa.qcri.iyas.representationsToString.types.DenseVectorRepresentationType;
import qa.qcri.iyas.representationsToString.types.InfoRepresentationType;
import qa.qcri.iyas.representationsToString.types.SparseVectorRepresentationType;
import qa.qcri.iyas.representationsToString.types.TargetRepresentationType;
import qa.qcri.iyas.representationsToString.types.TreePairRepresentationType;
import qa.qcri.iyas.representationsToString.types.TreeRepresentationType;
import qa.qcri.iyas.textpreprocessing.core.TextPreprocessor;
import qa.qcri.pagearnk4ke.BasicGraphGenerator;
import qa.qcri.qf.pipeline.UimaUtil;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import util.Stopwords;

/**
 * A class for the generation of the KeLP files for Semeval 2016 dataset. The class loads the CQA objects and for
 * each of them extract related representation and store it. 
 * @author sromeo
 *
 */
public class CQAGraphGeneratorSemeval2016A {

  //  private static final String COMMENT_GOOD_LABEL = "Good";
  //  
  //  private static final String COMMENT_POT_LABEL = "PotentiallyUseful";
  //  
  //  private static final String COMMENT_BAD_LABEL = "Bad";

  private final String LANGUAGE = "en";

  private static final Boolean USE_LEMMAS = true;

  private final String NODE_ID_SEPARATOR = "_";

  private  final String INDEX_DOC_QUESTION = "q";
  private  final String INDEX_DOC_COMMENT = "c";

  private final float WEIGHT_DEFAULT = 1;
  /**
   * Path to the XML files in which the CQA dataset is stored.
   */
  //Set folder of the input files
  public static final String XML_ROOT_DIR = "/data/alt/corpora/semeval2016/data/v3.2/xml-files/";


  //	public static final String OUT_DIR = "/data/alt/corpora/semeval2016/output/";

  //	private static final String PARAMETER_LIST = Joiner.on(",").join(
  //      new String[] { RichNode.OUTPUT_PAR_LEMMA, RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });


  //	//Set dev input files' paths
  //	public static final String XML_DEV_FILES[] = {XML_ROOT_DIR+"SemEval2016-Task3-CQA-QL-dev.xml"};
  //	
  //	//Set test input files' paths
  //	public static final String XML_TEST_FILES[] = {XML_ROOT_DIR+"SemEval2016-Task3-CQA-QL-test.xml"};
  //	public static final String XML_2017TEST_FILES[] = {"data/semeval2017/SemEval2017-task3-English-test-input.xml"};
  //	//Set train input files' paths
  //	public static String XML_TRAIN_FILES[] = {XML_ROOT_DIR+"train/SemEval2016-Task3-CQA-QL-train-part1.xml",
  //			XML_ROOT_DIR+"train/SemEval2016-Task3-CQA-QL-train-part2.xml"};
  //	
  private BooleanInvertedIndex invIndex;
  private BasicGraphGenerator graphGen;
  private final TextPreprocessor tp;


  public static final String STOPWORDS_PATH = "/stoplist-en.txt";

  private final Stopwords STOPWORDS;

  public CQAGraphGeneratorSemeval2016A () throws ResourceInitializationException, IOException {
    invIndex = new BooleanInvertedIndex();
    graphGen = new BasicGraphGenerator();
    tp = new TextPreprocessor();
    STOPWORDS = new Stopwords(STOPWORDS_PATH);
  }

  //	  public String computeSemeval2016TaskASubmissionExampleRepresentation(
  //	       CQAquestion2016 relatedQuestion, CQAcomment2016 comment, 
  //	      FeatureComputer fc,TextPreprocessor tp) {
  //	    
  //	    //Create the KelpFormatter object used for generating the dataset in KeLP format 
  //	    KelpFormatter kf = new KelpFormatter();
  //	    
  //	    //Add the target representation containing the gold label
  ////	    kf.addRepresentation(new TargetRepresentationType(getGoldLabel(comment)));
  //	    
  //	    //Preprocess the content (subject + body) of the related question
  //	    String text1 = tp.getSemevalNormalizedWholeText(relatedQuestion.getSubject(),
  //	        relatedQuestion.getBody(),"en");
  //	    JCas jcas1 = tp.getAnnotatedText(text1,"en");
  //	    
  //	    //Preprocess the content (subject + body) of the comment
  //	    String text2 = tp.getNormalizedText(comment.getBody(),"en");
  //	    JCas jcas2 = tp.getAnnotatedText(text2,"en");
  //	    
  //	    //Build the tree for the question and comment
  //	    TokenTree tree1 = RichTree.getPosChunkTree(jcas1);
  //	    TokenTree tree2 = RichTree.getPosChunkTree(jcas2);
  //	    //Add a tree-pair representation
  //	    kf.addRepresentation(new TreePairRepresentationType(
  //	        new TreeRepresentationType(tree1),new TreeRepresentationType(tree2)));
  //	    
  //	    //Compute the 21 similarities between question and comment
  //	    double _21sims[] = fc.computeTwentyOneSimilarities(jcas1,jcas2);
  //	    //Instantiate a DenseVectorRepresentationType for the 21 similarities
  //	    DenseVectorRepresentationType dr = new DenseVectorRepresentationType(_21sims);
  //	    //Change the name of the representation
  //	    dr.setRepresentationName("densefeatures-qqsim");
  //	    //Add the representation
  //	    kf.addRepresentation(dr);
  //	    
  //	    //TODO so far we compute the position in a very weird way.
  //	    double pos = (++COMMENT_POSITION) / 10;
  //	    String names[] = {"rank"};
  //	    double values[] = {pos};
  //	    SparseVectorRepresentationType sr = new SparseVectorRepresentationType(names,values);
  //	    //Change the name of the representation
  //	    sr.setRepresentationName("sparsefeatures-rank");
  //	    //Add the representation
  //	    kf.addRepresentation(sr);
  //	    
  //	    //Create an InfoRepresentationType containing the example ID
  //	    InfoRepresentationType ir = new InfoRepresentationType(comment.getId());
  //	    ir.setRepresentationName("identifiers");
  //	    kf.addRepresentation(ir);
  //	    
  //	    return kf.exampleToString();
  //	  }

  public  void generateIndex(CQAinstance2016 thread) throws ResourceInitializationException, IOException {
    //	  FeatureComputer fc = new FeatureComputer(STOPWORDS);

    CQAquestion2016 relatedQuestion = thread.getQuestion();

    String text= tp.getSemevalNormalizedWholeText(relatedQuestion.getSubject(),
        relatedQuestion.getBody(), LANGUAGE);
    for (RichTokenNode token : getTokens(tp.getAnnotatedText(text, LANGUAGE))) {
      invIndex.add(
          USE_LEMMAS 
          ? token.getRepresentation(RichNode.OUTPUT_PAR_LEMMA) 
              : token.getRepresentation(RichNode.OUTPUT_PAR_TOKEN_LOWERCASE)
              , INDEX_DOC_QUESTION
          );    
    }

    int i =1;
    for (CQAcomment2016 comment : thread.getComments()) {
      text = tp.getNormalizedText(comment.getBody(), LANGUAGE);
      for (RichTokenNode token : getTokens(tp.getAnnotatedText(text, LANGUAGE))) {
        invIndex.add(
            USE_LEMMAS 
            ? token.getRepresentation(RichNode.OUTPUT_PAR_LEMMA) 
                : token.getRepresentation(RichNode.OUTPUT_PAR_TOKEN_LOWERCASE)
                , String.format("%s%d", INDEX_DOC_COMMENT, i )
            );    
      }
      i++;
    }
    System.out.println(invIndex.size());

  }




  public void generateGraph() {
    String[] terms = invIndex.getTerms();
    Map<Integer, List<String>> doc2terms = new TreeMap<Integer, List<String>>();
    int[] documents = getSortedDocs(invIndex.getDocuments());
    int i;
    for (i=0; i < documents.length; i++) {
      doc2terms.put(documents[i], new ArrayList<String>());
    }

    for (i = 0; i < terms.length; i++) {
      int[]  docs = getSortedDocs(invIndex.getDocuments(terms[i]));
      for (int j = 0; j < docs.length; j++) {
        String nodeLabel = getNodeLabel(String.valueOf(docs[j]), terms[i]);
        graphGen.addNode(nodeLabel, terms[i]);
        doc2terms.get(docs[j]).add(nodeLabel);
      }
    }
    List<Integer> iter = new ArrayList<Integer>(doc2terms.keySet());
    for (i = 0 ; i < iter.size() -1 ; i++) {
      for (String src : doc2terms.get(iter.get(i))) {
        for (String trg : doc2terms.get(iter.get(i+1))) {
          graphGen.addEdge(src, trg, WEIGHT_DEFAULT);
        }
      }
    }
    
  }

  
  public void mergeNodes() {
    
  }
  
  public void displayPageRank() {
    graphGen.runPageRank();
  }
  
  private String getNodeLabel(String chunk, String token) {
    return String.format("%s%s%s", chunk, NODE_ID_SEPARATOR, token);
  }

  private int[] getSortedDocs(String[] docs) {
    Set<Integer> ids = new TreeSet<Integer>();
    int i;
    for (i = 0; i < docs.length; i++) {
      if (docs[i].startsWith(INDEX_DOC_QUESTION)) {
        ids.add(0);
      } else {
        ids.add(Integer.valueOf(docs[i].replace(INDEX_DOC_COMMENT, "")));
      }
    }
    Iterator<Integer> iter = ids.iterator();
    i = 0;
    int[] is = new int[docs.length];
    while (iter.hasNext()) {
      is[i++] = iter.next();
    }
    return is;
  }

  /**
   * Filters out the stopwords from the JCcas and generates a list of tokens.
   * @param cas
   *           JCas with all the pre-processing carried out already
   * @return
   *           List of tokens, without stopwords
   */
  private List<RichTokenNode> getTokens(JCas cas) {
    List<RichTokenNode> richTokens = UimaUtil.getRichTokens(cas);
    Iterator<RichTokenNode> i = richTokens.iterator();
    while(i.hasNext()) {
      RichTokenNode token = i.next();
      if(this.STOPWORDS.contains(token.getRepresentation(RichNode.OUTPUT_PAR_TOKEN_LOWERCASE))) {
        i.remove();
      }
    }

    return richTokens;
  }


  public static void main(String args[]) throws IOException, ResourceInitializationException {
//    int k = 100;

    Semeval2016XmlParser xmlParser = new Semeval2016XmlParser(new String[]{args[0]});
    List<CQAquestionThreadList> cqaQthreads = xmlParser.readXml();
    for (CQAquestionThreadList threadlist : cqaQthreads) {
      for (CQAinstance2016 thread : threadlist.getThreads()) {
        CQAGraphGeneratorSemeval2016A exp = new CQAGraphGeneratorSemeval2016A();
        exp.generateIndex(thread);
        exp.generateGraph();
        exp.displayPageRank();
        exp.mergeNodes();
        //  		  bgg.runPageRank();

        //  		  exp.mergeNodesContinuous();

        //      
        //        CQAquestion2016 relatedQuestion = thread.getQuestion();
        //        //TODO we temporarily reset this global variable here to start with 
        //        // the right value when computing the position feature
        //       
        //        for (CQAcomment2016 comment : thread.getComments()) {
        ////          ,args[0]+".klp");
        //        }
      }
    }
    //		
    //		exp.computeGraph(cqaQthreads,fc,tp,args[0]+".klp");
    //		
  }
}

