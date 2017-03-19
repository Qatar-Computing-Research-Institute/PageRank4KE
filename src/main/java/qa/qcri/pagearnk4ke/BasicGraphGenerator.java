package qa.qcri.pagearnk4ke;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingLabelColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.types.DependantOriginalColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.GraphDistance;
import org.gephi.statistics.plugin.PageRank;
import org.gephi.toolkit.demos.plugins.preview.PreviewSketch;
import org.openide.util.Lookup;
import org.gephi.graph.api.Node;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.impl.ImportContainerFactoryImpl;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;

import qa.qf.qcri.check.CHK;

///Iyas.cqaTextSelection/src/main/java/qa/qcri/iyas/textpreprocessing/CQAKelpFileGeneratorForSemeval2016.java"
public class BasicGraphGenerator {
  

  
  private GraphModel graphModel;
  private DirectedGraph directedGraph; 
  
  public BasicGraphGenerator() {
    ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
    pc.newProject();
    Workspace workspace = pc.getCurrentWorkspace();
    
    graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel(workspace);
    directedGraph = graphModel.getDirectedGraph();
  }
  
  public void addNode(String id, String label) {
//    Node node = graphModel.factory().newNode(new CqaNode(chunk, token));
    Node node = graphModel.factory().newNode(id);
    node.setLabel(id);
    CHK.CHECK(! directedGraph.contains(node), "ou seem to be adding the same node twice!");
    directedGraph.addNode(node);
    
  }
  
  public void addEdge(String id1, String id2, float weight) {
    Node node1 = directedGraph.getNode(id1);
    Node node2 = directedGraph.getNode(id2);
//    Node node1 = graphModel.factory().newNode(getNodeLabel(chunk1, token1));
//    Node node2 = graphModel.factory().newNode(getNodeLabel(chunk2, token2));
    Edge e = graphModel.factory().newEdge(node1, node2, 0, weight, true);
    directedGraph.addEdge(e);
  }
  
  public void runPageRank() {
    PageRank pagerank = new PageRank();
    pagerank.setDirected(true);
    pagerank.setProbability(0.85);
    pagerank.setUseEdgeWeight(true);
    pagerank.execute(directedGraph);
    System.out.println(pagerank.getReport());
    Column pageRankColumn = graphModel.getNodeTable().getColumn(PageRank.PAGERANK);
//    System.out.println(pageRankColumn);
    for (Node n : directedGraph.getNodes()) {
      System.out.println(n.getLabel() + " has " + n.getAttribute(pageRankColumn));
      Node[] neighbors = directedGraph.getNeighbors(n).toArray();
      System.out.println(n.getLabel() + " has " + neighbors.length + " neighbors");
    }
    
    
  }
  
  

  
  
  
  public static void script() {
    //Init a project - and therefore a workspace
    ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
    pc.newProject();
    Workspace workspace = pc.getCurrentWorkspace();

    Container container = Lookup.getDefault().lookup(ImportContainerFactoryImpl.class).newContainer();
    ImportController importController = Lookup.getDefault().lookup(ImportController.class);
    importController.process(container, new DefaultProcessor(), workspace);

    //Get a graph model - it exists because we have a workspace
    GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel(workspace);

    //Create three nodes
    Node nA = graphModel.factory().newNode("nA");
    nA.setLabel("A");
    Node nB = graphModel.factory().newNode("nB");
    nB.setLabel("B");
    Node nC = graphModel.factory().newNode("nC");
    nC.setLabel("C");

    //Create three edges
//    Edge e1 = graphModel.factory().newEdge(nB, nC, 0, 0.1, true);
//    Edge e2 = graphModel.factory().newEdge(nA, nC, 0, 0.2, true);
//    Edge e3 = graphModel.factory().newEdge(nC, nA, 0, 0.3, true);   //This is e2's mutual edge

//    //Append as a Directed Graph
//    DirectedGraph directedGraph = graphModel.getDirectedGraph();
//    directedGraph.addNode(nA);
//    directedGraph.addNode(nB);
//    directedGraph.addNode(nC);
//    directedGraph.addEdge(e1);
//    directedGraph.addEdge(e2);
//    directedGraph.addEdge(e3);
    
    //Create three edges
  Edge e1 = graphModel.factory().newEdge(nA, nC, 0, 1, true);
  Edge e2 = graphModel.factory().newEdge(nC, nA, 0, 1, true);
  Edge e3 = graphModel.factory().newEdge(nA, nB, 0, 1, true);
  Edge e4 = graphModel.factory().newEdge(nB, nC, 0, 1, true);

  //Append as a Directed Graph
  DirectedGraph directedGraph = graphModel.getDirectedGraph();
  directedGraph.addNode(nA);
  directedGraph.addNode(nB);
  directedGraph.addNode(nC);
  directedGraph.addEdge(e1);
  directedGraph.addEdge(e2);
  directedGraph.addEdge(e3);
  directedGraph.addEdge(e4);
    

    //Count nodes and edges
    System.out.println("Nodes: " + directedGraph.getNodeCount() + " Edges: " + directedGraph.getEdgeCount());

    
//    Get a UndirectedGraph now and count edges
    UndirectedGraph undirectedGraph = graphModel.getUndirectedGraph();
    System.out.println("Edges: " + undirectedGraph.getEdgeCount());   //The mutual edge is automatically merged

    //Iterate over nodes
    for (Node n : directedGraph.getNodes()) {
        Node[] neighbors = directedGraph.getNeighbors(n).toArray();
        System.out.println(n.getLabel() + " has " + neighbors.length + " neighbors");
    }

    //Iterate over edges
    for (Edge e : directedGraph.getEdges()) {
        System.out.println(e.getSource().getId() + " -> " + e.getTarget().getId());
    }

    //Find node by id
    Node node2 = directedGraph.getNode("nB");

    //Get degree
    System.out.println("Node2 degree: " + directedGraph.getDegree(node2)); 
    
    //Get Centrality
    GraphDistance distance = new GraphDistance();
    distance.setDirected(true);
    distance.execute(graphModel);

    AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
    AppearanceModel appearanceModel = appearanceController.getModel();
    //Rank color by Degree
    
    Function degreeRanking = appearanceModel.getNodeFunction(directedGraph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class);
    RankingElementColorTransformer degreeTransformer = (RankingElementColorTransformer) degreeRanking.getTransformer();
    degreeTransformer.setColors(new Color[]{new Color(0xFEF0D9), new Color(0xB30000)});
    degreeTransformer.setColorPositions(new float[]{0f, 1f});
    appearanceController.transform(degreeRanking);

    //Rank size by centrality
    Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
    Function centralityRanking = appearanceModel.getNodeFunction(directedGraph, centralityColumn, RankingNodeSizeTransformer.class);
    RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
    centralityTransformer.setMinSize(0.3f);
    centralityTransformer.setMaxSize(0.32f);
    appearanceController.transform(centralityRanking);

    
    
    PageRank pagerank = new PageRank();
    pagerank.setDirected(true);
    pagerank.setProbability(0.85);
    pagerank.setUseEdgeWeight(true);
    pagerank.execute(directedGraph);
    System.out.println(pagerank.getReport());
    
    Column pageRankColumn = graphModel.getNodeTable().getColumn(PageRank.PAGERANK);
    Function pagerankRanking = appearanceModel.getNodeFunction(directedGraph, pageRankColumn, RankingLabelColorTransformer.class);
    RankingLabelColorTransformer colorTransformer = pagerankRanking.getTransformer();
    appearanceController.transform(pagerankRanking);
//    Column pageRankColumn = graphModel.getNodeTable().getColumn(pagerank.PAGERANK);
    System.out.println(pageRankColumn);
    for (Node n : directedGraph.getNodes()) {
      System.out.println(n.getLabel() + " has " + n.getAttribute(pageRankColumn));
//      directedGraph.getgetNeighbors(n).toArray();
//      System.out.println(n.getLabel() + " has " + neighbors.length + " neighbors");
  }
    
    YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
    layout.setGraphModel(graphModel);
    layout.initAlgo();
    layout.resetPropertiesValues();
    layout.setOptimalDistance(200f);

    for (int i = 0; i < 100 && layout.canAlgo(); i++) {
       layout.goAlgo();
    }
    layout.endAlgo();
    
  //Preview configuration
    PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
    PreviewModel previewModel = previewController.getModel();
    previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, previewModel.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(0) );
    previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
    previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.WHITE));
    previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
    previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 500);
    previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, Color.BLACK);
//    previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, previewModel.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(1));

    //Export
    ExportController ec = Lookup.getDefault().lookup(ExportController.class);
    try {
        ec.exportFile(new File("kk_simple.pdf"));
    } catch (IOException ex) {
        ex.printStackTrace();
        return;
    }
    
    //New Processing target, get the PApplet
    G2DTarget target = (G2DTarget) previewController.getRenderTarget(RenderTarget.G2D_TARGET);
    final PreviewSketch previewSketch = new PreviewSketch(target);
    previewController.refreshPreview();

    //Add the applet to a JFrame and display
    JFrame frame = new JFrame("Test Preview");
    frame.setLayout(new BorderLayout());

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(previewSketch, BorderLayout.CENTER);

    frame.setSize(1024, 768);
    
    //Wait for the frame to be visible before painting, or the result drawing will be strange
    frame.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent e) {
            previewSketch.resetZoom();
        }
      });
    frame.setVisible(true);
  }
  
  public static void main(String[] args) {
    BasicGraphGenerator graphGen = new BasicGraphGenerator();
    //Create three nodes
    graphGen.addNode("n0", "Node 0");
    graphGen.addNode("n1", "Node 1");
    graphGen.addNode("n2", "Node 2");
    
    graphGen.addEdge("n1", "n2", 1);
    graphGen.addEdge("n0", "n2", 1);
    graphGen.addEdge("n2", "n0", 1);
    
    //Computing the page ranks
    graphGen.runPageRank();
//    script();    
    
  }
}
