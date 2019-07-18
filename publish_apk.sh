#!/usr/bin/env bash
mkdir $HOME/daily/
cd $TRAVIS_BUILD_DIR/app/build/outputs/apk/release/
cp -Rf *.apk $HOME/daily/
cd $HOME/daily/
git config --global user.email "kaleraj.rk@gmail.com"
git config --global user.name "rajkale99"


# add, commit and push files
git branch r
git checkout r
git remote add origin https://github.com/rajkale99/legionpaper
git add -f .
git commit -m "Travis build pushed in daily channel"
git push -fq origin r > /dev/null
