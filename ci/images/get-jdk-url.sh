#!/bin/bash
set -e

case "$1" in
	java8)
		echo "https://github.com/bell-sw/Liberica/releases/download/8u332+9/bellsoft-jdk8u332+9-linux-amd64.tar.gz"
	;;
  *)
		echo $"Unknown java version"
		exit 1
esac
