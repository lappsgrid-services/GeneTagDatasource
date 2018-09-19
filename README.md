# GeneTagDatasource


DataSource service to give access to GeneTag data from the MedTag corpus. MedTag can be downloaded from ftp://ftp.ncbi.nlm.nih.gov/pub/lsmith/MedTag/, the paper on MedTag is

> Lawrence H. Smith, Lorraine Tanabe, Thomas Rindflesch and W. John Wilbur. 2005. *MedTag: a collection of biomedical annotations*. ISMB '05 Proceedings of the ACL-ISMB Workshop on Linking Biological Literature, Ontologies and Databases: Mining Biological Semantics. Pages 32-37. Detroit, Michigan, June 24, 2005.

The paper can be downloaded from [ResearchGate](https://www.researchgate.net/publication/234785358_MedTag_a_collection_of_biomedical_annotations).

Before creating the service you need to create the data in the `src/main/resources` directory:

```
$ cd src/main/python
$ python3 create_data.py
```

This creates the data that will be included in the web service's war file.

To create and test the datasource service do the following (this assumes you have Maven installed):

```
$ mvn generate-resources
$ mvn clean package
```

The first line generates the files `src/main/java/org/anc/lapps/datasource/generic/Version.java` and `VERSION` using the version number it found in the Maven POM file, and the second line compiles and creates the war archive target/GeneTagDatasource#VERSION.war that can be put on the LAPPS server.

You can test the service with Jetty:

```
mvn jetty:run
```

Connect to the site at http://localhost:8080/genetag-datasource/jsServices. At that point you will see a simple page where the content includes the following.

<ul>
    <li>GeneTagDatasource</li>
    <ul>
        <li>interfaces</li>
        <ul>
            <li>DataSource</li>
            <ul>
                <li>String execute(String) [sample] +</li>
                <li>String getMetadata() [sample] +</li>
            </ul>
        </ul>
        </ul>
</ul>

Expand the plus next to execute(String), paste some JSON text into the text area and press the invoke link. You cannot just put any JSON in there, it needs to be the JSON serialization of a data container with discriminator and payload attributes, for testing you can paste in the following text (note that it needs to be on one line).

```
{ "discriminator": "http://vocab.lappsgrid.org/ns/action/get", "payload": "XXXXX" }
```

The response should include "XXXXX", see [here](/src/site/payload.md) for the full response.
