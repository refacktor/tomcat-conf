rm /usr/share/tomcat7/lib/tomcat7-conf-1.0-RELEASE.jar # old name
cp jdbc/target/jdbc-1.0-RELEASE.jar /usr/share/tomcat7/lib/
cp cloudwatch/target/shaded.jar /usr/share/tomcat7/lib/cloudwatch-shaded.jar

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

