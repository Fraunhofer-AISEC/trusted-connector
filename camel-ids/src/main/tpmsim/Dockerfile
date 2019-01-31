FROM debian:stretch

ENV BUILD_DIR /tpm

RUN apt-get update -qq && apt-get install -qq git \
  # libtpms dependencies
  automake autoconf libtool make gcc libc-dev libssl-dev
RUN git clone https://github.com/stefanberger/libtpms.git
RUN cd libtpms && ./autogen.sh --prefix=${BUILD_DIR} --with-openssl --with-tpm2 && make -j4 && \
  # Install libtpms to BUILD_DIR and the real user directory (for swtpm build)
  make install && make prefix=/usr/local install

RUN apt-get update -qq && apt-get install -qq gawk \
  # swtpm dependencies without tpm-tools and trousers
  # (original by stefanberger patched by quitschbo for TPM 2.0 only)
  automake autoconf bash coreutils expect libtool sed fuse libfuse-dev glib2.0 glib2.0-dev \
  # Removed selinux-policy-dev because of build error and added gawk
  net-tools python3 python3-twisted gawk gnutls-bin gnutls-dev \
  libtasn1-6 libtasn1-6-dev build-essential devscripts equivs socat
RUN git clone https://github.com/quitschbo/swtpm.git
RUN cd swtpm && ./autogen.sh --prefix=${BUILD_DIR} && make -j4 && make install


FROM debian:stretch-slim

RUN apt-get update -qq && apt-get install -qq bash fuse libglib2.0
COPY --from=0 /tpm /usr/
RUN echo "/usr/lib/swtpm" > /etc/ld.so.conf.d/swtpm.conf && ldconfig

# Create TPM state in /swtpm
#COPY --from=0 /tpm/etc /etc/
#RUN apt-get update -qq && apt-get install -qq gnutls-bin
#ENTRYPOINT swtpm_setup --tpm2 --tpmstate /swtpm --create-ek-cert && \
#  cp /var/lib/swtpm-localca/*cert.pem /swtpm/

ENTRYPOINT swtpm socket --tpmstate dir=/swtpm --tpm2 --ctrl type=unixio,path=/tmp/swtpm-sock