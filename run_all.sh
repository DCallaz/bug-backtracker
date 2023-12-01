#!/bin/bash
USAGE="./run_all.sh [-d <dataset (bugsinpy/defects4j)>] [-s <save dir>] [-f <save file suffix>] [-m diffs/lines] <projects file>"
dataset="bugsinpy"
savefile="backtrack_2"
mode="diffs"
while getopts ":hd:s:f:m:" opt; do
  case ${opt} in
    d )
      dataset="$OPTARG"
      ;;
    s )
      savedir="$OPTARG"
      ;;
    f )
      savefile="$OPTARG"
      ;;
    m )
      mode="$OPTARG"
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
if [ ! "$savedir" ]; then
  savedir="$dataset"
fi
projects_file="$1"
projects_file="$(readlink -f "$(echo ${projects_file/"~"/~})")"
if [ $# -lt 1 ] || [ ! -f "$projects_file" ]; then
  echo "$USAGE"
  echo "Please provide a valid projects file"
  exit 0
fi
if [ "$dataset" == "defects4j" ]; then
  file_type=".java"
elif [ "$dataset" == "bugsinpy" ]; then
  file_type=".py"
else
  echo "ERROR: unknown dataset: $dataset"
  exit 1
fi
for project in $(cat "$projects_file"); do
  echo "$project"
  all_shas="projects/$project/all_shas"
  diffs="projects/$project/diffs"
  bug_shas="$savedir/${project}_shas.csv"
  out="$savedir/${project}_$savefile.json"
  if [ "$mode" == "lines" ]; then
    shas="$savedir/${project}.csv"
  elif [ "$mode" == "diffs" ]; then
    shas="$savedir/${project}_shas.csv"
  else
    echo "ERROR: unknown mode: $mode"
    exit 1
  fi
  java -jar backtrack.jar --file-type $file_type $(< $savedir/${project}_src.txt) $mode $all_shas $diffs $shas > $out
done
