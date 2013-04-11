/**
 * Stephen Dunn
 * FileStats.java
 * Assignment #1 for cs880
 * 2/5/2013
 */

import edu.unh.sdb.common.AttributeDescriptor;
import edu.unh.sdb.common.Datum;
import edu.unh.sdb.common.RecordDescriptor;
import edu.unh.sdb.common.SDBError;
import edu.unh.sdb.datasource.DataCollection;
import edu.unh.sdb.datasource.DataSource;
import edu.unh.sdb.datasource.ISIterator;
import edu.unh.sdb.datasource.Log;

/**
 * @author Stephen
 */
public class FileStats {
  static DataSource ds = null;

  /**
   * @param args Opens xfdl file specified and prints stats to stdout.
   */
  public static void main(String[] args) {
    Log.init("Granite-errors", Log.LogToStdErr);
    SDBError.setErrorReportingLevel(SDBError.AlwaysThrow);

    if (args.length < 1) {
      Log.log("Usage: xfdlFile");
      return;
    }

    try {
      ds = DataSource.create(args[0], args[0]);
    } catch (Exception e) {
      Log.log("Couldn't open \"" + args[0] + "\n");
      return;
    }
    ds.activate();

    System.out.println("File: " + args[0]);
    printStats(ds);
  }

  // print file stats
  /******************** some blatant plagiarism from Dan's code ******************/
  static void printStats(DataCollection ds) {
    RecordDescriptor desc = ds.getRecordDescriptor();
    float[] max = new float[ds.getNumAttributes()];
    float[] min = new float[ds.getNumAttributes()];
    float[] stdDev = new float[ds.getNumAttributes()];
    float[] avgVal = new float[ds.getNumAttributes()];
    float[] points = new float[ds.getNumAttributes()];

    for (int i = 0; i < ds.getNumAttributes(); i++) {
      max[i] = Float.MIN_VALUE;
      min[i] = Float.MAX_VALUE;
    }

    System.out.println("Bounds: [ " + ds.getBounds().getLower()
        + ", " + ds.getBounds().getUpper() + " ]");

    Datum dat = ds.createDatum();          // make an "appropriate" datum
    ISIterator iter = new ISIterator(ds.getBounds()); // iterate over all

    for (; iter.valid(); iter.next()) {
      ds.datum(dat, iter);

      //--------------  Access Datum one attribute at a time -----------
      float[] vals = dat.getFloats();
      for (int i = 0; i < dat.getNumAttributes(); i++) {
        float val = vals[i];   // always read as float

        if (val > max[i]) max[i] = val;
        else if (val < min[i])
          min[i] = val;
        avgVal[i] += val;

        points[i]++;
      }
    }

    for (int i = 0; i < dat.getNumAttributes(); i++)
      // calculate averages
      avgVal[i] /= points[i];

    // prepare for stdev calculation, need to reiterate over data with avg
    float[] sumOfSquaredDiffs = new float[dat.getNumAttributes()];
    dat = ds.createDatum();          // make an "appropriate" datum
    iter = new ISIterator(ds.getBounds()); // iterate over all
    for (; iter.valid(); iter.next()) {
      ds.datum(dat, iter);
      float[] vals = dat.getFloats();
      for (int j = 0; j < dat.getNumAttributes(); j++)
        sumOfSquaredDiffs[j] += (vals[j] - avgVal[j]) * (vals[j] - avgVal[j]);
    }

    // all information is now available
    for (int i = 0; i < dat.getNumAttributes(); i++) {
      stdDev[i] = (float)Math.sqrt(sumOfSquaredDiffs[i] / points[i]);
      AttributeDescriptor attrib = desc.getAttribute(i);

      System.out.println("Attribute Name: " + attrib.name());
      System.out.println("Avg Value:      " + avgVal[i]);
      System.out.println("Std Dev:        " + stdDev[i]);
      System.out.println("Minimum:        " + min[i]);
      System.out.println("Maximum:        " + max[i]);
    }
  }
}
