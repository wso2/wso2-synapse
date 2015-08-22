package org.apache.synapse.flowtracer;

public class SQLQueries {

    public static final String INSERT_MESSAGE_FLOW_INFO_ENTRY = "INSERT INTO "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOW_INFO
            +" (MessageId, Component, ComponentId, Response, Start, Payload, Properties, Timestamp) VALUES (?,?,?,?,?,?,?,?)";

    public static final String INSERT_MESSAGE_FLOWS_ENTRY = "INSERT INTO "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS
            + " (MessageId, FlowTrace, EntryType, TimeStamp) VALUES(?,?,?,?)";

    public static final String GET_MESSAGE_FLOW_TRACE = "SELECT * FROM "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" WHERE messageId = ? ";

    public static final String GET_ALL_MESSAGE_FLOWS = "SELECT distinct ID,MessageId,EntryType,TimeStamp FROM " + MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" ORDER BY ID";

    public static final String GET_COMPONENT_INFO = "SELECT * FROM " + MessageFlowTracerConstants.TABLE_MESSAGE_FLOW_INFO+" WHERE MessageId = ? ORDER BY ID";

    public static final String DELETE_ALL = "DELETE FROM ";
}
