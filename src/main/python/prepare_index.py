"""

Quick and dirty script to take the orignal BioASQ data and create resources for the Datasource.

"""

import os
import json
import codecs

JSON_FILE = '../resources/BioASQ-SampleDataB.json'
JSON_FILE = '../resources/BioASQ-trainingDataset6b.json'

INDEX1 = codecs.open('../resources/index-identifiers.txt', 'w', encoding="utf8")
INDEX2 = codecs.open('../resources/index-questions.txt', 'w', encoding="utf8")

json_obj = json.load(codecs.open(JSON_FILE))

count = 0
for question in json_obj['questions']:
    count += 1
    #if count > 100: break
    identifier = question['id']
    text = question['body']
    subdir = identifier[:2]
    subdir_path = "../resources/data/%s" % subdir
    if not os.path.exists(subdir_path):
        os.makedirs(subdir_path)
    bioasq_file1 = "src/main/resources/data/%s/%s.json" % (subdir, identifier)
    bioasq_file2 = "../resources/data/%s/%s.json" % (subdir, identifier)
    print bioasq_file1
    INDEX1.write("%s\t%s\n" % (identifier, bioasq_file1))
    INDEX2.write("%s\t%s\n" % (text, bioasq_file1))
    with codecs.open(bioasq_file2, 'w', encoding="utf8") as fh:
        json.dump(question, fh, indent=4)
