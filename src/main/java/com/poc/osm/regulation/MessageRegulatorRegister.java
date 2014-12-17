package com.poc.osm.regulation;

import akka.actor.ActorRef;

public class MessageRegulatorRegister {
	
	private ActorRef actor;
	
	public MessageRegulatorRegister(ActorRef a)
	{
		this.actor = a;
	}

	public ActorRef getActor() {
		return actor;
	}
	
}