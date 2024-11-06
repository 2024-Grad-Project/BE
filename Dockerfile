FROM openjdk:17-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일을 로컬에서 직접 가져오도록 설정
COPY ./build/libs/*-SNAPSHOT.jar /app/sisyphus-core.jar

# 8080 포트를 외부에 노출
EXPOSE 8080

# 컨테이너 실행 시 자바 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "sisyphus-core.jar"]