# =====================================================
# 电商秒杀平台 - Dockerfile (多阶段构建)
# =====================================================
# 阶段一: Maven 构建
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# 跳过测试打包
RUN mvn clean package -DskipTests -q

# =====================================================
# 阶段二: 运行时镜像
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非 root 用户运行应用
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从构建阶段复制 JAR
COPY --from=builder /app/target/*.jar app.jar

# 切换到非 root 用户
USER appuser

# 默认端口，可通过环境变量 SERVER_PORT 覆盖
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]