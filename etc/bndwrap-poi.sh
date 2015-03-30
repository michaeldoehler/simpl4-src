#!/bin/bash


rm -f poi.bnd
cat >poi.bnd  <<STARTBND
Bundle-ManifestVersion                 2
Bundle-SymbolicName                  poi
Bundle-Name                  				 poi
Bundle-Version                       3.8beta4
Export-Package:                     org.apache.poi*
STARTBND

bnd wrap -output /tmp/poi-3.8beta4-all.bar -properties poi.bnd poi-3.8beta4-all.bar

mv /tmp/poi-3.8beta4-all.bar .
