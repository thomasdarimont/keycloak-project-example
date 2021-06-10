#!/bin/sh

set -e

#tools
java -version
mvn -v
docker -v
docker-compose -v

result=0
#docker-compose mounts (TODO fetch from docker-compose-files...)
for i in './keycloak/extensions/target/classes' './testrun/data'
do
  if [ ! -d $i ]; then
     echo "Not existing: " $i
     result=1
  fi
done

if [ $result -gt 0 ];
then
  echo "Error. Check the output above."
else
  echo "Good to go."
fi
exit $result


