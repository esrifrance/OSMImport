package com.osmimport.output.actors.gdb;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.osmimport.dslfacilities.OSMAttributedEntitiesOps;
import com.osmimport.output.OutCell;
import com.osmimport.output.ProcessModel;
import com.osmimport.output.Stream;
import com.osmimport.output.Transform;
import com.osmimport.output.dsl.TBuilder;

/**
 * construct the output chain
 * 
 * @author pfreydiere
 * 
 */
public class ChainCompiler {

	public ChainCompiler() {

	}

	/**
	 * compile the script into processing model, to be able to construct the
	 * pipeline
	 * 
	 * @param processFile
	 * @param osmStream
	 * @return
	 * @throws Exception
	 */
	public ProcessModel compile(File processFile, Stream mainStream,
			Map<String, String> builtinVariables) throws Exception {

		assert processFile != null;
		assert processFile.exists();

		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

		// custom imports for ease the use of the geometry types
		ImportCustomizer icz = new ImportCustomizer();
		icz.addStarImports("com.osmimport", "com.esri.core.geometry",
				"com.osmimport.model");
		icz.addStaticImport("org.fgdbapi.thindriver.xml.EsriGeometryType",
				"ESRI_GEOMETRY_POINT");
		icz.addStaticImport("org.fgdbapi.thindriver.xml.EsriGeometryType",
				"ESRI_GEOMETRY_MULTIPOINT");
		icz.addStaticImport("org.fgdbapi.thindriver.xml.EsriGeometryType",
				"ESRI_GEOMETRY_POLYLINE");
		icz.addStaticImport("org.fgdbapi.thindriver.xml.EsriGeometryType",
				"ESRI_GEOMETRY_POLYGON");
		icz.addStaticImport("org.fgdbapi.thindriver.xml.EsriGeometryType",
				"ESRI_GEOMETRY_MULTI_PATCH");
		icz.addStaticStars(com.osmimport.dslfacilities.Transform.class.getName());
		icz.addStaticStars(OSMAttributedEntitiesOps.class.getName());

		compilerConfiguration.addCompilationCustomizers(icz);

		File parentFile = processFile.getParentFile();

		if (parentFile == null) {
			parentFile = new File("."); // fix, the file is null when a name is
										// directly passed
		}

		GroovyScriptEngine gse = new GroovyScriptEngine(
				new URL[] { parentFile.toURL() });
		gse.setConfig(compilerConfiguration);

		Binding binding = new Binding();
		binding.setVariable("builder", new TBuilder());
		binding.setVariable("osmstream", mainStream);
		binding.setVariable("out", System.out);

		if (builtinVariables != null) {

			for (Entry<String, String> v : builtinVariables.entrySet()) {
				// adding builting variables
				String k = "var_" + v.getKey();
				binding.setVariable(k, v.getValue());
			}

		}

		Object result = gse.run(processFile.getName(), binding);

		if (result == null || !(result instanceof ProcessModel)) {
			throw new Exception(
					"invalid script, it must return the constructed process model, the script return :"
							+ result);
		}

		ProcessModel pm = (ProcessModel) result;
		pm.mainStream = mainStream;

		System.out.println("compiled model :" + pm.toString());

		return pm;
	}

	public static class ValidateResult {
		public String[] warnings;
	}

	/**
	 * Validate the process model, export warning messages errors in the model
	 * throw Exception
	 * 
	 * @return object that contain warnings, and front streams
	 */
	public ValidateResult validateProcessModel(ProcessModel pm)
			throws Exception {

		ArrayList<String> warnings = new ArrayList<String>();

		HashSet<Stream> markedStreams = new HashSet<Stream>();

		Collection<OutCell> outs = pm.outs;
		if (outs == null || outs.size() == 0)
			throw new Exception("no out specified, invalid model");

		for (OutCell c : outs) {

			Stream[] ss = c.streams;

			if (ss == null) {
				warnings.add("unused out " + c
						+ ", because there are no linked streams");
				continue;
			}

			for (Stream s : ss) {

				if (s == null) {
					warnings.add("null stream in collection for " + c);
					continue;
				}

				// mark stream
				markedStreams.add(s);

				Collection<Stream> analyzedStack = new ArrayList<Stream>();

				Stream current = s;
				while (current.parentStream != null
						&& current.parentStream != pm.mainStream) {
					markedStreams.add(current);
					analyzedStack.add(current);
					current = current.parentStream;
				}

				if (current.parentStream == null) {
					warnings.add("null parent, and the following streams [ "
							+ analyzedStack
							+ " ]are not connected to main stream");
				}

			} // for streams

		} // outcells

		ValidateResult r = new ValidateResult();
		r.warnings = warnings.toArray(new String[0]);
		return r;
	}

}
