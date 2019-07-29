sudo apt-get update
sudo apt-get install cloc -y
curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g ajv-cli
sudo apt-get install shunit2
docker pull repairnator/pipeline
docker pull webcenter/activemq:5.14.3
docker run -d --net=host webcenter/activemq:5.14.3
docker ps -a
