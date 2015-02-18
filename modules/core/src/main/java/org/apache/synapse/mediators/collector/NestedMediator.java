package org.apache.synapse.mediators.collector;

import java.util.ArrayList;


/**
 * This is the NestedMediator class to be used when there are mediators that extends AbstractListMediator or implements
 * FlowContinuableMediator
 * 
 *
 */
public class NestedMediator extends SuperMediator {

	private String type="nested";
	public NestedMediator(){
		super.setType("nested");
	}

	
}
