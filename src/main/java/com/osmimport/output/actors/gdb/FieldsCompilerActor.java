package com.osmimport.output.actors.gdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fgdbapi.thindriver.swig.Table;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.esri.core.geometry.Geometry;
import com.osmimport.actors.MeasuredActor;
import com.osmimport.model.OSMAttributedEntity;
import com.osmimport.model.OSMEntity;
import com.osmimport.output.actors.gdb.messages.CompiledFieldsMessage;
import com.osmimport.output.fields.AbstractFieldSetter;
import com.osmimport.output.fields.GeometryFieldSetter;

/**
 * this actor compile the fields values for a new row
 * 
 * @author pfreydiere
 * 
 */
public class FieldsCompilerActor extends MeasuredActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	// cloned elements
	private AbstractFieldSetter[] compiledFields;

	private ActorRef compiledOutputActor;

	public FieldsCompilerActor(Table t, ActorRef compiledOutputActor) {

		assert compiledOutputActor != null;

		this.compiledOutputActor = compiledOutputActor;

		log.debug("creating the compiled fields");

		ArrayList<AbstractFieldSetter> r = GDBOutputTools.createCompiledFieldSetters(t);

		compiledFields = r.toArray(new AbstractFieldSetter[0]);

	}

	@Override
	public void onReceiveMeasured(Object message) throws Exception {

		if (message instanceof OSMAttributedEntity) {

			handleSingleMessage((OSMAttributedEntity) message);

		} else if (message instanceof List) {

			List<OSMAttributedEntity> l = (List<OSMAttributedEntity>) message;
			for (OSMAttributedEntity e : l) {
				try {
					handleSingleMessage(e);
				} catch (Exception ex) {
					log.error("error in handling message " + ex.getMessage(),
							ex);
				}
			}

		}

		else {
			unhandled(message);
		}

	}

	private void handleSingleMessage(OSMAttributedEntity osme) {

		StringBuilder sb = new StringBuilder();

		AbstractFieldSetter[] n = new AbstractFieldSetter[compiledFields.length];
		for (int i = 0; i < n.length; i++) {

			n[i] = compiledFields[i].clone();

			String rs;
			if (n[i] instanceof GeometryFieldSetter
					&& osme instanceof OSMEntity) {

				OSMEntity entityWithGeometry = (OSMEntity) osme;

				Geometry geometry = entityWithGeometry.getGeometry();
				// log.info("geometry :" +
				// GeometryEngine.geometryToJson(4326, geometry));
				rs = ((GeometryFieldSetter) n[i]).setValue(geometry);
				if (rs != null){
					sb.append(rs).append("\n");
				}
			} else {

				Map<String, Object> flds = osme.getFields();

				if (flds != null) {
					rs = n[i].setValue(flds.get(n[i].getFieldName()));
					if (rs != null) {
						sb.append(rs).append("\n");
					}
				}

			}

		}

		if (sb.length() > 0) {
			log.error("error on entity :" + osme.getId() + " :" + sb.toString());
		}

		tell(compiledOutputActor, new CompiledFieldsMessage(n), getSelf());
	}

}
