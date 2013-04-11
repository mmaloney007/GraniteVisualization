package Demos;

/** SimpleStats
 *     This program is intended to provide an initial introduction to Granite
 * by using a few very features provided by the system. There is
 * no attempt to make the application as a whole a useful tool.
 * 
 * The program accepts a command line argument that is an fdl description
 * of a DataSource. Portions of the program care about the dimensionality
 * of the DataSource and the number of attributes; others do not.
 * 
 * The program then reads all the data in the file one Datum at a time
 * and computes the min/max values in the file for all the attributes
 * in the file
 * 
 * @author rdb
 */
import java.io.PrintStream;

import edu.unh.sdb.common.Datum;
import edu.unh.sdb.common.SDBError;
import edu.unh.sdb.datasource.DataCollection;
import edu.unh.sdb.datasource.DataSource;
import edu.unh.sdb.datasource.ISIterator;
import edu.unh.sdb.datasource.Log;

public class SimpleStats
{
  private static String     inName  = null;    // name of input descr file

  //------------------------------- main ---------------------------------
  public static void main( String[] args )
  {
    DataSource   ds = null;  // the original data source
    readArgs( args );

    // The user has some control over what happens with errors discovered
    //   during execution. The Log.init method below says to save error
    //   error messages in a file called "Granite-errors" and to also send
    //   them to standard error.
    Log.init( "Granite-errors", Log.LogToFile | Log.LogToStdErr );

    // Error reporting levels include AlwaysThrow, ThrowFatalOnly,
    // NeverThrow. It's probably best to generate errors as much as
    // possible to be sure that you don't "miss" important problems.
    SDBError.setErrorReportingLevel( SDBError.AlwaysThrow );

    // Using the "Factory Method" design pattern, we let Granite figure out
    //   exactly what kind of DataSource is needed. The second name is
    //   arbitrary and is mostly for debugging and error messages.
    ds = openDataSource( "Demo", inName );
    ds.activate();       // "activate" is equivalent to a file "open"

    //++++++++++++++++ Datum access examples -- compute min/max ++++++++++
    // Compute the min and max values for all attributes in the DataSource
    minMaxByDatumThenAttribute( ds );

  }

  //---------------------- openDataSource ----------------------------
  // a little utility function to read the descriptor file.
  //
  private static DataSource openDataSource( String dsName, String fileName )
  {
    DataSource ds = null;
    try
    {
      ds = DataSource.create( dsName, inName );
    } catch ( Exception ex )
    {
      System.err.println( "Open error: " + ex.getMessage() );
      System.exit( -1 );
    }
    return ds;
  }

  //------------------ minMaxByDatumThenAttribute -----------------------
  /** Compute minmax values by iterating over the data source bounds, 1
   * point at a time. We extract a Datum for each point, then
   * extract the attribute values one at a time from that Datum.
   * 
   * Uses:  datum( IndexSpaceID isid )
   * and:   Datum.getDouble( int attr )
   */
  static void minMaxByDatumThenAttribute( DataCollection ds )
  {
    double[] max  = new double[ ds.getNumAttributes() ];
    double[] min  = new double[ ds.getNumAttributes() ];

    for ( int i = 0; i < ds.getNumAttributes(); i++ )
    {
      max[i] = Double.MIN_VALUE;
      min[i] = Double.MAX_VALUE;
    }

    Datum dat = ds.createDatum();          // make an "appropriate" datum
    ISIterator iter = new ISIterator( ds.getBounds()); // iterate over all
    for ( ; iter.valid(); iter.next() )
    {
      ds.datum( dat, iter );
      //--------------  Access Datum one attribute at a time -----------
      for ( int i = 0; i < dat.getNumAttributes(); i++ )
      {
        double val = dat.getDouble(i) ;   // always read as double
        if ( val > max[i] )
          max[i] = val;
        else if ( val < min[i] )
          min[i] = val;
      }
    }
    printMinMax( min, max );
  }


  //--------------- printMinMax( ... ) ---------------------
  // Print the results
  static void printMinMax( double[] min, double[] max )
  {
    System.out.print( "Minimums: [ " + min[0] );
    for ( int i = 1; i < min.length; i++ )
      System.out.print( ", " + min[i] );
    System.out.println( "]" );

    System.out.print( "Maximums: [ " + max[0] );
    for ( int i = 1; i < max.length; i++ )
      System.out.print( ", " + max[i] );
    System.out.println( "]" );
  }

  //--------------- usage -----------------------------------
  static void usage()
  {
    PrintStream out = System.out; // should pass in output stream

    out.println( "Usage: java SimpleStats fdl " );
    out.println( "    fdl - xfdl file describing the input DataSource" );
  }

  //-------------- readArgs --------------------------------
  //
  static void readArgs( String[] args )
  {
    if ( args.length == 0 )
    {
      usage();
      System.exit( 0 );
    }
    inName = args[ 0 ];
  }
}
