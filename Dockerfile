FROM eclipse-temurin:11-jre-alpine
COPY target/dependency/BOOT-INF/lib /app/lib
COPY target/dependency/META-INF /app/META-INF
COPY target/dependency/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","cu.sld.ucmgt.directory.DirectoryApp"]
