#!/usr/bin/env bash
mkdir $HOME/daily/
ls
cd app/
ls
cd build/
ls
cp -Rf * $HOME/daily/

cd $HOME/
git config --global user.email "kaleraj.rk@gmail.com"
git config --global user.name "rajkale99"
git clone --depth=10 --branch=master https://rajkale99:$GITHUB_API_KEY@github.com/rajkale99/Travis.git master > /dev/null 
cd master
cp -Rf $HOME/daily/ .
ls
# add, commit and push files
git add -A
git status
git remote rm origin
git remote add origin https://rajkale99:$GITHUB_API_KEY@github.com/rajkale99/Travis.git
git add -A
ls
git commit -m "Travis build $TRAVIS_BUILD_NUMBER pushed"
git push -fq origin master > /dev/null

echo "Done"
