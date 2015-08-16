
import com.osmimport.model.OSMAttributedEntity
import com.osmimport.model.OSMEntity
import com.osmimport.model.OSMRelatedObject
import com.osmimport.model.OSMRelation


// construction de la chaine
builder.build(osmstream) {

	// d�fini un flux de sortie, et description de la structure
	sortie = gdb(path : var_outputgdb) {
		featureclass("pts", ESRI_GEOMETRY_POINT,"WGS84") {
			_long('id')
		}
		featureclass("lines", ESRI_GEOMETRY_POLYLINE,"WGS84") {
			_long('id')
		}
		featureclass("polygon", ESRI_GEOMETRY_POLYGON,"WGS84") {
			_long('id')
		}
		table("rels") {
			_long('id')
			_long('rid')
			_text('role')
			_text('type')
		}	
	}

	
	// relations stream
	
	rels = stream(osmstream, label:"relations") {
		filter {
			e ->
			 (e instanceof OSMRelation) && e.getFields() != null
	   }
		
		transform { OSMRelation e ->
			
			e.getFields()?.clear();
			
			e.setValue("id",e.getId());
			
			// r = []  don't do this, this lead to concurrent issues
			
			List r = new ArrayList<OSMAttributedEntity>(); 
			
			for (int i = 0 ; i < e.relations.size() ; i ++ )
			{
				OSMRelatedObject related = e.relations.get(i)
				OSMAttributedEntity ro = new OSMAttributedEntity(e.id, e.fields);
				ro.setValue("id", e.id);
				ro.setValue("rid", related.relatedId);
				ro.setValue('role', related.relation);
				ro.setValue('type', related.type);
				
				r.add(ro)
			}
			
			
			return r;
		}
	}
	
	
	t = stream(osmstream, label:"Points") {

		filter {
			 e ->
			  (e instanceof OSMEntity) && e.geometryType == Geometry.Type.Point && e.fields != null
		}
		
		transform { OSMEntity e ->
			e.getFields()?.clear();
			e.setValue("id",e.id);
			return e;
		}

	}
	// a stream
	l = stream(osmstream, label:"Polylines") {

		filter {
			 e ->
			   e instanceof OSMEntity && e.geometryType == Geometry.Type.Polyline && e.getFields() != null
		}
		
		transform { OSMEntity e ->
			e.getFields()?.clear();
			e.setValue("id",e.id);
			return e;
		}

	}
	polys = stream(osmstream, label:"polygones") {
		
				filter {
					 e ->
					(e instanceof OSMEntity) && e.geometryType == Geometry.Type.Polygon && e.getFields() != null
				}
				
				transform { OSMEntity e ->
					e.getFields()?.clear();
					e.setValue("id",e.id);
					return e;
				}
		
			}
		
	
	// flux de sortie
	out(streams : t, gdb : sortie, tablename:"pts")
	out(streams : l, gdb : sortie, tablename:"lines")
	out(streams : rels, gdb : sortie, tablename:"rels")
	out(streams : polys, gdb : sortie, tablename:"polygon")
	


}

