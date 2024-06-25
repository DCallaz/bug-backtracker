# Takes a defects4j-bug.json file and outputs a bugs.csv format file (by piping)

import json
import csv

def toString(l):
    ret = ""
    for element in l:
        ret += str(element) + ":"
    return ret[:-1]

projects = {}
jobj = json.load(open("defects4j-bugs.json"))
for bug in jobj:
    if bug["project"] not in projects:
        projects[bug["project"]] = []
    projects[bug["project"]].append(bug)
for proj in projects.keys():
    proj_file = open("{}.csv".format(proj), 'w')
    project = projects[proj]
    print(proj, len(project), sep=',', file=proj_file)
    project.sort(key=lambda x: x["bugId"])
    #proj_shafile = csv.reader(open(proj+"_shas.csv"), delimiter=',')
    for i in range(0, len(project)):
        assert(project[i]["bugId"] == i+1)
        print(project[i]["bugId"], project[i]["revisionId"], sep=',', file=proj_file)
        for file in project[i]["changedFiles"].keys():
            diffs = project[i]["changedFiles"][file]
            lines = set()
            if "changes" in diffs:
                for x in diffs["changes"]:
                    if -1 not in x:
                        lines.update(x)
            if "inserts" in diffs:
                for x in diffs["inserts"]:
                    if -1 not in x:
                        lines.update(x)
            if "deletes" in diffs:
                for x in diffs["deletes"]:
                    if -1 not in x:
                        lines.update(x)
            print("#", file, toString(sorted(lines)), sep=',', file=proj_file)
