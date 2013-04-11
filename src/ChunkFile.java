/**
 * Stephen Dunn
 * ChunkFile.java
 * Assignment #1 for cs880
 * 2/5/2013
 */

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import edu.unh.sdb.common.RecordDescriptor;
import edu.unh.sdb.common.SDBError;
import edu.unh.sdb.datasource.DataBlock;
import edu.unh.sdb.datasource.DataCollection;
import edu.unh.sdb.datasource.DataSource;
import edu.unh.sdb.datasource.ISBounds;
import edu.unh.sdb.datasource.ISBoundsIterator;
import edu.unh.sdb.datasource.Log;

/**
 * @author Stephen
 */
public class ChunkFile {
  static DataSource ds = null;
  private static DataOutputStream dataOut;

  /**
   * @param args Opens xfdl file specified and prints stats to stdout.
   */
  public static void main(String[] args) {
    Log.init("Granite-errors", Log.LogToStdErr);
    SDBError.setErrorReportingLevel(SDBError.AlwaysThrow);

    if (args.length < 2) {
      Log.log("Usage: xfdlfFile n");
      return;
    }

    int n = 0;
    try {
      n = Integer.parseInt(args[1]);
    } catch (Exception e) {
      Log.log("Usage: xfdlfFile n");
      return;
    }
    if (n <= 0) {
      Log.log("Usage: xfdlfFile n (n > 0)");
      return;
    }

    try {
      ds = DataSource.create(args[0], args[0]);
    } catch (Exception e) {
      Log.log("Couldn't open \"" + args[0] + "\n");
      return;
    }

    try {

      String [] parts = args[0].split("\\.xfdl");
      System.out.println("Input Xfdl:          " + args[0]);
      System.out.println("Requested Chunksize: " + n);
      System.out.println("Output Xfdl:         " + parts[0] + "-ch-" + args[1] + ".xfdl");
      System.out.println("Output Bin:          " + parts[0] + "-ch-" + args[1] + ".bin");
      ds.activate();
      chunk(ds, n, parts[0] + "-ch-" + args[1]);

    } catch (Exception e) {
      Log.log("Filename must end in \".xfdl\"");
      return;
    }
  }

  // print file stats
  /******************** some blatant plagiarism from Dan's code ******************/
  static void chunk(DataCollection ds, int n, String out) {

    System.out.print("Chunking to:         [ ");
    int[] blockSize = new int[ ds.dim() ];
    for (int i = 0; i < ds.dim(); i++) {
      blockSize[i] = n;
      System.out.print(n);
      if ( i != ds.dim()-1 ) System.out.print("x");
    }
    System.out.println(" ]");
    System.out.println("Please wait...");

    openOutput(out + ".bin");

    ISBounds blockBounds = new ISBounds(blockSize);
    ISBoundsIterator iter = new ISBoundsIterator(ds.getBounds(), blockBounds); // iterate over all
    DataBlock block = ds.createDataBlock(blockBounds);

    int discarded = 0, cols = 0;
    for (; iter.valid(); iter.next()) {
      try {

        ds.subblock(block, iter);
        float[] data = block.getFloats();
        if (blockBounds.hasSameShape(iter)) { // only save full blocks
          write(data);
          cols++;
        } else
          discarded++; //update incomplete block count

      } catch (Exception e) { System.err.println("Error while reading data"); break; }

    }

    // calculate new row length
    int rows = n;
    //ISBounds bounds = ds.getBounds();
    //rowLen = bounds.getUpper(0);
    for (int i = 1; i < ds.dim(); i++)
      rows *= n;//bounds.getUpper(i);

    try { writeXFDL(ds, out, rows, cols); }
    catch ( Exception e ) {  Log.log("Failed to write an output xfdl file."); return; }
    if (discarded != 0)
      System.out.println("Output file is not a 1:1 mapping because " + discarded + " incomplete blocks were discarded.");
    System.out.println("Done.");
  }


  //------------------- write( float[] ) ------------------------------
  private static void write( float[] data )
  {
    try
    {
      for (float element : data)
        dataOut.writeFloat( element);
    }
    catch ( IOException ioex )
    {
      System.err.println( "IOException in write: " + ioex );
    }
  }

  //----------------------- openOutput -------------------------------
  private static void openOutput( String name )
  {
    try {
      dataOut = new DataOutputStream(
          new FileOutputStream( name ));
    }
    catch ( IOException e )
    {
      System.err.println( "Can't open output file" );
      System.exit( 1 );
    }
  }
  //---------------------- openDataSource ----------------------------


  private static void writeXFDL( DataCollection block, String prefix, int rows, int cols )
      throws IOException
      {
    String fileName = prefix + ".bin";
    PrintWriter pw = new PrintWriter(
        new FileOutputStream( prefix + ".xfdl" ));

    pw.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
    pw.println( "<!DOCTYPE FileDescriptor PUBLIC" +
        " \"-//SDB//DTD//EN\"  \"fdl.dtd\">" );
    pw.println( "<FileDescriptor fileName=\"" + fileName +"\"" );
    pw.println( "                fileType=\"binary\">" );

    generateBounds( pw, rows, cols );

    // Now do the fieldNames and types:
    generateFieldDescription( block, pw );
    pw.println( "</FileDescriptor>" );
    pw.close();
      }

  private static void generateFieldDescription( DataCollection block,
                                                PrintWriter out )
  {
    RecordDescriptor rd = block.getRecordDescriptor( );
    String[] fieldNames = rd.getFieldNames( );
    String[] fieldTypes = rd.getFieldTypes( );
    for (String fieldName : fieldNames)
      out.println(  "    <Field fieldName=\"" + fieldName
          + "\"   fieldType=\"" + "float" + "\"/>");
    //+ "\"   fieldType=\"" + fieldTypes[i] + "\"/>");
  }



  private static void generateBounds( PrintWriter out, int rows, int cols )
  {
    out.println(  "    <Bounds lower= \"" + 0
        + "\"    upper= \"" + (cols-1) + "\"/>");
    out.println(  "    <Bounds lower= \"" + 0
        + "\"    upper= \"" + (rows-1) + "\"/>");
  }
  /*private static void generateBounds( DataCollection block, PrintWriter out )
  {
    ISBounds bounds = block.getBounds( );
    for( int i = 0; i < bounds.dim( ); i++)
      out.println(  "    <Bounds lower= \"" + bounds.getLower( i)
          + "\"    upper= \"" + bounds.getUpper( i) + "\"/>");
  }*/

}
