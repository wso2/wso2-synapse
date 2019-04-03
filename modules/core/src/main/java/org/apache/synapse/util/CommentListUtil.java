package org.apache.synapse.util;

import org.apache.axiom.om.*;

import java.util.Iterator;
import java.util.List;

/**
 * Contains the utils that are used to handle comments in synapse
 *
 */
public class CommentListUtil {

    /**
     * Adds comments in Synapse to a list of comments
     *
     * @param el    OMElement containing the comments
     * @param commentList List to be which the comments should be added to
     */
    public static void addAllCommentChildrenToList(OMElement el, List<String> commentList) {
        Iterator it = el.getChildren();

        while (it.hasNext()) {
            OMNode child = (OMNode) it.next();
            if (child instanceof OMComment && ((OMComment) child).getValue() != null) {
                commentList.add(((OMComment) child).getValue());
            }
        }
    }

    /**
     * Serialize string comment entries from a List
     *
     * @param parent      OMElement to be updated
     * @param commentList List of comment entries to be serialized
     */
    public static void serializeComments(OMElement parent, List<String> commentList) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        for (String comment : commentList) {
            OMComment commendNode = fac.createOMComment(parent, "comment");
            commendNode.setValue(comment);
            parent.addChild(commendNode);
        }
    }
}
