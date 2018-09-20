"""create_data.py

Quick and dirty script to take the orignal GENETAG data and create resources for
the Datasource.

Usage:

    $ python3 create_data.py

This script assumes that GENETAG data from the MedTag data download ftp site at
ftp://ftp.ncbi.nlm.nih.gov/pub/lsmith/MedTag/ are available in ../resources. In
particular, it expects the following files to be there (variables GENETAG_SENTS
and GENETAG_GOLD references those files):

    genetag/genetag.sent
    genetag/Gold.format

Data files are written to ../resources/data. There is a file for each sentence,
an example is (original does not have the indentation, it alos has the sentence
on one line):

    Yeast Gal11 protein mediates the transcriptional activation signal of two
    different transacting factors, Gal4 and general regulatory factor
    I/repressor/activator site binding protein 1/translation upstream factor.

    0	19	Yeast Gal11 protein
    105	109	Gal4
    114	141	general regulatory factor I
    142	184	repressor/activator site binding protein 1
    185	212	translation upstream factor

The original offsets in Gold.format were a bit peculiar in that they did not
refer to actual offsets in genetag.sent. Instead the offsets were only counting
non-space characters. The code in this script adjusts the offsets.

"""

import os
import codecs

GENETAG_SENTS = '../resources/genetag/genetag.sent'
GENETAG_GOLD = '../resources/genetag/Gold.format'


def read_data():
    sentences = {}
    sentence_id = None

    count = 0
    for line in open(GENETAG_SENTS):
        count += 1
        line = line.strip()
        if line.isalnum():
            sentence_id = line
        else:
            sentences[sentence_id] = (line, [])

    for line in open(GENETAG_GOLD):
        (sent_id, offsets, text) = line.strip().split('|')
        if sent_id in sentences:
            sentences[sent_id][1].append((offsets, text))
        else:
            print("WARNING: no sentence for %s" % sent_id)

    return sentences


def print_data(sentences):
    count = 0
    data_dir = os.path.join('..', 'resources', 'data')
    for s in sentences:
        sent = sentences[s][0]
        annotations = sentences[s][1]
        count += 1
        if count % 100 == 0: print(count)
        #if count > 1000: break
        subdir1 = s[:3]
        subdir2 = s[:4]
        path = os.path.join(data_dir, subdir1, subdir2)
        fname = os.path.join(path, s)
        if not os.path.exists(path):
            os.makedirs(path)
        with codecs.open(fname, 'w', encoding='utf8') as fh:
            fh.write(sent + u"\n\n")
            for annotation in annotations:
                offsets = annotation[0]
                text = annotation[1]
                p1, p2 = [int(p) for p in offsets.split()]
                p1a = adjust_offset(sent, p1)
                p2a = adjust_offset(sent, p2) + 1
                if sent[p1a:p2a] != text:
                    print("WARNING: incorrect offset calculation for %s" % s)
                    print("         %s %s %s" % (p1, p2, text))
                    print("         %s %s %s" % (p1a, p2a, sent[p1a:p2a]))
                    # This happens only once, not sure why, but we fix it here
                    # in a hackish way
                    if s == 'P02196565T0000' and p1a == 184:
                        p1a += 1
                        print("         %s %s %s" % (p1a, p2a, sent[p1a:p2a]))
                fh.write("%s\t%s\t%s\n" % (p1a, p2a, annotation[1]))


def adjust_offset(sentence, offset):
    adjusted = 0
    if offset == 0:
        return adjusted
    for i, c in enumerate(sentence):
        adjusted += 1
        if c != ' ':
            offset += -1
        if offset == 0:
            if sentence[i+1] == ' ':
                adjusted += 1
            return adjusted


def print_list(sentences):
    fname = os.path.join('..', 'resources', 'data', 'sentences.txt')
    with codecs.open(fname, 'w', encoding='utf8') as fh:
        for s in sentences:
            fh.write(s + u"\n")



if __name__ == '__main__':
    sentences = read_data()
    print_data(sentences)
    print_list(sentences)
