ARG baseImage=mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}-focal

FROM ${baseImage}
# Must match the args in git_scripts/devdocker-img/Dockerfile, so either of
# these can be used by
# test-infra.git/src/com/example/ts.groovy/getDevDocker().

ARG DEBIAN_FRONTEND=noninteractive
ARG TZ=America/Los_Angeles

USER root

# 1. Set up environemt variables.
# Those env variables can either be used in later steps in this build or be
# inherited by any container created/started from the built image.
ENV JENKINS_REMOTING_VERSION=2.52 \
    TZ=America/Los_Angeles \
    container=docker \
    K8S_MASTER_ADDRESS=http://192.168.2.185:8080 \
    BLINK_REPORT_SERVER=10.79.134.32:31300 \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8 \
    ANSIBLE_HOST_KEY_CHECKING=False \
    GIT_VERSION=2.17.0

RUN apt-get update && \
    apt-get install -y man \
    bzip2 \
    curl \
    rsync \
    sshfs \
    python3-pip \
    openssh-server \
    locales \
    sudo \
    supervisor \
    tigervnc-standalone-server \
    tigervnc-common

RUN pip3 install --upgrade pip setuptools && \
    locale-gen en_US.UTF-8  && \
    /usr/bin/ssh-keygen -A

RUN pip3 install \
    awscli \
    click \
    fuzzywuzzy \
    jinja2 \
    junitparser \
    junit-xml \
    lxml \
    pandas \
    paramiko \
    requests \
    slackclient \
    xmltodict

# Update root user password and add admin user
RUN echo 'root:th0ughtSp0t' | chpasswd && \
    useradd -ms /bin/bash admin && \
    adduser admin sudo && \
    echo 'admin:th0ughtSp0t' | chpasswd && \
    echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers && \
    mkdir -p /usr/local/scaligent && \
    chown admin:admin /usr/local/scaligent && \
    mkdir -p /export && \
    chown -R admin:admin /export

# Push any environment variables from Dockerfile into the image.
ENV NOTVISIBLE "in users profile"
ENV LANG en_US.UTF-8
RUN echo "export VISIBLE=now" >> /etc/profile

# Add sshd.
# RUN apt-get install -y openssh-server
# SSH login fix. Otherwise user is kicked off after login
RUN mkdir /var/run/sshd && \
    sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd && \
    mkdir -p /var/lock/apache2 /var/run/apache2 /var/run/sshd /var/log/supervisor

# Add sshd as the only supervisor service for now.
# Ubuntu supervisor only processes files with .conf extension in the following directory.
# ADD sshd.conf /etc/supervisor/conf.d/sshd.conf
# Add hudson SSH keys
ADD id_rsa /home/admin/.ssh/id_rsa
ADD id_rsa.pub /home/admin/.ssh/id_rsa.pub
COPY id_rsa.pub /home/admin/.ssh/authorized_keys
# ADD ssh_config /home/admin/.ssh/config
# RUN chmod 0400 /home/admin/.ssh/config && \
RUN chmod 0400 /home/admin/.ssh/id_rsa && \
    chown admin:admin -R /home/admin/.ssh

EXPOSE 22

# RUN useradd -rm -d /home/admin -s /bin/bash -g admin -G sudo -u 1001 admin
    #PATH="~/priority_bin:~/go/bin:${PATH}"

# 2. Download the jenkins-remote program and make it accessible.

RUN curl --create-dirs -sSLo /usr/share/jenkins/remoting-$JENKINS_REMOTING_VERSION.jar \
  http://repo.jenkins-ci.org/public/org/jenkins-ci/main/remoting/$JENKINS_REMOTING_VERSION/remoting-$JENKINS_REMOTING_VERSION.jar \
  && chmod 755 /usr/share/jenkins

# 9. Update Locale
#RUN localedef -c -f UTF-8 -i en_US en_US.UTF-8 &&

WORKDIR /home/admin
RUN mkdir -p /home/admin/.vnc
# Setup passwordless vnc
COPY vncpasswd /home/admin/.vnc/passwd
COPY config /home/admin/.vnc/config
RUN chmod 0600 /home/admin/.vnc/passwd \
    && chown -R admin.admin /home/admin/.vnc && \
    /etc/init.d/ssh start

USER admin

# 10. Entrypoint
# ENTRYPOINT ["/sbin/init"]
ENTRYPOINT ["sudo", "/usr/sbin/sshd", "-D"]
