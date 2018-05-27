#!/bin/bash

rm -fv /usr/share/tomcat8/lib/{jdbc-handler-*,cloudwatch-handler-*}.jar

cp -v jdbc-handler/target/jdbc-handler-*.jar /usr/share/tomcat8/lib/
cp -v cloudwatch-handler/target/cloudwatch-handler-*-shaded.jar /usr/share/tomcat8/lib/

for i in /usr/share/tomcat8/lib/*.jar
do
 list=$list:$i
done

cat >/usr/share/tomcat8/bin/setenv.sh <<EOF
CLASSPATH=\
/usr/share/java/mysql-connector-java.jar:\
/usr/share/tomcat8/bin/tomcat-juli.jar\
$list
EOF

