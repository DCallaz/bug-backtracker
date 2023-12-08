#!/bin/bash
# Executed in a projects base directory (where the .git file is), and given the
# start and end commit SHA's, this script will produce an all_shas SHAFILE and
# a directory of relevant diffs.
get_merge_base() {
  if [ "$(echo $1 | awk 'NF >= 3 {print}')" ]; then
    echo "$1 $(git merge-base $(echo $1 | cut -d ' ' -f 2,3))"
  else
    echo "$1"
  fi
}
export -f get_merge_base

if [ "$1" ] && [ "$2" ]; then
  shas=$(git log --topo-order --format="%H %P" | awk "/^$1/,/^$2/" | xargs -I %% bash -c 'get_merge_base "%%"')
else
  shas=$(git log --topo-order --format="%H %P" | xargs -I %% bash -c 'get_merge_base "%%"')
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
IFS=$'\n'
for line in $shas; do
  sha="$(echo "$line" | cut -d ' ' -f 1)"
  #echo newer ${newer_shas[$i]} older ${older_shas[$i]}
  git show -l10000 --format="" $sha > diffs/$sha.diff
  echo $line >> all_shas
done
