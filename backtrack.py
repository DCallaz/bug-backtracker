import json
import sys


def get_backtrack(backtracks_file, bugId, version):
    f = open(backtracks_file)
    backtrack = json.load(f)
    bugFix = None
    for bug in backtrack:
        if (bugId in bug["bug"].keys()):
            if (bugId == version):
                bugFix = bug["bug"]
            elif (version in bug):
                bugFix = bug[version]
            break
    if (bugFix is not None):
        if (bugFix == "failed"):
            bugFix = None
        else:
            bugFix = bugFix[bugId]
    return bugFix


if __name__ == "__main__":
    usage = ("USAGE: python3 backtrack.py <backtracks file> <bugId> <version>")
    if (len(sys.argv) < 4):
        print(usage)
        quit()
    backtracks_file = sys.argv[1]
    bugId = sys.argv[2]
    version = sys.argv[3]
    bugFix = get_backtrack(backtracks_file, bugId, version)
    if (bugFix is None):
        print("Bug not found: failed to apply backtrack")
    else:
        for file in bugFix:
            print(file, ','.join(map(str, bugFix[file])), sep=',')
