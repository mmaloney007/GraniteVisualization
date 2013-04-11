package Demos;





/**
 * This program shows some simple examples using the two CompositeDataSources:
 *     AttributeJoinDataSource
 *     BlockedDataSources
 * 
 * An AttributeJoinDataSource is created from components that must share
 * "compatible" ISBounds. In this case "compatible" means the same size in all
 * dimensions.
 * 
 * A BlockedDataSource is created from components that must share "compatible"
 * attribute sets. In this case, "compatible" means the same number and types
 * in the same order.
 * 
 * 
 * Since both compositing operations impose limitations on the components, 
 * this program does not allow the component files to be specified using
 * command line arguments, but hard-codes them.
 * 
 * @author rdb
 */
/*
 * Created on Feb 11, 2005
 * November 2005: rdb Modified to accept a DDL xfdl file; 
 *                    if DDL specified open it
 *                    else CREATE a new composite datasource.
 */
import java.util.*;
import java.io.*;
import edu.unh.sdb.datasource.*;

public class CompositeDemo
{
   private static String  dataName = null;
   private static String  testString = "ab";
   private static int[]   boundsArgs = null;

   public static void main( String[] args )
   {
      File dataFile = null;
      readArgs( args );
      
      if ( dataName != null )
         dataFile = new File( dataName );
      
      if ( dataFile == null || dataFile.isDirectory() )
         buildComposite( dataName );
      else
         openDDL( dataName );
   }

   //------------------ openDDL( File ) ---------------------------
   // Open a (purported) DDL file 
   //
   private static void openDDL( String ddlFile )
   {
      DataSource composite = DataSource.create( "DDL", ddlFile );
      composite.activate();
      
      System.out.println( "----RDesc: \n" + composite.getRecordDescriptor() );
      int[] loc = {0,0,0};
      IndexSpaceID isid = new IntegerIndexSpaceID( loc );
      
      float[] temp = null;
      temp = composite.getFloats( isid, temp );
      System.out.println( "Datum: " + temp[0] + " " + temp[1] + " " + temp[2] );
      
      printBlock( composite, "DDL defined datasource" );
   }
   //------------------ buildComposites() ---------------------------
   // Build composite datasources in the program
   //
   private static void buildComposite( String dataDirName )
   {      
      for ( int i = 0; i < testString.length(); i++ )
      {
         switch ( testString.charAt( i ))
         {
            case 'a': demoAJ( dataDirName );
                      break;
            case 'b': demoBlocked( dataDirName, true );
                      break;
            case 'B': demoBlocked( dataDirName, false );
                      break;
            default:  
               System.out.println( "Invalid test character: " 
                                      + testString.charAt(i));
         }
      }
   }

   //------------------------ demoAJ -----------------------------
   // 
   // Assume component attribute file descriptors have the names
   //     attr-0.xfdl, attr-1.xfdl, etc.
   // We'll actually take advantage of default naming conventions and 
   // leave off the ".xfdl"  in the code, so the files could end in
   // .xfdl or .fdl or .xml or have no extension at all.
   //
   private static void demoAJ( String dataDir )
   {
      int          attrCount = 4;
      String       partPrefix = dataDir + "/attr-";
      DataSource[] parts = new DataSource[ attrCount ];
      for ( int i = 0; i < parts.length; i++ )
         parts[ i ] = DataSource.create( partPrefix + i, partPrefix + i );
      
      DataSource  whole = new AttributeJoinDataSource( "AJ", parts );
      whole.activate();
      
      ISBounds printBounds = whole.getBounds();
      if ( CompositeDemo.boundsArgs != null )
      {
         if ( boundsArgs.length == whole.dim() )
            printBounds = new ISBounds( CompositeDemo.boundsArgs );
         else if ( boundsArgs.length == 2*whole.dim() )
            printBounds = new ISBounds( whole.dim(), CompositeDemo.boundsArgs );
         else 
            System.out.println( "Error: Invalid bounds arguments, must be same"
               + " as dimensionality or twice dimensionality." );
      }
      printBlock( whole.subblock( printBounds ), "Attribute Join test" );
   }  

   //------------------------ demoBlocked -----------------------------
   // Assume component block file descriptors have the names
   //     blk-0.xfdl, blk-1.xfdl, etc.
   // We'll actually take advantage of default naming conventions and 
   // leave off the ".xfdl"  in the code, so the files could end in
   // .xfdl or .fdl or .xml or have no extension at all.
   //
   private static void demoBlocked( String dataDir, 
                                    boolean preMapped )  // true => input already mapped
                                                     // to position in composite
   {
      DataSource   whole = null;
      String       title = null;
      int          attrCount = 8;
      String       partPrefix = dataDir + "/blk-";
      
      DataSource[] parts = new DataSource[ attrCount ];
      for ( int i = 0; i < parts.length; i++ )
         parts[ i ] = DataSource.create( partPrefix + i, partPrefix + i );
     
      // This is pretty messy here, but I "hard-code" the mappings of the
      // 8 input octants to their positions in the composite, by altering their
      // ISBounds. The octants are laid out:
      // front:  0  1    back:   4  5
      //         2  3            6  7
      //
      // There are 2 ways to place the component blocks into the composite
      //  1. We can change the component bounds so that they 
      //      match with their positions in the composite, or
      //  2. We can create ISIDMapper objects that tell the BlockedDataSource how
      //     to place them. 
      //  We use the testing codes to decide which.

      if ( preMapped )
      {
         title = "Components have bounds defined in Composite space";
         whole = makeBDSByMovingParts( parts );
      }
      else
      {
         title = "Components are mapped to composite space using ISIDMapper object";
         whole = makeBDSWithISIDMaps( parts );
      }
      whole.activate();

      ISBounds printBounds = whole.getBounds();
      
      if ( CompositeDemo.boundsArgs != null )
      {
         if ( boundsArgs.length == whole.dim() )
            printBounds = new ISBounds( CompositeDemo.boundsArgs );
         else if ( boundsArgs.length == 2*whole.dim() )
            printBounds = new ISBounds( whole.dim(), CompositeDemo.boundsArgs );
         else 
            System.out.println( "Error: Invalid bounds arguments, must be same"
               + " as dimensionality or twice dimensionality." );
      }
      printBlock( whole.subblock( printBounds ), title );
   }
   
   //------------------- makeBDSByMovingParts ------------------------------
   /**
    * Create the Blocked DataSource by moving components to their positions
    */
   private static DataSource makeBDSByMovingParts( DataSource[] parts )
   {
      // first get the dimensions sizes of the upper front left octant. 
      // Placement of all other octants are determined by these dimensions.
      int[] step = parts[ 0 ].getBounds().getDimensionsArray();
      
      parts[ 1 ].getBounds().move( new int[] { 0, 0, step[2] });  // move along col
      parts[ 2 ].getBounds().move( new int[] { 0, step[1], 0 });  // move along row
      parts[ 3 ].getBounds().move( new int[] { 0, step[1], step[2] });  // col and row
      parts[ 4 ].getBounds().move( new int[] { step[0], 0, 0 });  // move along layer
      parts[ 5 ].getBounds().move( new int[] { step[0], 0, step[2] });  // layer and col
      parts[ 6 ].getBounds().move( new int[] { step[0], step[1], 0 });  // layer and row
      parts[ 7 ].getBounds().move( step );
      
      return new BlockedDataSource( "Blocked", parts );     
   }
   //------------------- makeBDSWithISIDMaps ------------------------------
   /**
    * Create the Blocked DataSource using explicit creation of ISIDMaps
    */
   private static DataSource makeBDSWithISIDMaps( DataSource[] parts )
   {
      // An ISIDMapper defines how a block in one index space can be mapped
      // to a block in another. In principal, an ISIDMapper need not specify
      // an integer mapping. In practice, Granite doesn't yet fully support
      // such mappings.
      ISIDMapper[] isMap = new ISIDMapper[ parts.length ]; // need 1 for each block
      
      // the most general form of ISIDMapper takes 2 ISBounds and places the first
      // at an arbitrary place and size in the second. 
      
      // If the composite space 
      // starts at the origin and if you want an integral mapping, you only need
      // to specify one ISBounds specifying the target position and size in the 
      // composite space. For us, this means just moving copies of the input
      // bounds to their appropriate locations

      // first get the dimensions sizes of the upper front left octant. 
      // Placement of all other octants are determined by these dimensions.
      int[] step = parts[ 0 ].getBounds().getDimensionsArray();

      isMap[ 0 ] = new ISIDMapper( parts[0].getBounds() );
      
      isMap[ 1 ] = new ISIDMapper( parts[1].getBounds(),
            parts[ 1 ].copyBounds().move( new int[] { 0, 0, step[2] }));
      
      isMap[ 2 ] = new ISIDMapper( parts[2].getBounds(),
            parts[ 2 ].copyBounds().move( new int[] { 0, step[1], 0 }));
      
      isMap[ 3 ] = new ISIDMapper( parts[3].getBounds(),
            parts[ 3 ].copyBounds().move( new int[] { 0, step[1], step[2] }));
      
      isMap[ 4 ] = new ISIDMapper( parts[4].getBounds(),
            parts[ 4 ].copyBounds().move( new int[] { step[0], 0, 0 }));
      
      isMap[ 5 ] = new ISIDMapper( parts[5].getBounds(),
            parts[ 5 ].copyBounds().move( new int[] { step[0], 0, step[2] }));
      
      isMap[ 6 ] = new ISIDMapper( parts[6].getBounds(),
            parts[ 6 ].copyBounds().move( new int[] { step[0], step[1], 0 }));
      
      isMap[ 7 ] = new ISIDMapper( parts[7].copyBounds(),
            parts[ 7 ].copyBounds().move( step ));
      return new BlockedDataSource( "Blocked", parts, isMap );      
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
         printRow( 0, inData.getFloats(), inData.getNumAttributes() );
      else if ( inDim == 2 )
         printSlice( inData, 0 );
      else  // extract 3D slices one at a time (should use a block iterator)
      {
         int sliceDim = inDim - 3;  // for 3D data set this is axis 0
         int numSlices = inBounds.getDimension( sliceDim );
         for ( int slice = 0; slice < numSlices; slice++ )
         {
            System.out.println( "------------------ slice "+  slice + 
                                "----------------------------------------------");
            printSlice( inData, slice );
         }
      }
      System.out.println( "+++++++++++++++++++++++++++++++++++++++++++++++++++++++" 
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
      int      numAttr = inData.getNumAttributes();
      
      float[]  rowVals = null;  // will be assign data for a row
      
      ISBounds inBnds    = inData.getBounds();
      int[]    sliceSizes = null;   // this array will first be lengths of a slice
     
      if ( inDim > 2 )
      {
         sliceSizes = new int [ inDim ];
         if ( inDim > 3 )
         {
            System.err.println( "printSlice: supports slices of 3D volumes only. " +
            		             "Doing 1st 3d Hyperslice only.");
         }
         for ( int i = 0; i < rowDim; i++ )
            sliceSizes[ i ] = 1;
            
         sliceSizes[ rowDim ] = inBnds.getDimension( rowDim );
         sliceSizes[ colDim ] = inBnds.getDimension( colDim );
         ISBounds printBounds = new ISBounds( sliceSizes );
         
         // This printBounds, however actually identifies slice 0, we need to
         // change its bounds in the slice dimension to select the slice.
         try {
	        printBounds.setLower( sliceDim, whichSlice );
	        printBounds.setUpper( sliceDim, whichSlice );
         } catch ( Exception ex )
           { 
              System.err.println( "printSlice: invalid slice value. " + whichSlice );
           }
         printBlock = inData.subblock( printBounds );
      }
      
      ISBounds rowBounds = printBlock.getBounds();  // init to entire block, will change
      
      if ( inDim == 1 )
         printRow( 0, printBlock.getFloats(), numAttr );
      else
      {
         numRows = printBlock.getBounds().getDimension( rowDim );
         for ( int r = 0; r < numRows; r++ )
         {
            rowBounds.setLower( rowDim, r ); // select just 1 row
            rowBounds.setUpper( rowDim, r ); 
            rowVals = printBlock.subblock( rowBounds ).getFloats( rowVals ); // get
                                                                             // data
            // now print the data
            printRow( r, rowVals, numAttr );
         }
      }
      System.out.println( "----------------------------------------------------" );
   }
   //-------------- printRow  --------------------------------
   //
   static void printRow( int row, float[] vals, int numAttr )
   { 
      int      numValsPerLine = 20;
      int      numDatumsPerLine = numValsPerLine / numAttr;
      int      numOnLine = 0;
      
      String   indent  = " ";
      String   indentIncr = "    ";
      
      int val = 0;

      for ( int d = 0; d < vals.length / numAttr; d++ )
      {
         if ( numOnLine == 0 )
            System.out.print( "[" + row + "," + d + "]: " + indent );
         if ( numAttr == 1 )
            System.out.print( vals[ val++ ] + "   " );
         else
         {  
            System.out.print( "<" + vals[ val++ ] );
            for ( int v = 1; v < numAttr; v++ )
               System.out.print( "," + vals[ val++ ] );
            System.out.print( "> " );
         }
         if ( ++numOnLine >= numDatumsPerLine )
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
      System.out.println( "Usage: java CompositeDemo [data] [-t options] [bounds]" );
      System.out.println( " if 'data' is a plain file, it should be a ddl file for a composite " );
      System.out.println( " otherwise it should be a directory containing the " );
      System.out.println( "         xfdl files for the components. default is .");
      System.out.println( " -t identifies which code to run if ddl file is not specified." );
      System.out.println( "    Each character in the string defines a piece of code to " );
      System.out.println( "    execute in string order:");
      System.out.println( "        a - Attribute Join");
      System.out.println( "        b - Blocked, where input blocks already specify positions");
      System.out.println( "        B - Blocked, using ISIDMapper to position input");
      System.out.println();
      System.out.println( " bounds  select a subblock of the DataSource for dumb iteration." );
      System.out.println( "         bounds must be either n integers or 2n integers where" );
      System.out.println( "         n is the dimensionality of the DataSource. Implies -db");      
      System.exit( 0 );
   }

   //-------------- readArgs --------------------------------
   //
   static void readArgs( String[] args )
   {  
      if ( args.length == 0 )
      {
         usage();
      }
      ArrayList<Integer> intArgs = new ArrayList<Integer>();
      for ( int iarg = 0; iarg < args.length; iarg++ )
      {
         String sarg = args[ iarg ];
         if ( sarg.equals( "-t" ))    // minMax tests flags
         {
            if ( iarg++ < args.length )
               CompositeDemo.testString = args[ iarg ];
            else
               System.err.println( "Error: -t switch needs an argument string." ); 
         }
         else if ( CompositeDemo.dataName == null // havent got a dir
                  && !sarg.matches( "[0-9]*" ) )     // arg isn't start of bounds
            CompositeDemo.dataName = sarg;
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
         CompositeDemo.boundsArgs = new int[ intArgs.size() ];
         for ( int i = 0; i < intArgs.size(); i++ )
            CompositeDemo.boundsArgs[ i ] = ((Integer)intArgs.get( i )).intValue();
      }
   }
}
