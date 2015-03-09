package org.apache.synapse.message.store.impl.rabbitmq;

import java.io.Serializable;

/**
 * Created by eranda on 3/6/15.
 */
public class TestStorableClass implements Serializable{
	private static final long serialVersionUID = 1L;
	String name;
	public TestStorableClass(String name){
		this.name = name;
	}

	public TestStorableClass(){

	}
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
