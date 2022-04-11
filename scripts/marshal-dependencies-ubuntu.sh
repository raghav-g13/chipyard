#!/bin/bash
#set -x

# thanks to Giacomo Rizzi: some ideas have been taken from: https://github.com/gufoe/vuos-tutorial

while getopts p: flag
do
	case "${flag}" in
		p) prefix=${OPTARG};;
	esac
done

BASE="$(dirname "$0")"
cd $BASE
BASE=$(pwd)
echo "BASE: $BASE"

# User friendly messages on error
set -E
set -o functrace
function handle_error {
    local retval=$?
    local line=${last_lineno:-$1}
    echo "Failed at $line: $BASH_COMMAND"
    echo "Trace: " "$@"
    exit $retval
}
if (( ${BASH_VERSION%%.*} <= 3 )) || [[ ${BASH_VERSION%.*} = 4.0 ]]; then
	trap '[[ $FUNCNAME = handle_error ]] || { last_lineno=$real_lineno; real_lineno=$LINENO; }' DEBUG
fi
trap 'handle_error $LINENO ${BASH_LINENO[@]}' ERR

function install_repo {
	REPO=$1
	REPOBASE=${REPO##*/}
	REPOBASE=${REPOBASE%%.*}
	PREWD=$(pwd)
	echo installing $1
	cd  "$BASE"/gits
	git clone --recurse-submodules $1
	cd $REPOBASE
	if [ -f configure.ac ]
	then
		echo AUTOCONF
		autoreconf -vif
		if [ -z "$prefix" ]
		then
			./configure $2
		else
			./configure $2 --prefix=$prefix
		fi
		make
		make install
		# ldconfig
	elif [ -f CMakeLists.txt ] 
	then
		echo CMAKE
		mkdir -p build
		cd build
		if [ -z "$prefix" ]
		then
			cmake .. $2
			make
		else
			MARSHAL_PREFIX=$prefix CC=$BASE/gccwrap-ubuntu $prefix/bin/cmake .. -DCMAKE_PREFIX_PATH=$prefix -DCMAKE_INSTALL_PREFIX=$prefix
			C_INCLUDE_PATH="$prefix/include" LIBRARY_PATH="$prefix/lib" make
		fi
		make install
	fi
	cd $PREWD
}

function install_cmake {
	PREWD=$(pwd)
	version=3.23
	build=0
	echo installing cmake-$version.$build
	cd "$BASE"/gits
	wget https://cmake.org/files/v$version/cmake-$version.$build.tar.gz
	tar -xzvf cmake-$version.$build.tar.gz

	cd cmake-$version.$build/
	if [ -z "$prefix" ]
	then
		./bootstrap
	else
		./bootstrap --prefix=$prefix
	fi
	make -j$(nproc)
	make install
	cd $PREWD
}


function install_ninja {
	PREWD=$(pwd)
	release=1.10.2
	echo installing ninja-$release
	cd "$BASE"/gits
	wget https://github.com/ninja-build/ninja/releases/download/v$release/ninja-linux.zip
	if [ -z "$prefix" ]
	then
		unzip -d /usr/local/bin/ ninja-linux.zip
	else
		unzip -d $prefix/bin/ ninja-linux.zip
	fi
	cd $PREWD
}

function install_libslirp {
	echo installing libslirp
	PREWD=$(pwd)
	cd "$BASE"/gits
	git clone https://gitlab.freedesktop.org/slirp/libslirp.git
	cd libslirp
	if [ -z "$prefix" ]
	then
		python3.8 ../meson/meson.py build
		ninja -C build install
	else
		NINJA=$prefix/bin/ninja python3.8 ../meson/meson.py build -Dprefix=$prefix
		$prefix/bin/ninja -C build install
	fi
	cd $PREWD
}


# Start installation

# rm -rf gits
# mkdir gits

# install_cmake
install_ninja
install_repo https://github.com/mesonbuild/meson.git
install_libslirp

install_repo https://github.com/virtualsquare/s2argv-execs.git
install_repo https://github.com/rd235/vdeplug4.git
install_repo https://github.com/virtualsquare/libvdeslirp.git
install_repo https://github.com/virtualsquare/vdeplug_slirp.git

echo 'Installation completed'
