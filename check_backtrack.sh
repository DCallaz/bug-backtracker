#!/bin/bash
red=`tput setaf 1`
green=`tput setaf 2`
yellow=`tput setaf 3`
reset=`tput sgr0`
USAGE="./check_backtrack.sh <log dir> <project>"
if [ $# -lt 2 ]; then
  echo "$USAGE"
  exit 0
fi
log_dir="$1"
project="$2"
backtrack_file="$log_dir/${project}_backtrack.json"
sha_file="$log_dir/${project}_shas.csv"
srcs=($(cat "$log_dir/${project}_src.txt" | grep "^--include" | cut -d ' ' -f 2))
for i in "${!srcs[@]}"; do
  srcs[$i]="${srcs[$i]}/"
done
proj_dir="projects/$project"
curr_dir="$PWD"
num_bugs="$(grep "\"bug\"" "$backtrack_file" | wc -l)"
shas=( "" $(cat "$sha_file") )
for ((bug=1; bug <= $num_bugs; bug++)); do
  bug_lines=()
  for ((version=$bug; version <= $num_bugs; version++)); do
    #echo "Trying bug $bug in version $version"
    backtrack="$(python3 backtrack.py "$backtrack_file" "$bug" "$version")"
    if [[ "$backtrack" =~ "Bug not found:"* ]]; then
      echo "${yellow}Bug $bug failed in version $version${reset}"
      break
    else
      cd "$proj_dir"
      for file in $backtrack; do
        filename="$(echo "$file" | cut -d ',' -f 1)"
        lines=($(echo "$file" | cut -d ',' -f 1 --complement --output-delimiter ' '))
        for line in "${lines[@]}"; do
          if [ "$bug" == "$version" ]; then
            bline="$(git show "${shas[$bug]}^:${srcs[0]}$filename" 2> "/tmp/err" | sed "${line}q;d")" 
            if [[ "$(cat "/tmp/err")" =~ "fatal:"* ]]; then
              bline="$(git show "${shas[$bug]}^:${srcs[1]}$filename" | sed "${line}q;d")"
            fi
            bug_lines+=("$bline") 
          else
            vline="$(git show "${shas[$version]}^:${srcs[0]}$filename" 2> "/tmp/err" | sed "${line}q;d")"
            if [[ "$(cat "/tmp/err")" =~ "fatal:"* ]]; then
              vline="$(git show "${shas[$version]}^:${srcs[1]}$filename" 2> "/tmp/err" | sed "${line}q;d")"
              if [[ "$(cat "/tmp/err")" =~ "fatal:"* ]]; then
                echo "${red}ERROR: Version $version does not contain $filename for bug $bug"
                cd "$curr_dir"
                continue 3
              fi
            fi
            found=0
            for bline in "${bug_lines[@]}"; do
              if [ "$vline" == "$bline" ]; then
                found=1
              fi
            done
            if [ $found -eq 0 ]; then
              echo "${red}ERROR: Bug $bug in version $version has differing line $filename $line${reset}"
              echo "Line: $vline"
              for bline in "${bug_lines[@]}"; do
                echo "Bug line: $bline"
              done
              cd "$curr_dir"
              continue 3
            fi
          fi
        done
      done
      echo "${green}Bug $bug fine in version $version${reset}"
      cd "$curr_dir"
    fi
  done
done
