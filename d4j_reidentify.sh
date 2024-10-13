#!/bin/bash
# Script to shift the defects4j line identification so that it aligns with the
# numbers in the actual diffs.
USAGE="USAGE: ./d4j_reidentify.sh [-v <version>] [-s <source path>] <project>"
src=""
while getopts ":hv:s:" opt; do
  case ${opt} in
    v )
      versions="$OPTARG"
      ;;
    s )
      src="$OPTARG"
      ;;
    h )
      echo "$USAGE"
      exit 0
      ;;
    \? )
      echo "$USAGE"
      exit 0
      ;;
  esac
done
shift $((OPTIND -1))
if [ $# -lt 1 ]; then
  echo "Please provide a project"
  echo "$USAGE"
  exit 0
fi
project=$1
if [ "$versions" == "" ]; then
  versions="$(defects4j bids -p "$project")"
fi
for version in $versions; do
  echo "Version: $version"
  defects4j checkout -p "$project" -v "${version}b" -w "$project-${version}b" &> /dev/null
  defects4j checkout -p "$project" -v "${version}f" -w "$project-${version}f" &> /dev/null
  sha="$(grep "^$version," defects4j/$project.csv | cut -d ',' -f 2)"
  cd "$project-${version}f"
  patch -R -p1 < "../projects/$project/diffs/$sha.diff" &> /dev/null
  cd ../
  #mkdir "temp_diffs"
  diff -Nru "$project-${version}f/$src" "$project-${version}b/$src" > "projects/$project/diffs/start-$version.diff"
  #cp "projects/$project/diffs/$sha.diff" "temp_diffs/$sha.diff"
  #echo "$sha" > temp_shas
  #echo "temp" >> temp_shas
  #echo "$project,2" > temp.csv
  #awk "/^[^#]*,/{flag=0}/^$version,/{flag=1}flag" "$project.csv" >> temp.csv
  #echo "2,temp" >> temp.csv
  #echo "#,temp" >> temp.csv
  #java -cp backtrack Backtrack lines temp_shas temp_diffs temp.csv "$src" > temp.json
  #python3 -c "import json;js=json.load(open('temp.json'));print(js[0]['2']['$version'])"
  #rm temp.json
  #rm temp_shas
  #rm temp.csv
  #rm -rf temp_diffs
  rm -rf "$project-${version}b"
  rm -rf "$project-${version}f"
done
