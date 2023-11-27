#!/bin/bash
# Executed in a projects base directory (where the .git file is), and given the
# start and end commit SHA's, this script will produce an all_shas SHAFILE and
# a directory of relevant diffs.
if [ "$1" ] && [ "$2" ]; then
  shas=$(git log --format="%H" | awk "/$1/,/$2/;/$2/{getline;print;}")
else
  shas=$(git log --format="%H")
fi
#echo "$shas"
newer_shas=($shas)
older_shas=("${newer_shas[@]}")
#unset 'older_shas[0]'
#unset 'newer_shas[${#newer_shas[@]}-1]'
older_shas=("${newer_shas[@]:1:${#newer_shas[@]}}")
newer_shas=("${newer_shas[@]::${#newer_shas[@]}-1}")
mkdir diffs
> all_shas
for i in $(seq 0 $( expr ${#newer_shas[@]} - 1 )); do
  #echo newer ${newer_shas[$i]} older ${older_shas[$i]}
  git diff -l10000 ${newer_shas[$i]} ${older_shas[$i]} > diffs/${newer_shas[$i]}.diff
  echo ${newer_shas[$i]} >> all_shas
done
