package org.jivesoftware.openfire.trustanchor;


public class TrustAnchorAlreadyExistsException extends TrustAnchorException 
{
	private static final long serialVersionUID = 8247923837504152982L;

	public TrustAnchorAlreadyExistsException() 
    {
        super();
    }

    public TrustAnchorAlreadyExistsException(String msg) 
    {
        super(msg);
    }

    public TrustAnchorAlreadyExistsException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustAnchorAlreadyExistsException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}
