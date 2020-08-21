package org.jivesoftware.openfire.keystore;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.jivesoftware.openfire.trustbundle.TrustBundleAnchor;
import org.jivesoftware.openfire.trustcircle.TrustCircle;
import org.jivesoftware.openfire.trustcircle.TrustCircleManager;
import org.jivesoftware.util.ReferenceIDUtil;
import org.jivesoftware.util.crl.impl.CRLRevocationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

/**
 * A Trust Manager implementation that adds Openfire-proprietary functionality.
 * 
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
// TODO re-enable optional OCSP checking.
public class OpenfireX509TrustManager implements X509TrustManager
{
    private static final Logger Log = LoggerFactory.getLogger( OpenfireX509TrustManager.class );

    private static final Provider PROVIDER = new BouncyCastleProvider();
    
    private SSLEngine engine;
    
    static
    {
        // Add the BC provider to the list of security providers
        Security.addProvider( PROVIDER );
    }

    /**
     * A boolean that indicates if this trust manager will allow self-signed certificates to be trusted.
     */
    protected final boolean acceptSelfSigned;

    /**
     * A boolean that indicates if this trust manager will check if all certificates in the chain (including the root
     * certificates) are currently valid (notBefore/notAfter check).
     */
    private final boolean checkValidity;

    protected TrustCircleManager circleManager;
    
    public OpenfireX509TrustManager( TrustCircleManager circleManager, boolean acceptSelfSigned, boolean checkValidity ) throws NoSuchAlgorithmException, KeyStoreException
    {
    	this.circleManager = circleManager;
        this.acceptSelfSigned = acceptSelfSigned;
        this.checkValidity = checkValidity;

        Log.debug( "Constructed trust manager. Accepts self-signed: {}, checks validity: {}", acceptSelfSigned, checkValidity );
    }

    public void setSSLEngine(SSLEngine engine)
    {
    	this.engine = engine;
    }
    
    @Override
    public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException
    {
        // Find and use the end entity as the selector for verification.
        final X509Certificate endEntityCert = CertificateUtils.identifyEndEntityCertificate( Arrays.asList( chain ) );
        final X509CertSelector selector = new X509CertSelector();
        selector.setCertificate( endEntityCert );

        try
        {
            checkChainTrusted( selector, chain );
        }
        catch ( InvalidAlgorithmParameterException | NoSuchAlgorithmException | CertPathBuilderException ex )
        {
            throw new CertificateException( ex );
        }
    }

    @Override
    public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException
    {
        // Find and use the end entity as the selector for verification.
        final X509Certificate endEntityCert = CertificateUtils.identifyEndEntityCertificate( Arrays.asList( chain ) );
        final X509CertSelector selector = new X509CertSelector();
        selector.setCertificate( endEntityCert );

        try
        {
            checkChainTrusted( selector, chain );
        }
        catch ( InvalidAlgorithmParameterException | NoSuchAlgorithmException | CertPathBuilderException ex )
        {
            throw new CertificateException( ex );
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
    	final Set<X509Certificate> result = new HashSet<>();
    	
    	Collection<TrustCircle> circles = null;
    	
		// make sure we have a reference id so we no what domain connection is being requested
		final String referenceId = ReferenceIDUtil.getSessionReferenceId(engine.getSession());
		
		Log.info("Looking up trust anchors associated to domain " + referenceId);
		
    	try
    	{
			if (referenceId != null)
	    	{
	    		
	    		circles = circleManager.getCirclesByDomain(referenceId, true, true);
	    	}
	    	else
	    	{
	    		// fall back 
	    		circles = circleManager.getTrustCircles(true, true);
	
	    	}
    	}
    	catch (Exception e)
    	{
    		Log.warn("Could not get trust anchors for trust validation.");
    	}
    	
    	if (circles == null || circles.isEmpty())
    		return new X509Certificate[] {};
    	
    	for (TrustCircle circle : circles)
    	{
    		for (TrustBundle bundle : circle.getTrustBundles())
    			for (TrustBundleAnchor anchor : bundle.getTrustBundleAnchors())
    				if (checkValidity)
    					result.addAll(CertificateUtils.filterValid( anchor.asX509Certificate()));
    						
			for (org.jivesoftware.openfire.trustanchor.TrustAnchor anchor : circle.getAnchors())
				if (checkValidity)
					result.addAll(CertificateUtils.filterValid( anchor.asX509Certificate()));	
    		
    	}
    	
        return result.toArray( new X509Certificate[ result.size() ] );
    }

    /**
     * Determine if the given partial or complete certificate chain can be trusted to represent the entity that is
     * defined by the criteria specified by the 'selector' argument.
     *
     * A (valid) partial chain is a chain that, combined with certificates from the trust store in this manager, can be
     * completed to a full chain.
     *
     * Chains provided to this method do not need to be in any particular order.
     *
     * This implementation uses the trust anchors as represented by {@link #getAcceptedIssuers()} to verify that the
     * chain that is provided either includes a certificate from an accepted issuer, or is directly issued by one.
     *
     * Depending on the configuration of this class, other verification is done:
     * <ul>
     *     <li>{@link #acceptSelfSigned}: when {@code true}, any chain that has a length of one and is self-signed is
     *                                    considered as a 'trust anchor' (but is still subject to other checks, such as
     *                                    expiration checks).</li>
     * </ul>
     *
     * This method will either return a value, which indicates that the chain is trusted, or will throw an exception.
     *
     * @param selector Characteristics of the entity to be represented by the chain (cannot be null).
     * @param chain The certificate chain that is to be verified (cannot be null or empty).
     * @return A trusted certificate path (never null).
     *
     * @throws InvalidAlgorithmParameterException if the algorithm is invalid
     * @throws NoSuchAlgorithmException if the algorithm could not be found
     * @throws CertPathBuilderException if there was a problem with the certificate path
     */
    protected CertPath checkChainTrusted( CertSelector selector, X509Certificate... chain ) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertPathBuilderException
    {
        if ( selector == null )
        {
            throw new IllegalArgumentException( "Argument 'selector' cannot be null");
        }

        if ( chain == null || chain.length == 0 )
        {
            throw new IllegalArgumentException( "Argument 'chain' cannot be null or an empty array.");
        }

        Log.debug( "Attempting to verify a chain of {} certificates.", chain.length );

        /*
         * The certificate path validation process checks for valid dates in certificates,
         * but we will choose to fail fast in anything in the chain is expired.  This will save
         * time from having to lookup intermediate certificates (if necessary) if the given certificates
         * are already not valid.
         */
        X509Certificate endEntityCert = null;
		try
		{
			endEntityCert = CertificateUtils.identifyEndEntityCertificate( Arrays.asList( chain ) );
		} 
		catch (Exception e1)
		{
			throw new IllegalArgumentException( "Could not get end entity certificate from chain.");
		}
		
		final Set<X509Certificate> acceptedServerCerts = CertificateUtils.filterValid( endEntityCert );
        if (acceptedServerCerts.size() <= 0)
        {
        	// the end entity certificate is invalid
        	Log.warn( "TLS certificate chain has an expired certificate.  This is an invalid chain and the connection is rejected." );
        	throw new CertPathBuilderException("The certificate chain contains an expired certificate");
        }
        
        /*
         * Make sure the certificate isn't revoke.  The default cert path checker can do this work, but the revocation
         * manager uses some optimizations in terms of caching so the CRL doesn't have to be downloaded every time.
         */
        try
        {
        	
        	final CRLRevocationManager revManager = CRLRevocationManager.getInstance();
        	
        	if (revManager.isRevoked(endEntityCert))
        	{
        		Log.warn( "TLS end enity certificate has been marked as revoked.  The connection is rejected." );
        		throw new CertPathBuilderException("TLS end enity certificate has been marked as revoked.");
        	}
        }
        catch (CertPathBuilderException e)
        {
        	throw e;
        }
        catch (Exception e)
        {
        	throw new IllegalArgumentException("Could not check revocation status of end entity certificate.");
        }
        
        
        
        // The set of trusted issuers (for this invocation), based on the issuers from the truststore.
        final Set<X509Certificate> trustedIssuers = new HashSet<>();
        trustedIssuers.addAll( Arrays.asList(getAcceptedIssuers() ));
        
        // When accepting self-signed certificates, and the chain is a self-signed certificate, add it to the collection
        // of trusted issuers. Blindly accepting this issuer is undesirable, as that would circumvent other checks, such
        // as expiration checking.
        if ( acceptSelfSigned && chain.length == 1 )
        {
            Log.debug( "Attempting to accept the self-signed certificate of this chain of length one, as instructed by configuration." );

            final X509Certificate cert = chain[0];
            if ( cert.getSubjectDN().equals( cert.getIssuerDN() ) )
            {
                Log.debug( "Chain of one appears to be self-signed. Adding it to the set of trusted issuers." );
                trustedIssuers.add( cert );
            }
            else
            {
                Log.debug( "Chain of one is not self-signed. Not adding it to the set of trusted issuers." );
            }
        }

        // Turn trusted into accepted issuers.
        final Set<X509Certificate> acceptedIssuers;
        if ( checkValidity )
        {
            // See what certificates are currently valid.
            acceptedIssuers = CertificateUtils.filterValid( trustedIssuers );
        }
        else
        {
            acceptedIssuers = trustedIssuers;
        }

        Log.info("Using the following trust anchors for checking trust of the TLS connection for certificate " + endEntityCert.getSubjectDN());
        for (X509Certificate anchor : acceptedIssuers)
        	Log.info("\tDN=" + anchor.getIssuerDN());
        
        // Transform all accepted issuers into a set of unique trustAnchors.
        final Set<TrustAnchor> trustAnchors = CertificateUtils.toTrustAnchors( acceptedIssuers );

        // All certificates that are part of the (possibly incomplete) chain.
        final CertStore certificates = CertStore.getInstance( "Collection", new CollectionCertStoreParameters( Arrays.asList( chain ) ) );

        // Build the configuration for the path builder. It is based on the collection of accepted issuers / trust anchors
        final PKIXBuilderParameters parameters = new PKIXBuilderParameters( trustAnchors, selector );

        // Validity checks are enabled by default in the CertPathBuilder implementation.
        if ( !checkValidity )
        {
            Log.debug( "Attempting to ignore any validity (expiry) issues, as instructed by configuration." );

            // There is no way to configure the pathBuilder to ignore date validity. When validity checks are to be
            // ignored, try to find a point in time where all certificates in the chain are valid.
            final Date validPointInTime = CertificateUtils.findValidPointInTime( chain );

            // This strategy to 'disable' validity checks won't work if there's no overlap of validity periods of all
            // certificates. TODO improve the implementation.
            if ( validPointInTime == null )
            {
                Log.warn( "The existing implementation is unable to fully ignore certificate validity periods for this chain, even though it is configured to do so. Certificate checks might fail because of expiration for end entity: " + chain[0] );
            }
            else
            {
                parameters.setDate( validPointInTime );
            }
        }

        // Add all certificates that are part of the chain to the configuration. Together with the trust anchors, the
        // entire chain should now be in the store.
        parameters.addCertStore( certificates );

        // When true, validation will fail if no CRLs are provided!
        parameters.setRevocationEnabled( false );

        Log.debug( "Validating chain with {} certificates, using {} trust anchors.", chain.length, trustAnchors.size() );

        // Try to use BouncyCastle - if that doesn't work, pick one.
        CertPathBuilder pathBuilder;
        try
        {
            pathBuilder = CertPathBuilder.getInstance( "PKIX", "BC" );
        }
        catch ( NoSuchProviderException e )
        {
            Log.warn( "Unable to use the BC provider! Trying to use a fallback provider.", e );
            pathBuilder = CertPathBuilder.getInstance( "PKIX" );
        }

        try
        {
            // Finally, construct (and implicitly validate) the certificate path.
            final CertPathBuilderResult result = pathBuilder.build( parameters );
            return result.getCertPath();
        }
        catch ( CertPathBuilderException ex )
        {
            // This exception generally isn't very helpful. This block attempts to print more debug information.
            try
            {
                Log.debug( "** Accepted Issuers (trust anchors, \"root CA's\"):" );
                for ( X509Certificate acceptedIssuer : acceptedIssuers) {
                    Log.debug( "   - " + acceptedIssuer.getSubjectDN() + "/" + acceptedIssuer.getIssuerDN() );
                }
                Log.debug( "** Chain to be validated:" );
                Log.debug( "   length: " + chain.length );
                for (int i=0; i<chain.length; i++) {
                    Log.debug( " Certificate[{}] (valid from {} to {}):", i, chain[ i ].getNotBefore(), chain[ i ].getNotAfter() );
                    Log.debug( "   subjectDN: " + chain[ i ].getSubjectDN() );
                    Log.debug( "   issuerDN: " + chain[ i ].getIssuerDN() );

                    for ( X509Certificate acceptedIssuer : acceptedIssuers) {
                        if ( acceptedIssuer.getIssuerDN().equals( chain[i].getIssuerDN() ) ) {
                            Log.debug( "Found accepted issuer with same DN: " + acceptedIssuer.getIssuerDN() );
                        }
                    }
                }
            }
            finally
            {
                // rethrow the original exception.
                throw ex;
            }
        }

    }
}
