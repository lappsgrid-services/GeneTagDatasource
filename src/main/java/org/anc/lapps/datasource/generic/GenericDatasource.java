/*
 * Copyright (c) 2017 The American National Corpus
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

package org.anc.lapps.datasource.generic;

import org.lappsgrid.api.DataSource;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.DataSourceMetadata;
import org.lappsgrid.metadata.DataSourceMetadataBuilder;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lappsgrid.discriminator.Discriminators.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Keith Suderman
 */
public class GenericDatasource implements DataSource
{
	private static final Logger logger = LoggerFactory.getLogger(GenericDatasource.class);

	public static final String PROPERTY_NAME = "DATASOURCE_INDEX";

	private String metadata;
	private Map<String, File> index;
	private List<String> keys;

	private String cachedError;

	public GenericDatasource()
	{
		String path = System.getProperty(PROPERTY_NAME);
		if (path == null) {
			path = System.getenv(PROPERTY_NAME);
		}
		if (path == null)
		{
			//cachedError = error("DATASOURCE_INDEX property was not set.");
			path = "/var/lib/datasource/index.txt";
		}
//		else
//		{
		logger.info("Loading DataSource index from {}", path);
			loadIndex(path);
//		}
	}


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
	 * See <a href="https://lapp.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/Data.html>org.lappsgrid.serialization.Data</a><br />
	 * See <a href="https://lapp.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/lif/Container.html>org.lappsgrid.serialization.lif.Container</a><br />
	 *
	 * @param input A JSON string representing a Data object
	 * @return A JSON string containing a Data object with a Container payload.
	 */
	@Override
	public String execute(String input)
	{
		if (cachedError != null)
		{
			return cachedError;
		}
		Data data = Serializer.parse(input, Data.class);
		String discriminator = data.getDiscriminator();
		if (Uri.ERROR.equals(discriminator)) {
			return input;
		}

		String result;
		switch (discriminator)
		{
			case Uri.SIZE:
				Data<Integer> sizeData = new Data<Integer>();
				Data d;

				sizeData.setDiscriminator(Uri.OK);
				sizeData.setPayload(index.size());
				result = Serializer.toJson(sizeData);
				break;
			case Uri.LIST:
				Map payload = (Map) data.getPayload();
				if (payload == null)
				{
					payload = new HashMap<String,String>();
				}

				List<String> list = keys;
				Object startValue = payload.get("start");
				if (startValue != null)
				{
					int start = 0;
					int offset = Integer.parseInt(startValue.toString());
					if (offset >= 0) {
						start = offset;
					}
					int end = index.size();
					Object endValue = payload.get("end");
					if (endValue != null)
					{
						offset = Integer.parseInt(endValue.toString());
						if (offset >= start) {
							end = offset;
						}
					}
					list = keys.subList(start, end);
				}
				Data<java.util.List<String>> listData = new Data<>();
				listData.setDiscriminator(Uri.STRING_LIST);
				listData.setPayload(list);
				result = Serializer.toJson(listData);
				break;
			case Uri.GET:
				String key = data.getPayload().toString();
				if (key == null)
				{
					result = error("No key value provided");
				}
				else
				{
					File file = index.get(key);
					if (file == null)
					{
						result = error("No file with key " + key);
					}
					else if (!file.exists())
					{
						result = error("File not found: " + file.getPath());
					}
					else
					{
						try
						{
							result = new String(Files.readAllBytes(file.toPath()));
						}
						catch (IOException e)
						{
							result = error(e.getMessage());
						}
					}

				}
				break;
			case Uri.GETMETADATA:
				result = metadata;
				break;
			default:
				String message = String.format("Invalid discriminator: %s, Uri.List is %s", discriminator, Uri.LIST);
				//logger.warn(message);
				result = error(message);
				break;
		}
		return result;
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
					.vendor("http:/www.anc.org")
					.allow(Discriminators.Uri.ALL)
					.encoding("UTF-8")
					.format(Uri.LIF)
					.description("Generic DataSource")
					.license(Discriminators.Uri.APACHE2)
					.build();
			Data data = new Data();
			data.setDiscriminator(Discriminators.Uri.META);
			data.setPayload(md);
			metadata = data.asPrettyJson();
		}
		return metadata;
	}

	protected String[] addKey(String[] key) {
		logger.trace("Key: {} Path: {}", key[0], key[1]);
		keys.add(key[0]);
		index.put(key[0], new File(key[1]));
		return key;
	}

	protected void loadIndex(String path)
	{
		logger.debug("Loading the index from {}", path);
		keys = new ArrayList<>();
		try
		{
			Stream<String> stream = Files.lines(Paths.get(path));
			index = stream.map(s -> s.split(" +"))
					.map(a -> {keys.add(a[0]); return a;})
					.collect(Collectors.toMap(a->a[0], a->new File(a[1])));
			logger.debug("Index contains {} entries", index.size());
//			map.forEach((k,v) -> System.out.println(k + " = " + v));
		}
		catch (IOException e)
		{
			logger.error("Unable to load the index.", e);
			index = new HashMap<>();
		}
	}

	protected void _loadIndex(String path)
	{
		keys = new ArrayList<>();
		index = new HashMap<>();
		File file = new File(path);
		if (!file.exists()) {
			logger.error("Index index file does not exist. {}", path);
		}
		else {
			try(BufferedReader reader = new BufferedReader(new FileReader(file)))
			{
				String line = reader.readLine();
				while (line != null) {
					logger.trace("Parsing line: {}", line);
					String[] parts = line.split(" +");
					if (parts.length != 2) {
						logger.warn("Invalid line in index: {}", line);
					}
					else {
						keys.add(parts[0]);
						index.put(parts[0], new File(parts[1]));
					}
					line = reader.readLine();
				}
			}
			catch (IOException e)
			{
				logger.error("Error reading the index.", e);
			}
		}
//		try(Stream<String> stream = Files.lines(Paths.get(path)))
//		{
//			stream.map( s -> s.split(" "))
//					.map( this::addKey );
//					.collect(Collectors.toMap(a->a[0], a->new File(a[1])));
//		}
//		catch (IOException e) {
		//	 Initialize index to prevent NPEs
//			logger.error("Unable to load the index.", e);
//			index = new HashMap<>();
//			cachedError = error(e.getMessage());
//		}
		logger.debug("Index contains {} entries", index.size());
	}

	protected void dump() {
		keys.forEach(k -> {
			System.out.println(String.format("%s -> %s", k, index.get(k)));
		});
	}

	protected String error(String message)
	{
		return new Data<String>(Uri.ERROR, message).asPrettyJson();
	}
}
