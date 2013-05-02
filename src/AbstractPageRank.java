import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractPageRank {
  // the number of nodes in the graph
  protected int dimension;
  
  // the transition matrix
  protected Map<Integer, Set<Integer>> transitionMatrix;
  
  // the transposition of the transition matrix
  protected Map<Integer, Set<Integer>> transposedTransitionMatrix;
  
  public AbstractPageRank(int d, String mfp) {
    this.dimension = d;
    this.transitionMatrix = Utilities.readTransitionMatrix(mfp);
    this.transposedTransitionMatrix = Utilities.transposeMatrix(this.transitionMatrix);
  }
  
  // run the abstract iteration procedure
  public final void run() {
    int count = 1;
    while(!this.isConverged()) {
      System.out.println("Iteration " + count);
      this.runIteration();
      count++;
    }
  }
  
  // interfaces for subclasses
  abstract protected boolean isConverged();
  
  abstract protected void runIteration();
  
  // class for ranking, which records the document id and its pagerank score
  static class RankItem implements Comparable {
    private int docid;
    private double prscore;
    
    public RankItem(int id, double s) {
      this.docid = id;
      this.prscore = s;
    }
    
    public int getDocid() {
      return docid;
    }
    
    public double getPrscore() {
      return prscore;
    }
    
    public String toString() {
      return this.docid + " - " + this.prscore;
    }

    @Override
    public int compareTo(Object arg0) {
      RankItem target = (RankItem) arg0;
      if (this.getPrscore() - target.getPrscore() > 0) return -1;
      if (this.getPrscore() - target.getPrscore() < 0) return 1;
      return 0;
    }
  }
  
  // print out the ranking result
  static void printRankingResult(final List<Double> prvalues, final int topn) {
    List<RankItem> rankingRes = new ArrayList<RankItem>();
    for (int i = 0; i < prvalues.size(); i++) {
      rankingRes.add(new RankItem(i+1, prvalues.get(i)));
    }
    
    Collections.sort(rankingRes);
    
    for (int i = 0; i < topn; i++) {
      System.out.println((i+1) + ", " + rankingRes.get(i).getDocid() + ", " + rankingRes.get(i).getPrscore());
    }
  }
}
