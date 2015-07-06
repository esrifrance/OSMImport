package com.poc.osm.output;

import java.io.Closeable;
import java.io.IOException;

import com.poc.osm.model.OSMAttributedEntity;

public abstract class AbstractWriter implements Closeable {

	
	private long writtenCount = 0;
	
	public abstract void write(OSMAttributedEntity entity) throws Exception;

	public abstract void close() throws IOException;

}
