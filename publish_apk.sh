#!/usr/bin/env bash
mkdir $HOME/daily/
cd $TRAVIS_BUILD_DIR/app/build/outputs/apk/release/
cp -Rf *.apk $TRAVIS_BUILD_DIR/daily/
cd $TRAVIS_BUILD_DIR/daily/
git config --global user.email "kaleraj.rk@gmail.com"
git config --global user.name "rajkale99"


# add, commit and push files
git branch r
git checkout r
git remote add origin https://github.com/rajkale99/wallpaperboard
git add -f .
git commit -m "Travis build $TRAVIS_BUILD_NUMBER pushed in daily channel"
git push -fq origin r > /dev/null
