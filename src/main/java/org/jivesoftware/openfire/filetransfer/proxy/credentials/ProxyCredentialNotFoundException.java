package org.jivesoftware.openfire.filetransfer.proxy.credentials;

import java.io.PrintStream;
import java.io.PrintWriter;

public class ProxyCredentialNotFoundException extends Exception
{
	private static final long serialVersionUID = 1701002958075255822L;

	private Throwable nestedThrowable = null;

    public ProxyCredentialNotFoundException() 
    {
        super();
    }

    public ProxyCredentialNotFoundException(String msg) 
    {
        super(msg);
    }

    public ProxyCredentialNotFoundException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public ProxyCredentialNotFoundException(String msg, Throwable nestedThrowable) 
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
