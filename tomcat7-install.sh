#!/bin/bash

rm -fv /usr/share/tomcat7/lib/{tomcat7-conf-1.0-RELEASE,jdbc-handler-*,cloudwatch-handler-*}.jar

cp -v jdbc-handler/target/jdbc-handler-*.jar /usr/share/tomcat7/lib/
cp -v cloudwatch-handler/target/cloudwatch-handler-*-shaded.jar /usr/share/tomcat7/lib/

for i in /usr/share/tomcat7/lib/*.jar
do
 list=$list:$i
done

cat >/usr/share/tomcat7/bin/setenv.sh <<EOF
CLASSPATH=\
/usr/share/java/mysql-connector-java.jar:\
/usr/share/tomcat7/bin/tomcat-juli.jar\
$list
EOF

