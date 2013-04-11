/**
 * Stephen Dunn
 * ChunkTest.java
 * Assignment #1 for cs880
 * 2/5/2013
 */

import java.io.DataOutputStream;

import edu.unh.sdb.common.Datum;
import edu.unh.sdb.common.SDBError;
import edu.unh.sdb.datasource.DataBlock;
import edu.unh.sdb.datasource.DataSource;
import edu.unh.sdb.datasource.ISBounds;
import edu.unh.sdb.datasource.ISBoundsIterator;
import edu.unh.sdb.datasource.ISIterator;
import edu.unh.sdb.datasource.Log;

/**
 * @author Stephen
 */
public class ChunkTest {
  static DataSource chunked = null, original = null;
  private static DataOutputStream dataOut;

  /**
   * @param args Opens xfdl file specified and prints stats to stdout.
   */
  public static void main(String[] args) {
    Log.init("Granite-errors", Log.LogToStdErr);
    SDBError.setErrorReportingLevel(SDBError.AlwaysThrow);

    if (args.length < 1) {
      Log.log("Usage: xfdlfFile");
      return;
    }

    // open chunked file
    try {
      chunked = DataSource.create(args[0], args[0]);
    } catch (Exception e) {
      Log.log("Couldn't open \"" + args[0] + "\"\n");
      return;
    }

    // open original file
    String orig = "";
    try {
      String[] parts = args[0].split("-ch-\\d+.xfdl");
      orig = parts[0] + ".xfdl";
      original = DataSource.create(orig, orig);
      original.activate();
    } catch (Exception e) {
      Log.log("Couldn't open \"" + args[0] + "\"\n");
    }

    // calculate chunk size and check data
    try {
      int n = 0;
      String[] parts = args[0].split("\\.xfdl");
      String numStr = parts[0];
      String num = numStr.substring(numStr.length()-1);
      n = Integer.parseInt(num);

      System.out.println("Input Xfdl: " + args[0]);
      System.out.println("Original:   " + orig);
      chunked.activate();
      chunkTest(n, parts[0] + "-lin");

    } catch (Exception e) {
      Log.log("Filename must end in \"-ch-x.xfdl\", where x is the block size.");
      return;
    }
  }

  static boolean validate(float [] one, float [] two) {
    try {

      if (one.length != two.length) return false;
      for (int i = 0; i < one.length; i++)
        if (one[i] != two[i]) return false;

    } catch (Exception e) { Log.log("Data mismatch detected."); return false; }

    return true;
  }

  /******************** some blatant plagiarism from Dan's code ******************/
  static void chunkTest(int n, String out) {
    System.out.println("Please wait...");

    // calculate original dimensions
    int rowLen = chunked.getBounds().getUpper(0)+1; // row length

    int originalDim = 2;

    if (n*n == rowLen) // if n^2 == rowlen, it was NxN
      originalDim = 2;
    else
      originalDim = 3; // must have been NxNxN

    int[] blockSize = new int[ originalDim ];
    for (int i = 0; i < originalDim; i++)
      blockSize[i] = n;

    int chunkSize = original.volume() / (int)Math.pow(n, originalDim);
    System.out.println("Calculated chunk size: " + chunkSize);
    System.out.println("Actual chunk size:     " + rowLen);
    if (chunkSize != rowLen) {
      Log.log("Chunk sizes do not match.");
      return;
    }

    ISBounds blockBounds = new ISBounds(blockSize);
    Datum dat = chunked.createDatum();
    DataBlock origBlock = original.createDataBlock(blockBounds);
    ISBoundsIterator originalIter = new ISBoundsIterator(original.getBounds(), blockBounds);
    ISIterator chunkedIter = new ISIterator(chunked.getBounds());

    for (; chunkedIter.valid(); chunkedIter.next(), originalIter.next()) {
      try {

        original.subblock(origBlock, originalIter);
        chunked.datum(dat, chunkedIter);

        if (!validate( dat.getFloats(),  origBlock.getFloats()) ) {
          Log.log("The files do not match 1:1.");
          break;
        }

      } catch (Exception e) { System.err.println("Error while reading data."); return; }
    }

    System.out.println("Data sources match 1:1.");
  }
}