#!/bin/bash
# package/bump-version.sh <base> <revision>
# Single source of truth for all version strings in this project.
# Called by semantic-release (devel) and Groovy revision release (release/*).
set -euo pipefail

BASE=$1
REV=$2

# Maven
sed -i "s|<revision>.*</revision>|<revision>${BASE}</revision>|" pom.xml
sed -i "s|<changelist>.*</changelist>|<changelist>-${REV}</changelist>|" pom.xml

# PKGBUILD
sed -i "s|pkgver=\"[^\"]*\"|pkgver=\"${BASE}\"|" package/PKGBUILD
sed -i "s|pkgrel=\"[^\"]*\"|pkgrel=\"${REV}\"|" package/PKGBUILD
