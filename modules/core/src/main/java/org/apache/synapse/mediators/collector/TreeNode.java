package org.apache.synapse.mediators.collector;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.synapse.SynapseLog;

public class TreeNode implements Cloneable {

	private TreeNode parent;
	SuperMediator contents;
	private ArrayList<TreeNode> children;

	private int depth = 0;

	public TreeNode(TreeNode parent, SuperMediator contents,
			ArrayList<TreeNode> children) {
		this.parent = parent;
		this.contents = contents;
		this.children = children;
	}

	public TreeNode(SuperMediator contents, ArrayList<TreeNode> children) {
		this.contents = contents;
		this.children = children;
	}

	/**
	 * This method will create a deepcopy of the existing tree which can be used
	 * while cloning or iterating
	 * 
	 * @param node
	 *            that is subjected for cloning
	 * @param messageID
	 *            of the new Tree
	 * @param parent
	 *            of the node
	 * @return the deep copied node
	 */
	public static TreeNode copyTree(TreeNode node, String messageID,
			TreeNode parent) {
		TreeNode copy;
		SuperMediator contents = new SuperMediator();
		ArrayList<TreeNode> children = new ArrayList<TreeNode>();
		contents.copy(node.contents);
		contents.setMsgID(messageID);
		copy = new TreeNode(parent, contents, children);

		int i = 0;
		if (node.getChildren() != null) {
			while (i < node.getChildren().size()) {
				parent = copy;
				copy.children.add(copyTree(node.getChildren().get(i),
						messageID, parent));
				i++;
			}
		}
		return copy;
	}

	/**
	 * This method adds a child node to a parent node
	 * 
	 * @param childMediator
	 *            the node that is to be added as the child
	 */
	public void addChildTreeNodeMediator(TreeNode childMediator) {

		this.children.add(childMediator);
		int childPosition = children.size() - 1;
		children.get(childPosition).parent = this;

	}

	public TreeNode getParent() {
		return parent;
	}

	public void setParent(TreeNode parent) {
		this.parent = parent;
	}

	public SuperMediator getContents() {
		return contents;
	}

	public void setContents(SuperMediator contents) {
		this.contents = contents;
	}

	public ArrayList<TreeNode> getChildren() {
		return children;
	}

	public void setChildren(ArrayList<TreeNode> children) {
		this.children = children;
	}

	/**
	 * This method will be used to get the mediator that failed in execution It
	 * can either be the last added child or if the child list is empty it can
	 * be the current node
	 * 
	 * @return a reference to the TreeNode that contains data of the failed
	 *         mediator
	 */
	public TreeNode getLastFaultChild() {
		if (!this.getChildren().isEmpty())
			return this.getChildren().get(this.getChildren().size() - 1);
		else

			return this;
	}

	/**
	 * This method will be used to get the TreeNode that has the data of the
	 * last added child of a parent TreeNode
	 * 
	 * @return a reference to the TreeNode that contains data of the last added
	 *         child of a certain mediator
	 * 
	 */
	public TreeNode getLastChild() {
		if (this.getChildren() != null) {
			if (this.getChildren().size() > 0) {

				return this.getChildren().get(this.getChildren().size() - 1);

			} else
				return null;
		} else
			return null;
	}

	/**
	 * To print the details of each TreeNode on the console
	 * 
	 * @param node
	 *            of whose data is being printed on the cosole at the moment
	 */
	public void printChildren(TreeNode node) {
		int i = 0, l = 0;
		System.out.println(node.getContents().printData());
		while (i < node.getChildren().size()) {
			l = 0;

			if (node.getChildren().get(i).getContents().getType()
					.equals("single")) {
				System.out.println(node.getChildren().get(i).getContents()
						.printData());
			}

			else {
				depth++;
				printChildren(node.getChildren().get(i));
				depth--;
			}
			i++;
		}

	}

	/**
	 * To print the execution flow of the mediators
	 * 
	 * @param node
	 *            that is been executed according to the configuration
	 * 
	 */
	public synchronized void printTree(TreeNode node) {
		int i = 0;

		System.out.print(node.getContents().getId() + " : ");
		System.out.println(node.getContents().getMediatorName());
		StoreList.storage.add(node);
		while (i < node.getChildren().size()) {

			if (node.getChildren().get(i).getContents().getType()
					.equals("single")) {

				System.out.print(node.getChildren().get(i).getContents()
						.getId()
						+ " : ");

				System.out.println(node.getChildren().get(i).getContents()
						.getMediatorName());
			        StoreList.storage.add(node.getChildren().get(i));


			}

			else {

				printTree(node.getChildren().get(i));

			}
			i++;
		}
	}
}
