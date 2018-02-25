#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ -f ~/.timer-status ]]; then
  timing="$($DIR/timer-status)"
  statusLine="$(cat ~/.timer-status)"  

  if [[ "$timing" != "" ]]; then
      if [[ "${statusLine:0:7}" == "RUNNING" ]]; then
          echo " ${timing% - *} - %{F#f00}${timing##* - }%{F-}"
      else
          echo "${timing% - *} - %{F#0f0}${timing##* - }%{F-}"
      fi
  else
      echo "";
  fi
else
    echo "";
fi
