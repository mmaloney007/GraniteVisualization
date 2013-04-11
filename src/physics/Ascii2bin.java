/**
 * Stephen Dunn
 * Ascii2bin.java
 * 4/5/2013
 * Description: run with -? for man pages
 */

package physics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import edu.unh.sdb.common.SDBError;
import edu.unh.sdb.datasource.Log;

// ascii2bin main program, needs bin2granite.java and Granite to properly function
// if you're tracking i/o bugs, remember that input and output use different stream types
public class Ascii2bin {
  final ArrayList<Header> headers = new ArrayList<Header>();
  final String filename;
  final Scanner in;
  RandomAccessFile out = null; // reused if split=true
  Bin2granite util;

  final boolean floats; // use floats instead of ints?
  final boolean verify; // verify written data?
  final boolean split; // split input to multiple binaries?
  final boolean noheaders; // see usage
  final boolean [] skip; // skip by attrib. index (false=discard)
  final int integrate; // interleave all data, single output
  final int granite; // generate granite files with output?
  final String [] keep; // keep by attrib. name (keep specified names)
  int attribsWritten = 0;

  // the date argument is just me being sneaky so I can timestamp my self on every run
  static final String version = "0.1 (last built 04/09/2013)"; // function subs this out below
  static final String usage = "Error: unrecognized arguments (-? for usage)";

  static final String fullUsage = "ascii2bin v" + version + "\n"
      + "Converts an ASCII file of integers or floats to raw binary.\n\n"
      + "USAGE:\n    ascii2bin filename [args]\n"
      + "\tYour file must contain a series of ASCII integers/floats separated by whitespace.\n"
      + "\tHeaders are not expected unless arguments implicitly enable them or \"-headers\"\n"
      + "\tis specified. Most args will enable headers as they operate on attributes.\n"
      + "\tThe expected header and file formats are described below under FORMATS.\n\n"
      + "\t-f Reads floats instead of ints\n"
      + "\t-v Enable data verification (forces -g)\n"
      + "\t-gX Produce corresponding \"xddl\" and \"xfdl\" files for Granite\n"
      + "\t\tJust use bare '-g' if you want ascii2bin to auto-determine Granite boundaries\n"
      + "\t\tOtherwise, X is the # of dimensions for each kept attribute a1...an respectively\n"
      + "\t\tUse -g1 to extract attributes into xfdl files as a single block\n"
      + "\t\tNote: -g0/-g uses prime factorization (might be slow on very large datasets)\n"
      + "\t\tExample: \"ascii2bin -keep[ X Y Z ] -g3\"\n"
      + "\t\t\tOutput: 3 xddl/xfdl(s) with each broken into 3 parts with an xfdl for 3 fields: X, Y, Z\n"
      + "\t\tExample: \"ascii2bin -skip[0 1 1 0 1] -g -s\"\n"
      + "\t\t\tOutput: xddl/xfdl(s) with auto-generated boundaries for attributes at indexes 1 and 4\n"
      + "\t-keep[a1 a2 a3...] Keep only attributes listed by name separated by whitespace\n"
      + "\t\tExample: \"ascii2bin MyFile.txt -keep[X Z]\"\n"
      + "\t-skip[a1 a2 a3...] Skip attributes by index (0=keep, 1=skip)\n"
      + "\t\tExample: \"ascii2bin MyFile.txt -skip[0 0 1 0 1 0]\n"
      + "\t\tOutput: Skips attributes at indexes 2, 4. Keeps attributes > specified range\n"
      + "\t-s Disable output splitting into multiple files (force 1 file output)\n"
      + "\t\tExample: output for some MyText.txt with attributes X, Y, Z\n"
      + "\t\tWithout -s: MyText_X.raw MyText_Y.raw MyText_Z.raw\n"
      + "\t\tWith -s:    MyText.raw\n"
      + "\t-iX Enable integration mode (forces -s)\n"
      + "\t\tX specifies the # of attributes to be interleaved in a single output file\n"
      + "\t\tExample: \"ascii2bin -i -keep[ X Y Z ]\"\n"
      + "\t\tOutput: file format \"X[1]Y[1]Z[1]X[2]Y[2]Z[2]...\"\n"
      + "\t-headers Enable header parsing (see below for format)\n"
      + "\t-? Displays this message\n\n"
      + "FORMATS:\n"
      + "  Either of these is acceptable:\n"
      + "    (1) A single contiguous series of integer/floating point values with no attributes.\n"
      + "    (2) A series of tuples (header, data) specifying arbitrary attributes and additional\n"
      + "        information not captured by the attribute name.\n"
      + "          Example (newlines ommitted): HEADER[1] VALUES[1] HEADER[2] VALUES[2]...\n\n"
      + "HEADERS:\n"
      + "    Each header must have a signature line, then a series of attribute lines as specified.\n"
      + "    The attributes (N1 = start point (1 is the first point), N2 = # points to read) must be\n"
      + "    specified for every field to use several options (and should be included anyway).\n"
      + "    If N2 is set to 0, all points are read >= N1 (use the args to skip an attribute)\n"
      + "    Line (1): An attribute name (all UTF-8/ASCII are valid)\n"
      + "    Line (2): N1 N2 followed by a series of field names partitioned by whitespace.\n"
      + "       For every '/' on this line, an additional line is expected for values.\n"
      + "    Line (3): Values corresponding to FIELD names from Line 2.\n"
      + "    Line (X): More values for Line 2.\n"
      + "    NOTE: You are not required to use any '/'s in your headers, they are for convenience.\n"
      + "    Example 1: [comments in square brackets not allowed in real headers]\n"
      + "\tSomeAwesomeAttribute\n"
      + "\tN1 N2 Y O U R / FIELDS / spaces delimit / followed by their header values [#s only]\n"
      + "\t1 100 1.0 2 2.0 90 90 [this example used 4 '/'s, so there should be 3 more lines]\n"
      + "\t... [remaining lines] ...\n"
      + "\t[after the header is complete, your values begin immediately on the next line]\n"
      + "    Example 2:\n"
      + "\tMeshData\n"
      + "\tN1 N2 X Y Z U V / R G B A TextureIndex\n"
      + "\t10 0 18.65 12.22 88 1 0\n"
      + "\t0.5 0.98 0.1 1.0 78\n\n"
      + "NOTES:\n"
      + "    (1) All flags may be used in conjunction, including -skip and -keep\n"
      + "        (useful when some indexes and names are both known)\n"
      + "\tExample: \"ascii2bin file.txt -keep[density x y] -skip[1 1 0 1 0 1]\"\n"
      + "    (2) Unspecified attributes are kept by default without -skip/-keep\n"
      + "    (3) You may freely use whitespace: \"-skip[1 0]\" == \"-skip[  1  0]\"\n"
      + "    (4) Conflicts between -keep and -skip resolve as -keep\n"
      + "    (5) For all arguments, case is ignored and any ordering is acceptable\n\n"
      + "Defaults: split=YES, verify=OFF, integrate=NONE, headers=ON, keep=ALL, skip=NONE, granite=OFF";


  public Ascii2bin(String filename) throws FileNotFoundException
  { this(filename, 0, 0, false, true, false, false, null, null); }

  /**
   * Ascii2bin(filename, bool split)
   * split: split multiple attributes into multiple outputs
   * Reads an input ascii file and prepares a data structure for binary output
   * @throws FileNotFoundException
   * */
  public Ascii2bin(String filename, int integrate, int granite, boolean floats, boolean split,
                   boolean verify, boolean noheaders, boolean[] skip, String[] keep) throws FileNotFoundException
                   {
    out("ascii2bin v" + version);
    out("Output directory: \"" + System.getProperty("user.dir") + "\"");

    this.filename = new String(filename);
    this.integrate = integrate;
    this.granite = granite;
    this.floats = floats;
    this.split = split;
    this.skip = skip;
    this.keep = keep;
    this.verify = verify;
    this.noheaders = noheaders;

    renderArgs();

    in = new Scanner(new File(filename));
                   }

  // space saving
  public static void out(String s) { System.out.println(s); }

  // timestamp the source code with the compile date where I want it
  // just don't move this above the member string declarations and you're fine
  // (otherwise this function will stamp it's own date format instead of the string!)
  public void stampSource()
  {
    try
    {
      String Gödel = "src/" + this.getClass().getCanonicalName().replace('.','/') + ".java",
          sequence = "";
      RandomAccessFile io = new RandomAccessFile(new File(Gödel), "rw");
      long dna = 0;
      while ( (sequence += io.readUTF()) != null )
      {
        String [] split = sequence.split("(\\d{2}|M{2})/(\\d{2}|d{2})/(\\d{4}|y{4}])");
        dna = split[0].getBytes("UTF-8").length + "  ".getBytes("UTF-8").length;
        if (split != null && split.length > 1)
        {
          DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
          Date date = new Date();
          io.seek(dna);
          io.write(dateFormat.format(date).getBytes("UTF-8")); // mutate
          io.close(); return;
        }
      }
      io.close();
    }
    catch (Exception e) { } // if i fail, i fail - shouldn't report to user
  }

  // my hopefully legitimate factorization algorithm to determine the largest
  // prime divisor of N = values inputed/attribute (i.e. "-g" is on)
  // used to set correct boundaries on a DataSource for the sanity of Granite and all of Middle Earth
  public static long largestPrime(long n)
  {
    long max = 1;
    for (long i = 2; i < (n/2)+1; i++) // don't bother after the half-way mark
      if (n%i == 0) max = i;  // congruence check/update
    return max;
  }
  public static long smallestPrime(long n)
  {
    long min = (n%2==0) ? n/2 : n;
    for (long i = min; i > 1; i--)
      if (n%i == 0) min = i;  // congruence check/update
    return (min == n) ? 1 : min;
  }


  // should I skip the current attribute based on command-line args?
  // as specified in the man pages, keeps by default
  boolean skip(int index)
  {
    if (skip == null && keep == null) return false;

    if (keep != null)
      for (int i = 0; i < keep.length; i++)
        if (keep[i].equals( headers.get(index).signature ))
          return false;

    if (skip != null && index < skip.length && !skip[index])
      return false;

    return true;
  }

  // close all open files
  void close() { close(true); }
  // close all or just the output file (convenient for splitting attributes)
  void close(boolean all) {
    try { if (all) in.close(); out.close(); out = null; }
    catch (Exception e) { }
  }

  // open an output binary file
  boolean open(String name)
  {
    if (in == null) return false;
    try
    {
      out = new RandomAccessFile(new File(name), "rw");
      return true;
    }
    catch(FileNotFoundException e) { in.close(); return false; }
  }

  public int attribsToWrite(int fromIndex) {
    // calculate max number of segments to read because keep/skip may conflict
    int attribsToGo = 0;
    if (keep != null) attribsToGo = keep.length;
    if (skip != null) { // might be overlap here, but eliminated once we pass that index
      for (int i = fromIndex; i < skip.length; i++)
        if (!skip[i]) attribsToGo++;
    }
    return attribsToGo;
  }

  // this is more so I can verify things are working internally...
  // ...but is reassuring for all parties involved :)
  public void renderArgs() {
    System.out.print("\tfloats=");
    if (floats) out("ON"); else out("OFF");
    System.out.print("\tsplit=");
    if (split) out("ON"); else out("OFF");
    System.out.print("\tverify=");
    if (verify) out("ON"); else out("OFF");
    System.out.print("\theaders=");
    if (!noheaders) out("ON"); else out("OFF");
    System.out.print("\tgranite=");
    if (granite > -1) out(""+granite); else out("OFF");
    System.out.print("\tintegrate=");
    if (integrate > 0) out(""+integrate); else out("OFF");

    // display selected values for keeping/skipping
    System.out.print("\tKeeping named attributes: [ ");
    if (keep != null) {
      if (keep.length == 0 && skip == null) System.out.print("NONE ");
      else if (keep.length == 0) System.out.print("BY INDEX ");
      else
        for(int i = 0; i < keep.length; i++)
          System.out.print(keep[i] + " ");
      out("]");
    }
    else out("N/A ]");

    System.out.print("\tIndexed attributes: [ ");
    if (skip != null) {
      if (skip.length == 0) System.out.print("NONE ");
      else
        for(int i = 0; i < skip.length; i++)
          if (skip[i]) System.out.print("SKIP ");
          else System.out.print("KEEP ");
      out("]");
    }
    else out("N/A ]");
  }

  public boolean verifyOutput() {
    out("Verifying file integrity...");
    return util.verify();
  }

  public boolean processFile() throws NullPointerException, IOException
  {
    // stat keeping is always a good idea
    int fieldCount = 0, attribute = 0;
    int filesGenerated = 0, read = 0, wrote = 0, n1 = 1, n2 = 0;
    String toOpen = "";

    close(false); // close an output file if there is one

    out("Generating raw binary from ASCII...");
    while ( in.hasNext() )
    {
      if (!noheaders) // only do this for attributes, i.e. headers are ON:
      {
        if (integrate == 0) { // this is pointless while integrating, not used to break early

          int attribsToGo = attribsToWrite(attribute);
          // if # attribs written >= all, done
          if (keep != null && attribsToGo <= attribsWritten) break;

        }
        // or we're integrating and hit # of requested attributes
        else if (integrate == attribsWritten) break;

        // read the current header
        try { headers.add( new Header() ); }
        catch (NullPointerException e)
        {
          Log.log("failed parsing header for attribute [" + attribute+1 + "]");
          close(); return false;
        }

        try
        {
          n1 = headers.get(attribute).getField("n1").intValue();
          n2 = headers.get(attribute).getField("n2").intValue();
        }
        catch (Exception e)
        {
          Log.log("header for attribute " + headers.get(attribute).signature
              + " is missing an N1 or N2");
          close(); return false;
        }

        // should we skip this attribute?
        if ( skip(attribute) )
        {
          out("Skipping attribute \"" + headers.get(attribute).signature +
              "\" and " + n2 + " values...");
          if (floats) while (in.hasNextFloat()) in.nextFloat();
          else while (in.hasNextInt()) in.nextInt();
          attribute++;
          continue;
        }
      }

      // open next output file (or skipped if one is open)
      if (out == null)
      {
        toOpen = new String(filename);
        if (split) toOpen += "_" + headers.get(attribute).signature; // append attribute

        if(!open(toOpen + ".raw")) {
          Log.log("failed creating output file: " + toOpen); return false;
        }
        else filesGenerated++;
      }

      // fast-forward to specified N1 from header
      if (n1 != 1) {
        out("Seeking to specified N1 at [ " + n1 + " ]...");
        if (floats)
          while (read++ < n1-1 && in.hasNextFloat()) in.nextFloat();
        else
          while (read++ < n1-1 && in.hasNextInt()) in.nextInt();

        if (read != n1-1) {
          Log.log("not enough values for specified N1[ " + n1 + " ] at "
              + headers.get(attribute).signature);
          close(); attribute++; continue;
        }
      }

      out("Writing to \"" + toOpen + ".raw\"...");

      read = wrote = 0;
      boolean notDone = (floats) ? in.hasNextFloat() : in.hasNextInt();
      while ( notDone )
      {
        try
        {
          // seek by appropriate offset (sizeof(float=int)=4), needed for interleaving data
          if (integrate > 0) out.seek( (4*attribsWritten) + (4*read*integrate) );
          read++;

          if (floats) {
            float data = in.nextFloat();
            out.writeFloat(data);
          }
          else {
            int data = in.nextInt(); // read element
            out.writeInt(data); // write binary
          }

          // write padding for later values to have room for interleaving
          if (attribsWritten == 0 && integrate > 0) {
            for (int i = 0; i < integrate-1; i++)
              if (attribsWritten == 0) out.writeInt(0); // write 4 bytes
          }

          wrote++;
        }
        catch (Exception e) { Log.log("i/o error"); close(); return false; }

        notDone = (floats) ? in.hasNextFloat() : in.hasNextInt();
        notDone = ( (notDone && n2 == 0) || (notDone && read < n2) ) ? true : false;
      }

      // sanity check
      if (read != wrote) Log.log("data lost during output");
      out("\tattribute:\t[ " + headers.get(attribute).signature + " ]");
      out("\tin:\t\t[ " + read + " ]\n\tout:\t\t[ " + wrote + " ]");

      if (!noheaders)
      {
        // render fields for each attribute - should only see specified attributes
        try
        {
          fieldCount = headers.get(attribute).names.size();
          System.out.printf("\theader fields:\t");
          for (int i = 0; i < fieldCount; i++) {
            System.out.print(headers.get(attribute).names.get(i) + " [" +
                headers.get(attribute).vals.get(i).toString() + "] ");
            if (i%(fieldCount/3)==0 && i != 0 && i != fieldCount-1) System.out.print("\n\t\t\t");
          }
          out("\nDone.\n**********************************************************");
        }
        catch (Exception e) { Log.log("failed rendering attribute counts"); }

        attribute++;
        attribsWritten++;

        if (split) close(false); // close just the output file
      }

    }

    close(); // close everything

    out("Process complete: " + filesGenerated + " raw binary file(s) produced");

    // generate granite files
    if (granite >= 0) {
      util = new Bin2granite(this);
      if (util.makeXFiles()) out("Granite files sucessfully produced");
      else { Log.log("failed to generate granite files"); return false; }
    }

    // verification if requested
    if (verify)
      if (verifyOutput()) out("Validation succeeded - all data verified");
      else { Log.log("validation failed"); return false; }

    return true;
  }

  class Header {
    final ArrayList<String> names = new ArrayList<String>(); // field names
    final ArrayList<Number> vals = new ArrayList<Number>(); // field values in header
    final String signature; // aka attribute name

    public Header() throws NullPointerException
    {
      try
      { // you have better things to do with your time than read this part---
        // just a bunch of string manipulation
        String header = "", headerVals = "";

        header = in.nextLine().trim(); // grab signature ("ux", "db", etc.)
        if (header.length() < 2)
          header = in.nextLine().trim();
        signature = new String(header); // set signature field

        header = in.nextLine(); // header description string
        String [] split = header.split("/"); // line breakers
        int valueLines = split.length;
        split = header.split("[/ \\t]+");

        for (int i = 0; i < split.length; i++) // save string with clean attributes
          if (split[i].trim().length() > 0) names.add( split[i].toLowerCase().trim() );

        for (int i = 0; i < valueLines; i++) // merge all value lines
          headerVals += " " + in.nextLine().trim();

        split = headerVals.trim().split(" +");

        for (int i = 0; i < split.length; i++) // save string with clean attributes
          if (split[i].trim().length() > 0)
            vals.add( NumberFormat.getInstance().parse(split[i].trim()) );
      }
      catch (Exception e) { throw new NullPointerException("failed parsing header"); }
    }

    public int fieldCount() { return vals.size(); }
    public Number getField(int i) { return vals.get(i); }
    public Number getField(String name) {
      for (int i = 0; i < names.size(); i++)
        if (names.get(i).equals(name)) return getField(i);
      return null;
    }
    public String name(int i) { return names.get(i); }
  }

  // an offensively long main because I hate parsing in my constructor...
  // ...and I made a bad design choice.
  public static void main(String[] args)
  {
    Log.init("ascii2bin-errors", Log.LogToStdErr);
    SDBError.setErrorReportingLevel(SDBError.AlwaysThrow);

    if (args.length < 1) {
      Log.log(usage);
      System.exit(1);
    }

    if (args[0].trim().equals("-?")) {
      out(fullUsage);
      System.exit(0);
    }

    boolean[] skip = null; // skip by index
    String[] keep = null; // skip by name
    boolean split = true, verify = false, noheaders = true, floats = false;
    Integer integrate = new Integer(0);
    Integer granite = new Integer(-1); // indicates no granite, 0 = auto-dimensions

    // parse args:
    for (int i = 1; i < args.length; i++)
    {
      String arg = args[i].toLowerCase().trim();
      if (arg == null || arg.length() < 1) continue;

      if (arg.length() < 2 || arg.charAt(0) != '-') { Log.log(usage); System.exit(1); } // unknown arg
      if (arg.length() == 2)
      {
        if (arg.charAt(1) == 's') { split = false; continue; } // split OFF
        if (arg.charAt(1) == 'v') { verify = true; granite = 0; continue; } // verification ON
        if (arg.charAt(1) == 'f') { floats = true; continue; } // using floats ON
        if (arg.charAt(1) == '?') { out(fullUsage); System.exit(0); } // help!
      }
      if (arg.equals("-headers")) { noheaders = false; continue; } // headers ON
      else if (arg.charAt(1) == 'i') // integration ON
      {
        noheaders=false; split = false;
        args[i] = args[i].substring(2);
        try { integrate = Integer.parseInt(args[i]); }
        catch (NumberFormatException e) {
          Log.log("invalid # with integrate option: " + args[i]);
          System.exit(1);
        }
        continue;
      } else if (arg.charAt(1) == 'g') { // make granite files
        noheaders=false;
        args[i] = args[i].substring(2);
        try { granite = Integer.parseInt(args[i]); }
        catch (NumberFormatException e) { granite = 0; }
        continue;
      }

      else if (arg.length() < 6) { Log.log(usage); System.exit(1); } // < 6 because "-keep[" / "-skip["

      // must be a keep/skip list or bad arg
      boolean skipArg = false;
      if (arg.substring(1,5).equals("skip")) skipArg = true; // by string or boolean?
      else if (arg.substring(1,5).equals("keep")) { } //nothing to do
      else { Log.log(usage); System.exit(1); } // bad arg
      noheaders = false; // must have headers to use attribs

      args[i] = arg.substring(6); // move past the "-skip[" or "-keep["
      ArrayList<String> temp = new ArrayList<String>(); // save everything as a string for now

      // capture skip/keep list
      for (; i<args.length; i++)
      {
        String cur = args[i].toLowerCase().trim();

        if (cur == null || cur.length() < 1) continue;
        if (cur.charAt(0) == ']') break;
        if (cur.contains("]")) {
          temp.add(cur.substring(0, cur.indexOf(']')));
          break;
        }

        temp.add(cur); // add the attribute to skip
      }

      // convert to booleans if necessary
      if (temp.size() > 0) { // has to be something, i.e. ignore -keep[  ]
        if (skipArg)
        {
          skip = new boolean[temp.size()];
          for (int j = 0; j < skip.length; j++)
            if (temp.get(j).equals("1")) skip[j] = true;
            else skip[j] = false;
        }
        else keep = temp.toArray(new String[temp.size()]);
      }
    }

    Ascii2bin bin = null;
    try
    {
      bin = new Ascii2bin(args[0].trim(), integrate.intValue(), granite.intValue(),
          floats, split, verify, noheaders, skip, keep);
    }
    catch (FileNotFoundException e) { Log.log("failed to read from file \"" + args[0] + "\""); System.exit(1); }
    catch (NullPointerException e) { Log.log("failed to initialize with \"" + args[0] + "\""); System.exit(1); }

    boolean code = false;
    try { code = bin.processFile(); }
    catch (IOException e) { Log.log("failed to write output binary file: " + e.getMessage()); System.exit(1); }

    if (code) out("ascii2bin completed successfully");
    else out("ascii2bin completed with errors");

    bin.stampSource(); // date/time stamp this source code
    System.exit(0);
  }

}
