cp target/tomcat7-conf-1.0-RELEASE.jar /usr/share/tomcat7/lib/

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

