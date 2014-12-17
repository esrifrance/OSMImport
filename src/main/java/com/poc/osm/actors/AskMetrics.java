package com.poc.osm.actors;

import akka.dispatch.ControlMessage;

public class AskMetrics implements ControlMessage {

	/**
	 * special message send by the internal timer
	 */
	public static final AskMetrics CHILDREN_ASK = new AskMetrics(-1);

	private long correlationId;

	/**
	 * ask for metrics, with a correlation id
	 * @param correlationId
	 */
	public AskMetrics(long correlationId) {
		this.correlationId = correlationId;
	}

	public long getCorrelationId() {
		return correlationId;
	}

}