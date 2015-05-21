#!/bin/sh
make-all.sh
gzip -f domelements.html
scp domelements.html.gz  ms:$simpl4first/client/surface/
#scp elements.html.gz  ms:$web/client/surface/
