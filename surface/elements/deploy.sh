#!/bin/sh
make-all.sh
gzip -f elements.html
scp elements.html.gz  ms:$simpl4first/client/surface/
scp elements.html.gz  ms:$fis/client/surface/
