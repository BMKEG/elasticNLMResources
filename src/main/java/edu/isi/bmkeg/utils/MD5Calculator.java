// $Id: MD5Calculator.java 236 2009-06-22 23:40:27Z tommy $

// TESTING: Keep a store of 3-4 files with a known MD5 value in a directory
//          together with the code and just run the algorithm across them.
//          Also try on an empty file or a null argument.
// - emtpy file
// - null argument
// - 3 files with actual contents


package edu.isi.bmkeg.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes MD5 in hex.
 * @author tommying
 * @date $Date: 2009-06-22 16:40:27 -0700 (Mon, 22 Jun 2009) $
 * @version $Revision: $
 */
public class MD5Calculator
{
    private MessageDigest digestor;
    
    public MD5Calculator()
    {
        try
        {
            digestor = MessageDigest.getInstance("MD5");
        }
        catch(NoSuchAlgorithmException nsae)
        {
            System.err.println(nsae.getMessage());
            nsae.printStackTrace();
            System.exit(1);   // Terminate - if MD5 doesn't exist something is
                              // very very wrong
        }
    }
    
    
    /**
     * Computes the MD5 hash of an array of bytes.
     * @param b  IN: Byte array to hash
     * @return   A string containing the hash representation of the byte array,
     *           or null if something went wrong. Note that no leading '0'
     *           characters exist in the returned string. The standard Linux
     *           md5sum tool does prepend up to several '0's sometimes.
     */
    public String computeMD5(byte b[])
    {
        if(b == null || b.length <= 0)
            return null;
        
        digestor.update(b);
        byte md5[] = digestor.digest();
        BigInteger bigHex = new BigInteger(1, md5);  // Turn signed bit on
        
        // Reset so that the digest can be used again
        digestor.reset();
        return bigHex.toString(16);
    }
}
