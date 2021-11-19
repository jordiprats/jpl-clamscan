#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <path>"
  exit 1
fi

TMPDIR=$(mktemp -ud /tmp/scandir.XXXXXXXXXXXXXXXXXXXXXXX)

mkdir -p $TMPDIR

docker save "$1" > $TMPDIR/docker.tar

# extract layers
tar xvf $TMPDIR/docker.tar -C $TMPDIR

# extract layer's contents
find $TMPDIR -type f -name "*.tar" -exec tar xvf {} -C $TMPDIR \;

CLAMDBVOL=$(mktemp -d clamdb_XXXXXXX)

docker volume create $CLAMDBVOL

docker run -it --rm --mount source=${CLAMDBVOL},target=/var/lib/clamav clamav/clamav:unstable_base sh -c "sed -i -e 's/^NotifyClamd.*//' /etc/clamav/freshclam.conf; grep -v '^#' /etc/clamav/freshclam.conf; freshclam"

docker run -it --rm --mount source=${CLAMDBVOL},target=/var/lib/clamav --mount type=bind,source=${TMPDIR},target=/scandir clamav/clamav:latest clamscan -r /scandir

docker volume rm $CLAMDBVOL

if [ $? -eq 0 ]; then
  echo "== Scan OK"
else
  echo "== Scan KO"
fi

rm -fr "${TMPDIR}"