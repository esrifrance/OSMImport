package com.poc.osm.parsing.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.esri.core.geometry.Point;
import com.poc.osm.actors.MeasuredActor;
import com.poc.osm.messages.MessageNodes;
import com.poc.osm.model.OSMEntity;
import com.poc.osm.model.OSMEntityGeometry;
import com.poc.osm.model.OSMEntityPoint;
import com.poc.osm.model.OSMRelatedObject;
import com.poc.osm.model.OSMRelation;
import com.poc.osm.parsing.actors.messages.MessagePolygonToConstruct;
import com.poc.osm.parsing.actors.messages.MessageRelations;
import com.poc.osm.parsing.actors.messages.MessageWayToConstruct;
import com.poc.osm.parsing.model.OSMBlock;
import com.poc.osm.parsing.model.OSMContext;
import com.poc.osm.parsing.model.PolygonToConstruct;
import com.poc.osm.parsing.model.PolygonToConstruct.Role;
import com.poc.osm.parsing.model.WayToConstruct;

import crosby.binary.Osmformat.DenseNodes;
import crosby.binary.Osmformat.Node;
import crosby.binary.Osmformat.Relation;
import crosby.binary.Osmformat.Relation.MemberType;
import crosby.binary.Osmformat.Way;

/**
 * This actor construct object from the OSM Blocks reading
 * 
 * @author pfreydiere
 * 
 */
public class OSMObjectGenerator extends MeasuredActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private ActorRef dispatcher;

	private ActorRef flowRegulator;

	public OSMObjectGenerator(ActorRef dispatcher, ActorRef flowRegulator) {
		this.dispatcher = dispatcher;
		this.flowRegulator = flowRegulator;
	}

	/**
	 * Construct the objects, with fields
	 * 
	 * @param b
	 * @throws Exception
	 */
	protected void parseObjects(OSMBlock b) throws Exception {

		if (log.isDebugEnabled())
			log.debug(getSelf() + " " + System.currentTimeMillis()
					+ " handle block " + b.getCounter());

		List<OSMEntity> allNodes = constructNodesTreeSet(b);
		if (allNodes != null) {
			tell(dispatcher, new MessageNodes(allNodes), getSelf());
		}

		// emit the nodes received ...

		ArrayList<WayToConstruct> ways = constructWays(b);
		if (ways != null) {
			if (allNodes != null) {
				tell(dispatcher,
						new MessageWayToConstruct(b.getCounter(), ways),
						getSelf());
			}
		}

		parseRelations(b);

	}

	protected void parseRelations(OSMBlock b) {

		List<Relation> relations = b.getRelations();
		OSMContext ctx = b.getContext();

		// List<RelationToConstruct> ret = null;

		List<PolygonToConstruct> polygons = null;

		List<OSMRelation> outRels = null;

		if (relations != null && relations.size() > 0) {

			label_relation: for (Relation r : relations) {

				long id = r.getId();

				HashMap<String, Object> flds = null;
				for (int i = 0; i < r.getKeysCount(); i++) {
					String k = ctx.getStringById(r.getKeys(i));
					String v = ctx.getStringById(r.getVals(i));
					if (flds == null) {
						flds = new HashMap<String, Object>();
					}
					flds.put(k, v);
				}

				if (flds != null) {

					if (flds.containsKey("area")) {

						long[] relids = new long[r.getMemidsCount()];
						Role[] idsRoles = new Role[r.getMemidsCount()];

						// polygon

						for (int i = 0; i < r.getMemidsCount(); i++) {
							long rid = r.getMemids(i);
							String role = ctx.getStringById(r.getRolesSid(i));
							MemberType mt = r.getTypes(i);
							if (mt != MemberType.WAY) {
								log.warning("area relation "
										+ id
										+ " contains relation of other type than WAY, the object is skipped");
								continue label_relation;
							}

							relids[i] = rid;

							Role erole = null;

							if ("outer".equals(role)) {
								erole = Role.OUTER;
							} else if ("inner".equals(role)) {
								erole = Role.INNER;
							} else {
								log.warning("area relation "
										+ id
										+ " contains relation of other role outer or inner, the object is skipped");
								continue label_relation;
							}

							idsRoles[i] = erole;

							if (polygons == null) {
								polygons = new ArrayList<>();
							}

							polygons.add(new PolygonToConstruct(id, relids,
									flds, idsRoles));

						}

					} else {
						// relation ..

						List<OSMRelatedObject> relatedObjects = null;

						for (int i = 0; i < r.getMemidsCount(); i++) {
							long rid = r.getMemids(i);
							String role = ctx.getStringById(r.getRolesSid(i));

							MemberType types = r.getTypes(i);
							String stringType = null;
							switch (types) {

							case NODE:
								stringType = "node";
								break;
							case WAY:
								stringType = "way";
								break;
							case RELATION:
								stringType = "relation";
								break;
							default:
								log.warning("unknown relation type for object "
										+ id + " the object is skipped");
								continue label_relation;

							}

							if (relatedObjects == null) {
								relatedObjects = new ArrayList<>();
							}
							relatedObjects.add(new OSMRelatedObject(rid, role,
									stringType));

						}

						if (outRels == null) {
							outRels = new ArrayList<>();
						}

						outRels.add(new OSMRelation(id, flds, relatedObjects));

					}

				}

			}

		}

		if (polygons != null) {
			tell(dispatcher, new MessagePolygonToConstruct(b.getCounter(),
					polygons), getSelf());
		}

		if (outRels != null) {
			tell(dispatcher, new MessageRelations(outRels), getSelf());
		}

	}

	/**
	 * @param b
	 * @param ws
	 */
	protected ArrayList<WayToConstruct> constructWays(OSMBlock b) {
		List<Way> ws = b.getWays();

		ArrayList<WayToConstruct> ret = null;

		if (ws != null && ws.size() > 0) {
			ret = new ArrayList<WayToConstruct>();
			for (Way w : ws) {

				long lastRef = 0;
				List<Long> l = w.getRefsList();

				long[] refids = new long[l.size()];
				// liste des references ...
				int cpt = 0;
				for (Long theid : l) {
					lastRef += theid;
					refids[cpt++] = lastRef;
				}

				OSMContext ctx = b.getContext();

				HashMap<String, Object> flds = null;

				for (int i = 0; i < w.getKeysCount(); i++) {
					String k = ctx.getStringById(w.getKeys(i));
					String v = ctx.getStringById(w.getVals(i));
					if (flds == null) {
						flds = new HashMap<String, Object>();
					}
					flds.put(k, v);
				}

				WayToConstruct wayToConstruct = new WayToConstruct(w.getId(),
						refids, flds);

				// System.out.println("polyline done !");
				// emit the way to construct
				ret.add(wayToConstruct);
			}
		}
		return ret;
	}

	/**
	 * @param b
	 */
	protected List<OSMEntity> constructNodesTreeSet(OSMBlock b) {

		List<OSMEntity> parsedNodes = null;

		OSMContext ctx = b.getContext();

		DenseNodes denseNodes = b.getDenseNodes();
		if (denseNodes != null) {

			if (parsedNodes == null) {
				parsedNodes = constructEmptyTreeMap();
			}

			long lastId = 0;
			long lastLat = 0;
			long lastLon = 0;

			int j = 0;
			for (int i = 0; i < denseNodes.getIdCount(); i++) {

				lastId += denseNodes.getId(i);
				lastLat += denseNodes.getLat(i);
				lastLon += denseNodes.getLon(i);

				// construction

				// Optim

				// Point p = new Point();
				// p.setX(lastLon);
				// p.setY(lastLat);

				Map<String, Object> flds = null;

				try {
					if (denseNodes.getKeysValsCount() > 0) {
						while (denseNodes.getKeysVals(j) != 0) {
							int keyid = denseNodes.getKeysVals(j++);
							int valid = denseNodes.getKeysVals(j++);
							String k = "";
							if (keyid < ctx.getStringLength())
								k = ctx.getStringById(keyid);
							else
								System.out.println("error in encoding");

							String v = "";
							if (valid < ctx.getStringLength())
								v = ctx.getStringById(valid);
							else
								System.out.println("error in encoding");

							if (flds == null)
								flds = new HashMap<String, Object>();

							flds.put(k, v);

							// System.out.println(k + "->" + v);

						}
						j++; // Skip over the '0' delimiter.
					}
				} catch (Exception ex) {
					System.out.println("error : " + ex.getMessage());
				}
				OSMEntity o = new OSMEntityPoint(lastId, ctx.parseLon(lastLon),
						ctx.parseLat(lastLat), flds);
				parsedNodes.add(o);

			}
		}

		List<Node> n = b.getNodes();
		if (n != null) {

			if (parsedNodes == null) {
				parsedNodes = constructEmptyTreeMap();
			}

			for (Node thenode : n) {

				Point p = new Point();
				p.setXY(ctx.parseLon(thenode.getLon()),
						ctx.parseLat(thenode.getLat()));

				Map<String, Object> fields = null;

				for (int i = 0; i < thenode.getKeysCount(); i++) {
					int keys = thenode.getKeys(i);
					String k = ctx.getStringById(keys);
					String v = ctx.getStringById(thenode.getVals(i));
					if (fields == null) {
						fields = new HashMap<String, Object>();
					}
					fields.put(k, v);
				}

				OSMEntity o = new OSMEntityGeometry(thenode.getId(), p, fields);
				parsedNodes.add(o);

			}
		}

		// System.out.println(getSelf() + " " + System.currentTimeMillis() + " "
		// + (parsedNodes == null ? " None " : "" + parsedNodes.size())
		// + "  nodes processed");

		return parsedNodes;

	}

	/**
	 * @return
	 */
	protected List<OSMEntity> constructEmptyTreeMap() {
		return new ArrayList<OSMEntity>();
	}

	@Override
	public void onReceiveMeasured(Object message) throws Exception {

		if (message instanceof OSMBlock) {
			OSMBlock nds = (OSMBlock) message;
			if (log.isDebugEnabled())
				log.debug(nds + " Block receive in actor " + getSelf());
			parseObjects(nds);
			log.debug("block done !");
		} else {
			unhandled(message);
		}

	}

}
