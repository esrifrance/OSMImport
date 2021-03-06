package com.osmimport.output.dsl;

import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import com.osmimport.output.Stream;
import com.osmimport.output.dsl.TBuilder;

/**
 * factory for a stream
 * @author pfreydiere
 *
 */
public class StreamFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder, Object name,
			Object value, Map attributes) throws InstantiationException,
			IllegalAccessException {

		if (value == null)
			throw new IllegalAccessException("stream must have an input stream");

		if (!(value instanceof Stream)) {
			throw new IllegalAccessException("parent must be a stream instance");
		}
		
		Stream s = new Stream();
		
		Stream other = new Stream();
		other.parentStream = s;
		other.isOther = true;
		s.other = other;
		
		s.parentStream = (Stream)value;

		
		
		((TBuilder) builder).processModel.addStream(s);
		
		
		return s;

	}
	
	

}
