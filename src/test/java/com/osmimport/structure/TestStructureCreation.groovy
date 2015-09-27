package com.osmimport.structure;

import static org.junit.Assert.*

import org.fgdbapi.thindriver.xml.EsriGeometryType

import com.osmimport.output.dsl.TStructure

class TestStructureCreation extends GroovyTestCase{

	

	/**
	 * Test de d�finition de la s�mantique de flux de l'outil
	 */
	void testFilter() {
				// d�finition du flux initial des objets osm
				
				def sb = new TStructure()
				
				
				// construction de la chaine
				def s = sb.structure {
		
					// d�fini un flux de sortie, et description de la structure
						featureclass("matable", EsriGeometryType.ESRI_GEOMETRY_POINT,"WGS84") {
							_text("mon texte", size : 40)
							_integer("mon champ entier")
							_double("autre champ")
						}
		
				}
		
				System.out.println(s);
				
		
		
	}
}


