package org.jivesoftware.openfire.certificate;

import java.io.PrintStream;
import java.io.PrintWriter;

public class CertificateException extends Exception 
{

	private static final long serialVersionUID = -7537103622041683721L;

	protected Throwable nestedThrowable = null;

    public CertificateException() 
    {
        super();
    }

    public CertificateException(String msg) 
    {
        super(msg);
    }

    public CertificateException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public CertificateException(String msg, Throwable nestedThrowable) 
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
