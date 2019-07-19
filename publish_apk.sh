#!/usr/bin/env bash
mkdir $HOME/r/
ls
cd app/
ls
cd build/outputs/apk/release/
ls
cp -Rf * $HOME/r/

cd $HOME/
git config --global user.email "kaleraj.rk@gmail.com"
git config --global user.name "rajkale99"
git clone --depth=10 --branch=master https://rajkale99:$GITHUB_API_KEY@github.com/rajkale99/Travis.git ma > /dev/null 
cd ma
cp -Rf $HOME/r/ .
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
