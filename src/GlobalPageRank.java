import java.util.*;
import java.util.Map.Entry;

public class GlobalPageRank extends AbstractPageRank {
  
  private double dampingFactor; // 1 - alpha

  public GlobalPageRank(int d, double dp, String mfp) {
    super(d, mfp);
    this.dampingFactor = dp;
  }

  @Override
  protected void runIteration() {
    long curtime = System.currentTimeMillis();
    List<Double> curprs = this.getPageRankValues();
    List<Double> newprs = new ArrayList<Double>(this.dimension);
    
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
    }
    
    this.updatePageRankValue(newprs);
    System.out.println(System.currentTimeMillis() - curtime);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    GlobalPageRank gpr = new GlobalPageRank(81433, 0.85, "transition.txt");
    
    gpr.run();
    
    List<Double> scores = gpr.getPageRankValues();
    
    PriorityQueue<Double> temp = new PriorityQueue<Double>(100, new Comparator<Double>() {

      @Override
      public int compare(Double arg0, Double arg1) {
        if (arg0 - arg1 > 0) return 1;
        if (arg0 - arg1 < 0) return -1;
        return 0;
      }
      
    });
    for (Double s : scores){
      if (temp.size() == 100) {
        temp.poll();
        temp.offer(s);
      } else {
        temp.offer(s);
      }
    }
    
    while(!temp.isEmpty()) {
      System.out.println(temp.poll());
    }
  }

}
