#!/bin/sh

all="rpc \
libhelper \
git \
store \
auth.api \
nucleus.api \
setting.api \
entity.api \
team.api \
permission.api \
stencil.api \
system \
data.api \
workflow.api \
utils \
reporting \
namespace \
auth \
permission \
datamapper \
docbook \
camel \
enumeration \
entity \
domainobjects \
form \
activiti \
jetty \
data \
exporting \
importing \
management \
message \
nucleus \
setting \
smtp \
team \
workflow \
ea \
bhs"

dir=`pwd`
rm -f $dir/compile.log

for b in $all
do
	echo $dir/$b
	cd $dir/$b
  gradle -q >>$dir/compile.log
done
