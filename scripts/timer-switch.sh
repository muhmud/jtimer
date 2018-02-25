#!/bin/bash

if [[ -f ~/.timer-status ]]; then
    statusLine="$(cat ~/.timer-status)"
    directory="$(awk -F ',' '{print $3}' <<< $statusLine)"

    if [[ "${statusLine:0:6}" == "PAUSED" ]]; then
        if [[ -f ~/.timer/timers ]]; then
            newDirectory=$(zenity --list --column="Timer" --column="Directory" $(names=""; while read t; do names="$names `basename $t` $t"; done < ~/.timer/timers;echo $names) --width=500 --height=400 --print-column=2 --text="Choose Timer:" --title="Switch Timer")
            if [[ ! -z "$newDirectory" && "$directory" != "$newDirectory" ]]; then
                if [[ -f "$newDirectory/.timer-status" ]]; then
                    rm -f "$directory/.timer-status"
                    cp ~/.timer-status "$directory/.timer-status"
                    
                    rm -f ~/.timer-status
                    cp "$newDirectory/.timer-status" ~/.

                    ~/bin/timer -i -y "$newDirectory"
                else
                    TASK_DESCRIPTION=$(zenity --entry --width=500 --title="Start Timer (`basename $newDirectory`)" --text="Enter Task Description:" --ok-label="Start")
                    if [[ ! -z $TASK_DESCRIPTION ]]; then
                        rm -f "$directory/.timer-status"
                        cp ~/.timer-status "$directory/.timer-status"

                        rm -f ~/.timer-status
                        ~/bin/timer -g "$TASK_DESCRIPTION" -y "$newDirectory"
                    fi
                fi
            fi
        fi
    fi
fi
