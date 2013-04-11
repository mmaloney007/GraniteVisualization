package Demos;






/** SubblockDemo.java
 *     This program is intended to provide an introduction to Granite
 * techniques for extracting a subblock from an existing file.
 * 
 * The program accepts a command line argument that is an fdl description
 * of a DataSource. 
 * 
 * @author rdb
 */
import java.util.ArrayList;
import java.io.*;
import edu.unh.sdb.common.*;
import edu.unh.sdb.datasource.*;

public class SubblockDemo
{
   private static String     inName  = null;    // name of input descr file
   private static int[]      boundsArgs = null; // not null, do subset
   private static int[]      stepArgs   = null; // not null, use as skip vector
   private static DataOutputStream dataOut;
   
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
     ds = openDataSource( "SubblockDemo", SubblockDemo.inName );  
     ds.activate();       // "activate" is equivalent to a file "open"
     
     openOutput( "subblock.bin" );
     // Now create a DataBlock with desired subblock 
        ISBounds selection;
        if ( SubblockDemo.boundsArgs == null )
           selection = ds.getBounds(); // get all of it
        else
        {
           if ( boundsArgs.length == ds.dim() )
              selection = new ISBounds( boundsArgs );
           else if ( boundsArgs.length == 2 * ds.dim() )
           {
              int[] lower = new int[ ds.dim() ];
              int[] upper = new int[ ds.dim() ];
              for ( int i = 0; i < ds.dim(); i++ )
              {
                 lower[ i ] = boundsArgs[ i ];
                 upper[ i ] = boundsArgs[ i + ds.dim() ];
              }
              selection = new ISBounds( lower, upper );
           }
           else
           {
              System.out.println( "Error: bounds argument must be same"
                    + " as DataSource dimension (" + ds.dim()
                    + ") or 2 times that. bounds length is: " 
                    + boundsArgs.length );
              System.out.println( "Using entire DataSource.");
              selection = ds.getBounds();
           }
        }

        DataBlock block;
        if ( SubblockDemo.stepArgs == null )
        {
           block = ds.subblock( selection );
        }
        else
        {
           ArrayList<Datum> data = new ArrayList<Datum>();
           ISIterator iter = new ISIterator( selection, stepArgs );
           Datum dat = ds.createDatum();
           float[] vals = dat.getFloats();
           for ( ; iter.valid(); iter.next() )
           {
               ds.datum( dat, iter );
               vals = dat.getFloats( vals );     // extract all attr values 
               write( vals );
           }
        }
        try {
           dataOut.close();
        }
        catch ( Exception ex )
        {
           System.err.println( "dataOutput.close() threw exception " );
        }
     }
   //------------------- write( float[] ) ------------------------------
   private static void write( float[] data )
   {
      try
      {
         for ( int i=0; i < data.length; i++ )
            dataOut.writeFloat( data[ i ]);
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
   // a little utility function to read the descriptor file.
   //
   private static DataSource openDataSource( String dsName, String fileName )
   {
      DataSource ds = null;
      try 
      {
         ds = DataSource.create( dsName, SubblockDemo.inName );
      } catch ( Exception ex )
      {
         System.err.println( "Open error: " + ex.getMessage() );
         System.exit( -1 );
      }
      return ds;
   }
      
   //--------------- usage -----------------------------------
   static void usage()
   {
      PrintStream out = System.out; // should pass in output stream
      
      out.println( "Usage: java SubblockDemo fdlFile [-b bnds] [-s steps]" );
      out.println( " fdl  xfdl file describing the input DataSource" );
      out.println( " -b   extract subblock of the DataSource as block input." );
      out.println( " bnds  bnds must be either n ints or 2n ints where" );
      out.println( "       n is the dim of DataSource. ");      
      out.println( " -s   use steps array for incrementing through subblock" );
      out.println( " steps  array of steps, 1 per dimension. step 2 says" );
      out.println( "        skip every other entry in that dimension");      
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
      ArrayList<Integer> intArgs = new ArrayList<Integer>();
      int iarg = 0; 
      while ( iarg < args.length )
      {
         String sarg = args[ iarg ];
         if ( SubblockDemo.inName == null )  // not switch, check if ds
            SubblockDemo.inName = args[ iarg ];
         else // bounds and/or step arguments
         {
            if ( sarg.equals( "-b" )) 
            {
                iarg++;
                boundsArgs = readInts( args, iarg );
                iarg += boundsArgs.length;
            }
            else if ( sarg.equals( "-s" ))
            {
                iarg++;
                stepArgs = readInts( args, iarg );
                iarg += stepArgs.length;
            }
            else 
            {
                System.err.println( "Unrecognized option: " + sarg );
                System.exit( 2 );
            }
         }
         iarg++;
      }
      if ( intArgs.size() > 0 )
      {
         SubblockDemo.boundsArgs = new int[ intArgs.size() ];
         for ( int i = 0; i < intArgs.size(); i++ )
            SubblockDemo.boundsArgs[i] = ((Integer)intArgs.get( i )).intValue();
      }
   }
   //------------------- readInts ---------------------------------------
   /**
    * read as many ints from command line as you can
    */
   static int[] readInts( String[] args, int iarg )
   {
       return readInts( args, iarg, Integer.MAX_VALUE );
   }
   //------------------- readInts ---------------------------------------
   /**
    * read upto a specified number of ints from command line
    */
   static int[] readInts( String[] args, int iarg, int maxNum )
   {
       ArrayList<Integer> intArgs = new ArrayList<Integer>();

       int numsLeft = maxNum;
       while ( iarg < args.length && numsLeft > 0 )
       {
            try 
            {
               intArgs.add( new Integer( args[ iarg ] ));
               iarg++;
               numsLeft--;
            }
            catch ( NumberFormatException nfe )
            {
               System.err.println( "readInts nfe: " + args[ iarg ] );
               numsLeft = 0;   
            }
        }
        int[] ret = null;
        if ( intArgs.size() > 0 )
        {
           ret = new int[ intArgs.size() ];
           for ( int i = 0; i < ret.length; i++ )
              ret[ i ] = intArgs.get( i );
        }
        return ret;
    }
}
