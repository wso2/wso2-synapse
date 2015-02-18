package org.apache.synapse.mediators.collector;


public class SingleMediator extends SuperMediator {
	/**
	 * This class is  used when there are mediators that are added to the tree
	 * as leaf nodes
	 * 
	 *
	 */
	private String type = "single";
	public SingleMediator(){
		super.setType("single");
	}
	

}
