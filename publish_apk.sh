#!/usr/bin/env bash
mkdir $HOME/daily/
cp -Rf $TRAVIS_BUILD_DIR/app/build/outputs/apk/ $HOME/daily/

cd $HOME/
git config --global user.email "kaleraj.rk@gmail.com"
git config --global user.name "rajkale99"
git clone --depth=10 --branch=master  https://rajkale99:26d3db54f4ba731e28d8bb739df2a2602ec7e1db@github.com/rajkale99/Travis/ master > /dev/null 
cd master cp -Rf $HOME/
# add, commit and push files

git add -A
git commit -m "Travis build $TRAVIS_BUILD_NUMBER pushed"
git push -fq origin master > /dev/null
echo "Done"
