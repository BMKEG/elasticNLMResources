// $Id: DownloadError.java 236 2009-06-22 23:40:27Z tommy $

package edu.isi.bmkeg.elasticNlm.download;

/**
 * This class is a simple placeholder for an error code and an associated
 * filename, for use with MedlineDownloader when a file transfer fails.
 * @author tommying
 * @date $Date: 2009-06-22 16:40:27 -0700 (Mon, 22 Jun 2009) $
 * @version $Revision: $
 */
public class DownloadError
{
    public enum ErrorCode { DOWNLOAD_FAILED, MD5_FAILED };
    public String filename;
    public ErrorCode error;
    
   
    /**
     * 
     * @param f   IN: Name of problematic file
     * @param e   IN: Error description
     */
    public DownloadError(String f, ErrorCode e)
    {
        filename = f;
        error = e;
    }
}
