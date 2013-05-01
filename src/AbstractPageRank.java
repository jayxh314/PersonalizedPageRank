import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractPageRank {
  protected int dimension;
  
  protected Map<Integer, Set<Integer>> transitionMatrix;
  
  protected Map<Integer, Set<Integer>> transposedTransitionMatrix;
  
  public AbstractPageRank(int d, String mfp) {
    this.dimension = d;
    this.transitionMatrix = Utilities.readTransitionMatrix(mfp);
    this.transposedTransitionMatrix = Utilities.transposeMatrix(this.transitionMatrix);
  }
  
  public final void run() {
    int count = 1;
    while(!this.isConverged()) {
      System.out.println("Iteration " + count);
      this.runIteration();
      count++;
    }
  }
  
  abstract protected boolean isConverged();
  
  abstract protected void runIteration();
  
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
