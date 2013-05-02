import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class TopicSensitivePageRank extends AbstractPageRank {

  private double alpha;
  
  private double beta;
  
  private double gama;
  
  private int topicNumber;
  
  private List<List<Double>> topicPRValues;
  
  private List<List<Double>> preTopicPRValues;
  
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
//    TopicSensitivePageRank tpr = new TopicSensitivePageRank(81433, 12, 0.75, 0.15, "transition.txt", "doc_topics.txt");
//    
//    tpr.run();
//    
//    List<List<Double>> prs = tpr.getTopicPageRankValues();
//    int count = 1;
//    for (List<Double> tprs : prs) {
//      System.out.println("Topic " + count);
//      count ++;
//      
//      AbstractPageRank.printRankingResult(tprs, 20);
//    }
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
      
      TopicSensitivePageRank tpr = new TopicSensitivePageRank(81433, tnum, 0.75, 0.15, "transition.txt", "doc_topics.txt");
      tpr.run();
      
      List<List<Double>> prvectors = tpr.getTopicPageRankValues();
      
      Map<String, List<Double>> dist = Utilities.readTopicDist(topicDistFilename);
      
      for (File tfile : testFiles) {
        System.out.println("Processing " + tfile.getName());
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tfile)));
        String userqueryid = tfile.getName().substring(0, tfile.getName().indexOf('.'));
        String[] fields = userqueryid.split("-");
        int uid = Integer.parseInt(fields[0]);
        int qid = Integer.parseInt(fields[1]);
        Map<Integer, Double> result = new HashMap<Integer, Double>();
        
        String line = null;
        while((line = reader.readLine()) != null) {
          fields = line.split(" ");
          int docid = Integer.parseInt(fields[2]);
          double score = 0.0;
          
          switch (method) {
            case 1:
              for (int i = 0; i < tnum; i++) {
                score += dist.get(userqueryid).get(i) * prvectors.get(i).get(docid - 1);
              }
              break;
            case 2:
              break;
            case 3:
              break;
          }
          
          result.put(docid, score);
        }
        
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
          outputWriter.write(uid + " Q" + qid + " " + rankingItems.get(i).getKey() + " " + (i+1) + " " + rankingItems.get(i).getValue() + " indri\n");
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
