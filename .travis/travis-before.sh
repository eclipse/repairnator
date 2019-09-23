sudo apt-get update
sudo apt-get install cloc -y
curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g ajv-cli
sudo apt-get install shunit2
docker pull repairnator/pipeline
docker pull repairnator/sequencer:1.0
docker pull antonw/activemq-jmx:latest
docker run -d --net=host antonw/activemq-jmx:latest
docker ps -a
