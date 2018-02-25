#!/bin/bash

if [[ -f ~/.timer-status ]]; then
    statusLine="$(cat ~/.timer-status)"
    directory="$(awk -F ',' '{print $3}' <<< $statusLine)"

    if [[ "${statusLine:0:7}" != "STOPPED" ]]; then
        if [[ "${statusLine:0:7}" == "RUNNING" ]]; then
            ~/bin/timer -p -y "$directory"
        else
            ~/bin/timer -i -y "$directory"
        fi
    else
        if [[ -f "$director/.timer-status" ]]; then
            rm -f ~/.timer-status
            cp "$director/.timer-status" ~/.timer-status

            ~/bin/timer -i -y "$directory"
        else
            TASK_DESCRIPTION=$(zenity --entry --width=500 --title="Start Timer (`basename $directory`)" --text="Enter Task Description:" --ok-label="Start")
            if [[ ! -z $TASK_DESCRIPTION ]]; then
                ~/bin/timer -g "$TASK_DESCRIPTION" -y "$directory"
            fi
        fi
    fi
fi
