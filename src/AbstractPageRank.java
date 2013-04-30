import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractPageRank {
  protected int dimension;
  
  private List<Double> prvalues;
  
  private List<Double> preprvalues;
  
  protected Map<Integer, Set<Integer>> transitionMatrix;
  
  protected Map<Integer, Set<Integer>> transposedTransitionMatrix;
  
  public AbstractPageRank(int d, String mfp) {
    this.dimension = d;
    this.prvalues = this.initPageRankVector();
    this.preprvalues = null;
    this.transitionMatrix = Utilities.readTransitionMatrix(mfp);
    this.transposedTransitionMatrix = Utilities.transposeMatrix(this.transitionMatrix);
  }
  
  protected List<Double> initPageRankVector() {
    List<Double> res = new ArrayList<Double>();
    
    for (int i = 0; i < this.dimension; i++) {
      res.add(1.0/(double) this.dimension);
    }
    
    return res;
  }
  
  private boolean isConverged() {
    if (this.preprvalues == null) return false;
    
    double edistance = 0.0;
    
    for (int i = 0; i < this.dimension; i++) {
      edistance += Math.pow(this.preprvalues.get(i) - this.prvalues.get(i), 2.0);
//      System.out.println(this.preprvalues.get(i) + ", " + this.prvalues.get(i) + ", " + edistance);
    }
    edistance = Math.sqrt(edistance) / (double) this.dimension;
    
    System.out.println("Converge? " + edistance);
    return (edistance < 0.00000001);
  }
  
  public List<Double> getPageRankValues() {
    return Collections.unmodifiableList(this.prvalues);
  }
  
  public final void run() {
    int count = 1;
    while(!this.isConverged()) {
      System.out.println("Iteration " + count);
      this.runIteration();
      count++;
    }
  }
  
  protected void updatePageRankValue(List<Double> newprvalues) {
    this.preprvalues = this.prvalues;
    this.prvalues = newprvalues;
  }
  
  protected abstract void runIteration();
}
