#!/bin/bash
set -e

base_dir=`pwd`
cd ${base_dir}/commons-vscode
npm install

cd $base_dir
for i in vscode-boot-properties vscode-manifest-yaml ; do
    cd ${base_dir}/${i}
    echo "***************************************************************************************"
    echo "***************************************************************************************"
    echo "***************************************************************************************"
    echo "***** BUILDING: " $i
    echo "***************************************************************************************"
    echo "***************************************************************************************"
    echo "***************************************************************************************"
    
    npm install ../commons-vscode
    npm install
    npm run vsce-package
done
