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
}
