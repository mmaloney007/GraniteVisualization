package Demos;






/** BasicDemo
 *     This program is intended to provide an introduction to Granite
 * by using a variety of basic features provided by the system. There is
 * no attempt to make the application as a whole a useful tool.
 * 
 * The program accepts a command line argument that is an fdl description
 * of a DataSource. Portions of the program care about the dimensionality
 * of the DataSource and the number of attributes; others do not.
 * 
 * The command line option '-t' lets you select individual tests. 
 * The argument following -t is a string of characters indicating
 * the tests that are to be performed and the order of execution.
 * The legal characters are
 *    a - minmax; extract data by attribute, all values of 1 attr in 1 array
 *    A - minmax; extract all data at once ordered by Attribute
 *    P - minmax; extract all data at once ordered by Point
 *    d - minmax; extract by datum and then by individual attribute
 *    D - minmax; extract by datum and then get all attributes as a 1 array
 *    v - minmax; extract each attribute 1 value and 1 point at a time.
 *    V - minmax; extract all attributes of each point.
 *    w - generate a wavelet decomposition of the input file
 * 
 * The program also writes out elapsed time for each operation, but Java 
 * timing is tricky and timing I/O is even more problematic. There is a lot 
 * of initial overhead, so if you want to test a bunch of things in one run, 
 * don't trust the first test you do.
 * 
 * If you are testing DataSource computation (vs DataBlock), I/O becomes a 
 * big deal, especially if the file is large. There is also an issue with 
 * File system caching. If you really care about getting valid tests, you 
 * need to test each feature in a separate run AND you must run a program 
 * between the runs that makes sure that all of the operating system's file 
 * cache has been "emptied" of the data that is in the file you are using 
 * to test. The Granite project has such a program called "thrashcache".
 * 
 * @author rdb
 */
import java.util.ArrayList;
import java.io.*;
import edu.unh.sdb.common.*;
import edu.unh.sdb.datasource.*;

public class BasicDemo
{
   private static String     inName  = null;    // name of input descr file
   private static boolean    verbose = false;   // set by -v switch 
   private static boolean    useDS = false;     // true => run on DataSource
   private static boolean    useDB = false;     // true => run on DataBlock
   private static int[]      boundsArgs = null; // if not null, do subset
   
   private static String     testString = "dDvVaAPw"; // default code to run
   
   //------------------------------- main ---------------------------------
   public static void main( String[] args )
   {
     DataSource   ds = null;  // the original data source
     readArgs( args );
     if ( BasicDemo.boundsArgs != null ) // if bounds specified do DB tests
        BasicDemo.useDB = true;
     if ( !BasicDemo.useDB  ) // if DB testing not specified, don't need  DS
        BasicDemo.useDS = true;
      
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
     ds = openDataSource( "Demo", BasicDemo.inName );  
     ds.activate();       // "activate" is equivalent to a file "open"
     
     //++++++++++++++++ Datum access examples -- compute min/max ++++++++++
     // Compute the min and max values for all attributes in the DataSource
     if ( BasicDemo.useDS )
        minMaxTests( ds, "DataSource tests", BasicDemo.testString );
    
     // Now create a DataBlock with the same data and do the MinMaxTests again
     if ( BasicDemo.useDB )
     {
        ISBounds selection;
        if ( BasicDemo.boundsArgs == null )
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
                    + ") or 2 times that. bounds length is: " + boundsArgs.length );
              System.out.println( "Using entire DataSource.");
              selection = ds.getBounds();
           }
        }
        DataBlock block = ds.subblock( selection );
        minMaxTests( block, "DataBlock tests", BasicDemo.testString );
     }

     //+++++++++++++++++ Block and point iteration examples +++++++++++++++
     // Block iteration. Given 2n x 2n x 2n DataSource, produce n x n x n
     
     if ( BasicDemo.testString.indexOf( 'w' ) >= 0 )
        for ( int attr = 0; attr < ds.getNumAttributes(); attr++ )
           doWavelet( ds, attr, "1-level Haar wavelet for attr: " + attr );
   }
   
   //---------------------- openDataSource ----------------------------
   // a little utility function to read the descriptor file.
   //
   private static DataSource openDataSource( String dsName, String fileName )
   {
      DataSource ds = null;
      try 
      {
         ds = DataSource.create( dsName, BasicDemo.inName );
      } catch ( Exception ex )
      {
         System.err.println( "Open error: " + ex.getMessage() );
         System.exit( -1 );
      }
      return ds;
   }
   //----------- minMaxTests( DataSource ) ---------------------------
   // Select desired versions of the minMax test and invoke and time it.
   //
   static void minMaxTests( DataCollection ds, String header, String tests )
   {
      // We're going to do the min/max computations in a bunch of different 
      //   ways and we'll time it just for fun.
      
      long start;
      float  elapsedSecs;
      String whichTest;
            
      System.out.println( "+++++++++++++++++++++++ " + header 
                      + " Min/Max tests " + "++++++++++++++++++++++++" );
      
      for ( int t = 0; t < tests.length(); t++ )
      {
         System.gc();
         start = System.currentTimeMillis(); 
         switch ( tests.charAt( t ))
         {
            case 'd': 
               minMaxByDatumThenAttribute( ds );
               whichTest = "datum then attribute (d)";
               break;
            case 'D': 
               minMaxByDatumThenArray( ds );
               whichTest = "datum then array (D)";
               break;
            case 'v': 
               minMaxBySingleAttrValue( ds );
               whichTest = "single value (v)";
               break;
            case 'V': 
               minMaxByAllAttrAtPoint( ds );
               whichTest = "all attributes at each point (V)";
               break;
            case 'a': 
               minMaxByAllAttrValues( ds );
               whichTest = "all attribute values (a)";
               break;
            case 'A': 
               minMaxByAllInAttributeOrder( ds );
               whichTest = "all data in attribute order (A)";
               break;
            case 'P': 
               minMaxByAllInPointOrder( ds );
               whichTest = "all data in point order (P)";
               break;
            default:
               whichTest = null;
         }
         elapsedSecs = ( System.currentTimeMillis() - start ) / 1000.0f;
         if ( whichTest != null )
         {
            System.out.println( "Min/Max by " + whichTest + 
                                ". Time (secs) = " + elapsedSecs );
            System.out.println( "---------------------------------------");
         }
      }
      
      System.out.println( "+++++++++++++++++++++++++++++++++++++++++" +
      "++++++++++++++++++++++++" );
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

   //------------------ minMaxByDatumThenArray -----------------------------
   /**
    * Compute minmax values by iterating over the data source bounds, 1
    * point at a time. Extract a Datum for each point, then extract all the 
    * attributes from the datum once as a single array.
    * 
    * Uses: datum( IndexSpaceID isid )
    * and   Datum.getDoubles()
   */
   static void minMaxByDatumThenArray( DataCollection ds )
   {
      double[] max  = new double[ ds.getNumAttributes() ];
      double[] min  = new double[ ds.getNumAttributes() ];
      double[] vals = new double[ ds.getNumAttributes() ]; // for array
      
      for ( int i = 0; i < ds.getNumAttributes(); i++ )
      {
         max[i] = Double.MIN_VALUE;
         min[i] = Double.MAX_VALUE;
      }
      
      Datum dat = ds.createDatum();          
      ISIterator iter = new ISIterator( ds.getBounds()); // iterate over all
      for ( ; iter.valid(); iter.next() )
      {
         ds.datum( dat, iter );
         vals = dat.getDoubles( vals );     // extract all attr values 
         for ( int i = 0; i < dat.getNumAttributes(); i++ )
         { 
            if ( vals[i] > max[i] )
               max[i] = vals[i];
            else if ( vals[i] < min[i] )
               min[i] = vals[i];
         }
      }
      printMinMax( min, max );
   }

   //------------------ minMaxBySingleAttrValue ---------------------------
   /**
    * Compute minmax values by iterating over data source bounds, 1 point
    * at a time and for each point extract each attribute value 1 at a time.
    * Uses: getDouble( IndexSpaceID isid, int attr )
   */
   static void minMaxBySingleAttrValue( DataCollection ds )
   {
      double[] max = new double[ ds.getNumAttributes() ];
      double[] min = new double[ ds.getNumAttributes() ];
      for ( int i = 0; i < ds.getNumAttributes(); i++ )
      {
         max[i] = Double.MIN_VALUE;
         min[i] = Double.MAX_VALUE;
      }
      
      ISIterator iter = new ISIterator( ds.getBounds()); // iterate over all
      for ( ; iter.valid(); iter.next() )
      {
         for ( int i = 0; i < ds.getNumAttributes(); i++ )
         { 
            double val = ds.getDouble( iter, i ) ;   // read as double
            if ( val > max[i] )
               max[i] = val;
            else if ( val < min[i] )
               min[i] = val;
         }
      }
      printMinMax( min, max );
   }

   //------------------ minMaxByAllAttrValues -----------------------------
   /**
    * Compute minmax values by extracting all values of each attribute into 
    * a single array.
    * 
    * Uses: getDoubles(  int attr )
   */
   static void minMaxByAllAttrValues( DataCollection ds )
   {
      double[] max = new double[ ds.getNumAttributes() ];
      double[] min = new double[ ds.getNumAttributes() ];
      for ( int i = 0; i < ds.getNumAttributes(); i++ )
      {
         max[i] = Double.MIN_VALUE;
         min[i] = Double.MAX_VALUE;
      }
      
      for ( int attr = 0; attr < ds.getNumAttributes(); attr++ )
      {
         double[] vals = ds.getDoubles( attr ); // get all vals of 1 attr
         for ( int i = 0; i < ds.volume(); i++ )
         {
            if ( vals[i] > max[ attr ] )
               max[ attr ] = vals[i];
            else if ( vals[i] < min[ attr ] )
               min[ attr ] = vals[i];
         }
      }
      printMinMax( min, max );
   }

   //------------------ minMaxByAllAttrAtPoint ----------------------------
   /**
    * Compute minmax values by iterating over the data points and extracting 
    * all attribute values for the point into a single array.
    * 
    * Uses: getDoubles(  IndexSpaceID isid )
   */
   static void minMaxByAllAttrAtPoint( DataCollection ds )
   {
      double[] max = new double[ ds.getNumAttributes() ];
      double[] min = new double[ ds.getNumAttributes() ];
      for ( int i = 0; i < ds.getNumAttributes(); i++ )
      {
         max[i] = Double.MIN_VALUE;
         min[i] = Double.MAX_VALUE;
      }
      
      ISIterator iter = new ISIterator( ds.getBounds()); // iterate over entire ds
      for ( ; iter.valid(); iter.next() )
      {
         for ( int i = 0; i < ds.getNumAttributes(); i++ )
         { 
            double val = ds.getDouble( iter, i ) ;   // always read as double
            if ( val > max[i] )
               max[i] = val;
            else if ( val < min[i] )
               min[i] = val;
         }
      }
      printMinMax( min, max );
   }

   //------------------ minMaxByAllInPointOrder ---------------------------
   /**
    * Compute minmax values by extracting all the data into a single array
    * organized by point. We then find the min/max values by traversing the
    * array with a double for loop -- the outer loop steps over one point
    * (IndexSpaceID) at a time while the inner loop accesses the attribute
    * values for that point which are contiguous in the array.
    *
    * This approach is not advisable with very large data sets since it
    * forces the entire data set into memory. It might, however have
    * advantages for relatively small data sets, since there is only one 
    * data access request and the computational loop (what there is of it) 
    * just processes simple native Java arrays.
    * 
    * Uses: getDoublesByPoint();
    */
   static void minMaxByAllInPointOrder( DataCollection ds )
   {
      double[] max = new double[ ds.getNumAttributes() ];
      double[] min = new double[ ds.getNumAttributes() ];
      for ( int i = 0; i < ds.getNumAttributes(); i++ )
      {
         max[i] = Double.MIN_VALUE;
         min[i] = Double.MAX_VALUE;
      }
      
      double[] allVals = ds.getDoublesByPoint(); // get all in point order
      int      ival    = 0;                      // index into allVals
      for ( int pt = 0; pt < ds.volume(); pt++ ) // volume returns # points
      {
         for ( int attr = 0; attr < ds.getNumAttributes() ; attr++ )
         { 
            double val = allVals[ ival++ ];
            if ( val > max[ attr ] )
               max[ attr ] = val;
            else if ( val < min[ attr ] )
               min[ attr ] = val;
         }
      }
      printMinMax( min, max );
   }
   
   //------------------ minMaxByAllInAttributeOrder -----------------------
   /**
    * Compute minmax values by extracting all the data into a single array
    * organized by attribute. Then go through the array with a nested for
    * loop to find the min and max of each attribute.
    * Compute minmax values by extracting all the data into a single array
    * organized by attribute. Then find the min/max values by traversing the
    * array with a double for loop -- the outer loop steps over each at
    * while the inner loop accesses each of the attribute values for all
    * points which are contiguous in the array.
    *
    * This approach is not advisable with very large data sets since it 
    * forces the entire data set into memory at the same time. It might have
    * advantages for relatively small data sets, since there is only one 
    * data access request and the computational loop (what there is of it) 
    * just processes simple native Java arrays.
    * 
    * Uses: getDoublesByAttribute()
    */
   static void minMaxByAllInAttributeOrder( DataCollection ds )
   {
      double[] max = new double[ ds.getNumAttributes() ];
      double[] min = new double[ ds.getNumAttributes() ];
      for ( int i = 0; i < ds.getNumAttributes(); i++ )
      {
         max[i] = Double.MIN_VALUE;
         min[i] = Double.MAX_VALUE;
      }
      
      double[] allVals = ds.getDoublesByAttribute(); // get all by point
      int      ival    = 0;                   // index into allVals
      for ( int attr = 0; attr < ds.getNumAttributes() ; attr++ )
      {
         for ( int pt = 0; pt < ds.volume(); pt++ )  // volume -> # points
         { 
            double val = allVals[ ival++ ];
            if ( val > max[ attr ] )
               max[ attr ] = val;
            else if ( val < min[ attr ] )
               min[ attr ] = val;
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
   
   //----------------- doWavelet ----------------------------
   // A driver program to setup calls to the real Wavelet functions and do
   // some output.
   // 
   static void doWavelet( DataCollection highRes, 
                                 int attribute, String header )
   {
      long  start = 0;   // start time
      float elapsedSecs = 0.0f;
      
      System.out.println( "++++++++++++++++++++ Wavelet  ++++++++++++++++");
      System.out.println( "---------on attribute: " + attribute );
      DataBlock lowRes = null;
      
      if ( verbose )
         printBlock( highRes.subblock( 
                               new RecordSpec( new int[] { attribute }) ), 
                               "======== Input data ===========" );
      System.gc();
      start = System.currentTimeMillis();      
            
      switch ( highRes.dim() )
      {
         case 1: lowRes = wavelet1D( highRes, attribute );
                 break;
         case 2:       // same code does 2D and 3D wavelet (and nD as well)
         case 3: lowRes = wavelet( highRes, attribute );
                 break;
         default: System.err.println( "Wavelet not supported for dimon: " + 
                                                highRes.dim() );
      }
      elapsedSecs = ( System.currentTimeMillis() - start ) / 1000.0f;
      System.out.println( "Wavelet time  (secs) = " + elapsedSecs );

      printBlock( lowRes, "========== Output data ===========" );
   }
   
   //------------------------ wavelet1D -----------------------------
   /** Shows basic access to 1D DataSources
    * The wavelet method can also handle 1D wavelets, but this method shows
    * a different approach to extracting and working with the data.
    */
   static DataBlock wavelet1D( DataCollection inData, int attr )
   {
      // output bounds will be half the size of the input
      int[]  outSize = new int[] { inData.getBounds().getDimension(0) / 2 };
      
      ISBounds  outBounds = new ISBounds( outSize );
      DataBlock outBlock  = inData.createDataBlock( outBounds, 
                                       new RecordSpec( new int[] { attr }));
      
      // For the 1D version, we'll pull out the array, do the math
      //   and stuff the new array back into the output block in one shot.
      
      float[] outVals = new float [outBlock.volume() ];
      float[] inVals  = inData.getFloats( attr );
      
      // the Haar wavelet transform averages successive pairs of values
      for ( int i = 0; i < outVals.length; i++ )
         outVals[ i ] = ( inVals[ 2*i ] + inVals[ 2*i+1 ] )/ 2.0f;
      
      outBlock.setFloats( 0, outVals );
      return outBlock;
   }

   //------------------------ wavelet -----------------------------
   /**
    * Shows basic block access and block iteration over a data set.
    * Note that this code can do a Haar wavelet on any dimensionality,
    * with relatively simple code.
    */
   static DataBlock wavelet( DataCollection inData, int attr )
   {
      // First get the bounds of the input data; need it several places.
      // Notice that we use the "Reference" version to get the Bounds rather
      // than the copy version, getBounds(). 
      // Since we're not going to change the values, this is more efficient.
      
      ISBounds inBounds = inData.getBounds();
      // need output bounds to be 1/2 size of the input in all dimensions
      // first get the sizes of the input 
      int[]  outSizes = inBounds.getDimensionsArray(); 
      
      // now halve them -- for odd-length input, we'll just lose the end, 
      //        but thats ok
      for ( int i = 0; i < inData.dim(); i++ )    
         outSizes[i] = outSizes[i] / 2;
      
      // now make a bounds for the output
      ISBounds outBounds = new ISBounds( outSizes );
      
      // get a RecordSpec to extract just the right attribute
      RecordSpec rs = new RecordSpec( new int[] { attr });
           
      // get an output block with the right bounds and just one attribute
      DataBlock outBlock  = inData.createDataBlock( outBounds, rs );
      
      // For a 2D wavelet, extract a 2x2 block and average the 4 values.
      // To do the 3D wavelet, we need to extract 2x2x2 blocks from
      // the input and average all 8 values to get the output.
      // This code, however, is dimension-independent.
      
      int[]    waveBasis = new int[ inData.dim() ];
      for ( int i = 0; i < waveBasis.length; i++ )
         waveBasis[ i ] = 2;
      
      ISBounds waveBounds = new ISBounds( waveBasis );
      
      // And pre-allocated a DataBlock to take the 2x2x ... x 2 subblock
      DataBlock waveBlock = inData.createDataBlock( waveBounds, rs ); 
      
      // We need a block iterator that will go through the data space.      
      ISBoundsIterator inIter = new ISBoundsIterator( inBounds, waveBounds );
      
      // We also need an iterator over the output space, but this is a simple
      // iterator that just identifies one point at a time (not a block).
      ISIterator outIter = new ISIterator( outBounds );
      
      // pre-allocate an array to fill with the data
      float[] vals = new float[ waveBlock.volume() ];

      for ( ; inIter.valid(); inIter.next(), outIter.next() )
      {
         inData.subblock( waveBlock, inIter, rs );  // get the next block
         waveBlock.getFloats( vals );
         
         // now compute the average of the values in the block
         float sum = 0.0f;
         for ( int i = 0; i < vals.length; i++ )
            sum += vals[ i ];
         outBlock.setFloat( outIter, 0, sum/vals.length ); // store result
      }
      return outBlock;
   }

   //------------------------ printBlock -----------------------------
   // This method tries to do some basic formatting of the output. It works
   // ok for small files and dimensions of 3 or less.
   //
   static void printBlock( DataCollection inData, String header )
   {
      int      inDim = inData.dim();
      ISBounds inBounds = inData.getBounds();
      
      System.out.println( "++++++++++++++++++++++++++++++" + header 
            + "++++++++++++++++++++++++++++++");
      if ( inDim == 1 )
         printRow( 0, inData.getFloats());
      else if ( inDim == 2 )
         printSlice( inData, 0 );
      else  // extract 3D slices one at a time
      {
         int sliceDim = inDim - 3;  // for 3D data set this is axis 0
         int numSlices = inBounds.getDimension( sliceDim );
         for ( int slice = 0; slice < numSlices; slice++ )
         {
            System.out.println( "------------------ slice "+  slice + 
                                "----------------------------------------");
            printSlice( inData, slice );
         }
      }
      System.out.println( "++++++++++++++++++++++++++++++++++++++++++++++++" 
            + "++++++++++++++++++++++++++++++");
   }

   //------------------------ printSlice -----------------------------
   static void printSlice( DataCollection inData, int whichSlice )
   {
      DataCollection printBlock = inData;
      
      int      inDim     = inData.dim();
      int      rowDim = inDim - 2;
      int      colDim = inDim - 1;
      int      sliceDim = inDim - 3;
      int      numRows;              // will be set to # rows to print
      float[]  rowVals = null;  // will be assign data for a row
      
      ISBounds inBnds    = inData.getBounds();
      int[]    sliceSizes = null;   // this array will first be lengths of a slice
     
      if ( inDim > 2 )
      {
         sliceSizes = new int [ inDim ];
         if ( inDim > 3 )
         {
            System.err.println( "printSlice: supports slices of 3D data " 
                             + " only. Doing 1st 3d Hyperslice only.");
         }
         for ( int i = 0; i < rowDim; i++ )
            sliceSizes[ i ] = 1;
            
         sliceSizes[ rowDim ] = inBnds.getDimension( rowDim );
         sliceSizes[ colDim ] = inBnds.getDimension( colDim );
         ISBounds printBounds = new ISBounds( sliceSizes );
         
         // This printBounds actually identifies slice 0; we need to
         // change its bounds in the slice dimension to select the slice.
         try {
         printBounds.setLower( sliceDim, whichSlice );
         printBounds.setUpper( sliceDim, whichSlice );
         } catch ( Exception ex )
           { 
              System.err.println( "printSlice: bad slice. " + whichSlice );
           }
         printBlock = inData.subblock( printBounds );
      }
      
      ISBounds rowBounds = printBlock.getBounds();  // entire block at 1st
      
      if ( inDim == 1 )
         printRow( 0, printBlock.getFloats());
      else
      {
         numRows = printBlock.getBounds().getDimension( rowDim );
         for ( int r = 0; r < numRows; r++ )
         {
            rowBounds.setLower( rowDim, r ); // select just 1 row
            rowBounds.setUpper( rowDim, r ); 
            rowVals = printBlock.subblock( rowBounds ).getFloats( rowVals ); 
            // now print the data
            printRow( r, rowVals );
         }
      }
      System.out.println( "---------------------------------------------" );
   }
   //-------------- printRow  --------------------------------
   //
   static void printRow( int row, float[] vals )
   { 
      int      numValsPerLine = 20;
      int      numOnLine = 0;
      
      String   indent  = " ";
      String   indentIncr = "    ";

      for ( int c = 0; c < vals.length; c++ )
      {
         if ( numOnLine == 0 )
            System.out.print( "[" + row + "," + c + "]: " + indent );
         System.out.print( vals[ c ] + "   " );
         if ( ++numOnLine >= numValsPerLine )
         {
            System.out.println();
            numOnLine = 0;
            indent += indentIncr;
         }
      }
      if ( numOnLine != 0 )
         System.out.println();
   }
   
   //--------------- usage -----------------------------------
   static void usage()
   {
      PrintStream out = System.out; // should pass in output stream
      
      out.println( "Usage: java BasicDemo fdl [-t opts] [-ds] [-db] [bnds]" );
      out.println( " fdl  xfdl file describing the input DataSource" );
      out.println( " -t opts - code to run. Each character in the string" );
      out.println( "      defines  code to execute in string order:");
      out.println( "      d - minmax: Datum then attributes 1 at a time");
      out.println( "      D - minmax: Datum then all attributes as array");
      out.println( "      v - minmax: getDouble( isid, attr )");
      out.println( "      V - minmax: getDoubles( isid )");
      out.println( "      a - minmax: getDoubles( attr )");
      out.println( "      A - minmax: getDoublesByAttribute()");
      out.println( "      P - minmax: getDoublesByPoint()");
      out.println();
      out.println( "      w - wavelet: do Haar wavelet on input data.");
      out.println();
      out.println( " -ds use specified data source as  input. <default>" );
      out.println( " -db use data block extracted from ds as input." );
      out.println( " bnds  subblock of the DataSource as block input." );
      out.println( "       bnds must be either n ints or 2n ints where" );
      out.println( "       n is the dim of DataSource. Implies -db");      
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
      for ( int iarg = 0; iarg < args.length; iarg++ )
      {
         String sarg = args[ iarg ];
         if ( sarg.equals( "-v" ))    // verbose flag
            BasicDemo.verbose = true;
         else if ( sarg.equals( "-ds" ))    // use DS as input
            BasicDemo.useDS = true;
         else if ( sarg.equals( "-db" ))    // make a DB from DS
            BasicDemo.useDB = true;
         else if ( sarg.equals( "-t" ))    // minMax tests flags
         {
            if ( iarg++ < args.length )
               BasicDemo.testString = args[ iarg ];
            else
               System.err.println( "Error: -t needs an arg string." ); 
         }
         else if ( BasicDemo.inName == null )  // not switch, check if ds
            BasicDemo.inName = args[ iarg ];
         else // read bounds arguments
         {
            try 
            {
               intArgs.add( new Integer( sarg ));
            }
            catch ( NumberFormatException nfe )
            {
                  System.err.println( "Error: Invalid argument: "
                      + sarg + "  Expecting a number.");
            }
         }
      }
      if ( intArgs.size() > 0 )
      {
         BasicDemo.boundsArgs = new int[ intArgs.size() ];
         for ( int i = 0; i < intArgs.size(); i++ )
            BasicDemo.boundsArgs[i] = ((Integer)intArgs.get( i )).intValue();
      }
   }
}
