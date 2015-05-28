#!/bin/bash

VERSION="1.1"
NAME=management-api
cat >${NAME}.bnd  <<STARTBND
Bundle-Name                             ${NAME}
Bundle-SymbolicName                     ${NAME}
Bundle-Version                          ${VERSION}
Import-Package                          !*
Export-Package                          *;version="${VERSION}"
-noee=true 
STARTBND

bnd wrap -o ${NAME}-${VERSION}.bar -p ${NAME}.bnd management-api-1.1-rev-1.jar


rm -f ${NAME}.bnd
