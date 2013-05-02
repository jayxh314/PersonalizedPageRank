import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class GlobalPageRank extends AbstractPageRank {
  
  private double dampingFactor; // 1 - alpha
  
  // current pagerank values
  private List<Double> prvalues;
  
  // pagerank values of previous round
  private List<Double> preprvalues;

  public GlobalPageRank(int d, double dp, String mfp) {
    super(d, mfp);
    this.dampingFactor = dp;
    
    this.prvalues = this.initPageRankVector();
    this.preprvalues = null;
  }
  
  /**
   * get the initial pagerank vector.
   * @return
   */
  private List<Double> initPageRankVector() {
    List<Double> res = new ArrayList<Double>();
    
    for (int i = 0; i < this.dimension; i++) {
      res.add(1.0/(double) this.dimension);
    }
    
    return res;
  }
  
  /**
   * judge whether the pagerank vector is converged
   */
  @Override
  protected final boolean isConverged() {
    if (this.preprvalues == null) return false;
    
    double edistance = 0.0;
    
    for (int i = 0; i < this.dimension; i++) {
      edistance += Math.pow(this.preprvalues.get(i) - this.prvalues.get(i), 2.0);
    }
    edistance = Math.sqrt(edistance) / (double) this.dimension;
    
    return (edistance < 0.00000001);
  }

  /**
   * defines the procedure of each iteration, which is the main part
   * of pagerank algorithm
   */
  @Override
  protected final void runIteration() {
    long curtime = System.currentTimeMillis();
    List<Double> curprs = this.getPageRankValues();
    List<Double> newprs = new ArrayList<Double>(this.dimension);
    Set<Integer> nooutlink = new HashSet<Integer>(); // the set to accommodate those nodes without outlinks
    
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
      
      // find a node without outlink
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
    
    // update the pagerank value vector
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

  public static void main(String[] args) {
    
    if (args.length != 3) {
      System.err.println("Usage: <test_dir> <method> <output>");
      return ;
    }
    
    String testDir = args[0];
    int method = Integer.parseInt(args[1]);
    String outpuFilename = args[2];
    
    try {
      BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outpuFilename))));
      
      File[] testFiles = (new File(testDir)).listFiles(new FilenameFilter() {

        @Override
        public boolean accept(File dir, String fname) {
          return fname.endsWith(".results.txt");
        }
        
      });
      
      // run the pagerank algorithm
      GlobalPageRank gpr = new GlobalPageRank(81433, 0.85, "transition.txt");
      gpr.run();

      // get the pagerank scores
      List<Double> scores = gpr.getPageRankValues();
      
      // read the query files
      for (File tfile : testFiles) {
        System.out.println("Processing " + tfile.getName());
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tfile)));
        String userqueryid = tfile.getName().substring(0, tfile.getName().indexOf("."));
        Map<Integer, Double> result = new HashMap<Integer, Double>();
        
        String line = null;
        while((line = reader.readLine()) != null) {
          String[] fields = line.split(" ");
          int docid = Integer.parseInt(fields[2]);
          double relscore = Double.parseDouble(fields[4]);
          double score = 0.0;
          
          // compute the score by different methods
          switch (method) {
            case 1:
              score = scores.get(docid - 1);
              break;
            case 2:
              score = scores.get(docid - 1) + relscore;
              break;
            case 3:
              break;
          }
          
          result.put(docid, score);
        }
        
        // sort the result according to the final score
        List<Entry<Integer, Double>> rankingItems = new ArrayList<Entry<Integer, Double>>(result.entrySet());
        Collections.sort(rankingItems, new Comparator<Entry<Integer, Double>>() {

          @Override
          public int compare(Entry<Integer, Double> arg0, Entry<Integer, Double> arg1) {
            if (arg0.getValue() > arg1.getValue()) return -1;
            if (arg0.getValue() < arg1.getValue()) return 1;
            return 0;
          }
          
        });
        
        for (int i = 0; i < rankingItems.size(); i++) {
          outputWriter.write(userqueryid + " Q0 " + rankingItems.get(i).getKey() + " " + (i+1) + " " + rankingItems.get(i).getValue() + " indri\n");
        }
        outputWriter.flush();
        
        reader.close();
      }
      
      outputWriter.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (NumberFormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
 
  }

}
