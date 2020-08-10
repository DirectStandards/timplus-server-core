package org.jivesoftware.util.cert;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class Thumbprint
{
	private final byte[] digest;
	private final String digestString;
	
	/**
	 * Creates a thumbprint of an X509Certificate.
	 * @param cert The certificate to convert.
	 * @return A thumbprint of the certificate.
	 */
	public static Thumbprint toThumbprint(X509Certificate cert)
	{
		Thumbprint retVal = null;
		
		if (cert == null)
			throw new IllegalArgumentException();
		
		try
		{
			retVal =  new Thumbprint(cert);
		}
		///CLOVER:OFF
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
		///CLOVER:ON
		return retVal;
	}
	
	/**
	 * Creates a thumbprint of a byte array.
	 * @param bytes Array of bytes to create a thumbprint from..
	 * @return A thumbprint of the byte array.
	 */
	public static Thumbprint toThumbprint(byte bytes[]) throws NoSuchAlgorithmException
	{
		Thumbprint retVal = null;
		
		if (bytes == null)
			throw new IllegalArgumentException();
		
		try
		{
			retVal =  new Thumbprint(bytes);
		}
		///CLOVER:OFF
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
		///CLOVER:ON
		return retVal;
	}
	
	/*
	 * Private internal constructor
	 */
	private Thumbprint (X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException
	{
		this(cert.getEncoded());
	}
	
	/*
	 * Private internal constructor
	 */
	private Thumbprint (byte[] bytes) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] der = bytes;

		md.update(der);
        digest = md.digest();
        
        digestString = createStringRep();
	}
	
	/**
	 * Gets the raw byte digest of the certificate's DER encoding. 
	 * @return The certificates digest.
	 */
	public byte[] getDigest()
	{
		return digest.clone();
	}
	
	/*
	 * Create the String representation of the digest
	 */
	private String createStringRep()
	{
	    final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', 
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};		
		
        StringBuffer buf = new StringBuffer(digest.length * 2);

        for (byte bt : digest) 
        {
            buf.append(hexDigits[(bt & 0xf0) >> 4]);
            buf.append(hexDigits[bt & 0x0f]);
        }

        return buf.toString();
	}
	
	@Override
	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return digestString;
	}
	
	@Override
	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Thumbprint))
			return false;
		
		Thumbprint compareTo = (Thumbprint)obj;
		
		// deep compare
		return Arrays.equals(compareTo.digest, digest);
	}
	
	@Override
	public int hashCode()
	{
		// thumb prints should be unique, so should their string representations
		// use the string representation's hash code
		return toString().hashCode();
	}
}
