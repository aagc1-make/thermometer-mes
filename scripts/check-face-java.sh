#!/usr/bin/env bash
# 仅做类型检查：用 JDK8 javac + m2 仓库全量 jar 作为 classpath 编译本次改动的人脸相关文件。
set -u
cd "$(dirname "$0")/.." || exit 1
cd backend/ktg-mes || exit 1

JAVAC="C:/Program Files/Java/jdk1.8.0_261/bin/javac"
OUT_WIN="E:/JavaCode/.tmp-verify-javac/out"
OUT_UNIX="/e/JavaCode/.tmp-verify-javac/out"
ARGS_WIN="E:/JavaCode/.tmp-verify-javac/out-args.txt"
ARGS_UNIX="/e/JavaCode/.tmp-verify-javac/out-args.txt"

MODULE_CLASSES=$(ls -d */target/classes 2>/dev/null | tr '\n' ';')
JARS=$(find "C:/Users/LEnovo/.m2/repository" -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" 2>/dev/null | tr '\n' ';')
SLF4J="C:/Users/LEnovo/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
CP="${SLF4J};${MODULE_CLASSES}${JARS}"

mkdir -p "$OUT_UNIX"
{
  echo "-nowarn"
  echo "-encoding"
  echo "UTF-8"
  echo "-source"
  echo "1.8"
  echo "-target"
  echo "1.8"
  echo "-d"
  echo "\"$OUT_WIN\""
  echo "-cp"
  echo "\"$CP\""
  for f in \
    ktg-system/src/main/java/com/ktg/system/face/FaceTemplate.java \
    ktg-system/src/main/java/com/ktg/system/face/FaceMatchResult.java \
    ktg-system/src/main/java/com/ktg/system/domain/SysUserFace.java \
    ktg-system/src/main/java/com/ktg/system/domain/SysUserFaceAdminRow.java \
    ktg-system/src/main/java/com/ktg/system/mapper/SysUserFaceMapper.java \
    ktg-system/src/main/java/com/ktg/system/service/ISysUserFaceService.java \
    ktg-system/src/main/java/com/ktg/system/service/impl/SysUserFaceServiceImpl.java \
    ktg-admin/src/main/java/com/ktg/web/controller/system/SysUserFaceController.java \
    ktg-admin/src/main/java/com/ktg/web/controller/system/SysUserFaceAdminController.java \
    ktg-admin/src/main/java/com/ktg/web/controller/system/SysFaceLoginController.java \
    ktg-framework/src/main/java/com/ktg/framework/web/service/FaceLoginService.java ; do
    echo "\"$f\""
  done
} > "$ARGS_UNIX"

"$JAVAC" "@$ARGS_WIN" 2>&1 | head -40
echo "javac exit: ${PIPESTATUS[0]}"
