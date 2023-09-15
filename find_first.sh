#!/bin/bash
for sha in $(cat temp); do
  echo $(git log | grep -n "$sha" | cut -d ":" -f 1),$sha;
done > out.csv
