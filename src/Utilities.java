import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class Utilities {
  
  /**
   * Read the topic distribution from the file
   * @param fp
   * @return
   */
  public static Map<String, List<Double>> readTopicDist(String fp) {
    Map<String, List<Double>> res = new HashMap<String, List<Double>>();
    
    if (fp == null || fp.length() == 0) return res;
    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fp))));
      
      String line = null;
      
      while( (line = reader.readLine()) != null) {
        String[] fields = line.split(" ");
        int userid = Integer.parseInt(fields[0]);
        int queryid = Integer.parseInt(fields[1]);
        
        List<Double> probs = new ArrayList<Double>();
        for (int i = 2; i < fields.length; i++) {
          probs.add(Double.parseDouble(fields[i].split(":")[1]));
        }
        
        res.put(userid + "-" + queryid, probs);
      }
      
      reader.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return Collections.unmodifiableMap(res);
  }

  /**
   * Read the topic-document relations
   * @param fp
   * @return
   */
  public static Map<Integer, Set<Integer>> readTopicDocuments(String fp) {
    Map<Integer, Set<Integer>> res = new HashMap<Integer, Set<Integer>>();
    
    if (fp == null || fp.length() == 0) return res;
    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fp))));
      
      String line = null;
      while((line = reader.readLine()) != null) {
        String[] fields = line.split(" ");
        int docid = Integer.parseInt(fields[0]);
        int classid = Integer.parseInt(fields[1]);
        
        if (res.containsKey(classid)) {
          res.get(classid).add(docid);
        } else {
          Set<Integer> cset = new HashSet<Integer>();
          cset.add(docid);
          
          res.put(classid, cset);
        }
      }
      
      reader.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return Collections.unmodifiableMap(res);
  }
  
  /**
   * transform the topic-document relations to document-topic relation
   * @param tds
   * @return
   */
  public static Map<Integer, Set<Integer>> getDocumentTopics(Map<Integer, Set<Integer>> tds) {
    Map<Integer, Set<Integer>> res = new HashMap<Integer, Set<Integer>>();
    
    if (tds == null || tds.size() == 0) return res;
    
    for (Entry<Integer, Set<Integer>> entry : tds.entrySet()) {
      Integer tid = entry.getKey();
      
      for (Integer docid : entry.getValue()) {
        if (res.containsKey(docid)) {
          res.get(docid).add(tid);
        } else {
          Set<Integer> tset = new HashSet<Integer>();
          tset.add(tid);
          
          res.put(docid, tset);
        }
      }
    }
    
    return Collections.unmodifiableMap(res);
  }
  
  /**
   * read the transition matrix from the file
   * @param fp
   * @return
   */
  public static Map<Integer, Set<Integer>> readTransitionMatrix(String fp) {
    Map<Integer, Set<Integer>> res = new HashMap<Integer, Set<Integer>>();
    
    if (fp == null || fp.length() == 0) return res;
    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fp))));
 
      String line = null;
      while((line = reader.readLine()) != null) {
        String[] fields = line.split(" ");
        int from = Integer.parseInt(fields[0]);
        int to = Integer.parseInt(fields[1]);
        
        if (res.containsKey(from)) {
          res.get(from).add(to);
        } else {
          Set<Integer> cset = new HashSet<Integer>();
          cset.add(to);
          
          res.put(from, cset);
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (NumberFormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return Collections.unmodifiableMap(res);
  }
  
  /**
   * transpose the transition matrix
   * @param matrix
   * @return
   */
  public static Map<Integer, Set<Integer>> transposeMatrix(Map<Integer, Set<Integer>> matrix) {
    Map<Integer, Set<Integer>> tmatrix = new HashMap<Integer, Set<Integer>>();
    
    if (matrix == null) return tmatrix;
    
    for (Entry<Integer, Set<Integer>> entry : matrix.entrySet()) {
      int from = entry.getKey();
      
      for (Integer to : entry.getValue()) {
        if (tmatrix.containsKey(to)) {
          tmatrix.get(to).add(from);
        } else {
          Set<Integer> fset = new HashSet<Integer>();
          fset.add(from);
          
          tmatrix.put(to, fset);
        }
      }
    }
    
    return Collections.unmodifiableMap(tmatrix);
  }
}
