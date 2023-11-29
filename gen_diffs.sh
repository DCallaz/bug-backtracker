#!/bin/bash
# Executed in a projects base directory (where the .git file is), and given the
# start and end commit SHA's, this script will produce an all_shas SHAFILE and
# a directory of relevant diffs.
if [ "$1" ] && [ "$2" ]; then
  shas=$(git log --format="%H" | awk "/$1/,/$2/")
else
  shas=$(git log --format="%H")
fi
#echo "$shas"
#newer_shas=($shas)
#older_shas=("${newer_shas[@]}")
#unset 'older_shas[0]'
#unset 'newer_shas[${#newer_shas[@]}-1]'
#older_shas=("${newer_shas[@]:1:${#newer_shas[@]}}")
#newer_shas=("${newer_shas[@]::${#newer_shas[@]}-1}")
if [ -d diffs ]; then
  rm -r diffs/
fi
mkdir diffs
> all_shas
for sha in $shas; do
  #echo newer ${newer_shas[$i]} older ${older_shas[$i]}
  #git show -R --src-prefix="b/" --dst-prefix="a/" -l10000 --format="" $sha > diffs/$sha.diff
  git show -R -l10000 --format="" $sha > diffs/$sha.diff
  echo $sha >> all_shas
done
