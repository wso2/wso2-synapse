package org.apache.synapse.task;

public interface TaskStartupSubject {
	
	public void attach(TaskStartupObserver taskStartupObserver);
	
	public void notifySubjects();
	
}
