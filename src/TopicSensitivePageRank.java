import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class TopicSensitivePageRank extends AbstractPageRank {

  // the parameters
  private double alpha;
  
  private double beta;
  
  private double gama;
  
  // the number of topics
  private int topicNumber;
  
  // current pagerank values for each topic
  private List<List<Double>> topicPRValues;
  
  // pagerank values for each topic of previous iteration
  private List<List<Double>> preTopicPRValues;
  
  // the topic-document relation
  private Map<Integer, Set<Integer>> topicDocuments;
  
  public TopicSensitivePageRank(int d, int tn, double a, double b, String mfp, String dcfp) {
    super(d, mfp);
    
    this.alpha = a;
    this.beta = b;
    this.gama = 1.0 - this.alpha - this.beta;
    this.topicNumber = tn;
    
    this.topicDocuments = Utilities.readTopicDocuments(dcfp);
    this.topicPRValues = this.initPageRankVectors();
    this.preTopicPRValues = null;
  }
  
  /**
   * Initialize the pagerank vectors
   * @return
   */
  private List<List<Double>> initPageRankVectors() {
    List<List<Double>> res = new ArrayList<List<Double>>();
    
    for (int i = 0; i < this.topicNumber; i++) {
      List<Double> reslist = new ArrayList<Double>();
      
      for (int j = 0; j < this.dimension; j++) {
        reslist.add(1.0/(double) this.dimension);
      }
      
      res.add(reslist);
    }
    
    return res;
  }

  @Override
  protected void runIteration() {
    long curtime = System.currentTimeMillis();
    List<List<Double>> curprs = this.getTopicPageRankValues();
    List<List<Double>> newprs = new ArrayList<List<Double>>(this.dimension);
    for (int i = 0; i < this.topicNumber; i++) {
      newprs.add(new ArrayList<Double>(this.dimension));
    }
    
    for (int i = 0; i < this.topicNumber; i++) {
      List<Double> curtpr = curprs.get(i);
      List<Double> newtpr = newprs.get(i);
      Set<Integer> nooutlink = new HashSet<Integer>();
      
      // 1. compute the Beta * r
      double sumr = 0.0;
      for (Double pr : curtpr)
        sumr += pr;
      sumr = (sumr / (double) this.dimension) * this.beta;
      for (int j = 0; j < this.dimension; j++) {
        newtpr.add(j, sumr);
      }
      
      // 2. compute the Alpha * M^T * r
      for (Entry<Integer, Set<Integer>> entry : this.transposedTransitionMatrix.entrySet()) {
        Integer toDocId = entry.getKey();
        double tempsum = 0.0;
        
        for (Integer fromDocId : entry.getValue()) {
          Integer normlen = this.transitionMatrix.get(fromDocId).size();
          tempsum += (1.0/(double) normlen) * curtpr.get(fromDocId - 1) * this.alpha;
        }
        
        newtpr.set(toDocId - 1, newtpr.get(toDocId - 1) + tempsum);
        
        if (!this.transitionMatrix.containsKey(toDocId)) 
          nooutlink.add(toDocId);
      }
      
      // 3. distribute the pr score of those nodes without out links
      sumr = 0.0;
      for (Integer id : nooutlink) {
        sumr += curtpr.get(id - 1);
      }
      sumr = this.alpha * sumr / (double) this.dimension;
      for (int j = 0; j < this.dimension; j++) {
        newtpr.set(j, newtpr.get(j) + sumr);
      }
      
      // 4. for documents within current topic
      Set<Integer> docs = this.topicDocuments.get(i + 1);
      for (Integer docid : docs) {
        newtpr.set(docid - 1, newtpr.get(docid - 1) + this.gama * (1.0 / (double) docs.size()));
      }
    }
    
    this.updatePageRankValue(newprs);
    System.out.println(System.currentTimeMillis() - curtime);
  }
  
  public List<List<Double>> getTopicPageRankValues() {
    return Collections.unmodifiableList(this.topicPRValues);
  }
  
  private void updatePageRankValue(List<List<Double>> newprs) {
    this.preTopicPRValues = this.topicPRValues;
    this.topicPRValues = newprs;
  }

  /**
   * judge whether all the pagerank vectors are converged
   */
  @Override
  protected boolean isConverged() {
    if (this.preTopicPRValues == null) return false;
    
    for (int i = 0; i < this.topicNumber; i++) {
      if (!isConverged(this.topicPRValues.get(i), this.preTopicPRValues.get(i))) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * judge whether the pagerank vector for a specific topic is converged
   */
  private boolean isConverged(List<Double> newv, List<Double> oldv) {
    if (newv == null || oldv == null) return false;
    
    double edistance = 0.0;
    double temp = 0.0;
    for (int i = 0; i < this.dimension; i++) {
      edistance += Math.pow(newv.get(i) - oldv.get(i), 2.0);
      temp += newv.get(i);
    }
    edistance = Math.sqrt(edistance) / (double) this.dimension;
    System.out.println(temp + ", " + edistance);
    return (edistance < 0.00000001);
  }

  public static void main(String[] args) {
    if (args.length != 5) {
      System.err.println("Usage: <topic_num> <distribution_file> <test_dir> <method> <output>");
      return ;
    }
    
    String topicDistFilename = args[1];
    String testDir = args[2];
    int method = Integer.parseInt(args[3]);
    String outputFilename = args[4];
    final int tnum = Integer.parseInt(args[0]);
    
    try {
      BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFilename))));
      
      File[] testFiles = (new File(testDir)).listFiles(new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
          return filename.endsWith(".results.txt");
        }
        
      });
      
      // run the pagerank algorithm
      TopicSensitivePageRank tpr = new TopicSensitivePageRank(81433, tnum, 0.75, 0.15, "transition.txt", "doc_topics.txt");
      tpr.run();
      
      // get the pagerank scores
      List<List<Double>> prvectors = tpr.getTopicPageRankValues();
      
      // get the topic distribution probability
      Map<String, List<Double>> dist = Utilities.readTopicDist(topicDistFilename);
      
      // read the query files
      for (File tfile : testFiles) {
        System.out.println("Processing " + tfile.getName());
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tfile)));
        String userqueryid = tfile.getName().substring(0, tfile.getName().indexOf('.'));
        Map<Integer, Double> result = new HashMap<Integer, Double>();
        
        String line = null;
        while((line = reader.readLine()) != null) {
          String[] fields = line.split(" ");
          result.put(Integer.parseInt(fields[2]), Double.parseDouble(fields[4]));
        }
        
        // compute the score by different methods
        Set<Integer> docs = new HashSet<Integer>(result.keySet());
        switch (method) {
          case 1:
            for (int docid : docs) {
              double score = 0.0;
              for (int i = 0; i < tnum; i++) {
                score += dist.get(userqueryid).get(i) * prvectors.get(i).get(docid - 1);
              }
              result.put(docid, score);
            }
            break;
          case 2:
            for (int docid : docs) {
              double score = 0.0;
              for (int i = 0; i < tnum; i++) {
                score += dist.get(userqueryid).get(i) * prvectors.get(i).get(docid - 1);
              }
              result.put(docid, 0.2 * score + 0.8 * result.get(docid));
            }
            break;
          case 3:
            double prmax = Double.MIN_VALUE;
            double prmin = Double.MAX_VALUE;
            double relmax = Double.MIN_VALUE;
            double relmin = Double.MAX_VALUE;
            for (int docid : docs) {
              double prvalue = 0.0;
              for (int i = 0; i < tnum; i++) {
                prvalue += dist.get(userqueryid).get(i) * prvectors.get(i).get(docid - 1);
              }
              if (prvalue > prmax) prmax = prvalue;
              if (prvalue < prmin) prmin = prvalue;
              
              double relvalue = result.get(docid);
              if (relvalue > relmax) relmax = relvalue;
              if (relvalue < relmin) relmin = relvalue;
            }
            
            for (int docid : docs) {
              double prvalue = 0.0;
              for (int i = 0; i < tnum; i++) {
                prvalue += dist.get(userqueryid).get(i) * prvectors.get(i).get(docid - 1);
              }
              double relvalue = result.get(docid);
              
              result.put(docid, 0.2 * ((prvalue - prmin) / (prmax - prmin)) + 0.8 * ((relvalue - relmin) / (relmax - relmin)));
            }
            break;
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
