# Bug backtracking tool
This directory contains all the necessary components needed to backtrack bugs
through git histories. This is mostly done by the `backtrack.jar` JAR file,
however there are also some helper scripts included to setup the project
backtracking.
# Usage
## Setup
### Defects4j
To backtrack a particular `Defects4j` project by pre-conceived line numbers, you
must have a defects4j-bugs.json file containing the projects information. To
extract the projects information from this file, use the `compile_bugs.py`
script as follows:
```
python3 compile_bugs.py > bug.csv
```
executed in the directory where the `defects4j-bugs.json` file is.
Once this is complete, seperate CSV files for each project can be manually
created.
TODO: change this to automatically create seperate CSV files.
### Other project repositories
TODO
## Create diffs
In order to run the backtracking, a full list of (*backwards*) git diffs must be available. To
achieve this, the script `gen_diffs.sh` is provided. To use this script, clone
the project reposity into `projects` and navigate to the projects base
directory. Then use the `gen_diffs.sh` script by typing:
```
../../gen_diffs.sh <initial SHA> <final SHA>
```
where `<inital SHA>` points to the most recent commit to include, and `<final SHA>`
points to the oldest commit to include.

## Defects4j diff fix
The Defects4j versions included in the Defects4j repository are unfortunately
not exactly the same as the base versions from the original projects. In order
to deal with this phenomenon, a diff between the actual base version and the
supplied Defects4j version needs to be created and inserted into the diff
history so that the backtracking algorithm can correctly backtrack the bugs into
earlier versions. This can be done automatically by running the
`d4j-reidentify.sh` script using the following format:
```
./d4j_reidentify.sh <project> <version> <source directory>
```
This must be done for each version in each project, so bash looping is suggested
here. This will place `start-<ver>.diff` diff files for each version in the diff
directory for each project located in `projects`.
## Run the backtracking
In order to run the backtracking, the `backtrack.jar` file has been provided
(with sources in the `backtrack` directory. To see the running instructions,
type:
```
java -jar backtrack.jar
```
The backtracking algorithm needs 4 things to run:
1. The SHA file `all_shas` produced by the `gen_diffs.sh` script
2. The `diffs` directory of all diffs to include also produced as above
4. The path to the source directory in the project (e.g. `src/main/java/`)
3. The `<project>.csv` file produced by the `compile_bugs.py` script (optional)
# Description of backtracking algorithm
The actual backtracking algorithm works by constructing a list of lines for the
bug fix that need to be backtracked to previous versions. It then considers each
backwards diff in the git history and applies the diffs to the lines for the
bug fix. For this process, if lines above the bug fixes tracked line is changed,
then the tracked line's line number is changed in accordance with the shift
incurred. If a tracked line in the bug fix is directly modified or deleted, then
that line is removed from tracking as it can no longer be assumed to be buggy.

If all of the lines in a tracked bug fix are removed, the bug fix no longer
contains any viable locations, and thus the backtracking fails for that diff. If
this does not occur, the bug fix will be backtracked until the oldest commit
specified, and at each stage in the git history the updated bug fix lines are printed
out.
