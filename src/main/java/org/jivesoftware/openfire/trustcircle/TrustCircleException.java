package org.jivesoftware.openfire.trustcircle;

import java.io.PrintStream;
import java.io.PrintWriter;

public class TrustCircleException extends Exception 
{
	private static final long serialVersionUID = 4882350409994801126L;

	protected Throwable nestedThrowable = null;

    public TrustCircleException() 
    {
        super();
    }

    public TrustCircleException(String msg) 
    {
        super(msg);
    }

    public TrustCircleException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustCircleException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }

    @Override
    public void printStackTrace() 
    {
        super.printStackTrace();
        if (nestedThrowable != null) 
            nestedThrowable.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream ps) 
    {
        super.printStackTrace(ps);
        if (nestedThrowable != null) 
            nestedThrowable.printStackTrace(ps);
    }

    @Override
    public void printStackTrace(PrintWriter pw) 
    {
        super.printStackTrace(pw);
        if (nestedThrowable != null)
            nestedThrowable.printStackTrace(pw);
    }
}
