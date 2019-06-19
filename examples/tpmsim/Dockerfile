FROM debian:stretch

ENV BUILD_DIR /build
RUN mkdir ${BUILD_DIR}

RUN apt-get update -qq && apt-get install -qq git checkinstall \
  # libtpms dependencies
  automake autoconf libtool make gcc libc-dev libssl-dev
RUN git clone https://github.com/stefanberger/libtpms.git
RUN cd libtpms && ./autogen.sh --prefix=/usr/local --with-openssl --with-tpm2 && make -j4 && \
  # Install libtpms to BUILD_DIR and the real user directory (for swtpm build)
  checkinstall --fstrans=no --pkgname=libtpms -y && cp libtpms*.deb ${BUILD_DIR}

RUN apt-get update -qq && apt-get install -qq gawk libseccomp-dev \
  # swtpm dependencies
  automake autoconf bash coreutils expect libtool sed fuse libfuse-dev glib2.0 glib2.0-dev \
  # Removed selinux-policy-dev because of build error
  net-tools python3 python3-twisted trousers tpm-tools gnutls-bin gnutls-dev \
  libtasn1-6 libtasn1-6-dev build-essential devscripts equivs socat
RUN git clone https://github.com/stefanberger/swtpm.git && cd swtpm && \
  ./autogen.sh && make -j4 && checkinstall --fstrans=no --pkgname=swtpm -y && cp swtpm*.deb ${BUILD_DIR}

# tpm2d dependencies
RUN apt-get update -qq && apt-get install -qq protobuf-c-compiler libprotobuf-c-dev re2c check
# install libtss and copy/symlink so files
RUN git clone https://git.code.sf.net/p/ibmtpm20tss/tss ibmtpm20tss-tss && cd ibmtpm20tss-tss && \
  git checkout v1331 && cd utils && make && cp libibmtss*.so /usr/local/lib && ldconfig && \
  # make ibmtss headers available
  cp -r /ibmtpm20tss-tss/utils/ibmtss /usr/include && \
  # Copy so files/symlinks to BUILD_DIR
  mkdir -p ${BUILD_DIR}/local/lib && cp libibmtss*.so ${BUILD_DIR}/local/lib
# install protobuf-c-text
RUN git clone https://github.com/trustm3/external_protobuf-c-text.git && cd external_protobuf-c-text && \
  git checkout master && autoreconf -f -i && \
  CFLAGS="-pedantic -Wall -Wextra -Werror -O2 -DHAVE_PROTOBUF_C_MESSAGE_CHECK" ./configure --prefix=${BUILD_DIR} && \
  make -j4 && \
  # Install protobuf-c-text to BUILD_DIR and the real user directory (for tpm2d build)
  make install && make prefix=/usr install
# make tpm2d & scd
RUN git clone https://github.com/trustm3/device_fraunhofer_common_cml && cd device_fraunhofer_common_cml/tpm2d && \
  ln -s /ibmtss1331/utils/ibmtss && ln -s /protobuf-c-text/protobuf-c-text && mkdir ${BUILD_DIR}/bin && \
  make && cp tpm2d ${BUILD_DIR}/bin/ && cd ../scd && make && cp scd ${BUILD_DIR}/bin/

# openssl_tpm2_engine dependencies
RUN apt-get update -qq && apt-get install -qq help2man
# install openssl-tpm2-engine (scd dependency)
RUN git clone https://kernel.googlesource.com/pub/scm/linux/kernel/git/jejb/openssl_tpm2_engine && \
  cd openssl_tpm2_engine && git checkout v2.3.0 && bash bootstrap.sh && \
  ./configure --with-enginesdir="${BUILD_DIR}/lib/$(arch)-linux-gnu/engines-1.1" && \
  make -j4 && checkinstall --fstrans=no --pkgname=openssl-tpm2-engine -y && cp openssl-tpm2-engine*.deb ${BUILD_DIR}


FROM debian:stretch-slim

RUN apt-get update -qq && apt-get install -qq bash fuse libglib2.0 libseccomp-dev libprotobuf-c-dev
COPY --from=0 /build /usr/
RUN dpkg -i /usr/*.deb && rm /usr/*.deb && ldconfig

# TCP port for tpm2d communication
EXPOSE 9505

COPY run.sh .
RUN chmod +x run.sh

CMD ["/run.sh"]