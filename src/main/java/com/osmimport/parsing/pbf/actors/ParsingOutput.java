package com.osmimport.parsing.pbf.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.osmimport.actors.MeasuredActor;
import com.osmimport.messages.MessageNodes;
import com.osmimport.messages.MessageWay;
import com.osmimport.parsing.pbf.actors.messages.MessageParsingSystemStatus;

/**
 * Consolidate the parsing results, and send the result to a node
 * 
 * @author use
 * 
 */
public class ParsingOutput extends MeasuredActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private ActorRef resultSendingActors;

	public ParsingOutput(ActorRef output) {
		resultSendingActors = output;
	}

	private enum State {
		READING_POINTS, POINTS_HAVE_BEEN_READ
	}

	private State currentState = State.READING_POINTS;

	/*
	 * (non-Javadoc)
	 * @see akka.actor.UntypedActor#onReceive(java.lang.Object)
	 */
	@Override
	public void onReceiveMeasured(Object message) throws Exception {

		if (log.isDebugEnabled()) {
			log.debug("message received :" + message);
		}

		if (message instanceof MessageNodes) {

			if (currentState == State.READING_POINTS) {
				// first read, we send the points to the output for handling
				tell(resultSendingActors,message, getSelf());
			}
			
			// else points have already been sent, don't resend them ...

		} else if (message instanceof MessageWay) {
			
			tell(resultSendingActors,message, getSelf());

		} else if (message instanceof MessageParsingSystemStatus) {
			
			MessageParsingSystemStatus s = (MessageParsingSystemStatus) message;

			if (s == MessageParsingSystemStatus.END_READING_FILE) {
				currentState = State.POINTS_HAVE_BEEN_READ;
				log.info("All Points have been processed");
			}
			
		
		}  else {
			unhandled(message);
		}

	}

}
