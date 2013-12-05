package physics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import edu.unh.sdb.datasource.DataSource;
import edu.unh.sdb.datasource.Log;

public class Bin2granite
{
  // XFDL *************
  final String xfdl_open = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<!DOCTYPE FileDescriptor PUBLIC \"-//SDB//DTD//EN\"  \"fdl.dtd\">\n"
      + "<FileDescriptor\n" + "            fileName=\"FILENAME\"\n"
      + "            fileType=\"binary\"\n"
      + "            byteOrder=\"big\">\n";
  final String xfdl_granite = "<Bounds lower=\"0\"   upper=\"UPPER_BOUND\"/>\n";
  final String xfdl_field = "<Field fieldName= \"FIELD_NAME\"   fieldType=\"FIELD_TYPE\"/>\n";
  final String xfdl_close = "</FileDescriptor>\n";

  // XDDL *************
  final String xddl_open = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<!DOCTYPE DataSourceList PUBLIC \"-//SDB/DTD//EN\" \"ddl.dtd\">\n"
      + "<DataSourceList>\n";
  final String xddl_xfdl = "  <DataSource dsName = \"DS_NAME\"  fileName=\"XFDL_NAME\"/>\n"; // same
  // name
  // as
  // below
  final String xddl_source_open = "  <DataSource dsName=\"attrJoin\" type=\"joined\" >\n";
  final String xddl_component_open = "    <Component dsName = \"DS_NAME\" >\n";
  final String xddl_field = "      <Map fieldName = \"FIELD_NAME\" position= \"POSITION\"  />\n";
  final String xddl_component_close = "    </Component>\n";
  final String xddl_source_close = "    </DataSource>\n";
  final String xddl_close = "  <PublicDS dsName=\"attrJoin\"/>\n"
      + "</DataSourceList>\n";

  final String[] xfdls;
  final ArrayList<ArrayList<Integer>> bounds = new ArrayList<ArrayList<Integer>>();
  final Ascii2bin in;
  String filename;

  // NOTE: You *should* have the Granite log opened before using this class as it
  // assumes there is a log file. If using through ascii2bin, this is not a
  // concern.
  public Bin2granite(Ascii2bin in)
  {
    this.in = in;
    this.filename = in.filename;

    // prepare filenames for output
    filename += ".xddl";
    xfdls = new String[ in.headers.size() ];
    for (int i = 0; i < in.headers.size(); i++) {
      xfdls[i] = new String(in.filename + "_"
          + in.headers.get(i).signature + ".xfdl");
    }

  }

  public boolean makeXFiles()
  {
    PrintWriter w = null;
    int nx = 1, ny = 1, nz = 1;

    // first generate the xfdl files
    for (int i = 0, count = 0; i < in.headers.size(); i++)
    {
      if ( in.skip(i) ) continue;
      //if ( count >= in.attribsToWrite(count) ) break;

      int n2 = in.headers.get(i).getField("n2").intValue();
      nx = in.headers.get(i).getField("nx").intValue()-1;
      ny = in.headers.get(i).getField("ny").intValue()-1;
      nz = in.headers.get(i).getField("nz").intValue()-1;

      try
      {
        try { new File( xfdls[i] ).delete(); } // clear old files
        catch (Exception e2) {}

        w = new PrintWriter( new BufferedWriter( new FileWriter(xfdls[i]) ) );

        String [] split = filename.split("/");
        String rawName = split[split.length-1];
        split = rawName.split("\\.");
        rawName = split[0] + "_" + in.headers.get(i).signature;
        /*
        String rawName = filename;
        String[] p = xfdls[i].split("\\.");
        for (int j = 0; j < p.length - 1; j++)
          rawName += p[j] + ".";*/

        w.write(xfdl_open.replaceAll("FILENAME", rawName + ".raw"));
        //replace 3 with in.dim
        for (int j = 0; j < 3; j++)
        {
          if(j == 0)
            w.write(xfdl_granite.replaceAll("UPPER_BOUND", ""+ nx));
          if(j == 1)
            w.write(xfdl_granite.replaceAll("UPPER_BOUND", ""+ ny));
          if(j ==2)
            w.write(xfdl_granite.replaceAll("UPPER_BOUND", ""+ nz));
        }

        String temp = xfdl_field.replaceAll("FIELD_NAME",
            in.headers.get(i).signature);
        temp = temp.replaceAll("FIELD_TYPE", ((in.floats) ? "float"
            : "int"));

        w.write(temp);
        w.write(xfdl_close);
        w.close();

        out("\"" + xfdls[i] + "\" written");

      }

      catch (Exception e)
      {
        try { w.close(); }
        catch (Exception e2) { }
        Log.log("failed while generating xfdl files for " + xfdls[i]);
        return false;
      }

      count++;
    }

    /* // experimental auto-calculation of optimal boundaries
    else
    {
      // if (in.granite == 0 || (in.granite > 0 && n2%in.granite != 0))

      int uppers[] = new int[3], dim = 2;
      int smallPrime = (int) Ascii2bin.smallestPrime(n2);

      uppers[0] = n2 / smallPrime;
      uppers[1] = (int) Ascii2bin.smallestPrime(uppers[0]);
      if (uppers[1] != 1)
      {
        uppers[2] = (uppers[0] / uppers[1]);
        uppers[0] = smallPrime;
        dim = 3;
      }
      else
      {
        uppers[1] = uppers[0];
        uppers[0] = smallPrime;
      }
      if (smallPrime == 1)
      {
        dim = 1;
        uppers[0] = n2;
      }

      bounds.add(new ArrayList<Integer>());
      for (int j = 0; j < dim; j++)
        // save for granite verification
        bounds.get(count).add(uppers[j]);

      try
      {
        try
        {
          new File(xfdls[i]).delete();
        } catch (Exception e2)
        {
        }

        w = new PrintWriter(new BufferedWriter(new FileWriter(xfdls[i])));

        String rawName = filename;
        if (in.split || in.integrate > 0)
          rawName = "";
        String[] p = xfdls[i].split("\\.");
        for (int j = 0; j < p.length - 1; j++)
          rawName += p[j] + ".";

        w.write(xfdl_open.replaceAll("FILENAME", rawName + "raw"));

        for (int j = 0; j < dim; j++)
          w.write(xfdl_granite.replaceAll("UPPER_BOUND", ""
              + (uppers[j] - 1)));

        String temp = xfdl_field.replaceAll("FIELD_NAME",
            in.headers.get(i).signature);
        temp = temp.replaceAll("FIELD_TYPE", ((in.floats) ? "float"
            : "int"));

        w.write(temp);
        w.write(xfdl_close);
        w.close();

        out("\"" + xfdls[i] + "\" written");

      } catch (Exception e)
      {
        try
        {
          w.close();
        } catch (Exception e2)
        {
        }
        Log.log("failed while generating xfdl files for " + xfdls[i]);
        return false;
      }
      count++;

    }

  }*/

    try
    {
      for (int i =  0; i < xfdls.length; i++)
      {
        String [] split = xfdls[i].split("/");
        xfdls[i] = split[split.length-1];
      }

    } catch (Exception e)
    {

    }

    // generate xddl if composite
    try
    {
      try { new File(filename).delete(); } // clean old xddl
      catch (Exception e2) { }

      w = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
      w.write(xddl_open);

      for (int x = 0; x < in.headers.size(); x++)
        if (in.skip(x))
          continue;
        else
          w.write(xddl_xfdl.replaceAll("DS_NAME",
              in.headers.get(x).signature).replaceAll(
              "XFDL_NAME", xfdls[x]));

      w.write("\n");

      w.write(xddl_source_open);
      for (int p = 0, count_p = 0; p < in.headers.size(); p++)
      {
        if (in.skip(p))
          continue;
        w.write(xddl_component_open.replaceAll("DS_NAME",
            in.headers.get(p).signature));
        w.write(xddl_field.replaceAll("FIELD_NAME",
            in.headers.get(p).signature).replaceAll("POSITION",
            "" + count_p));
        w.write(xddl_component_close);
        count_p++;
      }
      w.write(xddl_source_close);

      w.write(xddl_close);
      w.close();
      out("\"" + filename + "\" written");
    } catch (Exception e)
    {
      try { w.close(); }
      catch (Exception e2) { }
      Log.log("failed producing xddl file");
      return false;
    }

    return true;
  }

  public boolean verify()
  {
    try
    {
      RandomAccessFile r = null;
      DataSource ds[] = new DataSource[in.attribsWritten];

      for (int i = 0; i < in.attribsWritten; i++)
      {
        long rawLen = 0, graniteLen = 0, index = 0;

        for (int j = 0; j < in.headers.size(); j++)
        {
          if (in.skip(j))
            continue;

          ds[i] = DataSource.create(xfdls[j], xfdls[j]);
          ds[i].activate();
          r = new RandomAccessFile(in.filename + "_"
              + in.headers.get(j).signature + ".raw", "r");

          long start = (in.headers.get(j).getField("n1")).longValue(); // start
          // point
          index = 0;
          rawLen = r.length();
          graniteLen = ds[i].volume() * 4;

          if (rawLen != graniteLen)
          {
            Log.log("file size mismatch: raw[ " + rawLen
                + " ] != granite[ " + graniteLen + " ]");
            try
            {
              r.close();
            } catch (Exception e2)
            {
            }
            return false;
          }

          // skip ahead to correct position in original file
          while (++index < start)
            if (in.floats)
              r.readFloat();
            else
              r.readInt();

          // data is there, but something is wrong with endianness,
          // still figuring it out
          Number temp1 = (in.floats) ? new Float(0) : new Integer(0);
          Number[] temp2 = (in.floats) ? new Float[ds[i].getFloats().length]
              : new Integer[ds[i].getInts().length];

          if (in.floats)
          {
            float[] f = ds[i].getFloats();
            for (int l = 0; l < f.length; l++)
              temp2[l] = new Float(f[l]);
          }
          else
          {
            int[] ints = ds[i].getInts();
            for (int l = 0; l < ints.length; l++)
              temp2[l] = new Integer(ints[l]);
          }

          for (int l = 0; l < ds[i].getNumAttributes(); l++)
          {
            for (int count = 0; count < temp2.length; count++)
            {
              if (in.floats)
                temp1 = r.readFloat();
              else
                temp1 = r.readInt();

              if ((in.floats && temp1.floatValue() != temp2[count]
                  .floatValue())
                  || (!in.floats && temp1.intValue() != temp2[count]
                  .intValue()))
              {
                r.close();
                out("failure at index " + count + " " + temp1
                    + " != " + temp2[count]);
                return false;
              }
            }
          }
          r.close();

        }

        out("File verified [" + rawLen + "]");
      }

    } catch (Exception e)
    {
      Log.log("bad, granite. bad.");
      return false;
    }

    return true;
  }

  public static void out(String s)
  {
    System.out.println(s);
  }

}

/*
 * // prepare input file path by ensuring "/quote/enclosure/for broken/paths"
 * String file = ""; try { file = args[0].trim(); if (file.contains(" ")) { if
 * (file.charAt(0) != '\"') if (file.charAt( file.length()-1 ) != '\"') file =
 * "\"" + file + "\""; else file = "\"" + file; else if (file.charAt(
 * file.length()-1 ) != '\"') file += "\""; } } catch (Exception e) {
 * Log.log(usage); System.exit(1); }
 */
