#!/bin/bash

VERSION="1.8.5"
NAME=aspectjrt
cat >${NAME}.bnd  <<STARTBND
Bundle-ManifestVersion                  2                                       
Bundle-Name                             aspectjrt
Bundle-SymbolicName                     aspectjrt
Bundle-Version                          ${VERSION}
Export-Package                          org.aspectj.lang*;version="${VERSION}",org.aspectj.runtime*;version="${VERSION}"
STARTBND

bnd wrap -o ${NAME}-${VERSION}.bar -p ${NAME}.bnd ${NAME}-${VERSION}.jar
