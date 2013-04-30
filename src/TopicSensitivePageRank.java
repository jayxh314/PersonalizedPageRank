import java.util.*;

public class TopicSensitivePageRank extends AbstractPageRank {

  private double alpha;
  
  private double beta;
  
  private double gama;
  
  private int topicNumber;
  
  private List<List<Double>> tsprvalues;
  
  private List<List<Double>> pretsprvalues;
  
  private Map<Integer, Set<Integer>> topicDocuments;
  
  private Map<Integer, Set<Integer>> documentTopics;
  
  public TopicSensitivePageRank(int d, int tn, double a, double b, String mfp, String dcfp) {
    super(d, mfp);
    
    this.alpha = a;
    this.beta = b;
    this.gama = 1.0 - this.alpha - this.alpha;
    this.topicNumber = tn;
    
    this.topicDocuments = Utilities.readTopicDocuments(dcfp);
    this.documentTopics = Utilities.getDocumentTopics(this.topicDocuments);
    this.pretsprvalues = this.initPageRankVectors();
    this.pretsprvalues = null;
  }
  
  private List<List<Double>> initPageRankVectors() {
    //TODO : initialize the PR vectors for each topics
    return null;
  }

  @Override
  protected void runIteration() {
  }

  @Override
  protected boolean isConverged() {
    for (int i = 0; i < this.topicNumber; i++) {
      if (!isConverged(this.tsprvalues.get(i), this.pretsprvalues.get(i))) {
        return false;
      }
    }
    return true;
  }
  
  private boolean isConverged(List<Double> newv, List<Double> oldv) {
    if (newv == null || oldv == null) return false;
    
    double edistance = 0.0;
    
    for (int i = 0; i < this.dimension; i++) {
      edistance += Math.pow(newv.get(i) - oldv.get(i), 2.0);
    }
    edistance = Math.sqrt(edistance) / (double) this.dimension;
    
    return (edistance < 0.00000001);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {

  }
}
