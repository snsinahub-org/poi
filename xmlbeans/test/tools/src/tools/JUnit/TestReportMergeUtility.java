/*   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package tools.JUnit;

import junit.framework.TestCase;
import junit.framework.Assert;

import noNamespace.TestLogDocument.TestLog;
import noNamespace.TestLogDocument.TestLog.HeaderInfo;
import noNamespace.*;
import noNamespace.TestResultContainerDocument.TestResultContainer;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

import org.apache.xmlbeans.XmlOptions;

/**
 * Utility to merge JUnit Xml log files generated by the custom Xml
 * ResultFormatter plugin for Ant's <code>JUnit</code> task.
 */
public class TestReportMergeUtility
{
    public static int INFO_VERBOSE = 3;
    public static int INFO_NORMAL = 2;
    public static int INFO_SILENT = 1;

    private int _infoLevel = INFO_NORMAL;
    private File dir, outFile;


    public TestReportMergeUtility(File baseDir, File outFile, int infoLevel)
    {
        this.dir = baseDir;
        this.outFile = outFile;
        this._infoLevel = infoLevel;
    }

    public void info(String message, int infoLevel)
    {
        if (infoLevel <= this._infoLevel)
            System.out.println(message);
    }

    public void doMerge()
        throws IOException
    {
        TestLogDocument logDoc = TestLogDocument.Factory.newInstance();
        TestLog log = logDoc.addNewTestLog();
        File[] files = getXmlFiles(dir);
        int testCount = 0;
        ArrayList results = new ArrayList();
        for (int i = 0; i < files.length; i++)
        {
            try
            {
                TestResultContainerDocument doc =
                        TestResultContainerDocument.Factory.parse(files[i]);
                TestResultType[] resArr =
                        doc.getTestResultContainer().getTestResultArray();
                for (int j = 0; j < resArr.length; j++)
                {
                    log.addNewTestResult();
                    log.setTestResultArray(testCount++, resArr[j]);
                }
                info("Processed: " + files[i].getAbsolutePath(), INFO_VERBOSE);
            }
            catch (org.apache.xmlbeans.XmlException xe)
            {
                // Possible parse error
                info("Could not process " + files[i].getAbsolutePath(), INFO_NORMAL);
                info(xe.getMessage(), INFO_NORMAL);
            }
        }
        // Populate the attributes for test-log

        // testtype
        String testtype = System.getProperty("TESTTYPE", "AUTO");
        if (testtype.equalsIgnoreCase("AUTO"))
            log.setTesttype(TestLog.Testtype.AUTOMATED);
        else
            log.setTesttype(TestLog.Testtype.MANUAL);

        // runid
        String dateFormatStr = "_yy_MMM_dd_HH_mm_ss_SS";
        String dateStr = new SimpleDateFormat(dateFormatStr).format(new java.util.Date());
        String defRunId = System.getProperty("user.name").toUpperCase() + dateStr;
        String runId = System.getProperty("RUNID", defRunId);
        log.setRunid(runId);

        // hostname
        String hostname;
        try
        {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e)
        {
            // Ignore.. not critical
            hostname = "UNKNOWN_HOST";
            info("Could not get Hostname. Using UNKNOWN_HOST", INFO_VERBOSE);
        }
        log.setHostname(hostname);

        // TODO: set Defaults/check sysprop for other attributes

        // Add <environment> element
        EnvironmentType env = log.addNewEnvironment();
        Map envMap = new HashMap();
        envMap.put("JVM_NAME", System.getProperty("java.vm.name"));
        envMap.put("JVM_VENDOR", System.getProperty("java.vm.vendor"));
        envMap.put("JVM_VERSION", System.getProperty("java.vm.version"));
        envMap.put("OS", System.getProperty("os.name"));
        String defFreq = "checkin";
        envMap.put("Frequency", System.getProperty("test.run.frequency", defFreq));

        Iterator itr = envMap.keySet().iterator();
        int envCount = 0;
        while (itr.hasNext())
        {
            EnvironmentType.EnvAttribute envAttr = env.addNewEnvAttribute();
            String name = (String) itr.next();
            String value = (String) envMap.get(name);
            envAttr.setValue(value);
            envAttr.setName(name);
        }

        // Add <header-info> element
        HeaderInfo hdrInfo = log.addNewHeaderInfo();
        hdrInfo.setResultcount(Integer.toString(testCount));
        hdrInfo.setChecksum(Integer.toString(testCount));
        hdrInfo.setExecdate(log.getTestResultArray(0).getExectime());
        hdrInfo.setExecaccount(System.getProperty("user.name"));

        XmlOptions opts = new XmlOptions().setSavePrettyPrint();

        // Write it out the outfile
        FileOutputStream fos = null;
        try
        {
            outFile.delete();
            fos = new FileOutputStream(outFile);
            fos.write(logDoc.xmlText(opts).getBytes());
            info("Log files merged to " + outFile.getAbsolutePath(), INFO_NORMAL);
        }
        catch(IOException ioe)
        {
            info("Could not write to outfile file: " + outFile.getAbsolutePath(),
                 INFO_NORMAL);
            info(ioe.getMessage(), INFO_NORMAL);
        }
        finally
        {
            if (fos != null)
                fos.close();
        }

    }

    public File[] getXmlFiles(File dir)
        throws IOException
    {
        if (dir.isFile() || !dir.exists())
            throw new IOException("Not a directory: " + dir.getAbsolutePath());

        return dir.listFiles(new XmlFileFilter());
    }


    public class XmlFileFilter implements java.io.FileFilter
    {
        public boolean accept(File pathname)
        {
            if (pathname == null)
                return false;
            String filename = pathname.getAbsolutePath();
            int index = filename.lastIndexOf(".");
            if (filename.substring(index+1).equalsIgnoreCase("xml"))
                return true;
            return false;
        }
    }

    //////////////////////////////////////////////////////////////////
    // Main
    public static void main(String args[])
        throws Exception
    {
        int level = INFO_NORMAL; // Default: Normal
        File baseDir = null;
        File outFile = null;
        // Process the arguments
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.charAt(0) == '-')
            {
                if (arg.equals("-silent"))
                    level = INFO_SILENT; // level: Silent
                else if (arg.equals("-normal"))
                    level = INFO_NORMAL; // level: Normal
                else if (arg.equals("-verbose"))
                    level = INFO_VERBOSE; // level: Verbose
                continue;
            }

            int index = arg.indexOf('=');
            if (index == -1)
                continue;     // Not a valid argument.. ignore.
            if (arg.substring(0, index).equals("dir"))
            {
                baseDir = new File(arg.substring(index+1));
                continue;
            }
            if (arg.substring(0, index).equals("outfile"))
            {
                outFile = new File(arg.substring(index+1));
                continue;
            }
        }

        // Verify baseDir and outfile
        if (baseDir == null)
            usage("Specify the directory containing the log files", true);
        if (outFile == null)
            usage("Specify the file to write the merged log", true);

        // Check validity of basedir and outfile
        if (!baseDir.isDirectory())
            usage("The 'dir' specified is not a directory: "
                  + baseDir.toString(),
                  true);
        if (outFile.exists() && !outFile.isFile())
            usage("The output file specified is not a valid file: "
                  + outFile.toString(),
                  true);

        // Everything looks good.
        new TestReportMergeUtility(baseDir, outFile, level).doMerge();
    }

    static void println(Object O)
    {
        System.out.println(O.toString());
    }

    public static void usage()
    {
        usage(null, false);
    }

    public static void usage(String extraMessage)
    {
        usage(extraMessage, false);
    }

    public static void usage(String extraMessage, boolean exit)
    {
        if ((extraMessage != null) && (extraMessage.trim().length() > 0))
            println(extraMessage);

        println("*** JUnit Xml Log Merge tool ***");
        println("Usage:");
        println("TestReportMergeUtility dir=<report dir> outfile=<outputfile> -<level>");
        println("");
        println("Where");
        println("         dir       - Directory containing report files to merge");
        println("                     All .xml files in this folder will be processed");
        println("         outfile   - The file to write out the merged Xml file");
        println("         -<level>  - Optional verbosity level");
        println("          silent   - No messages will be printed during processing");
        println("          normal   - A message will be printed for every time that");
        println("                     an error occurs when processing a file");
        println("          verbose  - A message will be printed for every file processed");

        if (exit)
            System.exit(1);
    }




}