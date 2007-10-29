

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

package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

/**
 * Collect info about the page and nodes, and make them availabe through
 * the PageInfo object.
 *
 * @author Kin-man Chung
 * @author Mark Roth
 */

class Collector {

    /**
     * A visitor for collecting information on the page and the body of
     * the custom tags.
     */
    static class CollectVisitor extends Node.Visitor {

        private int maxTagNesting = 0;
        private int curTagNesting = 0;
	private boolean scriptingElementSeen = false;
	private boolean usebeanSeen = false;
	private boolean includeActionSeen = false;
	private boolean paramActionSeen = false;
	private boolean setPropertySeen = false;
	private boolean hasScriptingVars = false;

	public void visit(Node.ParamAction n) throws JasperException {
	    if (n.getValue().isExpression()) {
		scriptingElementSeen = true;
	    }
	    paramActionSeen = true;
	}

	public void visit(Node.IncludeAction n) throws JasperException {
	    if (n.getPage().isExpression()) {
		scriptingElementSeen = true;
	    }
	    includeActionSeen = true;
            visitBody(n);
	}

	public void visit(Node.ForwardAction n) throws JasperException {
	    if (n.getPage().isExpression()) {
		scriptingElementSeen = true;
	    }
            visitBody(n);
	}

	public void visit(Node.SetProperty n) throws JasperException {
	    if (n.getValue() != null && n.getValue().isExpression()) {
		scriptingElementSeen = true;
	    }
	    setPropertySeen = true;
	}

	public void visit(Node.UseBean n) throws JasperException {
	    if (n.getBeanName() != null && n.getBeanName().isExpression()) {
		scriptingElementSeen = true;
	    }
	    usebeanSeen = true;
            visitBody(n);
	}

	public void visit(Node.PlugIn n) throws JasperException {
	    if (n.getHeight() != null && n.getHeight().isExpression()) {
		scriptingElementSeen = true;
	    }
	    if (n.getWidth() != null && n.getWidth().isExpression()) {
		scriptingElementSeen = true;
	    }
            visitBody(n);
	}

        public void visit(Node.CustomTag n) throws JasperException {

            curTagNesting++;
            if (curTagNesting > maxTagNesting) {
                maxTagNesting = curTagNesting;
            }
            
            // Check to see what kinds of element we see as child elements
            checkSeen( n.getChildInfo(), n );

            curTagNesting--;
        }

        /**
         * Check all child nodes for various elements and update the given
         * ChildInfo object accordingly.  Visits body in the process.
         */
        private void checkSeen( Node.ChildInfo ci, Node n ) 
            throws JasperException
        {
	    // save values collected so far
	    boolean scriptingElementSeenSave = scriptingElementSeen;
	    scriptingElementSeen = false;
	    boolean usebeanSeenSave = usebeanSeen;
	    usebeanSeen = false;
	    boolean includeActionSeenSave = includeActionSeen;
	    includeActionSeen = false;
	    boolean paramActionSeenSave = paramActionSeen;
	    paramActionSeen = false;
	    boolean setPropertySeenSave = setPropertySeen;
	    setPropertySeen = false;
	    boolean hasScriptingVarsSave = hasScriptingVars;
	    hasScriptingVars = false;

	    // Scan attribute list for expressions
            if( n instanceof Node.CustomTag ) {
                Node.CustomTag ct = (Node.CustomTag)n;
                Node.JspAttribute[] attrs = ct.getJspAttributes();
                for (int i = 0; attrs != null && i < attrs.length; i++) {
                    if (attrs[i].isExpression()) {
                        scriptingElementSeen = true;
                        break;
                    }
                }
            }

            visitBody(n);

            if( (n instanceof Node.CustomTag) && !hasScriptingVars) {
                Node.CustomTag ct = (Node.CustomTag)n;
		hasScriptingVars = ct.getVariableInfos().length > 0 ||
		    ct.getTagVariableInfos().length > 0;
	    }

	    // Record if the tag element and its body contains any scriptlet.
	    ci.setScriptless(! scriptingElementSeen);
	    ci.setHasUseBean(usebeanSeen);
	    ci.setHasIncludeAction(includeActionSeen);
	    ci.setHasParamAction(paramActionSeen);
	    ci.setHasSetProperty(setPropertySeen);
	    ci.setHasScriptingVars(hasScriptingVars);

	    // Propagate value of scriptingElementSeen up.
	    scriptingElementSeen = scriptingElementSeen || scriptingElementSeenSave;
	    usebeanSeen = usebeanSeen || usebeanSeenSave;
	    setPropertySeen = setPropertySeen || setPropertySeenSave;
	    includeActionSeen = includeActionSeen || includeActionSeenSave;
	    paramActionSeen = paramActionSeen || paramActionSeenSave;
	    hasScriptingVars = hasScriptingVars || hasScriptingVarsSave;
        }

 	public void visit(Node.JspElement n) throws JasperException {
 	    if (n.getNameAttribute().isExpression())
 		scriptingElementSeen = true;
 
 	    Node.JspAttribute[] attrs = n.getJspAttributes();
 	    for (int i = 0; i < attrs.length; i++) {
 		if (attrs[i].isExpression()) {
 		    scriptingElementSeen = true;
 		    break;
 		}
 	    }
 	    visitBody(n);
 	}

        public void visit(Node.JspBody n) throws JasperException {
            checkSeen( n.getChildInfo(), n );
        }
        
        public void visit(Node.NamedAttribute n) throws JasperException {
            checkSeen( n.getChildInfo(), n );
        }
        
	public void visit(Node.Declaration n) throws JasperException {
	    scriptingElementSeen = true;
	}

	public void visit(Node.Expression n) throws JasperException {
	    scriptingElementSeen = true;
	}

	public void visit(Node.Scriptlet n) throws JasperException {
	    scriptingElementSeen = true;
	}

        public void updatePageInfo(PageInfo pageInfo) {
            pageInfo.setMaxTagNesting(maxTagNesting);
	    pageInfo.setScriptless(! scriptingElementSeen);
        }
    }

    public static void collect(Compiler compiler, Node.Nodes page)
		throws JasperException {

	CollectVisitor collectVisitor = new CollectVisitor();
        page.visit(collectVisitor);
        collectVisitor.updatePageInfo(compiler.getPageInfo());

    }
}

