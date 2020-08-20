package org.jivesoftware.openfire.keystore;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.jivesoftware.openfire.trustbundle.TrustBundleAnchor;
import org.jivesoftware.openfire.trustcircle.TrustCircle;
import org.jivesoftware.openfire.trustcircle.TrustCircleManager;
import org.jivesoftware.util.CertificateManager;
import org.jivesoftware.util.crl.impl.CRLRevocationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.cert.*;
import java.util.*;

/**
 * A wrapper class for a store of certificates, its metadata (password, location) and related functionality that is
 * used to <em>verify</em> credentials, a <em>trust store</em>
 *
 * The trust store should only contain certificates for the "most-trusted" Certificate Authorities (the store should not
 * contain Intermediates"). These certificates are referred to as "Trust Anchors".
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class TrustStore extends CertificateStore
{
    private static final Logger Log = LoggerFactory.getLogger( TrustStore.class );

    public TrustStore( CertificateStoreConfiguration configuration, boolean createIfAbsent ) throws CertificateStoreConfigException
    {
        super( configuration, createIfAbsent );
    }

    /**
     * Imports one certificate as a trust anchor into this store.
     *
     * Note that this method explicitly allows one to add invalid certificates.
     *
     * As this store is intended to contain certificates for "most-trusted" / root Certificate Authorities, this method
     * will fail when the PEM representation contains more than one certificate.
     *
     * @param alias the name (key) under which the certificate is to be stored in the store (cannot be null or empty).
     * @param pemRepresentation The PEM representation of the certificate to add (cannot be null or empty).
     * @throws CertificateStoreConfigException if a single certificate could not be found
     */
    public void installCertificate( String alias, String pemRepresentation ) throws CertificateStoreConfigException
    {
        // Input validation
        if ( alias == null || alias.trim().isEmpty() )
        {
            throw new IllegalArgumentException( "Argument 'alias' cannot be null or an empty String." );
        }
        if ( pemRepresentation == null )
        {
            throw new IllegalArgumentException( "Argument 'pemRepresentation' cannot be null." );
        }
        alias = alias.trim();

        // Check that there is a certificate for the specified alias
        try
        {
            if ( store.containsAlias( alias ) )
            {
                throw new CertificateStoreConfigException( "Certificate already exists for alias: " + alias );
            }

            // From their PEM representation, parse the certificates.
            final Collection<X509Certificate> certificates = CertificateManager.parseCertificates( pemRepresentation );

            if ( certificates.isEmpty() ) {
                throw new CertificateStoreConfigException( "No certificate was found in the input.");
            }
            if ( certificates.size() != 1 ) {
                throw new CertificateStoreConfigException( "More than one certificate was found in the input." );
            }

            final X509Certificate certificate = certificates.iterator().next();

            store.setCertificateEntry(alias, certificate);
            persist();
        }
        catch ( CertificateException | KeyStoreException | IOException e )
        {
            throw new CertificateStoreConfigException( "Unable to install a certificate into a trust store.", e );
        }
        finally
        {
            reload(); // re-initialize store.
        }
    }

    /**
     * Decide whether or not to trust the given supplied certificate chain. For certain failures, we SHOULD generate
     * an exception - revocations and the like, but we currently do not.
     *
     * @param chain an array of X509Certificate where the first one is the endEntityCertificate.
     * @return true if the content of this trust store allows the chain to be trusted, otherwise false.
     */
    public boolean isTrusted( Certificate chain[] , String localDomain)
    {
        return getEndEntityCertificate( chain , localDomain) != null;
    }

    /**
     * Decide whether or not to trust the given supplied certificate chain, returning the
     * End Entity Certificate in this case where it can, and null otherwise.
     * A self-signed certificate will, for example, return null.
     * For certain failures, we SHOULD generate an exception - revocations and the like,
     * but we currently do not.
     *
     * @param chain an array of X509Certificate where the first one is the endEntityCertificate.
     * @return trusted end-entity certificate, or null.
     */
    public X509Certificate getEndEntityCertificate( Certificate chain[] , String localDomain)
    {
        if ( chain == null || chain.length == 0 )
        {
            return null;
        }

        final X509Certificate first = (X509Certificate) chain[ 0 ];
        try
        {
            first.checkValidity();
        }
        catch ( CertificateException e )
        {
            Log.warn( "EE Certificate not valid: " + e.getMessage() );
            return null;
        }

    	final CRLRevocationManager revManager = CRLRevocationManager.getInstance();
    	
    	if (revManager.isRevoked(first))
    	{
    		Log.warn( "TLS end enity certificate has been marked as revoked.  The connection is rejected." );
    		return null;
    				
    	}

        final List<X509Certificate> allCerts = new ArrayList<>();
        try
        {
        	final Collection<TrustCircle> circles;
        	
        	if (StringUtils.isEmpty(localDomain))
        		circles = TrustCircleManager.getInstance().getCirclesByDomain(localDomain, true, true);
        	else
        		circles = TrustCircleManager.getInstance().getTrustCircles(true, true);
        	
        	if (circles == null || circles.isEmpty())
        		return null;
        	
        	for (TrustCircle circle : circles)
        	{
        		for (TrustBundle bundle : circle.getTrustBundles())
        			for (TrustBundleAnchor anchor : bundle.getTrustBundleAnchors())
        				allCerts.add(anchor.asX509Certificate());
        		
        		for (org.jivesoftware.openfire.trustanchor.TrustAnchor anchor : circle.getAnchors())
        			allCerts.add(anchor.asX509Certificate());
        	}

            final Set<X509Certificate> trustedIssuers = new HashSet<>();
            trustedIssuers.addAll( allCerts);
            
            final Set<X509Certificate> acceptedIssuers = CertificateUtils.filterValid( trustedIssuers );

            // Transform all accepted issuers into a set of unique trustAnchors.
            final Set<TrustAnchor> trustAnchors = CertificateUtils.toTrustAnchors( acceptedIssuers );
            
            // All certificates that are part of the (possibly incomplete) chain.
            final CertStore certificates = CertStore.getInstance( "Collection", new CollectionCertStoreParameters( Arrays.asList( first ) ) );
        	
            // Build the configuration for the path builder. It is based on the collection of accepted issuers / trust anchors
            final X509CertSelector selector = new X509CertSelector();
            selector.setCertificate( first );
            final PKIXBuilderParameters parameters = new PKIXBuilderParameters( trustAnchors, selector );
            
            // Add all certificates that are part of the chain to the configuration. Together with the trust anchors, the
            // entire chain should now be in the store.
            parameters.addCertStore( certificates );

            // When true, validation will fail if no CRLs are provided!
            parameters.setRevocationEnabled( false );
            
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
            

            // Finally, construct (and implicitly validate) the certificate path.
            final CertPathBuilderResult result = pathBuilder.build( parameters );
            
            return (X509Certificate) result.getCertPath().getCertificates().get( 0 );
        }
        catch ( CertPathBuilderException e )
        {
            Log.warn( "Path builder exception while validating certificate chain:", e );
        }
        catch ( Exception e )
        {
            Log.warn( "Unknown exception while validating certificate chain:", e );
        }
        return null;
    }

}
