package org.apache.synapse.mediators.collector;

import java.util.ArrayList;
import java.util.List;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;

/**
 * This class is used to keep per mediator data
 * 
 */
public class SuperMediator {

	/**
	 * type of the mediator, single or nested
	 */
	private String type;
	/**
	 * CommonMessageID of the MessageContext that is used by the mediator
	 */
	private String msgID;
	private String mediatorName;
	/**
	 * ID of the mediator indicating its position in the configuration
	 */
	private String id;
	private String envelop;
	/**
	 * ProxyService name or API name of the MessageContext
	 */
	private String serviceName;
	/**
	 * Name of the root node
	 */
	private String rootType;
	/**
	 * either the mediator is a part of a request or a response
	 */
	private String req_resp = " ";
	/**
	 * Children list of the mediator
	 */
	private List<Mediator> children = new ArrayList<Mediator>();
	private long startTime;
	private long endTime;
	/**
	 * Is the Mediator executed successfully ?
	 */
	private boolean success = true;

	/**
	 * This method will generate a deepcopy of the existing data fields while
	 * creating a clone
	 * 
	 * @param other the mediator of which's data that are subjected to be cloned
	 */
	public void copy(SuperMediator other) {
		this.setType(new String(other.getType()));
		this.setId(new String(other.getId()));
		this.setMediatorName(new String(other.getMediatorName()));
		this.setId(new String(other.getId()));
		this.setEnvelop(new String(other.getEnvelop()));
		this.setServiceName(new String(other.getServiceName()));
		this.setRootType(new String(other.getRootType()));
		this.setStartTime(new Long(other.getStartTime()));
		this.setEndTime(new Long(other.getEndTime()));
		this.setSuccess(new Boolean(other.isSuccess()));

	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public String getRootType() {
		return rootType;
	}

	public void setRootType(String rootType) {
		this.rootType = rootType;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getEnvelop() {
		return envelop;
	}

	public void setEnvelop(String envelop) {
		this.envelop = envelop;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMediatorName() {
		return mediatorName;
	}

	public void setMediatorName(String mediatorName) {
		this.mediatorName = mediatorName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMsgID() {
		return msgID;
	}

	public void setMsgID(String msgID) {
		this.msgID = msgID;
	}

	public String getReq_resp() {
		return req_resp;
	}

	public void setReq_resp(String req_resp) {
		this.req_resp = req_resp;
	}

	public String printData() {
		String a = "Name        : " + mediatorName + "\n";
		String b = "Type        : " + type + "\n";
		String c = "MsgID       : " + msgID + "\n";
		String d = "Success     : " + success + "\n";
		String e = "Start Time  : " + startTime + "\n";
		String f = "End Time    : " + endTime + "\n";
		String g = "Proxy/API   : " + serviceName + "\n";
		String h = "Mediator id : " + id + "\n";
		String i = "envelop     : " + envelop + "\n";
		String j = "Root Name   : " + rootType + "\n";
		String k = "Resp/Req    : " + req_resp + "\n";
		return a + b + c + d + e + f + g + h + i + j + k;

	}

}
