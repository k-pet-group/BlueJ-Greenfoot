#! /bin/bash

any_errors=0
# Check only files modified in last 10 Git commits.  This is a rough heuristic that works
# fine for fairly linear merge requests, see full explanation here: 
# https://stackoverflow.com/questions/2221658/whats-the-difference-between-head-and-head-in-git
for java_file in $(git diff --name-only HEAD~10..HEAD . | grep src.*java\$)
do
    # Get the year of that file's latest commit and search for Copyright.*YYYY in the file:
    if [ -f "$java_file" ] && ! grep -q Copyright.*$(git log -1 --format=%ci "$java_file" | cut -c1-4) "$java_file"
    then
        echo "$(basename "$java_file" .java)" \(located in "$java_file"\) needs copyright year updating
        any_errors=1
    fi
done

exit $any_errors

