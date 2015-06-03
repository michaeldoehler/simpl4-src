#!/bin/bash

VERSION="1.13"
NAME=snakeyaml
cat >${NAME}.bnd  <<STARTBND
DynamicImport-Package: *
STARTBND

bnd wrap -o ${NAME}-${VERSION}.bar -p ${NAME}.bnd ${NAME}-${VERSION}.jar


rm -f ${NAME}.bnd
