# GeneTagDatasource


DataSource service to give access to GeneTag data from the MedTag corpus. The GeneTag data include about 15K sentences from MedLine abstracts annotated with genes, those being the sentences used in the BIoCreative task. The paper on MedTag is

> Lawrence H. Smith, Lorraine Tanabe, Thomas Rindflesch and W. John Wilbur. 2005. *MedTag: a collection of biomedical annotations*. ISMB '05 Proceedings of the ACL-ISMB Workshop on Linking Biological Literature, Ontologies and Databases: Mining Biological Semantics. Pages 32-37. Detroit, Michigan, June 24, 2005.

This paper can be downloaded from [ResearchGate](https://www.researchgate.net/publication/234785358_MedTag_a_collection_of_biomedical_annotations).

MedTag itself can be downloaded from ftp://ftp.ncbi.nlm.nih.gov/pub/lsmith/MedTag/, the files from GeneTag that are used for the GeneTag data service are included in this repository at `src/main/resources/genetag`.

Before creating the service you need to create the data in the `src/main/resources/data` directory:

```
$ cd src/main/python
$ python3 create_data.py
```

Use Maven to create the datasource service:

```
$ mvn generate-resources
$ mvn clean package
```

The first line generates the files `src/main/java/org/anc/lapps/datasource/generic/Version.java` and `VERSION` using the version number found in the Maven POM file, and the second line compiles and creates the war archive target/GeneTagDatasource#VERSION.war that can be put on the LAPPS server.

Test the service with Jetty:

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
{ "discriminator": "http://vocab.lappsgrid.org/ns/action/get", "payload": "P00027739T0000" }
```

The response should be:

```json
{
  "discriminator" : "http://vocab.lappsgrid.org/ns/media/jsonld#lif",
  "payload" : {
    "@context" : "http://vocab.lappsgrid.org/context-1.0.0.jsonld",
    "metadata" : { },
    "text" : {
      "@value" : "Serum gamma glutamyltransferase in the diagnosis of liver disease in cattle.",
      "@language" : "en"
    },
    "views" : [ {
      "id" : "v1",
      "metadata" : {
        "contains" : {
          "http://vocab.lappsgrid.org/NamedEntity" : {
            "namedEntityCategorySet" : "tags-ner-biomedical",
            "producer" : "GeneTag Gold Data",
            "type" : null
          }
        }
      },
      "annotations" : [ {
        "id" : "ne0",
        "start" : 0,
        "end" : 31,
        "@type" : "http://vocab.lappsgrid.org/NamedEntity",
        "features" : {
          "category" : "GENE"
        }
      } ]
    } ]
  }
}
```
