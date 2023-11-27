#!/bin/bash
USAGE="./svn_to_git_shas.sh <project> <sha file> [<columns in cut format>]"
if [ $# -lt 2 ]; then
  echo "$USAGE"
  exit 0
fi
project="$1"
shas_file="$2"
columns="$3"
new_shas="$(cat "$shas_file")"
if [[ "$shas_file" =~ .*\.csv$ ]]; then
  if [ "$columns" ]; then
    shas="$(cat $shas_file | cut -d ',' -f $columns --output-delimiter=' ')"
  else
    shas="$(cat $shas_file | tr ',' ' ')"
  fi
else
  shas="$(cat $shas_file)"
fi
cd "projects/$project/"
for sha in $shas; do
  new_sha=$(git log --grep="trunk@$sha " --format="%H")
  if [ "$new_sha" ]; then
    #new_shas="$(echo "$new_shas" | sed "s~\(,\|^\)$sha\(,\|$\)~\1$new_sha\2~g")"
    for col in $(echo "$columns" | tr ',' ' '); do
      new_shas="$(echo "$new_shas" | awk -v S="$sha" -v R="$new_sha" -v COL="$col" 'BEGIN{OFS=FS=","}{if ($COL == S) $COL=R}1')"
    done
  fi
done
echo "$new_shas"
