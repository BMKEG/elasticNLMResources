// $Id: MedlineDownloader.java 314 2009-07-22 18:11:15Z tommy $

package edu.isi.bmkeg.elasticNlm.download;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import edu.isi.bmkeg.utils.MD5Calculator;

/**
 * A utility class for downloading Medline files. This class assumes the usual
 * Medline file naming convention, with GZipped XML files. Each source files
 * ends with ".xml.gz" and the MD5 checksum files end with ".xml.gz.md5", e.g.
 * "medline09n0001.xml.gz" and "medline09n0001.xml.gz.md5".
 * 
 * @author tommying
 * @date $Date: 2009-07-22 11:11:15 -0700 (Wed, 22 Jul 2009) $
 * @version $Revision: $
 */
public class MedlineDownloader
{
    private static final String XML_ENDING = ".xml.gz";
    private static final String MD5_ENDING = ".xml.gz.md5";
    private String loginName;
    private String password;
    private String ftpServer;
    private String ftpDirectory;
    private String localDirectory;
    private int retries = 0;
    
    
    /**
     * 
     * @param login      IN: Login name for ftp server. With NLM this is usually
     *                       just 'anonymous'
     * @param passwd     IN: Password for ftp server. With NLM this is usually
     *                       your email address. At ISI we use 'tommying@isi.edu'
     * @param server     IN: ftp server. With NLM this is usually ftp.nlm.nih.gov
     * @param serverDir  IN: Directory on the server to download from
     * @param localDir   IN: Local directory to download to
     * @param retries    IN: Number of times to retry downloading a file if the
     *                       initial download fails
     */
    public MedlineDownloader(String login,
                             String passwd,
                             String server,
                             String serverDir,
                             String localDir,
                             int retries)
    {
        loginName = login;
        password = passwd;
        ftpServer = server;
        ftpDirectory = serverDir;
        localDirectory = localDir;
        this.retries = retries;
        
        if(!localDirectory.endsWith(File.separator))
            localDirectory = localDirectory + File.separator;
    }
    
    
    /**
     * Downloads all of the files in the directory given in the constructor
     * call. This method is not recursive.
     * @return  A list of download problems
     * @throws IOException 
     */
    public ArrayList<DownloadError> download()
            throws IOException
    {
        int reply;
        FTPFile fileList[];
        HashSet<String> transferredMD5Files = new HashSet<String>();
        ArrayList<String> transferredXMLFiles = new ArrayList<String>();
        ArrayList<DownloadError> errorList = new ArrayList<DownloadError>();
        FTPClient ftp = new FTPClient();
        
        ftp.connect(ftpServer);
        if(!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
        {
            ftp.disconnect();
            throw new IOException("FTP server refused connection");
        }
        
        ftp.login(loginName, password);
        ftp.changeWorkingDirectory(ftpDirectory);
        
        // Prefer binary with FTP always. Note that if we set
        // FTPClient.setFileTransferMode(FTPClient.BINARY_FILE_TYPE)
        // instead of calling setFileType, the file will transfer OK, but the
        // bytecount will be slightly off - the MD5 comparison will fail
        ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
        
        // List all of the files in the directory. The actual contents are the
        // files ending in .xml.gz and .xml.gz.md5, but typically the Medline
        // update directory contains text files and various other documentation
        // too.
        fileList = ftp.listFiles();
        for(int i=0; i<fileList.length; i++)
        {
            if(fileList[i].isFile())
            {
                // Download the new file, write it to disk
                System.out.println("Downloading " + fileList[i].getName());
                String newFilename = localDirectory + fileList[i].getName();
                boolean transferSucceeded = false;
                int transferAttempts = 0;
                do
                {
                    FileOutputStream fos = new FileOutputStream(newFilename);
                    transferSucceeded = ftp.retrieveFile(fileList[i].getName(), fos);
                    fos.close();
                }while(!transferSucceeded && (transferAttempts++ < retries));
                
                // Make list of all of the downloaded MD5/XML files so that we can
                // do all the checking after downloading
                if(transferSucceeded)
                {
                    if(fileList[i].getName().endsWith(MD5_ENDING))
                        transferredMD5Files.add(newFilename);
                    else if(fileList[i].getName().endsWith(XML_ENDING))
                        transferredXMLFiles.add(newFilename);
                }
                else errorList.add(new DownloadError(newFilename, DownloadError.ErrorCode.DOWNLOAD_FAILED));
            }
        }
        
        // Check the checksums
        MD5Calculator digestor = new MD5Calculator();
        for(String xmlFile : transferredXMLFiles)
        {
            System.out.println("MD5 checking " + xmlFile);
            String md5File = xmlFile + ".md5";
            String medlineMD5 = readMD5File(md5File);
            
            try
            {
                // Read the xml.gz file into a byte array, then compute MD5
                File downloadedFile = new File(xmlFile);
                long fileSize = downloadedFile.length();
                byte fileBytes[] = new byte[(int)fileSize]; // Assume int always enough
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(xmlFile));
                int bytesRead = bis.read(fileBytes, 0, (int)fileSize);
                
                if(bytesRead == fileSize)
                {
                    String downloadedMD5 = digestor.computeMD5(fileBytes);
                    if(!compareMD5Strings(downloadedMD5, medlineMD5))
                    {
                        errorList.add(new DownloadError(xmlFile, DownloadError.ErrorCode.DOWNLOAD_FAILED));
                        System.err.println("fail for " + xmlFile);
                    }
                }
                else errorList.add(new DownloadError(xmlFile, DownloadError.ErrorCode.DOWNLOAD_FAILED));
            }
            catch(IOException ioe)
            {
                errorList.add(new DownloadError(xmlFile, DownloadError.ErrorCode.MD5_FAILED));
            }
        }
        return errorList;
    }
    
    
    /**
     * Compares two MD5 strings
     * @param newMD5  IN: MD5 hash code to compare
     * @param oldMD5  IN: MD5 hash code to compare
     * @return  true if the codes are the same, false otherwise
     */
    protected boolean compareMD5Strings(String newMD5,
                                        String oldMD5)
    {
        if((newMD5 == null && oldMD5 != null) ||
           (newMD5 != null && oldMD5 == null))
            return false;
        
        if(stripLeadingZeros(newMD5).equals(stripLeadingZeros(oldMD5)))
            return true;
        else return false;
    }
    

    /**
     * Removes any leading '0' characters from a string
     * @param s  IN: String to process
     * @return  The given string, with any leading '0' characters removed, or
     *          null if the string consists of '0's only
     */
    @SuppressWarnings("empty-statement")
    protected String stripLeadingZeros(String s)
    {
        int i;
        if(s == null || !s.startsWith("0"))
            return s;
        
        StringBuilder b = new StringBuilder(s.length());
        for(i=0; i<s.length() && s.charAt(i) == '0'; i++)
            ;
        return s.substring(i, s.length());
    }
    
    
    /**
     * Reads in one of the .xml.md5 files that come with the Medline
     * distribution, returning the hexadecimal MD5 they contain. The syntax of
     * these files is:
     * MD5 (<xml-filename>) = <MD5 hex>
     * 
     * e.g.
     * 
     * MD5 (medline08n0270.xml.gz) = e45e6d0a8fcd69b4fc0d5fb48a54f16c
     * @param filename  IN: Full path to MD5 file
     * @return  The hexadecimal MD5 code contained within the given file, or
     *          null if something went wrong
     */
    public String readMD5File(String filename)
    {
        String line;
        String hex = null;
        try
        {
            BufferedReader ins = new BufferedReader(new FileReader(filename));
            while((line = ins.readLine()) != null)
            {
                String tokens[] = line.split("\\s+");
                hex = tokens[tokens.length-1];
            }
            ins.close();
        }
        catch(IOException ioe)
        {
            return null;
        }
        return hex;
    }
}
