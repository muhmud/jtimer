#!/bin/bash

if [[ -f ~/.timer-status ]]; then
    statusLine="$(cat ~/.timer-status)"
    directory="$(awk -F ',' '{print $3}' <<< $statusLine)"

    if [[ "${statusLine:0:6}" == "PAUSED" ]]; then
        TASK_DESCRIPTION=$(zenity --entry --width=500 --title="Change Task (`basename $directory`)" --text="Enter New Task Description:" --ok-label="Start")
        if [[ ! -z $TASK_DESCRIPTION ]]; then
            ~/bin/timer -i "$TASK_DESCRIPTION" -y "$directory"
        fi
    fi
fi
