import com.esri.core.geometry.Geometry;
import com.poc.osm.model.OSMEntity;


// construction de la chaine
builder.build(osmstream) {

	// d�fini un flux de sortie, et description de la structure
	sortie = gdb(path : "c:\\temp\\france.gdb") {
		featureclass("pts", ESRI_GEOMETRY_POINT,"WGS84") {
			/* _text("k", size : 40)
			_integer("mon champ entier")
			_double("autre champ") */
			_integer('id')
		}
		featureclass("lines", ESRI_GEOMETRY_POLYLINE,"WGS84") {
			/* _text("k", size : 40)
			_integer("mon champ entier")
			_double("autre champ") */
			_integer('id')
		}
	}

	// dummy filter
	// f = filter { e -> return true }

	// a stream
	t = stream(osmstream, label:"Points with informations") {

		filter {
			OSMEntity e ->
			   e.geometryType == Geometry.Type.Point && e.getFields() != null
		}
		
		transform { OSMEntity e ->
			e.getFields()?.clear();
			e.setValue("id",e.id);
			return e;
		}

	}
	// a stream
	l = stream(t.others, label:"modification 2") {

		filter {
			OSMEntity e ->
			   e.geometryType == Geometry.Type.Polyline && e.getFields() != null
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


}

