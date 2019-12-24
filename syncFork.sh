git fetch upstream

echo "continue?(y/n)"
read cont

if [ "$cont" == 'n' ]
then
        exit 0
fi

git checkout master

git merge upstream/master
