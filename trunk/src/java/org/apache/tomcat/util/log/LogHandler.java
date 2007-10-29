

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * glassfish/bootstrap/legal/CDDLv1.0.txt or
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Portions Copyright Apache Software Foundation.
 */ 
package org.apache.tomcat.util.log;

import org.apache.tomcat.util.log.*;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.StringWriter;

import java.util.*;


/**
 * Log destination ( or channel ). This is the base class that will be
 * extended by log handlers - tomcat uses util.qlog.QueueLogger,
 * in future we'll use log4j or java.util.logger adapters.
 *
 * The base class writes to a (default) writer, and it can
 * be used for very simple logging.
 * 
 * @author Anil Vijendran (akv@eng.sun.com)
 * @author Alex Chaffee (alex@jguru.com)
 * @author Ignacio J. Ortega (nacho@siapi.es)
 * @author Costin Manolache
 */
public  class LogHandler {

    protected PrintWriter sink = defaultSink;
    protected int level = Log.INFORMATION;

    
    /**
     * Prints log message and stack trace.
     * This method should be overriden by real logger implementations
     *
     * @param	prefix		optional prefix. 
     * @param	message		the message to log. 
     * @param	t		the exception that was thrown.
     * @param	verbosityLevel	what type of message is this?
     * 				(WARNING/DEBUG/INFO etc)
     */
    public void log(String prefix, String msg, Throwable t,
		    int verbosityLevel)
    {
	if( sink==null ) return;
	// default implementation ( in case no real logging is set up  )
	if( verbosityLevel > this.level ) return;
	
	if (prefix != null) 
	    sink.println(prefix + ": " + msg );
	else 
	    sink.println(  msg );
	
	if( t!=null )
	    t.printStackTrace( sink );
    }

    /**
     * Flush the log. 
     */
    public void flush() {
	if( sink!=null)
	    sink.flush();
    }


    /**
     * Close the log. 
     */
    public synchronized void close() {
	this.sink = null;
    }
    
    /**
     * Set the verbosity level for this logger. This controls how the
     * logs will be filtered. 
     *
     * @param	level		one of the verbosity level codes. 
     */
    public void setLevel(int level) {
	this.level = level;
    }
    
    /**
     * Get the current verbosity level.
     */
    public int getLevel() {
	return this.level;
    }


    // -------------------- Default sink
    
    protected static PrintWriter defaultSink =
	new PrintWriter( new OutputStreamWriter(System.err), true);

    /**
     * Set the default output stream that is used by all logging
     * channels.
     *
     * @param	w		the default output stream.
     */
    public static void setDefaultSink(Writer w) {
	if( w instanceof PrintWriter )
	    defaultSink=(PrintWriter)w;
	else 
	    defaultSink = new PrintWriter(w);
    }

    // -------------------- General purpose utilitiy --------------------

    

}
