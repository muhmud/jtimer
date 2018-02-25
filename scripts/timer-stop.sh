#!/bin/bash

if [[ -f ~/.timer-status ]]; then
    statusLine="$(cat ~/.timer-status)"
    directory="$(awk -F ',' '{print $3}' <<< $statusLine)"

    if [[ "${statusLine:0:7}" != "STOPPED" ]]; then
        ~/bin/timer -s -y "$directory"
        rm -f "$directory/.timer-status"
    else
        if [[ -f ~/.timer/timers ]]; then
            directory=$(zenity --list --column="Timer" --column="Directory" $(names=""; while read t; do names="$names `basename $t` $t"; done < ~/.timer/timers;echo $names) --width=500 --height=400 --print-column=2 --text="Choose Timer:" --title="Timer")
            if [[ ! -z $directory ]]; then
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
    fi
fi
