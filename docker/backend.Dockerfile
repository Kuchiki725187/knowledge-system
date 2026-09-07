# ============ 多阶段构建:第一阶段编译,第二阶段只留运行环境 ============
# 第一阶段:构建
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 先只拷 pom 并预下载依赖,利用 Docker 层缓存(不改依赖就不用重新下载)
COPY pom.xml .
RUN mvn dependency:go-offline -q
# 再拷源码编译
COPY src ./src
RUN mvn package -DskipTests -q

# 第二阶段:运行(镜像里只有 JRE,没有 JDK 和 Maven,体积小很多)
FROM eclipse-temurin:21-jre
WORKDIR /app
# 只拷贝构建产物 jar
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
# JVM 参数调整，优化内存使用
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:MaxMetaspaceSize=128m", "-XX:+UseSerialGC", "-Xss512k", "-jar", "app.jar"]
