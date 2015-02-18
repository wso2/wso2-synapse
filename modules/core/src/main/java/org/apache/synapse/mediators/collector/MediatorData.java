package org.apache.synapse.mediators.collector;

import java.util.ArrayList;

import org.apache.axiom.util.UIDGenerator;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.ListMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.rest.RESTConstants;

/**
 * This class is used to create tree nodes and to add per mediator data to the nodes
 */
public class MediatorData {

	/**
	 * @param m Mediator that is supposed to be executed
	 * @return is the mediator an instance of the ListMediator or FlowContinuableMediator?
	 */
	public static boolean hasList(Mediator m) {
		if (m instanceof FlowContinuableMediator || m instanceof ListMediator)
			return true;
		else
			return false;
	}

	/**
	 * @param synCtx       MessageContext
	 * @param sequenceType SequenceType
	 * @param mediator     Mediator that is being processed at the moment
	 */
	public static void addNestedMediator(MessageContext synCtx,
	                                     SequenceType sequenceType, Mediator mediator) {
		TreeNode checkForRoot = synCtx.getCurrent();
		// If there is no current node or the SequenceType is PROXY_FAULTSEQ, API_FAULTSEQ, then it is the root
		//else it is a parent node of the existing tree
		if (checkForRoot == null || "".equals(checkForRoot)
		    || sequenceType.toString().contains("FAULT"))
			addToRoot(synCtx, sequenceType, mediator);
		else {
			createNewMediator(synCtx, mediator);
		}
	}

	/**
	 * This method will create a treenode and add it as the root of the tree
	 *
	 * @param synCtx       MessageContext
	 * @param sequenceType SequenceType
	 * @param mediator     Mediator that is being processed at the moment
	 * @return a reference to the Root Node of the tree
	 */
	public static TreeNode addToRoot(MessageContext synCtx,
	                                 SequenceType sequenceType, Mediator mediator) {
		TreeNode root;
		SuperMediator newMediator = new SuperMediator();
		ArrayList<TreeNode> children = new ArrayList<TreeNode>();

		newMediator.setMediatorName(sequenceType.toString());
		newMediator.setStartTime(System.currentTimeMillis());
		newMediator.setType("nested");
		newMediator.setEnvelop(synCtx.getEnvelope().getBody().toString());
		newMediator.setMsgID((String) synCtx.getProperty("CommonMessageID"));
		newMediator.setRootType(sequenceType.toString());
		newMediator.setId("0");
		root = new TreeNode(null, newMediator, children);
		synCtx.setProperty("Root", root);

		switch (sequenceType) {

			case PROXY_INSEQ: {
				/////////////////////////////////////////////////////////////
				//	A PROXY_FAULTSEQ doesnt create a new MessageContext. As the propert "Root" is altered
				//	it will lose the original reference of the PROXY_INSEQ. Hence to keep the old reference
				//	intact  "NonFaultRoot" property is used
				//////////////////////////////////////////////////////////////////
				synCtx.setProperty("NonFaultRoot", root);
				newMediator.setServiceName(synCtx.getProperty(
						SynapseConstants.PROXY_SERVICE).toString());
				if (test(synCtx)) {
					newMediator.setReq_resp("Request");
				}
				break;
			}
			case PROXY_OUTSEQ: {
				synCtx.setProperty("NonFaultRoot", root);
				newMediator.setServiceName(synCtx.getProperty(
						SynapseConstants.PROXY_SERVICE).toString());
				if (!test(synCtx)) {
					newMediator.setReq_resp("Response");
				}
				break;
			}
			case PROXY_FAULTSEQ: {
				newMediator.setServiceName(synCtx.getProperty(
						SynapseConstants.PROXY_SERVICE).toString());
				break;
			}
			case API_INSEQ: {
				newMediator.setServiceName(synCtx.getProperty(
						RESTConstants.SYNAPSE_REST_API).toString());
				if (test(synCtx)) {
					newMediator.setReq_resp("Request");
				}
				synCtx.setProperty("NonFaultRoot", root);
				break;
			}
			case API_OUTSEQ: {
				synCtx.setProperty("NonFaultRoot", root);
				newMediator.setServiceName(synCtx.getProperty(
						RESTConstants.SYNAPSE_REST_API).toString());
				break;
			}
			case API_FAULTSEQ: {
				newMediator.setServiceName(synCtx.getProperty(
						RESTConstants.SYNAPSE_REST_API).toString());
				if (!test(synCtx)) {
					newMediator.setReq_resp("Response");
				}
				break;
			}
			default: {
				SequenceMediator seqMediator = (SequenceMediator) mediator;
				newMediator.setMediatorName(seqMediator.getType() + " "
				                            + seqMediator.getName());

				newMediator.setRootType(seqMediator.getType() + " "
				                        + seqMediator.getName());

				synCtx.setProperty("NonFaultRoot", root);
				break;
			}
		}
		synCtx.setCurrent(root);
		return root;
	}

	/**
	 * This method will create a TreeNode that are not leaf nodes
	 *
	 * @param synCtx   current MessageContext
	 * @param mediator that is being processed at the moment
	 * @return a reference to the newly added TreeNode
	 */
	public static TreeNode createNewMediator(MessageContext synCtx,
	                                         Mediator mediator) {

		SuperMediator newMediator = new NestedMediator();
		ArrayList<TreeNode> children = new ArrayList<TreeNode>();
		//To set the name of the mediator as SequenceMediator a in a configuration as
		//<sequence name="a">
		if (mediator instanceof SequenceMediator)
			newMediator.setMediatorName(((SequenceMediator) mediator).getType()
			                            + " " + ((SequenceMediator) mediator).getName());
		else
			newMediator.setMediatorName(mediator.getType().toString());

		newMediator.setType("nested");
		newMediator.setStartTime(System.currentTimeMillis());
		newMediator.setEnvelop(synCtx.getEnvelope().getBody().toString());
		newMediator
				.setRootType(synCtx.getCurrent().getContents().getRootType());
		newMediator.setMsgID((String) synCtx.getProperty("CommonMessageID"));

		newMediator.setServiceName(synCtx.getCurrent().getContents()
		                                 .getServiceName());
		TreeNode nonroot = new TreeNode(synCtx.getCurrent(), newMediator, children);
		try {
			synCtx.getCurrent().addChildTreeNodeMediator(nonroot);
			newMediator.setId(synCtx.getCurrent().getContents().getId() + "-" +
			                  (synCtx.getCurrent().getChildren().size() - 1));
		} catch (Exception e) {

		}
		synCtx.setCurrent(nonroot);
		return synCtx.getCurrent();
	}

	/**
	 * To create and add a TreeNode to the tree as a leaf-node
	 *
	 * @param synCtx current MessageContext
	 * @param child  node which is added as a leafnode to the tree
	 */
	public static void createNewSingleMediator(MessageContext synCtx,
	                                           Mediator child) {
		SuperMediator newMediator = new SingleMediator();
		ArrayList<TreeNode> children = new ArrayList<TreeNode>();
		newMediator.setMediatorName(child.getType());
		newMediator.setStartTime(System.currentTimeMillis());
		newMediator.setMsgID((String) synCtx.getProperty("CommonMessageID"));
		newMediator.setType("single");
		newMediator.setEnvelop(synCtx.getEnvelope().getBody().toString());
		newMediator.setServiceName(synCtx.getCurrent().getContents()
		                                 .getServiceName());

		TreeNode nonroot = new TreeNode(synCtx.getCurrent(), newMediator, children); // adding the new
		// node to the tree
		try {
			synCtx.getCurrent().addChildTreeNodeMediator(nonroot);
			newMediator.setId(synCtx.getCurrent().getContents().getId() + "-" +
			                  (synCtx.getCurrent().getChildren().size() - 1));
		} catch (Exception e) {

		}
		newMediator
				.setRootType(synCtx.getCurrent().getContents().getRootType());
	}

	public static void setEndingTime(TreeNode node) {
		node.getContents().setEndTime(System.currentTimeMillis());
	}

	/**
	 * @param node that is the root of the tree structure
	 */
	public static void toTheList(TreeNode node) {
		MediatorData.setEndingTime(node);
		node.printChildren(node);
		node.printTree(node);
	}

	/**
	 * @param synCtx current MessageContext
	 * @return is the Message that is being processed a request?
	 */
	public static boolean test(MessageContext synCtx) {
		return !synCtx.isResponse();
	}
}
