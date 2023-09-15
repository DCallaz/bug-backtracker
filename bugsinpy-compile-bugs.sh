#!/bin/bash
dir=$(bugsinpy-info -p pandas | grep "^Base dir" | cut -d ':' -f 2 | xargs)
projects=($(ls $dir/projects))
for project in "${projects[@]}"; do
#for project in thefuck; do
  bugs=$(bugsinpy-info -p $project | grep "Number of bugs" | cut -d ':' -f 2 | xargs)
  echo "$project,$bugs"
  for (( bug=1; bug<=$bugs; bug++ )); do
    buggy_sha=$(bugsinpy-info -p $project -i $bug | awk '/Buggy id/{getline; print $1}')
    #fixed_sha=$(bugsinpy-info -p $project -i $bug | awk '/Revision id/{getline; print $1}')
    echo $buggy_sha
  done
done
