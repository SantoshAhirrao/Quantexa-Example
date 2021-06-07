:: Requires additional port tunnels, can be done using plink by running: 
start plink Seb -L 10.0.75.1:19200:lassie:9200

cd ../docker

start docker-compose -f docker-compose.yml -f dev.yml up -d