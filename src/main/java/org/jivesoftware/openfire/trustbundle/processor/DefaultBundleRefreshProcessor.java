package org.jivesoftware.openfire.trustbundle.processor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.jivesoftware.openfire.trustbundle.BundleRefreshError;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.jivesoftware.openfire.trustbundle.TrustBundleAnchor;
import org.jivesoftware.openfire.trustbundle.TrustBundleManager;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.cert.Thumbprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultBundleRefreshProcessor implements BundleRefreshProcessor
{

    public static final String PROPERTY_ALLOW_DOWNLOAD_FROM_NONVERIRIDED = "xmpp.client.tls.trustBundle.allowDownloadFromNonVerifiedSite";
    
	protected static final int DEFAULT_URL_CONNECTION_TIMEOUT = 10000; // 10 seconds	
	protected static final int DEFAULT_URL_READ_TIMEOUT = 10000; // 10 hour seconds	
	
	private static final Logger Log = LoggerFactory.getLogger(DefaultBundleRefreshProcessor.class);
	
    
	/**
	 * Default constructor.
	 */
	public DefaultBundleRefreshProcessor()
	{
    	final String propStr = JiveGlobals.getProperty( PROPERTY_ALLOW_DOWNLOAD_FROM_NONVERIRIDED , "false");
        
        final boolean allowFromNonVerifiedSite = Boolean.parseBoolean(propStr);
        
		///CLOVER:OFF
		if (allowFromNonVerifiedSite)
		{
			try
			{
		        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() 
		        {
		            public java.security.cert.X509Certificate[] getAcceptedIssuers() 
		            {
		                return null;
		            }
		            
		            public void checkClientTrusted(X509Certificate[] certs, String authType) 
		            {
		            }
		            
		            public void checkServerTrusted(X509Certificate[] certs, String authType) 
		            {
		            }
		        }};
		        
		        // Install the all-trusting trust manager
		        final SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCerts, new java.security.SecureRandom());
		        
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		        
		        // Create all-trusting host name verifier
		        
		        HostnameVerifier allHostsValid = new HostnameVerifier() 
		        {
		            public boolean verify(String hostname, SSLSession session) 
		            {
		                return true;
		            }
		        };
		        
		        // Install the all-trusting host verifier
		        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			}
			catch (Exception e)
			{
				
			}
		}
		///CLOVER:ON
	}
	
	public void refreshBundle(TrustBundle bundle)
	{
		// track when the process started
		final Instant processAttempStart = Instant.now();

		// get the bundle from the URL
		final byte[] rawBundle = downloadBundleToByteArray(bundle, processAttempStart);
	
		if (rawBundle == null)
			return;
		
		// check to see if there is a difference in the anchor sets
		// use a checksum 
		boolean update = false;
		String checkSum;
		try
		{
			checkSum = Thumbprint.toThumbprint(rawBundle).toString();
		} 
		catch (NoSuchAlgorithmException ex)
		{
			bundle.setLastRefreshAttempt(processAttempStart);
			bundle.setLastRefreshError(BundleRefreshError.INVALID_BUNDLE_FORMAT);
			updateBundleAttributesQuitely(bundle.getBundleName(), bundle);	
				
			Log.error("Failed to generate downloaded bundle thumbprint ", ex);
			return;
		}
		
		if (bundle.getCheckSum() == null)
			// never got a check sum... 
			update = true;
		else
		{
			update = !bundle.getCheckSum().equals(checkSum);
		}
		
		if (!update)
		{
			bundle.setLastRefreshAttempt(processAttempStart);
			bundle.setLastRefreshError(BundleRefreshError.SUCCESS);
			updateBundleAttributesQuitely(bundle.getBundleName(), bundle);	

			return;
		}
		
		final Collection<X509Certificate> bundleCerts = convertRawBundleToAnchorCollection(rawBundle, bundle, processAttempStart);

		if (bundleCerts == null)
			return;
		
		final HashSet<X509Certificate> downloadedSet = new HashSet<X509Certificate>((Collection<X509Certificate>)bundleCerts);	

		try
		{
			final Collection<TrustBundleAnchor> newAnchors = new ArrayList<TrustBundleAnchor>();
			for (X509Certificate downloadedAnchor : downloadedSet)
			{
				try
				{
					final TrustBundleAnchor anchorToAdd = new TrustBundleAnchor();
					anchorToAdd.setAnchorData(downloadedAnchor.getEncoded());
					anchorToAdd.setTrustBundleId(bundle.getId());
					
					newAnchors.add(anchorToAdd);
				}
				///CLOVER:OFF
				catch (Exception e) 
				{ 
					Log.warn("Failed to convert downloaded anchor to byte array. ", e);
				}
				///CLOVER:ON
			}

			TrustBundleManager.getInstance().deleteAnchorsByBundleId(bundle.getId());
			for (TrustBundleAnchor anchor : newAnchors)
				TrustBundleManager.getInstance().addTrustBundleAnchor(anchor.asX509Certificate(), bundle.getId());
			
			
			bundle.setLastRefreshAttempt(processAttempStart);
			bundle.setLastRefreshError(BundleRefreshError.SUCCESS);
			bundle.setCheckSum(checkSum);
			bundle.setLastSuccessfulRefresh(processAttempStart);
			
			
			TrustBundleManager.getInstance().updateTrustBundleAttributes(bundle.getBundleName(), bundle, false);

		}
		catch (Exception e) 
		{ 
			bundle.setLastRefreshAttempt(processAttempStart);
			bundle.setLastRefreshError(BundleRefreshError.INVALID_BUNDLE_FORMAT);
			updateBundleAttributesQuitely(bundle.getBundleName(), bundle);	
			
			Log.error("Failed to write updated bundle anchors to data store ", e);
		}
    }
	
	/**
	 * Converts a trust raw trust bundle byte array into a collection of {@link X509Certificate} objects.
	 * @param rawBundle The raw representation of the bundle.  This generally the raw byte string downloaded from the bundle's URL.
	 * @param existingBundle The configured bundle object in the DAO.  This object may contain the signing certificate
	 * used for bundle authenticity checking.
	 * @param processAttempStart The time that the update process started.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	protected Collection<X509Certificate> convertRawBundleToAnchorCollection(byte[] rawBundle, final TrustBundle existingBundle,
			final Instant processAttempStart)
	{
		Collection<? extends Certificate> bundleCerts = null;
		InputStream inStream = null;
		// check to see if its an unsigned PKCS7 container
		try
		{
			inStream = new ByteArrayInputStream(rawBundle);
			bundleCerts = CertificateFactory.getInstance("X.509").generateCertificates(inStream);
			
			// in Java 7, an invalid bundle may be returned as a null instead of throw an exception
			// if its null and has no anchors, then try again as a signed bundle
			if (bundleCerts != null && bundleCerts.size() == 0)
				bundleCerts = null;
			
		}
		catch (Exception e)
		{
			/* no-op for now.... this may not be a p7b, so try it as a signed message*/
		}
		finally
		{
			IOUtils.closeQuietly(inStream);
		}
		
		// didnt work... try again as a CMS signed message
		if (bundleCerts == null)
		{
			try
			{
				final CMSSignedData signed = new CMSSignedData(rawBundle);
				
				// if there is a signing certificate assigned to the bundle,
				// then verify the signature
				if (existingBundle.getSigningCertificateData() != null)
				{
					boolean sigVerified = false;
					
					
					final X509Certificate signingCert = existingBundle.getSigningCertificateAsX509Certificate();
		    		for (SignerInformation sigInfo : (Collection<SignerInformation>)signed.getSignerInfos().getSigners())	
		    		{
		    			
		    			try
		    			{
				    		if (sigInfo.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(signingCert)))
				    		{
				    			sigVerified = true;
				    			break;
				    		}
		    			}
		    			catch (Exception e) {/* no-op... can't verify */}
		    		}
		    		
		    		if (!sigVerified)
		    		{
		    			existingBundle.setLastRefreshAttempt(processAttempStart);
		    			existingBundle.setLastRefreshError(BundleRefreshError.UNMATCHED_SIGNATURE);
		    			updateBundleAttributesQuitely(existingBundle.getBundleName(), existingBundle);	
		    			
						Log.warn("Downloaded bundle signature did not match configured signing certificate.");
						return null;
		    		}
				}
				
				final CMSProcessableByteArray signedContent = (CMSProcessableByteArray)signed.getSignedContent();
				
				inStream = new ByteArrayInputStream((byte[])signedContent.getContent());
				
				bundleCerts = CertificateFactory.getInstance("X.509").generateCertificates(inStream);
			}
			catch (Exception e)
			{
				existingBundle.setLastRefreshAttempt(processAttempStart);
				existingBundle.setLastRefreshError(BundleRefreshError.INVALID_BUNDLE_FORMAT);
				updateBundleAttributesQuitely(existingBundle.getBundleName(), existingBundle);	
				
				Log.warn("Failed to extract anchors from downloaded bundle at URL " + existingBundle.getBundleURL());
			}
			finally
			{
				IOUtils.closeQuietly(inStream);
			}
		}
		
		return (Collection<X509Certificate>)bundleCerts;
	}
	
	/**
	 * Downloads a bundle from the bundle's URL and returns the result as a byte array.
	 * @param bundle The bundle that will be downloaded.
	 * @param processAttempStart The time that the update process started. 
	 * @return A byte array representing the raw data of the bundle.
	 */
	@SuppressWarnings("deprecation")
	protected byte[] downloadBundleToByteArray(TrustBundle bundle, Instant processAttempStart)
	{
		InputStream inputStream = null;

		byte[] retVal = null;
		final ByteArrayOutputStream ouStream = new ByteArrayOutputStream();
		
		try
		{
			// in this case the cert is a binary representation
			// of the CERT URL... transform to a string
			final URL certURL = new URL(bundle.getBundleURL());
			
			final URLConnection connection = certURL.openConnection();
			
			// the connection is not actually made until the input stream
			// is open, so set the timeouts before getting the stream
			connection.setConnectTimeout(DEFAULT_URL_CONNECTION_TIMEOUT);
			connection.setReadTimeout(DEFAULT_URL_READ_TIMEOUT);
			
			// open the URL as in input stream
			inputStream = connection.getInputStream();
			
			int BUF_SIZE = 2048;		
			int count = 0;

			final byte buf[] = new byte[BUF_SIZE];
			
			while ((count = inputStream.read(buf)) > -1)
			{
				ouStream.write(buf, 0, count);
			}
			
			retVal = ouStream.toByteArray();
		}
		///CLOVER:OFF
		catch (SocketTimeoutException e)
		{
			bundle.setLastRefreshAttempt(processAttempStart);
			bundle.setLastRefreshError(BundleRefreshError.DOWNLOAD_TIMEOUT);
			updateBundleAttributesQuitely(bundle.getBundleName(), bundle);	

			Log.warn("Failed to download bundle from URL " + bundle.getBundleURL(), e);
		}
		///CLOVER:ON
		catch (Exception e)
		{
			bundle.setLastRefreshAttempt(processAttempStart);
			bundle.setLastRefreshError(BundleRefreshError.NOT_FOUND);
			updateBundleAttributesQuitely(bundle.getBundleName(), bundle);				

			Log.warn("Failed to download bundle from URL " + bundle.getBundleURL(), e);
		}
		finally
		{
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(ouStream);
		}
		
		return retVal;
	}
	
	protected void updateBundleAttributesQuitely(String bundleName, TrustBundle bundle)
	{
		try
		{
			TrustBundleManager.getInstance().updateTrustBundleAttributes(bundleName, bundle, false);		
		}
		catch (Exception e)
		{
			/*no-op*/
		}
	}
}
