package com.poc.osm.output.dsl;

import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import com.poc.osm.output.GDBReference;
import com.poc.osm.output.OutCell;
import com.poc.osm.output.dsl.TBuilder;

/**
 * factory for an outcell
 * @author pfreydiere
 *
 */
public class OutGdbFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder, Object name,
			Object value, Map attributes) throws InstantiationException,
			IllegalAccessException {

		return new OutCell();

	}
	
	@Override
	public void onNodeCompleted(FactoryBuilderSupport builder, Object parent,
			Object node) {

		TBuilder tb = (TBuilder)builder;
		tb.processModel.outs.add((OutCell)node);
		
		super.onNodeCompleted(builder, parent, node);
	}

}
