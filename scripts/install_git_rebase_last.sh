#!/bin/bash

# inspired from stackoverflow answer: http://stackoverflow.com/questions/11929766/how-to-delete-all-git-commits-except-the-last-five and http://stackoverflow.com/questions/7005513/pass-an-argument-to-a-git-alias-command
git config --global alias.rebase-last-x '!b="$(git branch --no-color | cut -c3-)" ; h="$(git rev-parse $b)" ; echo "Current branch: $b $h" ; c="$(git rev-parse $b~$(($1-1)))" ; echo "Recreating $b branch with initial commit $c ..." ; git checkout --orphan new-start $c ; git commit -C $c ; git rebase --onto new-start $c $b ; git branch -d new-start ; git gc; echo "Done."'
echo "You can now use the command 'git rebase-last-x 10' to rebase a repo keeping 10 commits"
