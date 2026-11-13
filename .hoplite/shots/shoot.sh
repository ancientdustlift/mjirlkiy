#!/usr/bin/env bash
# usage: shoot.sh <width> <height> <hash>...
cd /tmp/hoplite/workspace
w=$1; h=$2; shift 2
rm -f .hoplite/shots/s_*.png
for x in "$@"; do
  google-chrome --headless=new --no-sandbox --disable-gpu --hide-scrollbars --virtual-time-budget=1500 \
    --window-size=$w,$h --screenshot=.hoplite/shots/s_$x.png "file:///tmp/hoplite/workspace/${PAGE:-neck-exercises.html}#$x" >/dev/null 2>&1
done
python3 .hoplite/shots/montage.py
