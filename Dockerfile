# 1. Java 17 베이스 이미지 사용
FROM openjdk:17-jdk-slim

# 2. 빌드된 JAR 파일의 위치 변수 설정
ARG JAR_FILE=build/libs/*.jar

# 3. JAR 파일을 컨테이너 내부로 복사
COPY ${JAR_FILE} app.jar

LABEL authors="jasonkim"

# 4. 컨테이너 실행 시 스프링 부트 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]