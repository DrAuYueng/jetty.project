package org.eclipse.jetty.webapp;
//========================================================================
//Copyright (c) 2006-2012 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//All rights reserved. This program and the accompanying materials
//are made available under the terms of the Eclipse Public License v1.0
//and Apache License v2.0 which accompanies this distribution.
//The Eclipse Public License is available at 
//http://www.eclipse.org/legal/epl-v10.html
//The Apache License v2.0 is available at
//http://www.opensource.org/licenses/apache2.0.php
//You may elect to redistribute this code under either of these licenses. 
//========================================================================

public class AbstractConfiguration implements Configuration
{
    public void preConfigure(WebAppContext context) throws Exception
    {
    }

    public void configure(WebAppContext context) throws Exception
    {
    }

    public void postConfigure(WebAppContext context) throws Exception
    {
    }

    public void deconfigure(WebAppContext context) throws Exception
    {
    }

    public void destroy(WebAppContext context) throws Exception
    {
    }

    public void cloneConfigure(WebAppContext template, WebAppContext context) throws Exception
    {
    }
}
