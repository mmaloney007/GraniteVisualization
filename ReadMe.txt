********************************************************************************
****************************** GraniteVision ***********************************
********************************************************************************

--------------- Previously the Solar Winds Visualization System ----------------

This software was originally intended to facilitate rapid data manipulation and 
rendering for the solar winds project at the University of New Hampshire. It
has been modified and re-released as a general purpose data processing tool
founded on the Granite system (details below).

It accepts input as ASCII files (valid UTF-8) or RAW binary in both standard
and customizable format specifications, and then subsequently prepares them for
traversal and scientific analysis or further extraction using Granite. Simple
utilities are provided for basic visualization and processing tasks on any desired 
region of the data, which is handled by Granite to maximize likely utility and 
speed. The user does not require any familiarity with Granite's operation, 
although naturally such knowledge will only empower a user to make the most from
both Granite and GraniteVision. 

See the Wiki and Granite homepage below for more details.

Run the program passing "-?" or view the source for full documentation of accepted
formats, how to specify a custom header and format structure for your datam, and 
various examples to get you up and running with the commandline utilities, if
you require more advanced functionality.

Please consider submitting any contributions you make for consideration in the
official public repository! Bug reports can be filed on the BitBucket page at:
https://bitbucket.org/entangledloops/granitevision/issues?status=new&status=open


-----------------------------------------------
Authors:          Stephen Dunn              (stephen@cs.unh.edu)
                  Mike Maloney              (mikey.maloney@me.com)
                  Daniel Bergeron [Granite] (rdb@cs.unh.edu)
                  
Publication Date:  5/19/2013
-----------------------------------------------
Files Authored:

      >>>  Bin2granite.java  <<<
      >>>   Ascii2bin.java   <<<
      >>>    PhysView.java   <<<
      >>>      make.sh       <<<
      >>>       run.sh       <<<
      
* For information regarding Granite, see: http://www.cs.unh.edu/~sdb/index.html
* All source files should be found under the subdirectory "src"
* All scripts should be in the root directory

COMPILE: ./make.sh (if it fails, try "chmod 755 make.sh" first)
RUN:     ./run.sh

* Try opening some XFDL files in the "demo" subdirectory to ensure that 
everything is working properly before using your own data. 

* Converted ASCII files will be output to the same directory they are in with
the extensions "raw".

DEPENDENCIES:

--------------------------------------------------------------------------------
  Required        Name                      Purpose
  ---------       ---------                 ---------
  [X]             Java Runtime (>= 1.6)     Compiler/Runtime Environment
  [X]             Granite                   Data Management (http://www.cs.unh.edu/~sdb/)
  [ ]             ViewSlice.java            For use with PhysView
  [ ]             OrangeExtensions*         Additional OSX Support
--------------------------------------------------------------------------------

* NOTE: compiling without the included OrangeExtensions would require
commenting out the relevant OSX support code in PhysView.java. This extension
is provided to ensure compatability with older versions of OSX.

Summary of components:

********* Bin2granite *********

IN:   a file in raw binary format, big endian expected
OUT:  XFDL and XDDL files compatible with Granite 

********* Ascii2bin *********

IN:   an ascii file containing formatted integer or floating point values. for
formatting specifications, run with flag '-/?'
OUT:  raw binary files compatible with Bin2granite

********* PhysView *********

IN:   N/A
OUT:  depends on GUI selection

PhysView is a GUI wrapper for the previous 2 programs and a modified version
of ViewSlice.java by Prof. Daniel Bergeron (rdb@cs.unh.edu).

********* make.sh *********

IN:   files listed above and ViewSlice.java
OUT:  executable binaries (run PhysView first)
