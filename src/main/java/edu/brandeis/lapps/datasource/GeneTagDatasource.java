/*
 * Copyright (c) 2018 Brandeis University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This code is based on org.anc.lapps.datasource.generic.GenericDatasource, which
// was originally taken from https://github.com/oanc/org.anc.lapps.datasource.generic

package edu.brandeis.lapps.datasource;

import java.io.BufferedReader;
import org.lappsgrid.api.DataSource;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.DataSourceMetadata;
import org.lappsgrid.metadata.DataSourceMetadataBuilder;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lappsgrid.discriminator.Discriminators.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.anc.lapps.datasource.generic.Version;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.Contains;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.serialization.LifException;

public class GeneTagDatasource implements DataSource
{
	private static final Logger logger = LoggerFactory.getLogger(GeneTagDatasource.class);

	//public static final String PROPERTY_NAME = "DATASOURCE_INDEX";

	private final String path = "src/main/resources/data/";
	private String metadata;
	//private Map<String, File> index;
	//private List<String> keys;
	private String cachedError;


	public GeneTagDatasource() {}


	/**
	 * Entry point for a Lappsgrid service.
	 * <p>
	 * Each service on the Lappsgrid will accept {@code org.lappsgrid.serialization.Data} object
	 * and return a {@code Data} object with a {@code org.lappsgrid.serialization.lif.Container}
	 * payload.
	 * <p>
	 * Errors and exceptions the occur during processing should be wrapped in a {@code Data}
	 * object with the discriminator set to http://vocab.lappsgrid.org/ns/error
	 * <p>
	 * See <a href="https://lapps.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/Data.html>org.lappsgrid.serialization.Data</a><br />
	 * See <a href="https://lapps.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/lif/Container.html>org.lappsgrid.serialization.lif.Container</a><br />
	 *
	 * @param input A JSON string representing a Data object
	 * @return A JSON string containing a Data object with a Container payload.
	 */
	@Override
	public String execute(String input)
	{
		if (cachedError != null)
			return cachedError;
		Data data = Serializer.parse(input, Data.class);
		String discriminator = data.getDiscriminator();
		if (Uri.ERROR.equals(discriminator))
			return input;

		//System.out.println("INPUT: " + input + "\n");

		switch (discriminator)
		{
			case Uri.SIZE:

				Data<Integer> sizeData = new Data<>();
				sizeData.setDiscriminator(Uri.OK);
				sizeData.setPayload(14996);
				return Serializer.toJson(sizeData);

			case Uri.LIST:

				ArrayList<String> sentences = readSentences();
				Data<java.util.List<String>> listData = new Data<>();
				listData.setDiscriminator(Uri.STRING_LIST);
				listData.setPayload(sentences);
				return Serializer.toJson(listData);

			case Uri.GET:

				String key = data.getPayload().toString();
				if (key == null)
					return error("No key value provided");
				else {
					String fname = "/data/" + key.substring(0, 3) + File.separator
							+ key.substring(0, 4) + File.separator + key;
					String sentence = readSentence(fname);
					try {
						return createTextWithAnnotations(sentence); }
					catch (LifException ex) {
						return error(ex.getMessage()); }}
					/*
					System.out.println(">>> " + xx);
					fname = this.path + key.substring(0, 3) + File.separator
							+ key.substring(0, 4) + File.separator + key;
					File file = new File(fname);
					if (! file.exists())
						return error("File not found: " + file.getPath());
					try {
						String sentence = new String(Files.readAllBytes(file.toPath()));
						return createTextWithAnnotations(sentence); }
					catch (IOException | LifException e) {
						return error(e.getMessage()); }}
					*/

			case Uri.GETMETADATA:

				return metadata;

			default:

				String message = String.format("Invalid discriminator: %s, Uri.List is %s", discriminator, Uri.LIST);
				//logger.warn(message);
				return error(message);
		}

	}

	/**
	 * Returns a JSON string containing metadata describing the service. The
	 * JSON <em>must</em> conform to the json-schema at
	 * <a href="http://vocab.lappsgrid.org/schema/service-schema.json">http://vocab.lappsgrid.org/schema/service-schema.json</a>
	 * (processing services) or
	 * <a href="http://vocab.lappsgrid.org/schema/datasource-schema.json">http://vocab.lappsgrid.org/schema/datasource-schema.json</a>
	 * (datasources).
	 */
	@Override
	public String getMetadata()
	{
		if (metadata == null) {
			DataSourceMetadata md = new DataSourceMetadataBuilder()
					.name(this.getClass().getName())
					.version(Version.getVersion())
					.vendor("http:/www.lappsgrid.org")
					.allow(Uri.ALL)
					.encoding("UTF-8")
					.format(Uri.LIF)
					.description("GeneTag DataSource")
					.license(Discriminators.Uri.APACHE2)
					.build();
			Data data = new Data();
			data.setDiscriminator(Discriminators.Uri.META);
			data.setPayload(md);
			metadata = data.asPrettyJson();
		}
		return metadata;
	}

	protected String error(String message)
	{
		return new Data<>(Uri.ERROR, message).asPrettyJson();
	}

	/**
	 * Return a Data object for the sentence and associated GENE annotations.
	 * @param sentence
	 * @return A Data object whose payload is a LIF object.
	 * @throws LifException
	 */
	private String createTextWithAnnotations(String sentence) throws LifException
	{
		Container container = new Container();
		int idx = sentence.indexOf('\n');
		container.setText(sentence.substring(0, idx));
		container.setLanguage("en");

		String annotations = sentence.substring(idx).trim();
		String[] annos = annotations.split("\n");

		View view = container.newView();
		view.addContains(Uri.NE, "GeneTag Gold Data", null);
		Contains contains = view.getContains(Uri.NE);
		contains.put("namedEntityCategorySet", "tags-ner-biomedical");
		for (int i=0; i<annos.length; i++) {
			String[] fields = annos[i].split("\t");
			int p1 = Integer.parseInt(fields[0]);
			int p2 = Integer.parseInt(fields[1]);
			Annotation a = new Annotation("ne" + i, Uri.NE, p1, p2);
			a.addFeature("category", "GENE");
			view.addAnnotation(a); }

		Data<Container> alldata = new Data<>(Uri.LIF, container);
		return alldata.asPrettyJson();
	}


	/**
	 * Return the content of a single file in the data directory, representing a
	 * single sentence and it GENE annotations.
	 */
	private String readSentence(String fname)
	{
		StringBuilder buffer = new StringBuilder();
		try {
			BufferedReader reader = openResource(fname);
			String line = null;
			while((line = reader.readLine()) != null)
				buffer.append(line).append("\n");
		} catch (IOException ex) {
			java.util.logging.Logger.getLogger(GeneTagDatasource.class.getName()).log(Level.SEVERE, null, ex);
		}
		return buffer.toString();
	}

	/**
	 * Return a list of all sentence identifiers in /data/sentences.txt.
	 */
	private ArrayList<String> readSentences()
	{
		ArrayList<String> sentences = new ArrayList<>();
		try {
			BufferedReader reader = openResource("/data/sentences.txt");
			String line;
			line = reader.readLine();
			sentences.add(line.trim());
			while (true) {
				line = reader.readLine();
				if (line == null)
					break;
				sentences.add(line.trim()); }
		} catch (IOException ex) {
			java.util.logging.Logger.getLogger(GeneTagDatasource.class.getName()).log(Level.SEVERE, null, ex);
		}
		return sentences;
	}

	private BufferedReader openResource(String name)
	{
		InputStream stream = this.getClass().getResourceAsStream(name);
		return new BufferedReader(new InputStreamReader(stream));
	}
}
