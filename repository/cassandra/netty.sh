#!/bin/bash

VERSION="4.0.28"
NAME=netty-all
cat >${NAME}.bnd  <<STARTBND
Bundle-Name                             ${NAME}
Bundle-SymbolicName                     ${NAME}
Bundle-Version                          ${VERSION}
Import-Package                          *;resolution:=optional
Export-Package                          *;version="${VERSION}"
-noee=true 
STARTBND

bnd wrap -o ${NAME}-${VERSION}.bar -p ${NAME}.bnd ${NAME}-${VERSION}.jar


rm -f ${NAME}.bnd
