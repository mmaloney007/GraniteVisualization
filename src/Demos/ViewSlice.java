package Demos;






/**
 * @author rdb
 */

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;

import javax.swing.JApplet;
import javax.swing.JFrame;

import edu.unh.sdb.common.SDBError;
import edu.unh.sdb.datasource.DataBlock;
import edu.unh.sdb.datasource.DataSource;
import edu.unh.sdb.datasource.ISBounds;

public class ViewSlice extends JApplet
{
  private static final long serialVersionUID = 0; // for serialization inheritance
  //+++++++++++++ instance variables ++++++++++++++++++++++++++++++++
  private BufferedImage sliceImage = null; // map the data to this object
  private int           imageWidth  = 0;
  private int           imageHeight = 0;
  private DataSource    curDS       = null;
  private  JFrame       frame       = null;

  //------------------ arguments/switches ---------------------------
  private  String   inName      = null;   // spec for input DS
  private  String   lutName     = "cyan"; // default to cyan lut
  private  float    argMinValue = Float.MAX_VALUE;  // min/max from args
  private  float    argMaxValue = Float.MIN_VALUE;
  private  float    minValue    = Float.MAX_VALUE;  // min/max for display
  private  float    maxValue    = Float.MIN_VALUE;  // init to DS min/max

  private  int      sliceAxis   = 0;      // axis for slice
  private  int      sliceIndex  = -1;     // which slice
  private  int      attribute   = 0;      // attribute for data

  //----------------- ViewSlice -----------------------------------
  public ViewSlice( JFrame theFrame, String[] args )     
  {
    this.frame = theFrame;
    readArgs( args );

    this.curDS = openDataSource( this.inName );

    if ( this.attribute >= this.curDS.getNumAttributes() ) // invalid attribute 
    {
      System.err.println( "ERROR: invalid attribute: " + this.attribute + 
          ". Using attribute 0" );
      this.attribute = 0;
    }

    // We want the command line args to override data source values
    if ( this.argMinValue != Float.MAX_VALUE )
      setMin( this.argMinValue );
    if ( this.argMaxValue != Float.MIN_VALUE )
      setMax( this.argMaxValue );
  }

  //===================== Accessors =================================
  public void setFileName( String newName ) { this.inName = newName; }
  public void setLut( String newLutName )   { this.lutName = newLutName; }
  public void setSliceAxis( int newAxis )   { this.sliceAxis = newAxis; }
  public void setSliceIndex( int slice )    { this.sliceIndex = slice; }
  public void setMin( float newMin )        { this.minValue = newMin; };
  public void setMax( float newMax )        { this.maxValue = newMax; };
  public void setAttribute( int newAttr )   { this.attribute = newAttr; }
  public void setImage( BufferedImage bi )  { this.sliceImage = bi; }

  public String getFileName()               { return this.inName; }
  public String getLut()                    { return this.lutName; }
  public int    getSliceAxis()              { return this.sliceAxis; }
  public int    getSliceIndex()             { return this.sliceIndex; }
  public float  getMin()                    { return this.minValue; }
  public float  getMax()                    { return this.maxValue; }
  public int    getAttribute()              { return this.attribute; };

  public boolean minSet()    { return this.minValue != Float.MAX_VALUE; }
  public boolean maxSet()    { return this.maxValue != Float.MIN_VALUE; }

  //-------------- openDataSource ---------------------------------
  //
  public DataSource openDataSource( String name )
  {
    // First argument to create is an arbitrary name associated with the 
    // created DataSource object; the second is used to find the FDL file.
    // In this case, I used the same string for both.
    DataSource ds = DataSource.create( name, name );
    if ( ds == null || ds.dim() != 3 )
    {
      if ( ds == null )
        System.err.println( "ERROR: Unable to open: " + name );
      else
        System.err.println( "ERROR: Only support 3D input data." );
      System.exit( -1 );
    }      

    ds.activate();
    setMinMax( ds );
    return ds;
  }
  //-------------- init ---------------------------------
  //
  @Override
  public void init()
  {
    if ( this.sliceIndex < 0 )
    {
      System.err.println( "Invalid sliceIndex. Using 0." );
      this.sliceIndex = 0;
    }

    updateSlice();      
    String title = curDS.getName() + "@" + this.sliceIndex 
        + " axis= " + this.sliceAxis;
    this.frame.setTitle( title );
  }

  //------------------ paint ----------------------------------------
  @Override
  public void paint( Graphics g ) 
  {
    Graphics2D g2 = (Graphics2D) g;
    int        w  = getSize().width;  // get space for image
    int        h  = getSize().height;
    int        bw = this.sliceImage.getWidth(this);      // get image size
    int        bh = this.sliceImage.getHeight(this);

    System.out.println( "Sizes: " + w + " " + h + " " + bw + " " + bh );
    AffineTransform at = new AffineTransform();
    at.scale( (float) w / bw, (float) h / bh );
    System.out.println( "Scale: " + (float)w/bw + " " + (float)h/bh);
    BufferedImageOp biop = new AffineTransformOp( at, 
        AffineTransformOp.TYPE_NEAREST_NEIGHBOR );
    g2.drawImage( this.sliceImage, biop, 0, 0 );  
  }

  //+++++++++++++++++++++++++++++++ main ++++++++++++++++++++++++++++++++
  //
  public static void viewSlice( String[] args ) throws IOException
  {
    //---------------- check command line arguments ------------
    if ( args.length == 0 )
    {
      usage();
      System.exit( 0 );
    }

    //Log.init("errors",Log.LogToFile | Log.LogToStdErr);
    SDBError.setErrorReportingLevel(SDBError.AlwaysThrow);

    JFrame frame = new JFrame( "ViewSlice" );

    ViewSlice viewApp = new ViewSlice( frame, args );
    frame.getContentPane().add( BorderLayout.CENTER, viewApp );
    viewApp.init();
    viewApp.start();

    //try to get a "reasonable" initial size
    //The title bar seems to be 22 pixels tall, so add that to the
    //  height.
    int titleHeight = 22;
    frame.setSize( viewApp.getWidth(), viewApp.getHeight() + titleHeight );

    frame.setVisible( true );

    WindowListener listener = new WindowAdapter() 
    {
      @Override
      public void windowClosing( WindowEvent e ) { System.exit( 0 ); }
    };     
    frame.addWindowListener( listener );
  }

  //-------------- usage() ----------------------------------
  static void usage()
  {
    String[] message = { 
        "Usage: java ViewSlice  [-#] [-x #] [-a #][-l lutname]" 
            + " [-min min] [-max max] fdlFile",
            "     -#    - slice of the 3d volume to view" ,
            "     -x #  - slice axis",
            "     -a #  - attribute selection",
            " ",
            "     file.xfdl - fdl file defining file to view",
            "     -l lutname - lutname to define lookup table: currently", 
            "              support red, green, blue, magenta, yellow, gray.",
            "     -min v - use v as smallest allowed value in file",
            "     -max v - use v as largest allowed value in file",
            " Argument order is arbitrary."
    };
    for ( int i = 0; i < message.length; i++ )
      System.out.println( message[ i ]);
  }
  //-------------- readArgs --------------------------------
  //
  void readArgs( String[] args )
  {
    String    axisString = null;
    String    sliceString = null;
    String    attrString  = null;
    String    minString   = null;
    String    maxString   = null;

    int sarg = 0;
    while ( sarg < args.length )
    {
      String arg = args[ sarg++ ];
      if ( arg.equals( "-l" ))
        setLut( args[ sarg++ ] );
      else if ( arg.equals( "-min" ))
        minString = args[ sarg++ ];
      else if ( arg.equals( "-max" ))
        maxString = args[ sarg++ ];
      else if ( arg.equals( "-x" ))
        axisString = args[ sarg++ ];
      else if ( arg.equals( "-a" ))
        attrString = args[ sarg++ ];
      else if ( arg.startsWith( "-" )) // must be slice spec
        sliceString = arg.substring( 1 );
      else if ( getFileName() == null )
        setFileName( arg );
      else
        System.err.println( "ERROR: unrecognized argument: " + arg );
    }
    if ( getFileName() == null )
    {
      System.err.println( "ERROR: No input file defined " );
      System.exit( -1 );
    }

    try
    {
      if ( axisString != null )
        setSliceAxis( new Integer( axisString ).intValue() );
    } 
    catch ( NumberFormatException nfe )
    {
      System.err.println( "Axis value not a valid #: " + axisString );
      axisString = null;
    }

    try
    {
      if ( attrString != null )
        setAttribute( new Integer( attrString ).intValue() );
    } 
    catch ( NumberFormatException nfe )
    {
      System.err.println( "Attribute spec not a valid #: " + attrString );
      attrString = null;
    }

    try
    {
      if ( minString != null )
        this.argMinValue = new Float( minString ).floatValue();
    } 
    catch ( NumberFormatException nfe )
    {
      System.err.println( "Minimum value not a valid #: " + minString );
      minString = null;
    }
    try
    {
      if ( maxString != null )
        this.argMaxValue = new Float( maxString ).floatValue();
    } 
    catch ( NumberFormatException nfe )
    {
      System.err.println( "Maximum value not a valid #: " + maxString );
      maxString = null;
    }
    try
    {
      if ( sliceString != null )
        setSliceIndex( new Integer( sliceString ).intValue() );
    } 
    catch ( NumberFormatException nfe )
    {
      System.err.println( "Slice index not valid: " + sliceString );
      setSliceIndex( 0 );
    }
  }

  //-------------- setMinMax ---------------------------------
  /**
   * get the min and max values for the DataSource.
   * 
   * This information should really be available as metadata, but this
   * feature is not yet implemented in Granite, so we'll have to read
   * the file and compute the min/max.
   */
  public void setMinMax( DataSource ds )
  {
    // We'll read the data slice by slice. We could do it in a single
    // read, but for large files that could be impractical.
    // 
    // We'll start with the volume bounds, then modify the 0-axis
    // to pick out each slice.
    // 
    // In general, object creation in Java is very expensive, so there
    // are many cases where Granite lets you reuse objects. 
    // In this case we will create a block to hold each slice
    // and an array to hold the actual values pulled out of the block.
    //
    float     vals[]   = null;          // we'll store values here

    // get a Bounds for the slice by starting with volume slice, later
    //  we'll change the dimensions of the 0 axis.
    ISBounds  sliceBounds = ds.copyBounds();
    int       numSlice = sliceBounds.getDimension( 0 );
    // now make it a true bounds for the slice
    sliceBounds.setLower( 0, 0 );  // set upper/lower bounds of 
    sliceBounds.setUpper( 0, 0 );  //   axis 0 to slice #

    // and use the slice bounds to create a data block to hold the slice 
    // createDataBlock.DataSource creates the "right" kind of block for this DS
    DataBlock sliceBlk = ds.createDataBlock( sliceBounds );

    float minVal = Float.MAX_VALUE;
    float maxVal = Float.MIN_VALUE;

    for ( int slice = 0; slice < numSlice; slice++ )
    {
      sliceBounds.setLower( 0, slice );  // set upper/lower bounds of 
      sliceBounds.setUpper( 0, slice );  //   axis 0 to slice #

      ds.subblock( sliceBlk, sliceBounds );

      // if you pass an array of the right size to getFloats it will fill
      // it and return it. If the argument is null or not the right size,
      // it will allocate the correct size array, fill it and return it.
      // So when you pass null, the first call will allocate the right
      // size, which gets re-used for all subsequent calls.
      vals = sliceBlk.getFloats( vals );
      for ( int i = 0; i < vals.length; i++ )
      {
        if ( vals[i] < minVal ) 
          minVal = vals[i];
        else if ( vals[i] > maxVal ) 
          maxVal = vals[i];
      }
    }    
    System.out.println( "Min: " + minVal + "  Max: " + maxVal );
    setMin( minVal );
    setMax( maxVal );
  }

  //-------------- updateSlice ---------------------------------
  /**
   * Using current settings, extract a slice from the data source
   * and prepare the image; store it in this.sliceImage
   */
  public void updateSlice()
  {
    ISBounds   blockBounds = curDS.copyBounds();
    ISBounds   sliceBounds = null;

    blockBounds.setLower( sliceAxis, sliceIndex );  // define the slice
    blockBounds.setUpper( sliceAxis, sliceIndex );
    sliceBounds = blockBounds.project( sliceAxis );

    DataBlock block = curDS.subblock( blockBounds );

    System.out.println( "block Bounds: " + block.getBounds() );
    this.imageHeight = sliceBounds.getDimension( 0 );
    this.imageWidth  = sliceBounds.getDimension( 1 );

    setImage( getBufferedImage( getByteBuffer( block ), 
        this.imageWidth, this.imageHeight, lutName ));
    this.setSize( this.imageWidth, this.imageHeight );
  }

  //-------------- getByteBuffer --------------------------------
  /**
   * Convert the data in the DataBlock argument to 2D DataBufferByte object
   */
  DataBufferByte getByteBuffer( DataBlock bb )
  {
    float[] vals      = null;

    // get all the data into a 1D array     
    vals = bb.getFloats( attribute );

    // Make sure input values are within range specified by the arguments
    // if no min or max argument specified, compute them
    clampToRange( vals );

    // newval = (val - min)/(max - min) = val/range - min/range
    float scale = 255.0f/( maxValue - minValue );
    float offset = -minValue * scale;

    byte[] bytes = new byte[ vals.length ];
    for ( int i = 0; i < vals.length; i++ )
      bytes[ i ] = (byte) ( vals[i] * scale + offset );
    //outputData( bytes );
    return new DataBufferByte( bytes, bytes.length );                                  
  }
  // rdb 4/11/10
  // this is a hack to get the slice output as a separate text file
  // and to double the x direction so it is square.
  // this only works if the slice axis is chosen as 2 and for
  // the head data set.
  private void outputData( byte[] data )
  {
    int i = 0;

    int[] line = new int[ this.imageWidth ];
    // replicate first line so get exactly twice the resolution
    for ( int j = 0; j < line.length; j++ )
      line[ j ] = 0xFF & data[ j ]; 
    for ( int r = 0; r < this.imageHeight; r++ )
    {
      // first output the average of last line and this one
      for ( int c = 0; c < this.imageWidth; c++ )
      {
        int ubyte = ( 0xFF & data[ i++ ] + line[ c ] ) / 2;
        System.out.print(  ubyte + " " );
        line[ c ] = ubyte;
      }
      System.out.println();
      // now output this line
      for ( int c = 0; c < this.imageWidth; c++ )
      {
        System.out.print(  line[ c ] + " " );
      }
      System.out.println();        
    }
    System.out.println();
  }



  //------------ clampToRange -------------------------------------
  /**
   * Make sure all values are within range; if not, clamp them to min or max. 
   * 
   * For information, count and print the # of values outside the range.
   */
  void clampToRange( float[] vals )
  {
    int minClamp = 0; // count the number of a values that get clamped
    int maxClamp = 0;

    for ( int i = 0; i < vals.length; i++ )
    {
      if ( vals[i] < this.minValue ) 
      {
        vals[ i ] = this.minValue;  // clamp the value
        minClamp++;
      }
      else if ( vals[i] > this.maxValue ) 
      {
        vals[ i ] = this.maxValue;
        maxClamp++;
      }
    }
    System.out.println( "min= " + minValue + "   max= " + maxValue );
    if ( minClamp > 0 || maxClamp > 0 )
      System.err.println( "Warning: Some values out of range. " 
          + minClamp + " clamped to min. "
          + maxClamp + " clamped to max." );
  } 

  //-------------- getBufferedImage  --------------------------------
  /**
   * Map a Java DataBufferByte version of the slice to a Java Image object
   */
  BufferedImage getBufferedImage( DataBufferByte data, int w, int h, 
                                  String lutName )
  {
    IndexColorModel cm = getLUTColorModel( lutName );
    BufferedImage  bimg = new BufferedImage( w, h, 
        BufferedImage.TYPE_BYTE_INDEXED, cm );
    SampleModel sm = new ComponentSampleModel( 
        DataBuffer.TYPE_BYTE, w, h, 1, w, new int[]{0} );

    Raster imgRaster = Raster.createWritableRaster( 
        sm, data, new Point( 0,0) );
    bimg.setData( imgRaster );
    return bimg;
  }

  //-------------- getLookupTable --------------------------------
  /**
   * Create a color lookup table to represent the image.
   * This is a bit of a hack; we really ought to be able to read a lookup
   * table from a file.
   */
  static IndexColorModel getLUTColorModel( String lutName )
  {
    byte[] r = new byte[ 256 ];
    byte[] g = new byte[ 256 ];
    byte[] b = new byte[ 256 ];

    // We support lookup tables for: 
    //             gray, red, blue, green, cyan, magenta, yellow
    // The r, g, and b arrays should contain values from 0 to 255 or all 0. 
    // The different lookup table names determine which arrays are 0 and 
    // which are 0..255 (a linear ramp).      
    String validLUTs[] = 
      { "gray", "red", "green", "blue", "cyan", "magenta", "yellow" };

    // each lut either has a linear ramp or all 0 for the rgb arrays; 
    // encode that info for each table in a parallel String array with 
    // 1 character for each of rgb and where the character 'r' is for ramp, 
    // and '0' for zeroes
    String rgbCodes[]  = 
      { "rrr",  "r00", "0r0",   "00r",  "0rr",  "r0r",     "rr0" };

    // First figure out which lut we have
    if ( lutName == null )  // if none, use gray
      lutName = "gray";     
    String code = null; 
    for ( int i = 0; i < validLUTs.length && code == null; i++ )
    {
      if ( lutName.equalsIgnoreCase( validLUTs[ i ]))
        code = rgbCodes[ i ];
    }
    if ( code == null )
    {
      System.err.println( "ERROR: Invalid Lookup table name: " + lutName +
          "Using gray scale." );
      code = "rrr";                              
    }

    // now fill in the r,g, and b arrays with a ramp or 0
    for ( int i = 0; i < 256 ; i++ )
    {
      if ( code.charAt( 0 ) == 'r' )
        r[i] =  (byte) i;
      else 
        r[i] = 0;
      if ( code.charAt( 1 ) == 'r' )
        g[i] =  (byte) i;
      else 
        g[i] = 0;
      if ( code.charAt( 2 ) == 'r' )
        b[i] =  (byte) i;
      else 
        b[i] = 0;
    }     
    return new IndexColorModel( 8, 256, r, g, b );
  }
}