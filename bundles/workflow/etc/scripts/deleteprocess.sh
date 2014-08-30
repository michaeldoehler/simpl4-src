#!/bin/zsh

curl -v  -Xget  "http://admin:admin@ea-dev.ms123.org:8075/testapp/workflow/deployment/${1}?cascade=true"
