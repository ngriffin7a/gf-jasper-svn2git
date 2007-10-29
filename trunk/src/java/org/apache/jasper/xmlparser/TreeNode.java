

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
package org.apache.jasper.xmlparser;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Simplified implementation of a Node from a Document Object Model (DOM)
 * parse of an XML document.  This class is used to represent a DOM tree
 * so that the XML parser's implementation of <code>org.w3c.dom</code> need
 * not be visible to the remainder of Jasper.
 * <p>
 * <strong>WARNING</strong> - Construction of a new tree, or modifications
 * to an existing one, are not thread-safe and such accesses must be
 * synchronized.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.1.1.1 $ $Date: 2005/05/27 22:55:14 $
 */

public class TreeNode {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new node with no parent.
     *
     * @param name The name of this node
     */
    public TreeNode(String name) {

        this(name, null);

    }


    /**
     * Construct a new node with the specified parent.
     *
     * @param name The name of this node
     * @param parent The node that is the parent of this node
     */
    public TreeNode(String name, TreeNode parent) {

        super();
        this.name = name;
        this.parent = parent;
        if (this.parent != null)
            this.parent.addChild(this);

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The attributes of this node, keyed by attribute name,
     * Instantiated only if required.
     */
    protected HashMap attributes = null;


    /**
     * The body text associated with this node (if any).
     */
    protected String body = null;


    /**
     * The children of this node, instantiated only if required.
     */
    protected ArrayList children = null;


    /**
     * The name of this node.
     */
    protected String name = null;


    /**
     * The parent node of this node.
     */
    protected TreeNode parent = null;


    // --------------------------------------------------------- Public Methods


    /**
     * Add an attribute to this node, replacing any existing attribute
     * with the same name.
     *
     * @param name The attribute name to add
     * @param value The new attribute value
     */
    public void addAttribute(String name, String value) {

        if (attributes == null)
            attributes = new HashMap();
        attributes.put(name, value);

    }


    /**
     * Add a new child node to this node.
     *
     * @param node The new child node
     */
    public void addChild(TreeNode node) {

        if (children == null)
            children = new ArrayList();
        children.add(node);

    }


    /**
     * Return the value of the specified node attribute if it exists, or
     * <code>null</code> otherwise.
     *
     * @param name Name of the requested attribute
     */
    public String findAttribute(String name) {

        if (attributes == null)
            return (null);
        else
            return ((String) attributes.get(name));

    }


    /**
     * Return an Iterator of the attribute names of this node.  If there are
     * no attributes, an empty Iterator is returned.
     */
    public Iterator findAttributes() {

        if (attributes == null)
            return (Collections.EMPTY_LIST.iterator());
        else
            return (attributes.keySet().iterator());

    }


    /**
     * Return the first child node of this node with the specified name,
     * if there is one; otherwise, return <code>null</code>.
     *
     * @param name Name of the desired child element
     */
    public TreeNode findChild(String name) {

        if (children == null)
            return (null);
        Iterator items = children.iterator();
        while (items.hasNext()) {
            TreeNode item = (TreeNode) items.next();
            if (name.equals(item.getName()))
                return (item);
        }
        return (null);

    }


    /**
     * Return an Iterator of all children of this node.  If there are no
     * children, an empty Iterator is returned.
     */
    public Iterator findChildren() {

        if (children == null)
            return (Collections.EMPTY_LIST.iterator());
        else
            return (children.iterator());

    }


    /**
     * Return an Iterator over all children of this node that have the
     * specified name.  If there are no such children, an empty Iterator
     * is returned.
     *
     * @param name Name used to select children
     */
    public Iterator findChildren(String name) {

        if (children == null)
            return (Collections.EMPTY_LIST.iterator());

        ArrayList results = new ArrayList();
        Iterator items = children.iterator();
        while (items.hasNext()) {
            TreeNode item = (TreeNode) items.next();
            if (name.equals(item.getName()))
                results.add(item);
        }
        return (results.iterator());

    }


    /**
     * Return the body text associated with this node (if any).
     */
    public String getBody() {

        return (this.body);

    }


    /**
     * Return the name of this node.
     */
    public String getName() {

        return (this.name);

    }


    /**
     * Remove any existing value for the specified attribute name.
     *
     * @param name The attribute name to remove
     */
    public void removeAttribute(String name) {

        if (attributes != null)
            attributes.remove(name);

    }


    /**
     * Remove a child node from this node, if it is one.
     *
     * @param node The child node to remove
     */
    public void removeNode(TreeNode node) {

        if (children != null)
            children.remove(node);

    }


    /**
     * Set the body text associated with this node (if any).
     *
     * @param body The body text (if any)
     */
    public void setBody(String body) {

        this.body = body;

    }


    /**
     * Return a String representation of this TreeNode.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        toString(sb, 0, this);
        return (sb.toString());

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Append to the specified StringBuffer a character representation of
     * this node, with the specified amount of indentation.
     *
     * @param sb The StringBuffer to append to
     * @param indent Number of characters of indentation
     * @param node The TreeNode to be printed
     */
    protected void toString(StringBuffer sb, int indent,
                            TreeNode node) {

        int indent2 = indent + 2;

        // Reconstruct an opening node
        for (int i = 0; i < indent; i++)
            sb.append(' ');
        sb.append('<');
        sb.append(node.getName());
        Iterator names = node.findAttributes();
        while (names.hasNext()) {
            sb.append(' ');
            String name = (String) names.next();
            sb.append(name);
            sb.append("=\"");
            String value = node.findAttribute(name);
            sb.append(value);
            sb.append("\"");
        }
        sb.append(">\n");

        // Reconstruct the body text of this node (if any)
        String body = node.getBody();
        if ((body != null) && (body.length() > 0)) {
            for (int i = 0; i < indent2; i++)
                sb.append(' ');
            sb.append(body);
            sb.append("\n");
        }

        // Reconstruct child nodes with extra indentation
        Iterator children = node.findChildren();
        while (children.hasNext()) {
            TreeNode child = (TreeNode) children.next();
            toString(sb, indent2, child);
        }

        // Reconstruct a closing node marker
        for (int i = 0; i < indent; i++)
            sb.append(' ');
        sb.append("</");
        sb.append(node.getName());
        sb.append(">\n");

    }


}
