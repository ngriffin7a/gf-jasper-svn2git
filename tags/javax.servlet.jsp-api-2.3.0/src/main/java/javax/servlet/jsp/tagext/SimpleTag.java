/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.servlet.jsp.tagext;

import javax.servlet.jsp.JspContext;

/**
 * Interface for defining Simple Tag Handlers.
 * 
 * <p>Simple Tag Handlers differ from Classic Tag Handlers in that instead 
 * of supporting <code>doStartTag()</code> and <code>doEndTag()</code>, 
 * the <code>SimpleTag</code> interface provides a simple 
 * <code>doTag()</code> method, which is called once and only once for any 
 * given tag invocation.  All tag logic, iteration, body evaluations, etc. 
 * are to be performed in this single method.  Thus, simple tag handlers 
 * have the equivalent power of <code>BodyTag</code>, but with a much 
 * simpler lifecycle and interface.</p>
 *
 * <p>To support body content, the <code>setJspBody()</code> 
 * method is provided.  The container invokes the <code>setJspBody()</code> 
 * method with a <code>JspFragment</code> object encapsulating the body of 
 * the tag.  The tag handler implementation can call 
 * <code>invoke()</code> on that fragment to evaluate the body as
 * many times as it needs.</p>
 *
 * <p>A SimpleTag handler must have a public no-args constructor.  Most
 * SimpleTag handlers should extend SimpleTagSupport.</p>
 * 
 * <p><b>Lifecycle</b></p>
 *
 * <p>The following is a non-normative, brief overview of the 
 * SimpleTag lifecycle.  Refer to the JSP Specification for details.</p>
 *
 * <ol>
 *   <li>A new tag handler instance is created each time by the container 
 *       by calling the provided zero-args constructor.  Unlike classic
 *       tag handlers, simple tag handlers are never cached and reused by
 *       the JSP container.</li>
 *   <li>The <code>setJspContext()</code> and <code>setParent()</code> 
 *       methods are called by the container.  The <code>setParent()</code>
 *       method is only called if the element is nested within another tag 
 *       invocation.</li>
 *   <li>The setters for each attribute defined for this tag are called
 *       by the container.</li>
 *   <li>If a body exists, the <code>setJspBody()</code> method is called 
 *       by the container to set the body of this tag, as a 
 *       <code>JspFragment</code>.  If the action element is empty in
 *       the page, this method is not called at all.</li>
 *   <li>The <code>doTag()</code> method is called by the container.  All
 *       tag logic, iteration, body evaluations, etc. occur in this 
 *       method.</li>
 *   <li>The <code>doTag()</code> method returns and all variables are
 *       synchronized.</li>
 * </ol>
 * 
 * @see SimpleTagSupport
 * @since JSP 2.0
 */
public interface SimpleTag extends JspTag {
    
    /** 
     * Called by the container to invoke this tag.
     * The implementation of this method is provided by the tag library
     * developer, and handles all tag processing, body iteration, etc.
     *
     * <p>
     * The JSP container will resynchronize any AT_BEGIN and AT_END
     * variables (defined by the associated tag file, TagExtraInfo, or TLD)
     * after the invocation of doTag().
     * 
     * @throws javax.servlet.jsp.JspException If an error occurred 
     *     while processing this tag.
     * @throws javax.servlet.jsp.SkipPageException If the page that
     *     (either directly or indirectly) invoked this tag is to
     *     cease evaluation.  A Simple Tag Handler generated from a 
     *     tag file must throw this exception if an invoked Classic 
     *     Tag Handler returned SKIP_PAGE or if an invoked Simple
     *     Tag Handler threw SkipPageException or if an invoked Jsp Fragment
     *     threw a SkipPageException.
     * @throws java.io.IOException If there was an error writing to the
     *     output stream.
     */ 
    public void doTag() 
        throws javax.servlet.jsp.JspException, java.io.IOException;
    
    /**
     * Sets the parent of this tag, for collaboration purposes.
     * <p>
     * The container invokes this method only if this tag invocation is 
     * nested within another tag invocation.
     *
     * @param parent the tag that encloses this tag
     */
    public void setParent( JspTag parent );
    
    /**
     * Returns the parent of this tag, for collaboration purposes.
     *
     * @return the parent of this tag
     */ 
    public JspTag getParent();
    
    /**
     * Called by the container to provide this tag handler with
     * the <code>JspContext</code> for this invocation.
     * An implementation should save this value.
     * 
     * @param pc the page context for this invocation
     * @see Tag#setPageContext
     */
    public void setJspContext( JspContext pc );
                
    /** 
     * Provides the body of this tag as a JspFragment object, able to be 
     * invoked zero or more times by the tag handler. 
     * <p>
     * This method is invoked by the JSP page implementation 
     * object prior to <code>doTag()</code>.  If the action element is
     * empty in the page, this method is not called at all.
     * 
     * @param jspBody The fragment encapsulating the body of this tag.
     */ 
    public void setJspBody( JspFragment jspBody );

    
}
