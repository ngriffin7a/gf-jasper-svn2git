

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


package org.apache.catalina.ant;


import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.apache.catalina.util.Base64;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;


/**
 * Abstract base class for Ant tasks that interact with the
 * <em>Manager</em> web application for dynamically deploying and
 * undeploying applications.  These tasks require Ant 1.4 or later.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.1.1.1 $ $Date: 2005/05/27 22:55:01 $
 * @since 4.1
 */

public abstract class AbstractCatalinaTask extends Task {


    // ----------------------------------------------------- Instance Variables


    // ------------------------------------------------------------- Properties


    /**
     * The login password for the <code>Manager</code> application.
     */
    protected String password = null;

    public String getPassword() {
        return (this.password);
    }

    public void setPassword(String password) {
        this.password = password;
    }


    /**
     * The URL of the <code>Manager</code> application to be used.
     */
    protected String url = "http://localhost:8080/manager";

    public String getUrl() {
        return (this.url);
    }

    public void setUrl(String url) {
        this.url = url;
    }


    /**
     * The login username for the <code>Manager</code> application.
     */
    protected String username = null;

    public String getUsername() {
        return (this.username);
    }

    public void setUsername(String username) {
        this.username = username;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Execute the specified command.  This logic only performs the common
     * attribute validation required by all subclasses; it does not perform
     * any functional logic directly.
     *
     * @exception BuildException if a validation error occurs
     */
    public void execute() throws BuildException {

        if ((username == null) || (password == null) || (url == null)) {
            throw new BuildException
                ("Must specify all of 'username', 'password', and 'url'");
        }

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Execute the specified command, based on the configured properties.
     *
     * @param command Command to be executed
     *
     * @exception BuildException if an error occurs
     */
    public void execute(String command) throws BuildException {

        execute(command, null, null, -1);

    }


    /**
     * Execute the specified command, based on the configured properties.
     * The input stream will be closed upon completion of this task, whether
     * it was executed successfully or not.
     *
     * @param command Command to be executed
     * @param istream InputStream to include in an HTTP PUT, if any
     * @param contentType Content type to specify for the input, if any
     * @param contentLength Content length to specify for the input, if any
     *
     * @exception BuildException if an error occurs
     */
    public void execute(String command, InputStream istream,
                        String contentType, int contentLength)
        throws BuildException {

        URLConnection conn = null;
        InputStreamReader reader = null;
        try {

            // Create a connection for this command
            conn = (new URL(url + command)).openConnection();
            HttpURLConnection hconn = (HttpURLConnection) conn;

            // Set up standard connection characteristics
            hconn.setAllowUserInteraction(false);
            hconn.setDoInput(true);
            hconn.setUseCaches(false);
            if (istream != null) {
                hconn.setDoOutput(true);
                hconn.setRequestMethod("PUT");
                if (contentType != null) {
                    hconn.setRequestProperty("Content-Type", contentType);
                }
                if (contentLength >= 0) {
                    hconn.setRequestProperty("Content-Length",
                                             "" + contentLength);
                }
            } else {
                hconn.setDoOutput(false);
                hconn.setRequestMethod("GET");
            }
            hconn.setRequestProperty("User-Agent",
                                     "Catalina-Ant-Task/1.0");

            // Set up an authorization header with our credentials
            String input = username + ":" + password;
            String output = new String(Base64.encode(input.getBytes()));
            hconn.setRequestProperty("Authorization",
                                     "Basic " + output);

            // Establish the connection with the server
            hconn.connect();

            // Send the request data (if any)
            if (istream != null) {
                BufferedOutputStream ostream =
                    new BufferedOutputStream(hconn.getOutputStream(), 1024);
                byte buffer[] = new byte[1024];
                while (true) {
                    int n = istream.read(buffer);
                    if (n < 0) {
                        break;
                    }
                    ostream.write(buffer, 0, n);
                }
                ostream.flush();
                ostream.close();
                istream.close();
            }

            // Process the response message
            reader = new InputStreamReader(hconn.getInputStream());
            StringBuffer buff = new StringBuffer();
            String error = null;
            boolean first = true;
            while (true) {
                int ch = reader.read();
                if (ch < 0) {
                    break;
                } else if ((ch == '\r') || (ch == '\n')) {
                    String line = buff.toString();
                    buff.setLength(0);
                    log(line, Project.MSG_INFO);
                    if (first) {
                        if (!line.startsWith("OK -")) {
                            error = line;
                        }
                        first = false;
                    }
                } else {
                    buff.append((char) ch);
                }
            }
            if (buff.length() > 0) {
                log(buff.toString(), Project.MSG_INFO);
            }
            if (error != null) {
                throw new BuildException(error);
            }

        } catch (Throwable t) {
            throw new BuildException(t);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable u) {
                    ;
                }
                reader = null;
            }
            if (istream != null) {
                try {
                    istream.close();
                } catch (Throwable u) {
                    ;
                }
                istream = null;
            }
        }

    }


}
