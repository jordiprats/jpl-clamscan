import clam.ClamScan

void call(String image) {
  def execute_scan = sh(
          returnStatus: true,
          script: """
                      set -x

                      TMPDIR=\044(mktemp -ud /tmp/scandir.XXXXXXXXXXXXXXXXXXXXXXX)

                      mkdir -p \044{TMPDIR}

                      docker save ${image} > \044{TMPDIR}/docker.tar

                      # extract layers
                      tar xvf \044{TMPDIR}/docker.tar -C \044{TMPDIR}

                      # extract layer's contents
                      find \044{TMPDIR} -type f -name "*.tar" -exec tar xvf {} -C \044{TMPDIR} \\;

                      CLAMDBVOL=\044(mktemp -d clamdb_XXXXXXX)

                      docker volume create \044{CLAMDBVOL}

                      docker run -it --rm --mount source=\044{CLAMDBVOL},target=/var/lib/clamav clamav/clamav:unstable_base sh -c "sed -i -e 's/^NotifyClamd.*//' /etc/clamav/freshclam.conf; grep -v '^#' /etc/clamav/freshclam.conf; freshclam"

                      docker run -it --rm --mount source=\044{CLAMDBVOL},target=/var/lib/clamav --mount type=bind,source=\044{TMPDIR},target=/scandir clamav/clamav:latest clamscan -r /scandir

                      docker volume rm \044{CLAMDBVOL}

                      if [ \044? -eq 0 ]; then
                        echo "== Scan OK"
                        EXIT_VAL=0ku 
                      else
                        echo "== Scan KO"
                        EXIT_VAL=1
                      fi

                      rm -fr "\044{TMPDIR}" 
                      exit \044{EXIT_VAL} """.stripIndent())

  if(execute_scan) {
      throw new Exception("clamscan - please check scan report")
  }
}