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
        if [ "$bug" == "$version" ]; then
          # Get the full file
          if [ -f "/tmp/err" ]; then
            rm "/tmp/err"
          fi
          full_file="$(git show "${shas[$bug]}^:${srcs[0]}$filename" 2> "/tmp/err")"
          if [[ "$(cat "/tmp/err")" =~ "fatal:"* ]]; then
            full_file="$(git show "${shas[$bug]}^:${srcs[1]}$filename")"
          fi
          # check for start diff
          if [ -f "diffs/start-$bug.diff" ]; then
            tmp_file="$(mktemp)"
            echo "$full_file" > "$tmp_file"
            # get the relevant diff
            perl_re='m{^diff.*\Q'"$filename"'\E}...m{^diff} and !m{^diff(?!.*\Q'"$filename"'\E)} and print'
            diff="$(perl -ne "$perl_re" "diffs/start-$bug.diff")"
            if [ -n "$diff" ]; then
              full_file="$(patch -s -N -o - -r - "$tmp_file" <(echo "$diff") 2> /dev/null)"
            fi
            rm "$tmp_file"
          fi
        fi
        # Get lines
        lines=($(echo "$file" | cut -d ',' -f 1 --complement --output-delimiter ' '))
        for line in "${lines[@]}"; do
          if [ "$bug" == "$version" ]; then
            bline="$(echo "$full_file" | sed "${line}q;d")"
            bug_lines+=("$bline")
          else
            if [ -f "/tmp/err" ]; then
              rm "/tmp/err"
            fi
            vline="$(git show "${shas[$version]}^:${srcs[0]}$filename" 2> "/tmp/err" | sed "${line}q;d")"
            if [[ "$(cat "/tmp/err")" =~ "fatal:"* ]]; then
              rm "/tmp/err"
              vline="$(git show "${shas[$version]}^:${srcs[1]}$filename" 2> "/tmp/err" | sed "${line}q;d")"
              if [[ "$(cat "/tmp/err")" =~ "fatal:"* ]]; then
                rm "/tmp/err"
                echo "${red}ERROR: Version $version does not contain $filename for bug $bug${reset}"
                cd "$curr_dir"
                continue 3
              fi
            fi
            found=0
            fuzzy_val=0
            fuzzy_line=""
            for bline in "${bug_lines[@]}"; do
              if [ "$vline" == "$bline" ]; then
                found=1
                break
              else
                cur_fuzzy="$(java -cp "$curr_dir/backtrack" LCS "$vline" "$bline")"
                if [ "$cur_fuzzy" -ge 70 ] && [ "$cur_fuzzy" -gt "$fuzzy_val" ]; then
                  fuzzy_val="$cur_fuzzy"
                  fuzzy_line="$bline"
                fi
              fi
            done
            # Check if fuzzy found
            if [ $found -eq 0 ] && [ $fuzzy_val -gt 0 ]; then
                echo "${yellow}Fuzzy match for bug $bug line $filename $line ($fuzzy_val):${reset}"
                echo "Line: $vline"
                echo "Bug line: $fuzzy_line"
            elif [ $found -eq 0 ]; then
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
      if [ "$bug" == "$version" ]; then
        echo "Bug $bug Lines:"
        for bline in "${bug_lines[@]}"; do
          echo "$bline"
        done
        echo "-------------------- End --------------------"
      fi
      echo "${green}Bug $bug fine in version $version${reset}"
      cd "$curr_dir"
    fi
  done
done
