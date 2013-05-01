import java.util.*;
import java.util.Map.Entry;

public class GlobalPageRank extends AbstractPageRank {
  
  private double dampingFactor; // 1 - alpha
  
  private List<Double> prvalues;
  
  private List<Double> preprvalues;

  public GlobalPageRank(int d, double dp, String mfp) {
    super(d, mfp);
    this.dampingFactor = dp;
    
    this.prvalues = this.initPageRankVector();
    this.preprvalues = null;
  }
  
  private List<Double> initPageRankVector() {
    List<Double> res = new ArrayList<Double>();
    
    for (int i = 0; i < this.dimension; i++) {
      res.add(1.0/(double) this.dimension);
    }
    
    return res;
  }
  
  protected final boolean isConverged() {
    if (this.preprvalues == null) return false;
    
    double edistance = 0.0;
    
//    for (int i = 0; i < this.dimension; i++) {
//      edistance += Math.pow(this.preprvalues.get(i) - this.prvalues.get(i), 2.0);
//    }
//    edistance = Math.sqrt(edistance) / (double) this.dimension;
    
    for (int i = 0; i < this.dimension; i++) {
      edistance += Math.abs(this.preprvalues.get(i) - this.prvalues.get(i));
    }
    
    System.out.println("Converge? " + edistance);
//    return (edistance < 0.00000001);
    return (edistance < 0.001);
  }

  @Override
  protected final void runIteration() {
    long curtime = System.currentTimeMillis();
    List<Double> curprs = this.getPageRankValues();
    List<Double> newprs = new ArrayList<Double>(this.dimension);
    Set<Integer> nooutlink = new HashSet<Integer>();
    
    // 1. compute Alpha * r
    double sumr = 0.0;
    for (Double pr : curprs)
      sumr += pr;
    sumr = (sumr / (double) this.dimension) * (1.0 - this.dampingFactor);
    for (int i = 0; i < this.dimension; i++) {
      newprs.add(i, sumr);
    }
    
    // 2. compute (1-Alpha) * M^T * r
    for (Entry<Integer, Set<Integer>> entry : this.transposedTransitionMatrix.entrySet()) {
      Integer toDocId = entry.getKey();
      double tempsum = 0.0;
      
      for (Integer fromDocId : entry.getValue()) {
        Integer normlen = this.transitionMatrix.get(fromDocId).size();
        tempsum += (1.0/(double) normlen) * curprs.get(fromDocId - 1) * this.dampingFactor;
      }
      
      newprs.set(toDocId - 1, newprs.get(toDocId - 1) + tempsum);
      
      if (!this.transitionMatrix.containsKey(toDocId))
        nooutlink.add(toDocId);
    }
    
    // 3. distribute the pr score of those nodes without out links
    sumr = 0.0;
    for (Integer id : nooutlink) {
      sumr += curprs.get(id - 1);
    }
    sumr = this.dampingFactor * sumr / (double) this.dimension;
    for (int i = 0; i < this.dimension; i++) {
      newprs.set(i, newprs.get(i) + sumr);
    }
    
    this.updatePageRankValue(newprs);
    System.out.println(System.currentTimeMillis() - curtime);
  }
  
  private void updatePageRankValue(List<Double> newprvalues) {
    this.preprvalues = this.prvalues;
    this.prvalues = newprvalues;
  }
  
  public List<Double> getPageRankValues() {
    return Collections.unmodifiableList(this.prvalues);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    GlobalPageRank gpr = new GlobalPageRank(81433, 0.85, "transition.txt");
    
    gpr.run();
    
    List<Double> scores = gpr.getPageRankValues();
    
    AbstractPageRank.printRankingResult(scores, 30);
  }

}
