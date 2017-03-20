package qa.qcri.iyas.cqa.semeval2016;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.PageRank;
import org.openide.util.Lookup;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import qa.qcri.iyas.dataReader.xml.Semeval2016XmlParser;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAcomment2016;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAinstance2016;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAquestion2016;
import qa.qcri.iyas.datastructures.cqa.semeval2016.CQAquestionThreadList;
import qa.qcri.iyas.ds.BooleanInvertedIndex;
import qa.qcri.iyas.textpreprocessing.core.TextPreprocessor;
import qa.qcri.iyas.utils.Log4jFactory;

import qa.qf.qcri.check.CHK;
import util.Stopwords;

/**
 * Contains the necessary methods to build a graph representing a question-comments
 * thread. 
 * 
 * The nodes are the tokens or lemmas (there is a flag in the class) and
 * and an edge is made between all the comments of c_i and c_{i+1}. In this 
 * default implementation q is added before c_0 and treated as just another comment.
 * 
 * Method mergeNodes merges all those nodes that correspond to a common token,
 * even if they come from different comments. The resulting graph can be used
 * to assess the "importance" of different tokens; e.g., on the basis of PageRank
 * (method runPageRank)
 *  
 * @author albarron
 *
 */
public class CQAGraphGeneratorSemeval2016A {

  private final GraphModel graphModel;
  private final DirectedGraph directedGraph;
  
  private final BooleanInvertedIndex invIndex;
  
  private final TextPreprocessor tp;
  private final Stopwords STOPWORDS;

  private final Boolean DISCARD_1CHAR_TOKENS = true;
  private final Boolean USE_LEMMAS = true;

  private final String LANGUAGE = "en";
  private final String NODE_ID_SEPARATOR = "_";

  private final String INDEX_DOC_QUESTION = "q";
  private final String INDEX_DOC_COMMENT = "c";

  private final float WEIGHT_DEFAULT = 1;
  
  /** Path to the XML files in which the CQA dataset is stored. */
  public static final String XML_ROOT_DIR = 
              "/data/alt/corpora/semeval2016/data/v3.2/xml-files/";

  public static final String STOPWORDS_PATH = "/stoplist-en.txt";

  private Map<Integer, List<String>> doc2terms;

  private static Logger logger;

  
  /**
   * Initialises the logger, inverted index, text preprocessor, stopwords, and
   * graph-related resources.
   * 
   * @throws ResourceInitializationException
   * @throws IOException
   */
  public CQAGraphGeneratorSemeval2016A () 
  throws ResourceInitializationException, IOException {
    logger = Log4jFactory.getLogger();
    invIndex = new BooleanInvertedIndex();
    tp = new TextPreprocessor();
    STOPWORDS = new Stopwords(STOPWORDS_PATH);
    
    ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
    pc.newProject();
    Workspace workspace = pc.getCurrentWorkspace();
    
    graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel(workspace);
    directedGraph = graphModel.getDirectedGraph();
  }

  /**
   * Generates an inverted index with all the words from the thread. If USE_LEMMAS
   * is set to true, lemmas are indexed; tokens otherwise. 
   * The document identifier for the question becomes "q0" for the comments it's 
   * "ci" with i being the comment position.
   *  
   * @param thread
   *          a CQA thread in the 2016 format
   * @throws ResourceInitializationException
   * @throws IOException
   */
  public void generateIndex(CQAinstance2016 thread) 
  throws ResourceInitializationException, IOException {
    CQAquestion2016 relatedQuestion = thread.getQuestion();

    String text= tp.getSemevalNormalizedWholeText(relatedQuestion.getSubject(),
        relatedQuestion.getBody(), LANGUAGE);
    for (Token token : getTokens(tp.getAnnotatedText(text, LANGUAGE))) {
      invIndex.add(
          USE_LEMMAS 
          ? token.getLemma().getValue() 
              : token.getCoveredText().toLowerCase()
              , getDocId(0)
          );    
    }

    int i =1;
    for (CQAcomment2016 comment : thread.getComments()) {
      text = tp.getNormalizedText(comment.getBody(), LANGUAGE);
      for (Token token : getTokens(tp.getAnnotatedText(text, LANGUAGE))) {
        invIndex.add(
            USE_LEMMAS 
            ? token.getLemma().getValue() //Representation(RichNode.OUTPUT_PAR_LEMMA) 
                : token.getCoveredText().toLowerCase() //Representation(RichNode.OUTPUT_PAR_TOKEN_LOWERCASE)
                ,  getDocId(i)
            );    
      }
      i++;
    }
    logger.info("Number of terms in the index: " + invIndex.size());
  }
  

  
  /**
   * Generates a graph on the basis of a previously-loaded inverted index.
   */
  public void generateGraph() {
    String[] terms = invIndex.getTerms();
    doc2terms = new TreeMap<Integer, List<String>>();
    int[] documents = getSortedDocs(invIndex.getDocuments());
    int i;
    for (i=0; i < documents.length; i++) {
      doc2terms.put(documents[i], new ArrayList<String>());
    }

    for (i = 0; i < terms.length; i++) {
      int[]  docs = getSortedDocs(invIndex.getDocuments(terms[i]));
      for (int j = 0; j < docs.length; j++) {
        String nodeLabel = getNodeLabel(String.valueOf(docs[j]), terms[i]);
        addNode(nodeLabel, terms[i]);
        doc2terms.get(docs[j]).add(nodeLabel);
      }
    }
    List<Integer> iter = new ArrayList<Integer>(doc2terms.keySet());
    for (i = 0 ; i < iter.size() -1 ; i++) {
      for (String src : doc2terms.get(iter.get(i))) {
        for (String trg : doc2terms.get(iter.get(i+1))) {
          addEdge(src, trg, WEIGHT_DEFAULT);
        }
      }
    }
    logger.info(String.format("Graph dimensions: %d nodes; %d edges", 
            directedGraph.getNodeCount(), 
            directedGraph.getEdgeCount()));
  }

  /**
   * Add an edge between existing nodes with id id1 and id2. 
   * @param srcNodeId
   *          id of the source node (should exist in the graph)
   * @param trgNodeId
   *          id of the target node (should exist in the graph)
   * @param weight
   *          weight for the edge
   */
  public void addEdge(String srcNodeId, String trgNodeId, double weight) {
    Node node1 = directedGraph.getNode(srcNodeId);
    Node node2 = directedGraph.getNode(trgNodeId);
    addEdge(node1, node2, weight);
  }
  
  /**
   * Add an edge between existing nodes
   * @param srcNode
   *          source node (should exist in the graph)
   * @param trgNode
   *          target node (should exist in the graph)
   * @param weight
   *          weight for the edge
   */
  public void addEdge(Node srcNode, Node trgNode, double weight) {
    Edge e = graphModel.factory().newEdge(srcNode, trgNode, 0, weight, true);
    directedGraph.addEdge(e);
  }
  
  /**
   * Adds a new node to the graph with the given id and label. It crashes if
   * a node exists already with this id?
   * @param id
   * @param label
   */
  public void addNode(String id, String label) {
    Node node = graphModel.factory().newNode(id);
    node.setLabel(id);
    CHK.CHECK(directedGraph.getNode(id) == null, 
        "you are adding the same node twice! " + id);
    directedGraph.addNode(node);
  }
  
  /** 
   * Merge all the nodes in the graph which represent the same lemma, but in
   * different texts. 
   */
  public void mergeNodes() {
    String[] allTerms = invIndex.getTerms();
    for (int i = 0; i < allTerms.length ; i++) {
//      System.out.println(allTerms[i]);
      String[] documents = invIndex.getDocuments(allTerms[i]);
      List<String> toMerge = new ArrayList<String>();
      
      for (int j = 0; j < documents.length; j++) {
        toMerge.add(getNodeLabel(documents[j], allTerms[i]));
      }
      if (toMerge.size() > 1) {
        mergeNodes(toMerge);
      }
    }
  }
  
  public void runPageRank() {
    PageRank pagerank = new PageRank();
    pagerank.setDirected(true);
    pagerank.setProbability(0.85);
    pagerank.setUseEdgeWeight(true);
    pagerank.execute(directedGraph);
    System.out.println(pagerank.getReport());
    Column pageRankColumn = graphModel.getNodeTable().getColumn(PageRank.PAGERANK);
    System.out.println("node\tpagerank\tneighbours");
    for (Node n : directedGraph.getNodes()) {
      Node[] neighbors = directedGraph.getNeighbors(n).toArray();
      System.out.format("%s\t%s\t%s%n", n.getId(), n.getAttribute(pageRankColumn), neighbors.length);
   }
    
    
  }
  
  public void displayPageRank() {
    runPageRank();
  }
  
  private void mergeNodes(List<String> ids) {
    //Adding the new node
    String mergedNode = getMergedNodeLabel(ids);
    addNode(mergedNode, mergedNode);
    double weight;
    Node newNode = directedGraph.getNode(mergedNode);
    
    System.out.print("MERGING to: " + newNode.getId().toString());
    
    
    for (String id : ids) {      
      
      Node oldNode = directedGraph.getNode(id.substring(1));
      //TODO this could be done in 1/2 methods.
      //  Transferring the edges entering the old node into the new node
      Edge[] inEdges = directedGraph.getInEdges(oldNode).toArray();
//      System.out.print(id + " ");      
      for (int i = 0 ; i < inEdges.length ; i++) {        
        Edge currentEdge = inEdges[i];
        Node srcNode = currentEdge.getSource();
        weight = currentEdge.getWeight();
        Edge existingEdge = directedGraph.getEdge(srcNode, newNode);
        if (existingEdge == null) {  // The edge does not exist. Add a new one
          addEdge(srcNode, newNode, weight);
        } else {
          existingEdge.setWeight(existingEdge.getWeight() + weight);
        }
        removeEdge(srcNode, oldNode);     
      }
      
      // Transferring the edges departing from the old node to the new node
      Edge[] outEdges = directedGraph.getOutEdges(oldNode).toArray();  
      for (int i = 0; i < outEdges.length; i++) {
        Edge currentEdge = outEdges[i];
        Node trgNode = currentEdge.getTarget();
        weight = currentEdge.getWeight();
        Edge existingEdge = directedGraph.getEdge(newNode, trgNode);
        if (existingEdge == null) {
          addEdge(newNode, trgNode, weight);
        } else {
          existingEdge.setWeight(existingEdge.getWeight() + weight);

        }
        removeEdge(oldNode, trgNode);
      }
      
    //Removing the old nodes (although they should remain there, disconnected)
      //CURRENTLY NOT DONE
    }
    System.out.println();
  }
  
  
  
  private void removeEdge(Node srcNode, Node trgNode) {
    Boolean removed = directedGraph.removeEdge(directedGraph.getEdge(srcNode, trgNode));
    if (removed) {
      System.out.format("Edge %s-%s removed%n", srcNode.getLabel(), trgNode.getLabel());
    } else {
      System.exit(-1);
    }
  }
  
  /**
   * Concatenates the chunk ids of all the documents and the term at the end.
   * It assumes the term value is always the same 
   * @param ids
   *        List of chunk_term (term is always the same)
   * @return
   *        One id with d1_d2_..._term
   */
  private String getMergedNodeLabel(List<String> ids) {
    StringBuffer sb = new StringBuffer();
    for (String id : ids) {
      sb.append(id.split(NODE_ID_SEPARATOR)[0].substring(1))
        .append(NODE_ID_SEPARATOR);
    }
    sb.append(ids.get(0).split(NODE_ID_SEPARATOR)[1]);
    return sb.toString();
  }
  
  

  
  private String getDocId(int i) {
    if (i == 0) {
      return String.format("%s%d", INDEX_DOC_QUESTION, 0);
    }
    
    return String.format("%s%d", INDEX_DOC_COMMENT, i );
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
   * Iterates over the tokens in the cas (first over sentences, then over
   * tokens) and returns a list of the stopword-filtered tokens. It discards 
   * 1-char tokens if this is defined by the global flag.
   * @param cas
   *          a cas covering a text 
   * @return
   *          list of tokens in the cas (if they fulfill a number of conditions)
   */
  private List<Token> getTokens(JCas cas) {
    List<Token> tokens = new ArrayList<Token>();
    for (Sentence sent : JCasUtil.select(cas, Sentence.class)) {
      for (Token token : JCasUtil.selectCovered(Token.class, sent)) {
        if (STOPWORDS.contains(token.getCoveredText().toLowerCase())) {
          continue;
        }
        if (DISCARD_1CHAR_TOKENS && token.getCoveredText().length() < 2) {
          continue;
        }
        tokens.add(token);
      }
    }
    return tokens;
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
        exp.displayPageRank();
      }
    }
  }
}

