package org.jivesoftware.openfire.certificate;



public enum CertificateStatus
{
    /**
     * Indicates that the certificate has been revoked by the CA that issued it.
     */
    REVOKED(1),
    /**
     * Indicates that the certificate is valid.
     */
    GOOD(0),
    /**
     * Indicates that the certificate's status could not be retrieved. For safety,
     * {@code UNKNOWN} certificates should generally be treated as {@link #REVOKED}, though for
     * diagnostic purposes it may be appropriate to distinguish the two cases.
     */
    UNKNOWN(2);
    
	private final int code;

    private CertificateStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
    
    public static CertificateStatus fromCode(int code)
    {
    	switch (code) 
    	{
		    case 0:
			    return GOOD; 	
    		case 1:
    			return REVOKED;
    	    default:
    	    	return UNKNOWN;
    	}
    		
    }
}
