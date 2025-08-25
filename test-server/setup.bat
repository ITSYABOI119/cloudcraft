@echo off
REM Download Paper for testing
curl -o paper.jar https://api.papermc.io/v2/projects/paper/versions/1.20.4/builds/496/downloads/paper-1.20.4-496.jar

REM Create server properties
echo eula=true > eula.txt
echo server-port=25565> server.properties
echo online-mode=false>> server.properties
echo max-players=100>> server.properties

REM Create start script
echo @echo off > start.bat
echo java -Xmx8G -jar paper.jar nogui >> start.bat
