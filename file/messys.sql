/*
 Navicat Premium Dump SQL

 Source Server         : localhost8
 Source Server Type    : MySQL
 Source Server Version : 80034 (8.0.34)
 Source Host           : localhost:3308
 Source Schema         : messys

 Target Server Type    : MySQL
 Target Server Version : 80034 (8.0.34)
 File Encoding         : 65001

 Date: 22/11/2024 16:17:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dv_check_machinery
-- ----------------------------
DROP TABLE IF EXISTS `dv_check_machinery`;
CREATE TABLE `dv_check_machinery`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '点检设备表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_check_machinery
-- ----------------------------
INSERT INTO `dv_check_machinery` VALUES (200, 200, 200, 'M0056', '设备11', '华为', '111', '', NULL, NULL, 0, 0, '', '2024-11-08 17:01:33', '', NULL);

-- ----------------------------
-- Table structure for dv_check_plan
-- ----------------------------
DROP TABLE IF EXISTS `dv_check_plan`;
CREATE TABLE `dv_check_plan`  (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `plan_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划编码',
  `plan_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划名称',
  `plan_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划类型',
  `start_date` datetime NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` datetime NULL DEFAULT NULL COMMENT '结束日期',
  `cycle_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '频率',
  `cycle_count` int NULL DEFAULT NULL COMMENT '次数',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`plan_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备点检保养计划头表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_check_plan
-- ----------------------------
INSERT INTO `dv_check_plan` VALUES (200, 'PLAN2024001', '计划1', 'MAINTEN', '2024-11-08 00:00:00', '2024-11-16 00:00:00', 'DAY', 1, 'FINISHED', '', NULL, NULL, 0, 0, '', '2024-11-08 17:01:25', '', '2024-11-08 17:01:46');

-- ----------------------------
-- Table structure for dv_check_record
-- ----------------------------
DROP TABLE IF EXISTS `dv_check_record`;
CREATE TABLE `dv_check_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `plan_id` bigint NULL DEFAULT NULL COMMENT '计划ID',
  `plan_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划编码',
  `plan_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划名称',
  `plan_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划类型',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `check_time` datetime NOT NULL COMMENT '点检时间',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PREPARE' COMMENT '状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备点检记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_check_record
-- ----------------------------

-- ----------------------------
-- Table structure for dv_check_record_line
-- ----------------------------
DROP TABLE IF EXISTS `dv_check_record_line`;
CREATE TABLE `dv_check_record_line`  (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `record_id` bigint NOT NULL COMMENT '计划ID',
  `subject_id` bigint NOT NULL COMMENT '项目ID',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目编码',
  `subject_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目类型',
  `subject_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标准',
  `check_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '点检结果',
  `check_result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '异常描述',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备点检记录行表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_check_record_line
-- ----------------------------

-- ----------------------------
-- Table structure for dv_check_subject
-- ----------------------------
DROP TABLE IF EXISTS `dv_check_subject`;
CREATE TABLE `dv_check_subject`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `subject_id` bigint NOT NULL COMMENT '项目ID',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目编码',
  `subject_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目类型',
  `subject_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标准',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '点检项目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_check_subject
-- ----------------------------
INSERT INTO `dv_check_subject` VALUES (200, 200, 200, 'SUB049', '保养1', 'CHECK', '检查电路', NULL, '', NULL, NULL, 0, 0, '', '2024-11-08 17:01:38', '', NULL);

-- ----------------------------
-- Table structure for dv_dss_record
-- ----------------------------
DROP TABLE IF EXISTS `dv_dss_record`;
CREATE TABLE `dv_dss_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `plan_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '记录编号',
  `record_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'STOP' COMMENT '记录类型',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `record_time` datetime NOT NULL COMMENT '记录时间',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PREPARE' COMMENT '状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备保养记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_dss_record
-- ----------------------------

-- ----------------------------
-- Table structure for dv_machinery
-- ----------------------------
DROP TABLE IF EXISTS `dv_machinery`;
CREATE TABLE `dv_machinery`  (
  `machinery_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备类型ID',
  `machinery_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备类型编码',
  `machinery_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备类型名称',
  `machinery_brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `machinery_type_id` bigint NOT NULL COMMENT '设备类型ID',
  `machinery_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型编码',
  `machinery_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型名称',
  `workshop_id` bigint NOT NULL COMMENT '所属车间ID',
  `workshop_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属车间编码',
  `workshop_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属车间名称',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STOP' COMMENT '设备状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`machinery_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_machinery
-- ----------------------------
INSERT INTO `dv_machinery` VALUES (200, 'M0056', '设备11', '华为', '111', 201, NULL, NULL, 200, NULL, NULL, 'STOP', '', NULL, NULL, 0, 0, '', '2024-11-08 16:59:22', '', NULL);

-- ----------------------------
-- Table structure for dv_machinery_type
-- ----------------------------
DROP TABLE IF EXISTS `dv_machinery_type`;
CREATE TABLE `dv_machinery_type`  (
  `machinery_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备类型ID',
  `machinery_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备类型编码',
  `machinery_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备类型名称',
  `parent_type_id` bigint NULL DEFAULT 0 COMMENT '父类型ID',
  `ancestors` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所有父节点ID',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`machinery_type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 205 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_machinery_type
-- ----------------------------
INSERT INTO `dv_machinery_type` VALUES (200, 'M_TYPE_000', '设备分类', 0, '0', 'Y', '', NULL, NULL, 0, 0, '', NULL, '', NULL);
INSERT INTO `dv_machinery_type` VALUES (201, 'M_TYPE_025', '设备1', 200, '0,200', 'Y', '', NULL, NULL, 0, 0, '', '2024-11-07 22:38:57', '', NULL);
INSERT INTO `dv_machinery_type` VALUES (202, 'M_TYPE_026', '设备2', 200, '0,200', 'Y', '', NULL, NULL, 0, 0, '', '2024-11-07 22:39:11', '', NULL);
INSERT INTO `dv_machinery_type` VALUES (203, 'M_TYPE_000', '设备分类', 0, '0', 'Y', '', NULL, NULL, 0, 0, '', NULL, '', NULL);
INSERT INTO `dv_machinery_type` VALUES (204, 'M_TYPE_000', '设备分类', 0, '0', 'Y', '', NULL, NULL, 0, 0, '', NULL, '', NULL);

-- ----------------------------
-- Table structure for dv_mainten_record
-- ----------------------------
DROP TABLE IF EXISTS `dv_mainten_record`;
CREATE TABLE `dv_mainten_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `plan_id` bigint NULL DEFAULT NULL COMMENT '计划ID',
  `plan_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划编码',
  `plan_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划名称',
  `plan_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划类型',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `mainten_time` datetime NOT NULL COMMENT '保养时间',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PREPARE' COMMENT '状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备保养记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_mainten_record
-- ----------------------------

-- ----------------------------
-- Table structure for dv_mainten_record_line
-- ----------------------------
DROP TABLE IF EXISTS `dv_mainten_record_line`;
CREATE TABLE `dv_mainten_record_line`  (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `record_id` bigint NOT NULL COMMENT '计划ID',
  `subject_id` bigint NOT NULL COMMENT '项目ID',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目编码',
  `subject_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目类型',
  `subject_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标准',
  `mainten_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '保养结果',
  `mainten_result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '异常描述',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备保养记录行表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_mainten_record_line
-- ----------------------------

-- ----------------------------
-- Table structure for dv_repair
-- ----------------------------
DROP TABLE IF EXISTS `dv_repair`;
CREATE TABLE `dv_repair`  (
  `repair_id` bigint NOT NULL AUTO_INCREMENT COMMENT '维修单ID',
  `repair_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '维修单编号',
  `repair_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '维修单名称',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `machinery_type_id` bigint NOT NULL COMMENT '设备类型ID',
  `require_date` datetime NULL DEFAULT NULL COMMENT '报修日期',
  `finish_date` datetime NULL DEFAULT NULL COMMENT '维修完成日期',
  `confirm_date` datetime NULL DEFAULT NULL COMMENT '验收日期',
  `repair_result` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '维修结果',
  `accepted_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '维修人员',
  `confirm_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '验收人员',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`repair_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备维修单' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_repair
-- ----------------------------
INSERT INTO `dv_repair` VALUES (200, 'REP2024001', '维修1', 200, 'M0056', '设备11', '华为', NULL, 201, '2024-11-09 00:00:00', NULL, NULL, '', NULL, NULL, 'PREPARE', '', NULL, NULL, 0, 0, '', '2024-11-08 17:07:42', '', '2024-11-08 17:10:24');

-- ----------------------------
-- Table structure for dv_repair_line
-- ----------------------------
DROP TABLE IF EXISTS `dv_repair_line`;
CREATE TABLE `dv_repair_line`  (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `repair_id` bigint NOT NULL COMMENT '维修单ID',
  `subject_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目编码',
  `subject_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目类型',
  `subject_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标准',
  `malfunction` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '故障描述',
  `malfunction_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '故障描述资源',
  `repair_des` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '维修情况',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 202 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备维修单行' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_repair_line
-- ----------------------------
INSERT INTO `dv_repair_line` VALUES (200, 200, NULL, NULL, '维系设备', NULL, NULL, NULL, '线路有问题', '线路有问题', '已解决', '', NULL, NULL, 0, 0, '', '2024-11-08 17:08:07', '', '2024-11-08 17:08:39');
INSERT INTO `dv_repair_line` VALUES (201, 200, NULL, NULL, '维修2', NULL, NULL, NULL, '机器故障', NULL, '维修完毕', '', NULL, NULL, 0, 0, '', '2024-11-08 17:10:17', '', NULL);

-- ----------------------------
-- Table structure for dv_subject
-- ----------------------------
DROP TABLE IF EXISTS `dv_subject`;
CREATE TABLE `dv_subject`  (
  `subject_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目编码',
  `subject_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '项目类型',
  `subject_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标准',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`subject_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备点检保养项目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dv_subject
-- ----------------------------
INSERT INTO `dv_subject` VALUES (200, 'SUB049', '保养1', 'CHECK', '检查电路', NULL, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-08 17:00:50', '', NULL);

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成业务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成业务表字段' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------

-- ----------------------------
-- Table structure for md_client
-- ----------------------------
DROP TABLE IF EXISTS `md_client`;
CREATE TABLE `md_client`  (
  `client_id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `client_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户编码',
  `client_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户名称',
  `client_nick` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户简称',
  `client_en` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户英文名称',
  `client_des` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户简介',
  `client_logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户LOGO地址',
  `client_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ENTERPRISE' COMMENT '客户类型',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户地址',
  `website` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户官网地址',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户邮箱地址',
  `tel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户电话',
  `contact1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人1',
  `contact1_tel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人1-电话',
  `contact1_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人1-邮箱',
  `contact2` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人2',
  `contact2_tel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人2-电话',
  `contact2_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人2-邮箱',
  `credit_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '统一社会信用代码',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`client_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_client
-- ----------------------------
INSERT INTO `md_client` VALUES (200, 'C00200', '张三', 'zs', 'zs', NULL, NULL, 'ENTERPRISE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-07 21:52:42', '', NULL);

-- ----------------------------
-- Table structure for md_item
-- ----------------------------
DROP TABLE IF EXISTS `md_item`;
CREATE TABLE `md_item`  (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品物料ID',
  `item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位名称',
  `item_or_product` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品物料标识',
  `item_type_id` bigint NULL DEFAULT 0 COMMENT '物料类型ID',
  `item_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '物料类型编码',
  `item_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '物料类型名称',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `safe_stock_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'N' COMMENT '是否设置安全库存',
  `min_stock` double(12, 4) NULL DEFAULT 0.0000 COMMENT '最低库存量',
  `max_stock` double(12, 4) NULL DEFAULT 0.0000 COMMENT '最大库存量',
  `high_value` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'N' COMMENT '高价值物资',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`item_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 204 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物料产品表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_item
-- ----------------------------
INSERT INTO `md_item` VALUES (200, '1001', '测试物料', '200', 'PCS', '个', 'ITEM', 202, 'ITEM_TYPE_0097', '五金类', 'Y', 'N', 0.0000, 0.0000, 'N', '', NULL, NULL, 0, 0, 'admin', '2024-11-01 21:00:33', 'admin', '2024-11-12 10:27:42');
INSERT INTO `md_item` VALUES (201, 'IF2024110635', '铁板', '100', 'mm', '毫米', 'ITEM', 202, 'ITEM_TYPE_0097', '五金类', 'Y', 'N', 0.0000, 0.0000, 'true', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:21:42', 'admin', '2024-11-08 16:26:56');
INSERT INTO `md_item` VALUES (202, 'IF2024110625', '螺丝刀刀柄', '10', 'PCS', '个', 'PRODUCT', 207, 'ITEM_TYPE_0102', '半成品', 'Y', 'N', 0.0000, 0.0000, 'N', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:25:11', 'admin', '2024-11-08 16:28:48');
INSERT INTO `md_item` VALUES (203, 'IF2024110838', '测试物料2', '100', 'PCS', '个', 'ITEM', 203, 'ITEM_TYPE_0098', '包装类', 'Y', 'N', 0.0000, 0.0000, 'N', '', NULL, NULL, 0, 0, 'admin', '2024-11-08 16:31:00', 'admin', '2024-11-08 16:31:20');

-- ----------------------------
-- Table structure for md_item_type
-- ----------------------------
DROP TABLE IF EXISTS `md_item_type`;
CREATE TABLE `md_item_type`  (
  `item_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品物料类型ID',
  `item_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品物料类型编码',
  `item_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品物料类型名称',
  `parent_type_id` bigint NOT NULL DEFAULT 0 COMMENT '父类型ID',
  `ancestors` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所有层级父节点',
  `item_or_product` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品物料标识',
  `order_num` int NULL DEFAULT 1 COMMENT '排列顺序',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`item_type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 211 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物料产品分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_item_type
-- ----------------------------
INSERT INTO `md_item_type` VALUES (200, 'ITEM_TYPE_0000', '物料产品分类', 0, '0', 'PRODUCT', 1, 'Y', '', NULL, NULL, 0, 0, '', NULL, '', NULL);
INSERT INTO `md_item_type` VALUES (201, 'ITEM_TYPE_0096', '原材料', 200, '0,200', 'ITEM', 1, 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:10:03', '', NULL);
INSERT INTO `md_item_type` VALUES (202, 'ITEM_TYPE_0097', '五金类', 201, '0,200,201', 'ITEM', 1, 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:10:22', '', NULL);
INSERT INTO `md_item_type` VALUES (203, 'ITEM_TYPE_0098', '包装类', 201, '0,200,201', 'ITEM', 1, 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:10:44', '', NULL);
INSERT INTO `md_item_type` VALUES (204, 'ITEM_TYPE_0099', '原料药', 201, '0,200,201', 'ITEM', 1, 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:11:10', '', NULL);
INSERT INTO `md_item_type` VALUES (205, 'ITEM_TYPE_0100', '医药辅料', 201, '0,200,201', 'ITEM', 1, 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:11:27', '', NULL);
INSERT INTO `md_item_type` VALUES (206, 'ITEM_TYPE_0101', '产品', 200, '0,200', 'PRODUCT', 2, 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:11:52', '', NULL);
INSERT INTO `md_item_type` VALUES (207, 'ITEM_TYPE_0102', '半成品', 206, '0,200,206', 'PRODUCT', 1, 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:12:11', '', NULL);
INSERT INTO `md_item_type` VALUES (208, 'ITEM_TYPE_0103', '产成品', 206, '0,200,206', 'PRODUCT', 2, 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-06 22:12:23', '', NULL);
INSERT INTO `md_item_type` VALUES (209, 'ITEM_TYPE_0000', '物料产品分类', 0, '0', 'PRODUCT', 1, 'Y', '', NULL, NULL, 0, 0, '', NULL, '', NULL);
INSERT INTO `md_item_type` VALUES (210, 'ITEM_TYPE_0000', '物料产品分类', 0, '0', 'PRODUCT', 1, 'Y', '', NULL, NULL, 0, 0, '', NULL, '', NULL);

-- ----------------------------
-- Table structure for md_product_bom
-- ----------------------------
DROP TABLE IF EXISTS `md_product_bom`;
CREATE TABLE `md_product_bom`  (
  `bom_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `item_id` bigint NOT NULL COMMENT '物料产品ID',
  `bom_item_id` bigint NOT NULL COMMENT 'BOM物料ID',
  `bom_item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BOM物料编码',
  `bom_item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BOM物料名称',
  `bom_item_spec` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'BOM物料规格',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BOM物料单位',
  `item_or_product` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品物料标识',
  `quantity` double(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '物料使用比例',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`bom_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 204 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产品BOM关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_product_bom
-- ----------------------------
INSERT INTO `md_product_bom` VALUES (200, 202, 202, 'IF2024110625', '螺丝刀刀柄', '10', 'PCS', 'PRODUCT', 1.0000, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-06 22:33:38', '', NULL);
INSERT INTO `md_product_bom` VALUES (201, 201, 201, 'IF2024110635', '铁板', '100', 'mm', 'ITEM', 1.0000, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-08 16:26:51', '', NULL);
INSERT INTO `md_product_bom` VALUES (202, 200, 200, '1001', '测试物料', NULL, 'PCS', 'ITEM', 1.0000, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-08 16:28:16', '', NULL);
INSERT INTO `md_product_bom` VALUES (203, 203, 203, 'IF2024110838', '测试物料2', '100', 'PCS', 'ITEM', 1.0000, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-08 16:31:18', '', NULL);

-- ----------------------------
-- Table structure for md_product_sip
-- ----------------------------
DROP TABLE IF EXISTS `md_product_sip`;
CREATE TABLE `md_product_sip`  (
  `sip_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `item_id` bigint NOT NULL COMMENT '物料产品ID',
  `order_num` int NULL DEFAULT NULL COMMENT '排列顺序',
  `process_id` bigint NULL DEFAULT NULL COMMENT '对应的工序',
  `process_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工序名称',
  `sip_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `sip_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细描述',
  `sip_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片地址',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sip_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产品SIP表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_product_sip
-- ----------------------------

-- ----------------------------
-- Table structure for md_product_sop
-- ----------------------------
DROP TABLE IF EXISTS `md_product_sop`;
CREATE TABLE `md_product_sop`  (
  `sop_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `item_id` bigint NOT NULL COMMENT '物料产品ID',
  `order_num` int NULL DEFAULT NULL COMMENT '排列顺序',
  `process_id` bigint NULL DEFAULT NULL COMMENT '对应的工序',
  `process_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工序名称',
  `sop_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `sop_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细描述',
  `sop_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片地址',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sop_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 202 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产品SOP表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_product_sop
-- ----------------------------
INSERT INTO `md_product_sop` VALUES (201, 202, 1, NULL, NULL, NULL, '测试sop1', NULL, 'http://127.0.0.1:9000/public-bucket-mes/2024/11/06/d1_20241106225546A003.jpg', '', NULL, NULL, 0, 0, '', '2024-11-06 22:55:53', '', NULL);

-- ----------------------------
-- Table structure for md_unit_measure
-- ----------------------------
DROP TABLE IF EXISTS `md_unit_measure`;
CREATE TABLE `md_unit_measure`  (
  `measure_id` bigint NOT NULL AUTO_INCREMENT COMMENT '单位ID',
  `measure_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位编码',
  `measure_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位名称',
  `primary_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否是主单位',
  `primary_id` bigint NULL DEFAULT NULL COMMENT '主单位ID',
  `change_rate` double(12, 4) NULL DEFAULT NULL COMMENT '与主单位换算比例',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`measure_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 207 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '单位表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_unit_measure
-- ----------------------------
INSERT INTO `md_unit_measure` VALUES (200, 'kg', '公斤', 'Y', NULL, NULL, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-01 21:00:15', '', '2024-11-06 22:09:04');
INSERT INTO `md_unit_measure` VALUES (201, 'g', '克', 'N', 200, 1000.0000, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-06 22:05:12', '', '2024-11-06 22:06:44');
INSERT INTO `md_unit_measure` VALUES (202, 'm', '米', 'Y', NULL, NULL, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-06 22:07:04', '', NULL);
INSERT INTO `md_unit_measure` VALUES (203, 'cm', '厘米', 'N', 202, 100.0000, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-06 22:07:27', '', NULL);
INSERT INTO `md_unit_measure` VALUES (204, 'mm', '毫米', 'N', 202, 1000.0000, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-06 22:07:49', '', NULL);
INSERT INTO `md_unit_measure` VALUES (205, 'T', '顿', 'Y', NULL, NULL, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-06 22:08:14', '', NULL);
INSERT INTO `md_unit_measure` VALUES (206, 'PCS', '个', 'Y', NULL, NULL, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-06 22:24:15', '', NULL);

-- ----------------------------
-- Table structure for md_vendor
-- ----------------------------
DROP TABLE IF EXISTS `md_vendor`;
CREATE TABLE `md_vendor`  (
  `vendor_id` bigint NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `vendor_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商简称',
  `vendor_en` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商英文名称',
  `vendor_des` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商简介',
  `vendor_logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商LOGO地址',
  `vendor_level` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商等级',
  `vendor_score` int NULL DEFAULT NULL COMMENT '供应商评分',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商地址',
  `website` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商官网地址',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商邮箱地址',
  `tel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商电话',
  `contact1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人1',
  `contact1_tel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人1-电话',
  `contact1_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人1-邮箱',
  `contact2` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人2',
  `contact2_tel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人2-电话',
  `contact2_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人2-邮箱',
  `credit_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '统一社会信用代码',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`vendor_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_vendor
-- ----------------------------
INSERT INTO `md_vendor` VALUES (200, 'V00059', '比亚迪', 'BYD', 'BYD', NULL, NULL, 'B', 0, NULL, NULL, NULL, NULL, '15866667777', NULL, NULL, NULL, NULL, NULL, NULL, 'Y', '', NULL, NULL, 0, 0, '', '2024-11-07 21:54:15', '', NULL);

-- ----------------------------
-- Table structure for md_workshop
-- ----------------------------
DROP TABLE IF EXISTS `md_workshop`;
CREATE TABLE `md_workshop`  (
  `workshop_id` bigint NOT NULL AUTO_INCREMENT COMMENT '车间ID',
  `workshop_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车间编码',
  `workshop_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车间名称',
  `area` double(12, 2) NULL DEFAULT NULL COMMENT '面积',
  `charge` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`workshop_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车间表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_workshop
-- ----------------------------
INSERT INTO `md_workshop` VALUES (200, 'WS081', '五金车间', 50.00, '李四', 'Y', '', NULL, NULL, 0, 0, '', '2024-11-07 21:55:02', '', NULL);

-- ----------------------------
-- Table structure for md_workstation
-- ----------------------------
DROP TABLE IF EXISTS `md_workstation`;
CREATE TABLE `md_workstation`  (
  `workstation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作站ID',
  `workstation_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作站名称',
  `workstation_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作站地点',
  `workshop_id` bigint NULL DEFAULT NULL COMMENT '所在车间ID',
  `workshop_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所在车间编码',
  `workshop_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所在车间名称',
  `process_id` bigint NULL DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工序名称',
  `warehouse_id` bigint NOT NULL COMMENT '线边库ID',
  `warehouse_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '线边库编码',
  `warehouse_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '线边库名称',
  `location_id` bigint NOT NULL COMMENT '库区ID',
  `location_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint NOT NULL COMMENT '库位ID',
  `area_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库位名称',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`workstation_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作站表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_workstation
-- ----------------------------
INSERT INTO `md_workstation` VALUES (200, 'WS0120', '测试工作站', NULL, 200, 'WS081', '五金车间', 200, 'PROCESS195', '注塑', 200, 'XBK_VIRTUAL', '线边库-虚拟', 200, 'XBKKQ_VIRTUAL', '线边库库区-虚拟', 200, 'XBKKW_VIRTUAL', '线边库库位-虚拟', 'Y', '', NULL, NULL, 0, 0, '', '2024-11-07 22:13:26', '', NULL);

-- ----------------------------
-- Table structure for md_workstation_machine
-- ----------------------------
DROP TABLE IF EXISTS `md_workstation_machine`;
CREATE TABLE `md_workstation_machine`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备名称',
  `quantity` int NULL DEFAULT 1 COMMENT '数量',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备资源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_workstation_machine
-- ----------------------------

-- ----------------------------
-- Table structure for md_workstation_tool
-- ----------------------------
DROP TABLE IF EXISTS `md_workstation_tool`;
CREATE TABLE `md_workstation_tool`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `tool_type_id` bigint NOT NULL COMMENT '工装夹具类型ID',
  `tool_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类型编码',
  `tool_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类型名称',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工装夹具资源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_workstation_tool
-- ----------------------------

-- ----------------------------
-- Table structure for md_workstation_worker
-- ----------------------------
DROP TABLE IF EXISTS `md_workstation_worker`;
CREATE TABLE `md_workstation_worker`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位编码',
  `post_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位名称',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人力资源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_workstation_worker
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob NULL COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Blob类型的触发器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_blob_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`, `calendar_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日历信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_calendars
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Cron类型的触发器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_cron_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`, `entry_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '已触发的触发器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_fired_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob NULL COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务详细信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_job_details
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`, `lock_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存储的悲观锁信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_locks
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '暂停的触发器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_paused_trigger_grps
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`, `instance_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度器状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_scheduler_state
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '简单触发器的信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_simple_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int NULL DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int NULL DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint NULL DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint NULL DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13, 4) NULL DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13, 4) NULL DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '同步机制的行锁表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_simprop_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint NULL DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint NULL DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int NULL DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint NULL DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint NULL DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob NULL COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  INDEX `sched_name`(`sched_name` ASC, `job_name` ASC, `job_group` ASC) USING BTREE,
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '触发器详细信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for sys_auto_code_part
-- ----------------------------
DROP TABLE IF EXISTS `sys_auto_code_part`;
CREATE TABLE `sys_auto_code_part`  (
  `part_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分段ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `part_index` int NOT NULL COMMENT '分段序号',
  `part_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分段类型，INPUTCHAR：输入字符，NOWDATE：当前日期时间，FIXCHAR：固定字符，SERIALNO：流水号',
  `part_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分段编号',
  `part_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分段名称',
  `part_length` int NOT NULL COMMENT '分段长度',
  `date_format` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `input_character` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '输入字符',
  `fix_character` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '固定字符',
  `seria_start_no` int NULL DEFAULT NULL COMMENT '流水号起始值',
  `seria_step` int NULL DEFAULT NULL COMMENT '流水号步长',
  `seria_now_no` int NULL DEFAULT NULL COMMENT '流水号当前值',
  `cycle_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '流水号是否循环',
  `cycle_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '循环方式，YEAR：按年，MONTH：按月，DAY：按天，HOUR：按小时，MINITE：按分钟，OTHER：按传入字符变',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`part_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 304 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '编码生成规则组成表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_auto_code_part
-- ----------------------------
INSERT INTO `sys_auto_code_part` VALUES (200, 205, 2, 'SERIALNO', 'P1', '流水号', 8, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-04-26 21:13:17', 'admin', '2022-04-26 22:47:49');
INSERT INTO `sys_auto_code_part` VALUES (201, 205, 1, 'FIXCHAR', 'P0', '前缀', 4, NULL, NULL, 'ITEM', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-04-26 22:44:03', 'admin', '2022-08-15 15:59:19');
INSERT INTO `sys_auto_code_part` VALUES (202, 206, 1, 'FIXCHAR', 'P1', '前缀', 10, NULL, NULL, 'ITEM_TYPE_', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-04-26 23:02:12', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (203, 206, 2, 'SERIALNO', 'P2', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-04-26 23:02:42', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (204, 207, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'C', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-06 21:21:04', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (205, 207, 2, 'SERIALNO', 'SERIAL', '流水号部分', 5, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-06 21:21:44', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (206, 208, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'V', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-06 22:50:38', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (207, 208, 2, 'SERIALNO', 'SERIAL', '流水号', 5, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-06 22:51:02', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (208, 209, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'WS', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-07 17:49:16', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (209, 209, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-07 17:49:40', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (210, 210, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'WH', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-07 22:00:17', 'admin', '2022-08-16 18:58:36');
INSERT INTO `sys_auto_code_part` VALUES (211, 210, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-07 22:00:40', 'admin', '2022-07-30 11:26:14');
INSERT INTO `sys_auto_code_part` VALUES (212, 211, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'L', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 14:50:29', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (213, 211, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 14:52:12', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (214, 212, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'A', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 18:38:29', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (215, 212, 2, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 18:38:51', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (216, 213, 1, 'FIXCHAR', 'PREFIX', '前缀', 7, NULL, NULL, 'M_TYPE_', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 19:46:42', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (217, 213, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 19:47:03', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (218, 214, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'M', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 21:26:59', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (219, 214, 2, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 21:27:18', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (220, 215, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'MO', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-09 11:40:23', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (222, 215, 2, 'NOWDATE', 'DATEPART', '年月日部分', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-09 11:58:57', 'admin', '2022-05-09 12:46:34');
INSERT INTO `sys_auto_code_part` VALUES (223, 215, 3, 'SERIALNO', 'SERIAL', '流水号部分', 4, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-09 11:59:31', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (224, 216, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'WS', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-10 21:55:51', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (225, 216, 2, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-10 21:56:19', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (226, 217, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'TT', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-11 00:22:02', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (227, 217, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-11 00:22:25', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (228, 218, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'T', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-11 22:07:44', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (229, 218, 2, 'SERIALNO', 'SERIAL', '流水号', 5, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-11 22:08:17', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (230, 219, 1, 'FIXCHAR', 'PREFIX', '前缀', 7, NULL, NULL, 'PROCESS', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-12 00:10:13', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (231, 219, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-12 00:10:33', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (232, 220, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'R', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-12 23:07:01', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (233, 220, 2, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1000, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-12 23:07:23', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (234, 221, 1, 'FIXCHAR', 'PREFIX', '固定前缀', 4, NULL, NULL, 'TASK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-15 18:22:53', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (235, 221, 2, 'NOWDATE', 'YEAR', '年份', 4, 'yyyy', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-15 18:23:39', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (236, 221, 3, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'YEAR', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-15 18:24:03', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (237, 222, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'I', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-17 21:57:46', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (238, 222, 2, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-17 21:58:05', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (239, 223, 1, 'FIXCHAR', 'PREFIX', '前缀', 3, NULL, NULL, 'QCT', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-17 22:43:31', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (240, 223, 2, 'NOWDATE', 'YEAR', '年份', 4, 'yyyy', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-17 22:44:04', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (241, 223, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'YEAR', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-17 22:44:25', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (242, 224, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'DF', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-19 11:33:52', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (243, 224, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-19 11:34:11', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (244, 225, 1, 'FIXCHAR', 'PREFIX', '前缀', 3, NULL, NULL, 'IQC', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-19 16:29:59', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (245, 225, 2, 'NOWDATE', 'DATE', '年月日', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-19 16:30:28', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (246, 225, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-19 16:31:00', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (247, 226, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, NULL, NULL, 'R', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-22 20:51:47', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (248, 226, 2, 'NOWDATE', 'DATE', '日期', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-05-22 20:52:10', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (249, 226, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-22 20:52:58', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (250, 227, 1, 'FIXCHAR', 'PREFIX', '固定前缀', 1, NULL, NULL, 'T', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-06 19:54:45', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (251, 227, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-06 19:55:06', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (252, 228, 1, 'FIXCHAR', 'PREFIX', '前缀', 4, NULL, NULL, 'PLAN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-06 22:08:39', 'admin', '2022-07-31 16:42:59');
INSERT INTO `sys_auto_code_part` VALUES (253, 228, 2, 'NOWDATE', 'YEAR', '年', 4, 'yyyy', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-06 22:08:59', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (254, 228, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'YEAR', NULL, NULL, NULL, 0, 0, 'admin', '2022-06-06 22:09:24', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (255, 229, 1, 'FIXCHAR', 'PREFIX', '前缀', 3, NULL, NULL, 'RTV', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-13 16:06:14', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (256, 229, 2, 'NOWDATE', 'DATE', '日期', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-13 16:06:42', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (257, 229, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-06-13 16:07:10', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (258, 230, 1, 'FIXCHAR', 'PREFIX', '固定前缀', 3, NULL, NULL, 'SUB', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-16 20:28:22', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (259, 230, 2, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-16 20:28:44', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (260, 231, 1, 'FIXCHAR', 'PREFIX', '前缀', 4, NULL, NULL, 'PLAN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-16 21:50:22', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (261, 231, 2, 'NOWDATE', 'YEAR', '年份', 4, 'yyyy', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-06-16 21:50:43', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (262, 231, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'YEAR', NULL, NULL, NULL, 0, 0, 'admin', '2022-06-16 21:51:07', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (263, 232, 1, 'FIXCHAR', '1', '1', 3, NULL, NULL, 'BAT', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-07-14 12:02:54', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (264, 232, 2, 'NOWDATE', '2', '2', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-07-14 12:03:16', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (265, 233, 1, 'FIXCHAR', 'PREFIX', '前缀', 5, NULL, NULL, 'ISSUE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-07-17 19:32:46', 'admin', '2022-07-17 19:35:44');
INSERT INTO `sys_auto_code_part` VALUES (266, 233, 2, 'NOWDATE', 'DATE', '年月日', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-07-17 19:33:22', 'admin', '2022-07-17 19:35:57');
INSERT INTO `sys_auto_code_part` VALUES (267, 233, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-07-17 19:33:45', 'admin', '2022-07-17 19:36:05');
INSERT INTO `sys_auto_code_part` VALUES (271, 234, 1, 'INPUTCHAR', 'PREFIX', '1', 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-07-30 14:20:49', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (275, 236, 1, 'FIXCHAR', '前缀', '固定字符', 2, NULL, NULL, 'IF', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-08-19 10:48:20', 'admin', '2022-08-19 14:13:30');
INSERT INTO `sys_auto_code_part` VALUES (276, 236, 2, 'NOWDATE', '后缀', '固定字段', 4, 'yyyyMMddss', NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-08-19 14:12:19', 'admin', '2022-08-19 14:19:39');
INSERT INTO `sys_auto_code_part` VALUES (277, 237, 1, 'FIXCHAR', 'PREFIX', '前缀', 4, NULL, NULL, 'IPQC', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-08-29 22:07:43', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (278, 237, 2, 'NOWDATE', 'DATE', '年月日', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-08-29 22:08:18', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (279, 237, 3, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-08-29 22:08:46', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (280, 238, 1, 'FIXCHAR', 'PREFIX', '前缀', 3, NULL, NULL, 'OQC', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-01 20:30:53', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (281, 238, 2, 'NOWDATE', 'DATE', '日期', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-01 20:32:11', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (282, 238, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-01 20:32:38', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (283, 239, 1, 'FIXCHAR', '001', '前缀', 5, NULL, NULL, 'PBACK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-03 23:49:07', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (284, 239, 2, 'NOWDATE', '002', '日期', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-03 23:49:31', 'admin', '2022-09-03 23:49:44');
INSERT INTO `sys_auto_code_part` VALUES (285, 239, 3, 'SERIALNO', '003', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'N', NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-03 23:50:10', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (286, 240, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'RT', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-15 23:19:25', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (287, 240, 2, 'NOWDATE', 'DATE', '日期', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-15 23:19:47', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (288, 240, 3, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-15 23:20:09', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (289, 241, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'PR', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-23 10:58:17', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (290, 241, 2, 'NOWDATE', 'DATE', '年月日', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-23 10:58:44', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (291, 241, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-23 10:59:06', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (292, 242, 1, 'FIXCHAR', 'PREFIX', '前缀', 3, NULL, NULL, 'REP', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-28 22:01:19', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (293, 242, 2, 'NOWDATE', 'DATE', '日期', 4, 'yyyy', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-09-28 22:01:39', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (294, 242, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'YEAR', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-28 22:02:00', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (295, 243, 1, 'FIXCHAR', 'PERFIX', '前缀', 2, NULL, NULL, 'PS', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-10-05 19:46:02', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (296, 243, 2, 'NOWDATE', 'DATA', '日期', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-10-05 19:46:24', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (297, 243, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-10-05 19:46:48', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (298, 244, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, NULL, NULL, 'RS', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-10-06 21:40:42', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (299, 244, 2, 'NOWDATE', 'DATE', '日期', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-10-06 21:41:03', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (300, 244, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-10-06 21:41:22', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (301, 245, 1, 'FIXCHAR', 'PREFIX', '前缀', 4, NULL, NULL, 'PACK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-10-11 01:22:38', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (302, 245, 2, 'NOWDATE', 'DATE', '日期', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'admin', '2022-10-11 01:23:09', '', NULL);
INSERT INTO `sys_auto_code_part` VALUES (303, 245, 3, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, NULL, NULL, 1, 1, NULL, 'Y', 'DAY', NULL, NULL, NULL, 0, 0, 'admin', '2022-10-11 01:23:35', '', NULL);

-- ----------------------------
-- Table structure for sys_auto_code_result
-- ----------------------------
DROP TABLE IF EXISTS `sys_auto_code_result`;
CREATE TABLE `sys_auto_code_result`  (
  `code_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `gen_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '生成日期时间',
  `gen_index` int NULL DEFAULT NULL COMMENT '最后产生的序号',
  `last_result` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后产生的值',
  `last_serial_no` int NULL DEFAULT NULL COMMENT '最后产生的流水号',
  `last_input_char` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后传入的参数',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`code_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 430 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '编码生成记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_auto_code_result
-- ----------------------------
INSERT INTO `sys_auto_code_result` VALUES (200, 205, '20220808095803', 76, 'ITEM00000076', 76, NULL, '', NULL, NULL, 0, 0, '', '2022-04-26 22:28:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (201, 206, '20220813231829', 62, 'ITEM_TYPE_0062', 62, NULL, '', NULL, NULL, 0, 0, '', '2022-04-27 10:17:19', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (202, 207, '20220802144206', 10, 'C00010', 10, NULL, '', NULL, NULL, 0, 0, '', '2022-05-06 21:22:23', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (203, 208, '20220805160743', 10, 'V00010', 10, NULL, '', NULL, NULL, 0, 0, '', '2022-05-06 22:52:14', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (204, 209, '20220805111938', 12, 'WS012', 12, NULL, '', NULL, NULL, 0, 0, '', '2022-05-07 17:54:01', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (205, 210, '20220729090506', 2, 'WH002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-05-07 22:01:31', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (206, 211, '20220729085518', 10, 'L010', 10, NULL, '', NULL, NULL, 0, 0, '', '2022-05-08 14:52:34', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (207, 212, '20220810082205', 11, 'A0011', 11, NULL, '', NULL, NULL, 0, 0, '', '2022-05-08 18:39:15', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (208, 213, '20220823091541', 24, 'M_TYPE_024', 24, NULL, '', NULL, NULL, 0, 0, '', '2022-05-08 19:47:31', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (209, 214, '20220819091251', 26, 'M0026', 26, NULL, '', NULL, NULL, 0, 0, '', '2022-05-08 21:38:26', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (210, 215, '20220509234547', 21, 'MO202205090021', 21, NULL, '', NULL, NULL, 0, 0, '', '2022-05-09 12:46:53', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (212, 216, '20220818103207', 34, 'WS0034', 34, NULL, '', NULL, NULL, 0, 0, '', '2022-05-10 21:59:44', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (213, 217, '20220816195823', 25, 'TT025', 25, NULL, '', NULL, NULL, 0, 0, '', '2022-05-11 00:22:40', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (214, 218, '20220804162652', 10, 'T00010', 10, NULL, '', NULL, NULL, 0, 0, '', '2022-05-11 22:35:05', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (215, 219, '20220729122036', 46, 'PROCESS046', 46, NULL, '', NULL, NULL, 0, 0, '', '2022-05-12 00:10:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (217, 220, '20220809171633', 29, 'R1028', 1028, NULL, '', NULL, NULL, 0, 0, '', '2022-05-12 23:11:24', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (219, 215, '20220514155158', 26, 'MO202205140026', 26, NULL, '', NULL, NULL, 0, 0, '', '2022-05-14 15:24:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (220, 215, '20220515134342', 5, 'MO202205150005', 5, NULL, '', NULL, NULL, 0, 0, '', '2022-05-15 13:41:34', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (221, 221, '20220927111039', 87, 'TASK20220087', 87, NULL, '', NULL, NULL, 0, 0, '', '2022-05-15 18:29:41', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (222, 222, '20220812142031', 13, 'I0013', 13, NULL, '', NULL, NULL, 0, 0, '', '2022-05-17 21:58:16', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (223, 223, '20220901212456', 42, 'QCT2022042', 42, NULL, '', NULL, NULL, 0, 0, '', '2022-05-17 22:44:37', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (224, 224, '20220819144546', 8, 'DF008', 8, NULL, '', NULL, NULL, 0, 0, '', '2022-05-19 11:37:14', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (225, 225, '20220519230422', 21, 'IQC20220519021', 21, NULL, '', NULL, NULL, 0, 0, '', '2022-05-19 16:49:49', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (226, 226, '20220522220740', 8, 'R20220522008', 8, NULL, '', NULL, NULL, 0, 0, '', '2022-05-22 20:53:25', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (227, 227, '20220814214249', 25, 'T025', 25, NULL, '', NULL, NULL, 0, 0, '', '2022-06-06 19:55:18', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (228, 228, '20220823150049', 121, 'PLAN2022121', 121, NULL, '', NULL, NULL, 0, 0, '', '2022-06-06 22:09:41', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (229, 229, '20220613162434', 2, 'RTV20220613002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-06-13 16:19:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (230, 226, '20220613164021', 1, 'R20220613001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-06-13 16:40:22', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (231, 229, '20220614161107', 3, 'RTV20220614003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-06-14 11:11:21', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (232, 226, '20220614122043', 3, 'R20220614003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-06-14 11:34:53', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (233, 230, '20220812221323', 4, 'SUB004', 4, NULL, '', NULL, NULL, 0, 0, '', '2022-06-16 20:29:01', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (234, 231, '20220901230257', 50, 'PLAN2022050', 50, NULL, '', NULL, NULL, 0, 0, '', '2022-06-16 21:51:19', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (235, 226, '20220717124046', 2, 'R20220717002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-07-17 12:40:42', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (236, 233, '20220717194751', 2, 'ISSUE20220717002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-07-17 19:36:18', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (237, 233, '20220725150745', 8, 'ISSUE20220725008', 8, NULL, '', NULL, NULL, 0, 0, '', '2022-07-25 11:16:26', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (238, 215, '20220727133912', 3, 'MO202207270003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-07-27 13:39:10', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (239, 229, '20220727135402', 3, 'RTV20220727003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-07-27 13:53:57', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (240, 215, '20220728231952', 1, 'MO202207280001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-07-28 23:19:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (241, 211, '20220729085519', 1, 'L011', 11, NULL, '', NULL, NULL, 0, 0, '', '2022-07-29 08:55:19', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (242, 211, '20220801170339', 6, 'L017', 17, NULL, '', NULL, NULL, 0, 0, '', '2022-07-29 08:55:20', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (243, 229, '20220729091253', 4, 'RTV20220729004', 4, NULL, '', NULL, NULL, 0, 0, '', '2022-07-29 08:56:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (244, 210, '20220729090520', 3, 'WH005', 5, NULL, '', NULL, NULL, 0, 0, '', '2022-07-29 09:05:11', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (245, 210, '20220801161858', 52, 'WH060', 60, NULL, '', NULL, NULL, 0, 0, '', '2022-07-29 09:05:39', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (246, 215, '20220729111608', 3, 'MO202207290003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-07-29 09:30:33', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (247, 219, '20220729122132', 3, 'PROCESS049', 49, NULL, '', NULL, NULL, 0, 0, '', '2022-07-29 12:21:02', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (248, 215, '20220730170708', 6, 'MO202207300006', 6, NULL, '', NULL, NULL, 0, 0, '', '2022-07-30 14:41:08', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (249, 226, '20220730164005', 3, 'R20220730003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-07-30 16:30:37', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (250, 219, '20220730170439', 3, 'PROCESS052', 52, NULL, '', NULL, NULL, 0, 0, '', '2022-07-30 17:02:43', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (251, 215, '20220731161554', 1, 'MO202207310001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-07-31 16:15:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (252, 226, '20220801162854', 33, 'R20220801033', 33, NULL, '', NULL, NULL, 0, 0, '', '2022-08-01 11:11:28', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (253, 215, '20220801154407', 1, 'MO202208010001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-08-01 15:44:07', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (254, 210, '20220801161857', 1, 'WH060', 60, NULL, '', NULL, NULL, 0, 0, '', '2022-08-01 16:18:57', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (255, 210, '20220801161857', 1, 'WH060', 60, NULL, '', NULL, NULL, 0, 0, '', '2022-08-01 16:18:57', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (256, 210, '20220809140656', 13, 'WH073', 73, NULL, '', NULL, NULL, 0, 0, '', '2022-08-01 16:26:43', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (257, 219, '20220801172652', 3, 'PROCESS055', 55, NULL, '', NULL, NULL, 0, 0, '', '2022-08-01 17:14:02', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (258, 215, '20220802174313', 13, 'MO202208020013', 13, NULL, '', NULL, NULL, 0, 0, '', '2022-08-02 09:27:31', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (259, 225, '20220802180956', 2, 'IQC20220802002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-08-02 17:54:11', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (260, 229, '20220802180930', 2, 'RTV20220802002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-08-02 18:01:18', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (261, 215, '20220803152758', 3, 'MO202208030003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-08-03 15:19:17', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (262, 218, '20220810105807', 7, 'T00017', 17, NULL, '', NULL, NULL, 0, 0, '', '2022-08-04 16:34:19', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (263, 209, '20220813133944', 14, 'WS026', 26, NULL, '', NULL, NULL, 0, 0, '', '2022-08-05 11:19:39', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (264, 226, '20220805131441', 4, 'R20220805004', 4, NULL, '', NULL, NULL, 0, 0, '', '2022-08-05 13:09:41', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (265, 208, '20220808161115', 4, 'V00014', 14, NULL, '', NULL, NULL, 0, 0, '', '2022-08-05 16:27:10', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (266, 205, '20220812160022', 12, 'ITEM00000088', 88, NULL, '', NULL, NULL, 0, 0, '', '2022-08-08 10:01:12', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (267, 229, '20220808105742', 2, 'RTV20220808002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-08-08 10:44:56', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (268, 207, '20220813112402', 14, 'C00024', 24, NULL, '', NULL, NULL, 0, 0, '', '2022-08-08 15:52:04', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (269, 208, '20220814231644', 10, 'V00024', 24, NULL, '', NULL, NULL, 0, 0, '', '2022-08-08 16:11:16', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (270, 219, '20220809091746', 2, 'PROCESS057', 57, NULL, '', NULL, NULL, 0, 0, '', '2022-08-09 09:17:40', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (271, 219, '20220813232649', 11, 'PROCESS068', 68, NULL, '', NULL, NULL, 0, 0, '', '2022-08-09 09:22:42', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (272, 215, '20220809173457', 7, 'MO202208090007', 7, NULL, '', NULL, NULL, 0, 0, '', '2022-08-09 12:27:34', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (273, 210, '20220809140657', 1, 'WH074', 74, NULL, '', NULL, NULL, 0, 0, '', '2022-08-09 14:06:57', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (274, 210, '20220814231315', 5, 'WH079', 79, NULL, '', NULL, NULL, 0, 0, '', '2022-08-09 14:06:58', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (275, 220, '20220813113700', 12, 'R1040', 1040, NULL, '', NULL, NULL, 0, 0, '', '2022-08-09 17:16:34', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (276, 226, '20220809174609', 2, 'R20220809002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-08-09 17:36:22', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (277, 211, '20220810082146', 5, 'L022', 22, NULL, '', NULL, NULL, 0, 0, '', '2022-08-10 08:21:39', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (278, 211, '20220927131733', 23, 'L045', 45, NULL, '', NULL, NULL, 0, 0, '', '2022-08-10 08:21:48', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (279, 212, '20220810082207', 2, 'A0013', 13, NULL, '', NULL, NULL, 0, 0, '', '2022-08-10 08:22:06', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (280, 212, '20220810083445', 4, 'A0017', 17, NULL, '', NULL, NULL, 0, 0, '', '2022-08-10 08:22:08', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (281, 218, '20220814194551', 6, 'T00023', 23, NULL, '', NULL, NULL, 0, 0, '', '2022-08-10 10:58:10', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (282, 215, '20220810141929', 7, 'MO202208100007', 7, NULL, '', NULL, NULL, 0, 0, '', '2022-08-10 12:30:21', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (283, 215, '20220811155054', 2, 'MO202208110002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-08-11 15:50:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (284, 215, '20220812163345', 36, 'MO202208120036', 36, NULL, '', NULL, NULL, 0, 0, '', '2022-08-12 11:40:15', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (285, 225, '20220812202238', 5, 'IQC20220812005', 5, NULL, '', NULL, NULL, 0, 0, '', '2022-08-12 13:54:10', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (286, 222, '20220822090243', 21, 'I0034', 34, NULL, '', NULL, NULL, 0, 0, '', '2022-08-12 14:20:34', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (287, 205, '20220813012808', 21, 'ITEM00000109', 109, NULL, '', NULL, NULL, 0, 0, '', '2022-08-12 16:00:23', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (288, 226, '20220812214513', 3, 'R20220812003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-08-12 16:51:18', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (289, 205, '20220813133124', 10, 'ITEM00000119', 119, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 01:28:09', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (290, 226, '20220813152657', 5, 'R20220813005', 5, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 01:47:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (291, 215, '20220813232357', 10, 'MO202208130010', 10, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 08:50:33', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (292, 207, '20220813134013', 7, 'C00031', 31, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 11:24:03', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (293, 220, '20220815153054', 12, 'R1052', 1052, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 11:37:04', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (294, 209, '20220816172957', 8, 'WS034', 34, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 13:39:46', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (295, 207, '20220813134017', 6, 'C00037', 37, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 13:40:15', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (296, 207, '20220815132333', 20, 'C00057', 57, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 13:40:18', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (297, 225, '20220813225044', 3, 'IQC20220813003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 15:14:15', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (298, 205, '20220815085111', 12, 'ITEM00000131', 131, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 21:48:41', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (299, 230, '20220815103546', 5, 'SUB009', 9, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 22:00:17', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (300, 206, '20220815143043', 4, 'ITEM_TYPE_0066', 66, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 23:18:41', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (301, 219, '20220815164921', 9, 'PROCESS077', 77, NULL, '', NULL, NULL, 0, 0, '', '2022-08-13 23:32:16', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (302, 215, '20220814231425', 5, 'MO202208140005', 5, NULL, '', NULL, NULL, 0, 0, '', '2022-08-14 10:40:29', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (303, 212, '20220818105233', 4, 'A0021', 21, NULL, '', NULL, NULL, 0, 0, '', '2022-08-14 11:19:56', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (304, 227, '20220814214249', 1, 'T026', 26, NULL, '', NULL, NULL, 0, 0, '', '2022-08-14 21:42:49', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (305, 227, '20220817195511', 33, 'T058', 58, NULL, '', NULL, NULL, 0, 0, '', '2022-08-14 21:42:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (306, 210, '20220817170523', 21, 'WH100', 100, NULL, '', NULL, NULL, 0, 0, '', '2022-08-14 23:13:23', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (307, 208, '20220816133617', 7, 'V00031', 31, NULL, '', NULL, NULL, 0, 0, '', '2022-08-14 23:16:45', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (308, 205, '20220815110633', 5, 'ITEM00000136', 136, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 08:51:13', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (309, 218, '20220823093332', 36, 'T00059', 59, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 09:53:55', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (310, 215, '20220815185151', 18, 'MO202208150018', 18, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 10:12:07', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (311, 226, '20220815143729', 8, 'R20220815008', 8, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 10:44:44', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (312, 205, '20220815140036', 3, 'ITEM00000139', 139, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 11:06:35', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (313, 225, '20220815111142', 1, 'IQC20220815001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 11:11:42', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (314, 207, '20220815132338', 6, 'C00063', 63, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 13:23:35', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (315, 207, '20220815132340', 4, 'C00067', 67, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 13:23:39', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (316, 207, '20220822100445', 95, 'C00162', 162, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 13:23:41', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (317, 230, '20220818164931', 21, 'SUB030', 30, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 14:40:05', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (318, 205, '20220817093425', 51, 'ITEM00000190', 190, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 15:43:11', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (319, 219, '20220818141839', 28, 'PROCESS105', 105, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 16:50:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (320, 229, '20220815172658', 2, 'RTV20220815002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 17:12:49', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (321, 220, '20220815200538', 2, 'R1054', 1054, NULL, '', NULL, NULL, 0, 0, '', '2022-08-15 19:52:44', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (322, 220, '20220816155501', 6, 'R1060', 1060, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 00:59:25', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (323, 226, '20220816173419', 18, 'R20220816018', 18, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 09:24:37', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (324, 215, '20220816232550', 38, 'MO202208160038', 38, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 10:18:25', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (325, 225, '20220816201615', 4, 'IQC20220816004', 4, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 10:32:14', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (326, 208, '20220816140813', 5, 'V00036', 36, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 13:36:19', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (327, 208, '20220823111843', 12, 'V00048', 48, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 14:08:15', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (328, 229, '20220816195356', 6, 'RTV20220816006', 6, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 17:35:14', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (329, 220, '20220818105353', 6, 'R1066', 1066, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 19:55:41', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (330, 217, '20220819093714', 7, 'TT032', 32, NULL, '', NULL, NULL, 0, 0, '', '2022-08-16 19:58:24', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (331, 215, '20220817222518', 14, 'MO202208170014', 14, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 08:47:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (332, 209, '20220820094836', 17, 'WS051', 51, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 08:55:43', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (333, 229, '20220817091138', 2, 'RTV20220817002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 09:11:36', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (334, 205, '20220818115033', 32, 'ITEM00000222', 222, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 09:34:26', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (335, 206, '20220822091118', 19, 'ITEM_TYPE_0085', 85, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 11:14:35', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (336, 226, '20220817205507', 22, 'R20220817022', 22, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 14:03:20', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (337, 225, '20220817203604', 5, 'IQC20220817005', 5, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 15:57:28', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (338, 210, '20220817170524', 1, 'WH101', 101, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 17:05:24', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (339, 233, '20220817231312', 6, 'ISSUE20220817006', 6, NULL, '', NULL, NULL, 0, 0, '', '2022-08-17 23:02:13', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (340, 226, '20220818211659', 12, 'R20220818012', 12, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 09:30:26', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (341, 210, '20220822145345', 35, 'WH136', 136, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 09:39:40', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (342, 225, '20220818165508', 12, 'IQC20220818012', 12, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 09:41:21', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (343, 229, '20220818180526', 7, 'RTV20220818007', 7, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 10:10:40', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (344, 215, '20220818230633', 77, 'MO202208180077', 77, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 10:33:04', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (345, 220, '20220819091351', 17, 'R1083', 1083, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 10:53:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (346, 212, '20220820170019', 5, 'A0026', 26, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 11:07:01', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (347, 216, '20220818164828', 11, 'WS0045', 45, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 11:39:49', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (348, 227, '20220818160823', 8, 'T066', 66, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 13:40:18', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (349, 205, '20220818134540', 1, 'ITEM00000223', 223, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 13:45:40', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (350, 205, '20220818141238', 3, 'ITEM00000226', 226, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 14:04:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (351, 205, '20220819101459', 33, 'ITEM00000259', 259, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 14:13:35', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (352, 219, '20220818144345', 2, 'PROCESS107', 107, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 14:43:21', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (353, 227, '20220818175718', 3, 'T069', 69, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 16:08:27', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (354, 216, '20220927110257', 73, 'WS0118', 118, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 16:48:30', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (355, 230, '20220823102743', 18, 'SUB048', 48, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 16:56:44', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (356, 227, '20220820110823', 7, 'T076', 76, NULL, '', NULL, NULL, 0, 0, '', '2022-08-18 17:57:45', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (357, 219, '20220927100835', 87, 'PROCESS194', 194, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 00:22:13', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (358, 226, '20220819211451', 22, 'R20220819022', 22, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 08:46:50', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (359, 215, '20220819211957', 44, 'MO202208190044', 44, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 08:47:15', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (360, 225, '20220819213654', 41, 'IQC20220819041', 41, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 08:49:46', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (361, 214, '20220821094813', 15, 'M0041', 41, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 09:12:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (362, 220, '20220819173023', 13, 'R1096', 1096, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 09:13:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (363, 217, '20220821133355', 15, 'TT047', 47, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 09:37:15', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (364, 236, '20220819111319', 3, '32323232323232323232323232ITEM_TYPE_', 14, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 11:13:16', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (365, 236, '20220819142129', 64, 'IF2022081929', 72, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 11:16:04', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (366, 236, '20220819145515', 12, 'IF2022081915', 1094, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 14:21:30', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (367, 236, '20220819205101', 16, 'IF2022081901', 21, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 16:18:20', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (368, 236, '20220823103219', 76, 'IF2022082319', 48, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 20:51:03', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (369, 229, '20220819205514', 1, 'RTV20220819001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 20:55:14', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (370, 220, '20220927100854', 26, 'R1122', 1122, NULL, '', NULL, NULL, 0, 0, '', '2022-08-19 21:12:27', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (371, 209, '20220820160129', 7, 'WS058', 58, NULL, '', NULL, NULL, 0, 0, '', '2022-08-20 09:48:37', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (372, 215, '20220820222241', 39, 'MO202208200039', 39, NULL, '', NULL, NULL, 0, 0, '', '2022-08-20 10:00:14', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (373, 226, '20220820215444', 21, 'R20220820021', 21, NULL, '', NULL, NULL, 0, 0, '', '2022-08-20 10:13:38', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (374, 227, '20220820110825', 2, 'T078', 78, NULL, '', NULL, NULL, 0, 0, '', '2022-08-20 11:08:24', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (375, 227, '20220820220731', 17, 'T095', 95, NULL, '', NULL, NULL, 0, 0, '', '2022-08-20 11:08:26', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (376, 225, '20220820160353', 3, 'IQC20220820003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-08-20 13:27:37', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (377, 209, '20220927105615', 22, 'WS080', 80, NULL, '', NULL, NULL, 0, 0, '', '2022-08-20 16:01:30', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (378, 227, '20220823083639', 10, 'T105', 105, NULL, '', NULL, NULL, 0, 0, '', '2022-08-20 22:07:33', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (379, 215, '20220821192938', 5, 'MO202208210005', 5, NULL, '', NULL, NULL, 0, 0, '', '2022-08-21 00:51:58', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (380, 214, '20220821094815', 1, 'M0042', 42, NULL, '', NULL, NULL, 0, 0, '', '2022-08-21 09:48:15', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (381, 212, '20221005194759', 8, 'A0034', 34, NULL, '', NULL, NULL, 0, 0, '', '2022-08-21 13:28:13', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (382, 214, '20220823173340', 13, 'M0055', 55, NULL, '', NULL, NULL, 0, 0, '', '2022-08-21 13:31:39', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (383, 217, '20220823205336', 11, 'TT058', 58, NULL, '', NULL, NULL, 0, 0, '', '2022-08-21 13:45:53', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (384, 226, '20220821225220', 7, 'R20220821007', 7, NULL, '', NULL, NULL, 0, 0, '', '2022-08-21 15:16:36', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (385, 215, '20220822234830', 19, 'MO202208220019', 19, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 08:38:59', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (386, 226, '20220822192029', 25, 'R20220822025', 25, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 08:48:16', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (387, 219, '20220822090456', 1, 'PROCESS140', 140, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 09:04:56', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (388, 219, '20220822090503', 1, 'PROCESS152', 152, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 09:05:03', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (389, 206, '20220927101231', 10, 'ITEM_TYPE_0095', 95, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 09:11:56', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (390, 229, '20220822155824', 7, 'RTV20220822007', 7, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 09:13:49', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (391, 207, '20220822100442', 1, 'C00153', 153, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 10:04:42', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (392, 207, '20220822102252', 5, 'C00167', 167, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 10:05:02', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (393, 207, '20220927102713', 32, 'C00199', 199, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 10:22:53', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (394, 210, '20220823083833', 3, 'WH139', 139, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 14:53:46', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (395, 225, '20220822154721', 1, 'IQC20220822001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-08-22 15:47:21', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (396, 226, '20220823203609', 20, 'R20220823020', 20, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 00:41:07', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (397, 215, '20220823200644', 52, 'MO202208230052', 52, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 07:51:01', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (398, 210, '20220927131638', 18, 'WH157', 157, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 08:38:34', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (399, 229, '20220823205401', 5, 'RTV20220823005', 5, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 08:43:32', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (400, 227, '20220823184025', 12, 'T117', 117, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 08:58:07', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (401, 225, '20220823192837', 7, 'IQC20220823007', 7, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 09:47:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (402, 236, '20220823173612', 17, 'IF2022082312', 51, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 10:41:04', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (403, 208, '20220823181426', 10, 'V00058', 58, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 15:09:35', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (404, 236, '20220823192141', 10, 'IF2022082341', 154, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 19:08:33', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (405, 236, '20220927102019', 16, 'IF2022092719', 95, NULL, '', NULL, NULL, 0, 0, '', '2022-08-23 19:21:45', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (406, 215, '20220828084935', 6, 'MO202208280006', 6, NULL, '', NULL, NULL, 0, 0, '', '2022-08-28 08:36:52', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (407, 237, '20220829224554', 4, 'IPQC202208290004', 4, NULL, '', NULL, NULL, 0, 0, '', '2022-08-29 22:09:01', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (408, 225, '20220831211548', 3, 'IQC20220831003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-08-31 20:57:09', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (409, 238, '20220901212705', 4, 'OQC20220901004', 4, NULL, '', NULL, NULL, 0, 0, '', '2022-09-01 20:35:11', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (410, 226, '20220902104056', 4, 'R20220902004', 4, NULL, '', NULL, NULL, 0, 0, '', '2022-09-02 10:36:07', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (411, 233, '20220902104302', 1, 'ISSUE20220902001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-09-02 10:43:02', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (412, 239, '20220904223417', 10, 'PBACK20220904010', 10, NULL, '', NULL, NULL, 0, 0, '', '2022-09-03 23:50:32', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (413, 237, '20220909211934', 1, 'IPQC202209090001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-09-09 21:19:38', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (414, 226, '20220910111422', 2, 'R20220910002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-09-10 10:57:18', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (415, 226, '20220912232033', 1, 'R20220912001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-09-12 23:20:33', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (416, 226, '20220913223354', 1, 'R20220913001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-09-13 22:33:54', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (417, 233, '20220913225049', 2, 'ISSUE20220913002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-09-13 22:35:02', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (418, 240, '20220915232017', 1, 'RT202209150001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-09-15 23:20:17', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (419, 226, '20220916212404', 1, 'R20220916001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-09-16 21:24:06', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (420, 241, '20220923105914', 1, 'PR20220923001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-09-23 10:59:14', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (421, 215, '20220926212817', 2, 'MO202209260002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-09-26 21:27:05', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (422, 215, '20220927112106', 2, 'MO202209270002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-09-27 10:25:50', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (423, 226, '20220927131833', 2, 'R20220927002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-09-27 13:15:57', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (424, 242, '20220928220423', 2, 'REP2022002', 2, NULL, '', NULL, NULL, 0, 0, '', '2022-09-28 22:02:26', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (425, 243, '20221005194837', 3, 'PS20221005003', 3, NULL, '', NULL, NULL, 0, 0, '', '2022-10-05 19:46:57', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (426, 233, '20221006132703', 1, 'ISSUE20221006001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-10-06 13:27:03', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (427, 244, '20221006214134', 1, 'RS20221006001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-10-06 21:41:34', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (428, 245, '20221011233528', 7, 'PACK202210110007', 7, NULL, '', NULL, NULL, 0, 0, '', '2022-10-11 01:23:50', '', NULL);
INSERT INTO `sys_auto_code_result` VALUES (429, 240, '20221015152217', 1, 'RT202210150001', 1, NULL, '', NULL, NULL, 0, 0, '', '2022-10-15 15:22:17', '', NULL);

-- ----------------------------
-- Table structure for sys_auto_code_rule
-- ----------------------------
DROP TABLE IF EXISTS `sys_auto_code_rule`;
CREATE TABLE `sys_auto_code_rule`  (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则编码',
  `rule_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称',
  `rule_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `max_length` int NULL DEFAULT NULL COMMENT '最大长度',
  `is_padded` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否补齐',
  `padded_char` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '补齐字符',
  `padded_method` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'L' COMMENT '补齐方式',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rule_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 246 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '编码生成规则表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_auto_code_rule
-- ----------------------------
INSERT INTO `sys_auto_code_rule` VALUES (206, 'ITEM_TYPE_CODE', '物料分类编码', NULL, 14, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-04-26 23:01:09', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (207, 'CLIENT_CODE', '客户编码规则', NULL, 6, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-06 21:20:29', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (208, 'VENDOR_CODE', '供应商编码规则', NULL, 6, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-06 22:50:13', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (209, 'WORKSHOP_CODE', '车间编码生成规则', NULL, 5, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-07 17:48:52', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (210, 'WAREHOUSE_CODE', '仓库编码规则', NULL, 5, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-07 21:59:51', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (211, 'LOCATION_CODE', '库区编码生成规则', NULL, 4, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 14:49:56', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (212, 'AREA_CODE', '库位编码规则', NULL, 5, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 18:38:08', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (213, 'MACHINERY_TYPE_CODE', '设备类型编码规则', NULL, 10, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 19:46:09', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (214, 'MACHINERY_CODE', '设备编码规则', NULL, 13, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-08 21:26:39', 'admin', '2022-08-23 09:15:17');
INSERT INTO `sys_auto_code_rule` VALUES (215, 'WORKORDER_CODE', '生产工单编码规则1', NULL, 14, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-09 11:39:59', 'admin', '2022-08-20 09:12:40');
INSERT INTO `sys_auto_code_rule` VALUES (216, 'WORKSTATION_CODE', '工作站编码规则', NULL, 6, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-10 21:55:24', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (217, 'TOOL_TYPE_CODE', '工装夹具类型编码', NULL, 5, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-11 00:21:37', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (218, 'TOOL_CODE', '工装夹具编码规则', NULL, 6, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-11 22:07:17', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (219, 'PROCESS_CODE', '工序编码规则', NULL, 10, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-12 00:09:45', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (220, 'ROUTE_CODE', '工艺流程编码规则', NULL, 5, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-12 23:06:36', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (221, 'TASK_CODE', '生产任务编码规则', NULL, 12, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-15 18:22:29', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (222, 'QC_INDEX_CODE', '检测项编码规则', NULL, 5, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-17 21:57:23', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (223, 'QC_TEMPLATE_CODE', '检测模板编码规则', NULL, 10, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-17 22:43:08', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (224, 'DEFECT_CODE', '常见缺陷编码', NULL, 5, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-19 11:33:27', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (225, 'QC_IQC_CODE', '来料检验单编码规则', NULL, 14, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-19 16:28:07', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (226, 'ITEMRECPT_CODE', '物料入库单编码规则', NULL, 12, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-05-22 20:51:29', 'admin', '2022-05-22 20:53:12');
INSERT INTO `sys_auto_code_rule` VALUES (227, 'CAL_TEAM_CODE', '班组编码规则', NULL, 4, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-06-06 19:54:22', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (228, 'CAL_PLAN_CODE', '排班计划编号', NULL, 11, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-06-06 22:08:10', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (229, 'WM_RTVENDOR_CODE', '供应商退货单编码', NULL, 14, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-06-13 15:48:07', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (230, 'SUBJECT_CODE', '设备点检保养项目编码', NULL, 6, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-06-16 20:27:54', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (231, 'CHECKPLAN_CODE', '点检编码规则', NULL, 11, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-06-16 21:50:00', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (232, 'BATCH_CODE', '批次规则', NULL, 11, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-07-14 12:02:10', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (233, 'ISSUE_CODE', '生产领料单编码', NULL, 16, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-07-17 19:32:10', 'admin', '2022-07-17 19:32:57');
INSERT INTO `sys_auto_code_rule` VALUES (234, '1', '2', NULL, 1, 'Y', '3', 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-07-29 16:34:20', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (236, 'ITEM_CODE', '物料规则', NULL, 10, 'N', '32', 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-08-19 10:44:20', 'admin', '2022-08-19 11:26:02');
INSERT INTO `sys_auto_code_rule` VALUES (237, 'IPQC_CODE', '过程检验单编码', NULL, 16, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-08-29 22:07:13', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (238, 'OQC_CODE', '出货编码规则', NULL, 14, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-01 20:30:31', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (239, 'PBACK_CODE', '生产退料单编码', '生产退料单编码', 16, 'N', '0', 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-03 23:47:11', 'admin', '2022-09-03 23:47:57');
INSERT INTO `sys_auto_code_rule` VALUES (240, 'RTISSUE_CODE', '生产退库单编号规则', NULL, 14, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-15 23:18:40', 'admin', '2022-09-15 23:19:04');
INSERT INTO `sys_auto_code_rule` VALUES (241, 'PRODUCTRECPT_CODE', '产品入库单编码规则', NULL, 13, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-23 10:57:47', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (242, 'REPAIR_CODE', '维修工单编号规则', NULL, 10, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-09-28 21:59:54', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (243, 'PRODUCTSALSE_CODE', '销售出库单编号', NULL, 13, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-10-05 19:45:35', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (244, 'RTSALSE_CODE', '销售退货单编码规则', NULL, 13, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-10-06 21:40:18', '', NULL);
INSERT INTO `sys_auto_code_rule` VALUES (245, 'PACKAGE_CODE', '装箱单编码规则', NULL, 16, 'N', NULL, 'L', 'Y', NULL, NULL, NULL, 0, 0, 'admin', '2022-10-11 01:22:08', '', NULL);

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2024-11-16 21:27:26', '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2024-11-16 21:27:26', '', NULL, '初始化密码 123456');
INSERT INTO `sys_config` VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2024-11-16 21:27:26', '', NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` VALUES (4, '账号自助-验证码开关', 'sys.account.captchaOnOff', 'true', 'Y', 'admin', '2024-11-16 21:27:26', '', NULL, '是否开启验证码功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', '2024-11-16 21:27:26', '', NULL, '是否开启注册用户功能（true开启，false关闭）');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '部门名称',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (100, 0, '0', '青岛科技', 0, '青岛', '15888888888', 'qd@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', 'admin', '2024-11-16 21:45:05');
INSERT INTO `sys_dept` VALUES (101, 100, '0,100', '青岛总公司', 1, '青岛', '15888888888', 'qd@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', 'admin', '2024-11-16 21:45:29');
INSERT INTO `sys_dept` VALUES (102, 100, '0,100', '济南分公司', 2, '济南', '15888888888', 'jn@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', 'admin', '2024-11-16 21:45:46');
INSERT INTO `sys_dept` VALUES (103, 101, '0,100,101', '研发部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL);
INSERT INTO `sys_dept` VALUES (104, 101, '0,100,101', '市场部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL);
INSERT INTO `sys_dept` VALUES (105, 101, '0,100,101', '测试部门', 3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL);
INSERT INTO `sys_dept` VALUES (106, 101, '0,100,101', '财务部门', 4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL);
INSERT INTO `sys_dept` VALUES (107, 101, '0,100,101', '运维部门', 5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL);
INSERT INTO `sys_dept` VALUES (108, 102, '0,100,102', '市场部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL);
INSERT INTO `sys_dept` VALUES (109, 102, '0,100,102', '财务部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, 1, '男', '0', 'sys_user_sex', '', 'info', 'Y', '0', 'admin', '2022-04-07 00:29:32', 'admin', '2022-08-19 20:53:31', '性别男');
INSERT INTO `sys_dict_data` VALUES (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '性别女');
INSERT INTO `sys_dict_data` VALUES (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '性别未知');
INSERT INTO `sys_dict_data` VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '显示菜单');
INSERT INTO `sys_dict_data` VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (8, 1, '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (9, 2, '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '默认分组');
INSERT INTO `sys_dict_data` VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '系统分组');
INSERT INTO `sys_dict_data` VALUES (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '系统默认是');
INSERT INTO `sys_dict_data` VALUES (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '系统默认否');
INSERT INTO `sys_dict_data` VALUES (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '通知');
INSERT INTO `sys_dict_data` VALUES (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '公告');
INSERT INTO `sys_dict_data` VALUES (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '关闭状态');
INSERT INTO `sys_dict_data` VALUES (18, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '新增操作');
INSERT INTO `sys_dict_data` VALUES (19, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '修改操作');
INSERT INTO `sys_dict_data` VALUES (20, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '删除操作');
INSERT INTO `sys_dict_data` VALUES (21, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '授权操作');
INSERT INTO `sys_dict_data` VALUES (22, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '导出操作');
INSERT INTO `sys_dict_data` VALUES (23, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '导入操作');
INSERT INTO `sys_dict_data` VALUES (24, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '强退操作');
INSERT INTO `sys_dict_data` VALUES (25, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '生成操作');
INSERT INTO `sys_dict_data` VALUES (26, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '清空操作');
INSERT INTO `sys_dict_data` VALUES (27, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (28, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (100, 1, '物料', 'ITEM', 'mes_item_product', NULL, 'default', 'N', '0', 'admin', '2022-04-16 16:34:46', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (101, 2, '产品', 'PRODUCT', 'mes_item_product', NULL, 'default', 'N', '0', 'admin', '2022-04-16 16:35:02', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (102, 1, '输入字符', 'INPUTCHAR', 'sys_autocode_parttype', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:33:47', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (103, 2, '当前日期时间', 'NOWDATE', 'sys_autocode_parttype', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:34:07', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (104, 3, '固定字符', 'FIXCHAR', 'sys_autocode_parttype', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:34:27', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (105, 4, '流水号', 'SERIALNO', 'sys_autocode_parttype', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:34:53', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (106, 1, '按年', 'YEAR', 'sys_autocode_cyclemethod', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:35:17', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (107, 2, '按月', 'MONTH', 'sys_autocode_cyclemethod', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:35:29', 'admin', '2022-04-26 15:35:55', NULL);
INSERT INTO `sys_dict_data` VALUES (108, 3, '按天', 'DAY', 'sys_autocode_cyclemethod', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:36:13', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (109, 4, '按小时', 'HOUR', 'sys_autocode_cyclemethod', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:36:34', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (110, 5, '按分钟', 'MINUTE', 'sys_autocode_cyclemethod', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:36:59', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (111, 6, '按传入字符', 'OTHER', 'sys_autocode_cyclemethod', NULL, 'default', 'N', '0', 'admin', '2022-04-26 15:37:19', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (112, 1, '企业客户', 'ENTERPRISE', 'mes_client_type', NULL, 'default', 'N', '0', 'admin', '2022-05-06 20:54:37', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (113, 2, '个人', 'PERSON', 'mes_client_type', NULL, 'default', 'N', '0', 'admin', '2022-05-06 20:55:00', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (114, 1, '优质供应商', 'A', 'mes_vendor_level', NULL, 'default', 'N', '0', 'admin', '2022-05-06 22:45:52', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (115, 2, '正常', 'B', 'mes_vendor_level', NULL, 'default', 'N', '0', 'admin', '2022-05-06 22:46:02', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (116, 3, '重点关注', 'C', 'mes_vendor_level', NULL, 'default', 'N', '0', 'admin', '2022-05-06 22:46:15', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (117, 4, '劣质供应商', 'D', 'mes_vendor_level', NULL, 'default', 'N', '0', 'admin', '2022-05-06 22:46:41', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (118, 5, '黑名单', 'E', 'mes_vendor_level', NULL, 'default', 'N', '0', 'admin', '2022-05-06 22:46:54', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (119, 1, '草稿', 'PREPARE', 'mes_order_status', NULL, 'default', 'N', '0', 'admin', '2022-05-09 10:35:34', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (120, 2, '已确认', 'CONFIRMED', 'mes_order_status', NULL, 'default', 'N', '0', 'admin', '2022-05-09 10:36:37', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (121, 3, '审批中', 'APPROVING', 'mes_order_status', NULL, 'default', 'N', '0', 'admin', '2022-05-09 10:39:30', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (122, 4, '已审批', 'APPROVED', 'mes_order_status', NULL, 'default', 'N', '0', 'admin', '2022-05-09 10:39:45', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (123, 5, '已完成', 'FINISHED', 'mes_order_status', NULL, 'default', 'N', '0', 'admin', '2022-05-09 10:40:07', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (124, 1, '客户订单', 'ORDER', 'mes_workorder_sourcetype', NULL, 'default', 'N', '0', 'admin', '2022-05-09 11:23:47', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (125, 2, '库存备货', 'STORE', 'mes_workorder_sourcetype', NULL, 'default', 'N', '0', 'admin', '2022-05-09 11:24:04', 'admin', '2022-05-09 11:24:24', NULL);
INSERT INTO `sys_dict_data` VALUES (126, 1, '定期维护', 'REGULAR', 'mes_mainten_type', NULL, 'default', 'N', '0', 'admin', '2022-05-10 23:40:57', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (127, 2, '按使用次数维护', 'USAGE', 'mes_mainten_type', NULL, 'default', 'N', '0', 'admin', '2022-05-10 23:41:31', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (128, 1, '在库', 'STORE', 'mes_tool_status', NULL, 'default', 'N', '0', 'admin', '2022-05-11 20:51:13', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (129, 2, '被领用', 'ISSUE', 'mes_tool_status', NULL, 'default', 'N', '0', 'admin', '2022-05-11 20:51:36', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (130, 3, '报废', 'SCRAP', 'mes_tool_status', NULL, 'default', 'N', '0', 'admin', '2022-05-11 20:52:02', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (131, 4, '维修中', 'REPARE', 'mes_tool_status', NULL, 'default', 'N', '0', 'admin', '2022-05-11 20:52:20', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (132, 1, 'S-to-S', 'SS', 'mes_link_type', NULL, 'default', 'N', '0', 'admin', '2022-05-13 21:51:28', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (133, 2, 'F-to-F', 'FF', 'mes_link_type', NULL, 'default', 'N', '0', 'admin', '2022-05-13 21:51:51', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (134, 3, 'S-to-F', 'SF', 'mes_link_type', NULL, 'default', 'N', '0', 'admin', '2022-05-13 21:52:05', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (135, 4, 'F-to-S', 'FS', 'mes_link_type', NULL, 'default', 'N', '0', 'admin', '2022-05-13 21:52:21', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (136, 1, '秒', 'SECOND', 'mes_time_type', NULL, 'default', 'N', '0', 'admin', '2022-05-14 08:41:14', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (137, 2, '分钟', 'MINUTE', 'mes_time_type', NULL, 'default', 'N', '0', 'admin', '2022-05-14 08:41:25', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (138, 3, '小时', 'HOUR', 'mes_time_type', NULL, 'default', 'N', '0', 'admin', '2022-05-14 08:41:46', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (139, 4, '天', 'DAY', 'mes_time_type', NULL, 'default', 'N', '0', 'admin', '2022-05-14 08:41:57', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (140, 5, '周', 'WEEK', 'mes_time_type', NULL, 'default', 'N', '0', 'admin', '2022-05-14 08:42:12', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (141, 6, '月', 'MONTH', 'mes_time_type', NULL, 'default', 'N', '0', 'admin', '2022-05-14 08:42:26', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (142, 1, '尺寸', 'SIZE', 'mes_index_type', NULL, 'default', 'N', '0', 'admin', '2022-05-17 21:26:34', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (143, 2, '外观', 'APPEARANCE', 'mes_index_type', NULL, 'default', 'N', '0', 'admin', '2022-05-17 21:28:19', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (144, 3, '重量', 'WEIGHT', 'mes_index_type', NULL, 'default', 'N', '0', 'admin', '2022-05-17 21:28:31', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (145, 4, '性能', 'PERFORMANCE', 'mes_index_type', NULL, 'default', 'N', '0', 'admin', '2022-05-17 21:29:28', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (146, 5, '成分', 'COMPONENT', 'mes_index_type', NULL, 'default', 'N', '0', 'admin', '2022-05-17 21:30:34', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (147, 1, '[来料检验]', 'IQC', 'mes_qc_type', NULL, 'default', 'N', '0', 'admin', '2022-05-18 09:38:58', 'admin', '2022-05-18 10:38:43', NULL);
INSERT INTO `sys_dict_data` VALUES (148, 2, '[首检]', 'FIRST', 'mes_qc_type', NULL, 'default', 'N', '0', 'admin', '2022-05-18 09:39:19', 'admin', '2022-09-08 22:22:34', NULL);
INSERT INTO `sys_dict_data` VALUES (149, 3, '[末检]', 'FINAL', 'mes_qc_type', NULL, 'default', 'N', '0', 'admin', '2022-05-18 09:39:46', 'admin', '2022-09-08 22:22:39', NULL);
INSERT INTO `sys_dict_data` VALUES (150, 4, '[巡检]', 'PATROL', 'mes_qc_type', NULL, 'default', 'N', '0', 'admin', '2022-05-18 09:40:05', 'admin', '2022-09-08 22:22:55', NULL);
INSERT INTO `sys_dict_data` VALUES (151, 5, '[成品检验]', 'FQC', 'mes_qc_type', NULL, 'default', 'N', '0', 'admin', '2022-05-18 09:40:27', 'admin', '2022-05-18 10:39:08', NULL);
INSERT INTO `sys_dict_data` VALUES (152, 6, '[发货检验]', 'OQC', 'mes_qc_type', NULL, 'default', 'N', '0', 'admin', '2022-05-18 09:40:52', 'admin', '2022-05-18 10:39:14', NULL);
INSERT INTO `sys_dict_data` VALUES (153, 1, '致命缺陷', 'CR', 'mes_defect_level', NULL, 'default', 'N', '0', 'admin', '2022-05-19 10:24:05', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (154, 2, '严重缺陷', 'MAJ', 'mes_defect_level', NULL, 'default', 'N', '0', 'admin', '2022-05-19 10:24:20', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (155, 3, '轻微缺陷', 'MIN', 'mes_defect_level', NULL, 'default', 'N', '0', 'admin', '2022-05-19 10:24:33', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (156, 1, '检验通过', 'ACCEPT', 'mes_qc_result', NULL, 'default', 'N', '0', 'admin', '2022-05-19 16:38:07', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (157, 2, '检验不通过', 'REJECT', 'mes_qc_result', NULL, 'default', 'N', '0', 'admin', '2022-05-19 16:38:22', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (159, 1, '白班', 'SINGLE', 'mes_shift_type', NULL, 'default', 'N', '0', 'admin', '2022-06-06 21:33:56', 'admin', '2022-06-06 21:35:05', NULL);
INSERT INTO `sys_dict_data` VALUES (160, 2, '两班倒', 'SHIFT_TWO', 'mes_shift_type', NULL, 'default', 'N', '0', 'admin', '2022-06-06 21:34:19', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (161, 3, '三班倒', 'SHIFT_THREE', 'mes_shift_type', NULL, 'default', 'N', '0', 'admin', '2022-06-06 21:34:38', 'admin', '2022-06-06 21:34:45', NULL);
INSERT INTO `sys_dict_data` VALUES (162, 1, '按天', 'DAY', 'mes_shift_method', NULL, 'default', 'N', '0', 'admin', '2022-06-06 21:39:51', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (163, 2, '按周', 'WEEK', 'mes_shift_method', NULL, 'default', 'N', '0', 'admin', '2022-06-06 21:40:14', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (164, 3, '按月', 'MONTH', 'mes_shift_method', NULL, 'default', 'N', '0', 'admin', '2022-06-06 21:40:31', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (165, 4, '按季度', 'QUARTER', 'mes_shift_method', NULL, 'default', 'N', '0', 'admin', '2022-06-06 21:40:55', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (166, 1, '注塑', 'ZS', 'mes_calendar_type', NULL, 'default', 'N', '0', 'admin', '2022-06-08 13:27:23', 'admin', '2022-06-11 21:05:57', NULL);
INSERT INTO `sys_dict_data` VALUES (167, 2, '机加工', 'CNC', 'mes_calendar_type', NULL, 'default', 'N', '0', 'admin', '2022-06-08 13:27:35', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (168, 3, '组装', 'ZZ', 'mes_calendar_type', NULL, 'default', 'N', '0', 'admin', '2022-06-08 13:27:55', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (169, 4, '仓库', 'CK', 'mes_calendar_type', NULL, 'default', 'N', '0', 'admin', '2022-06-08 13:28:24', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (170, 1, '设备点检', 'CHECK', 'mes_dvsubject_type', NULL, 'default', 'N', '0', 'admin', '2022-06-16 16:50:51', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (171, 2, '设备保养', 'MAINTEN', 'mes_dvsubject_type', NULL, 'default', 'N', '0', 'admin', '2022-06-16 16:51:19', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (172, 1, '小时', 'HOUR', 'mes_cycle_type', NULL, 'default', 'N', '0', 'admin', '2022-06-16 20:47:54', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (173, 2, '天', 'DAY', 'mes_cycle_type', NULL, 'default', 'N', '0', 'admin', '2022-06-16 20:49:07', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (174, 3, '周', 'WEEK', 'mes_cycle_type', NULL, 'default', 'N', '0', 'admin', '2022-06-16 20:49:21', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (175, 4, '月', 'MONTH', 'mes_cycle_type', NULL, 'default', 'N', '0', 'admin', '2022-06-16 20:49:32', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (176, 5, '季度', 'QUARTER', 'mes_cycle_type', NULL, 'default', 'N', '0', 'admin', '2022-06-16 20:50:00', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (177, 6, '年', 'YEAR', 'mes_cycle_type', NULL, 'default', 'N', '0', 'admin', '2022-06-16 20:50:22', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (178, 1, '设备点检计划', 'CHECK', 'dv_plan_type', NULL, 'default', 'N', '0', 'admin', '2022-06-19 17:03:44', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (179, 2, '设备保养计划', 'MAINTEN', 'dv_plan_type', NULL, 'default', 'N', '0', 'admin', '2022-06-19 17:03:56', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (180, 1, 'QR二维码', 'QR_CODE', 'mes_barcode_formart', NULL, 'default', 'N', '0', 'admin', '2022-08-01 11:06:50', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (181, 2, 'EAN码', 'EAN_CODE', 'mes_barcode_formart', NULL, 'default', 'N', '0', 'admin', '2022-08-01 11:07:40', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (182, 3, 'UPC码', 'UPC_CODE', 'mes_barcode_formart', NULL, 'default', 'N', '0', 'admin', '2022-08-01 11:07:58', 'admin', '2022-08-01 11:08:03', NULL);
INSERT INTO `sys_dict_data` VALUES (183, 1, '物料产品条码', 'ITEM', 'mes_barcode_type', NULL, 'default', 'N', '0', 'admin', '2022-08-01 11:09:07', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (184, 2, '小包装条码', 'BOX_SMALL', 'mes_barcode_type', NULL, 'default', 'N', '0', 'admin', '2022-08-01 11:10:19', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (185, 3, '库存条码', 'STOCK', 'mes_barcode_type', NULL, 'default', 'N', '0', 'admin', '2022-08-01 11:10:40', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (186, 1, '修复成功', 'SUCCESS', 'mes_repair_result', NULL, 'default', 'N', '0', 'admin', '2022-08-06 11:27:48', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (187, 2, '报废', 'SCRAP', 'mes_repair_result', NULL, 'default', 'N', '0', 'admin', '2022-08-06 11:28:18', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (188, 0, '汉族', '0', 'nation_type', NULL, 'default', 'N', '0', 'admin', '2022-08-15 14:26:52', 'admin', '2022-08-15 14:27:35', NULL);
INSERT INTO `sys_dict_data` VALUES (189, 1, '首检', 'FIRST', 'mes_ipqc_type', NULL, 'default', 'N', '0', 'admin', '2022-08-29 20:19:20', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (190, 2, '末检', 'FINAL', 'mes_ipqc_type', NULL, 'default', 'N', '0', 'admin', '2022-08-29 20:19:38', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (191, 3, '自检', 'SELF', 'mes_ipqc_type', NULL, 'default', 'N', '0', 'admin', '2022-08-29 20:19:52', 'admin', '2022-08-29 20:19:57', NULL);
INSERT INTO `sys_dict_data` VALUES (192, 4, '巡检', 'PATROL', 'mes_ipqc_type', NULL, 'default', 'N', '0', 'admin', '2022-08-29 20:20:13', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (193, 5, '点检', 'CHECK', 'mes_ipqc_type', NULL, 'default', 'N', '0', 'admin', '2022-08-29 20:20:28', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (194, 6, '终检', 'FQC', 'mes_ipqc_type', NULL, 'default', 'N', '0', 'admin', '2022-08-29 20:20:44', 'admin', '2022-08-29 20:58:07', NULL);
INSERT INTO `sys_dict_data` VALUES (195, 1, '自行报工', 'SELF', 'mes_feedback_type', NULL, 'default', 'N', '0', 'admin', '2022-10-02 15:52:45', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (196, 2, '统一报工', 'UNI', 'mes_feedback_type', NULL, 'default', 'N', '0', 'admin', '2022-10-02 15:53:03', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (197, 1, '停机', 'STOP', 'mes_machinery_status', NULL, 'default', 'N', '0', 'admin', '2022-10-09 19:24:34', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (198, 2, '生产中', 'WORKING', 'mes_machinery_status', NULL, 'default', 'N', '0', 'admin', '2022-10-09 19:24:54', '', NULL, NULL);
INSERT INTO `sys_dict_data` VALUES (199, 3, '维修中', 'REPAIR', 'mes_machinery_status', NULL, 'default', 'N', '0', 'admin', '2022-10-09 19:25:28', '', NULL, NULL);

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (4, '任务状态', 'sys_job_status', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '任务状态列表');
INSERT INTO `sys_dict_type` VALUES (5, '任务分组', 'sys_job_group', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '任务分组列表');
INSERT INTO `sys_dict_type` VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (10, '系统状态', 'sys_common_status', '0', 'admin', '2022-04-07 00:29:32', '', NULL, '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (100, '物料or产品', 'mes_item_product', '0', 'admin', '2022-04-16 16:34:20', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (101, '编码规则组成类型', 'sys_autocode_parttype', '0', 'admin', '2022-04-26 15:32:42', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (102, '编码规则组成循环方式', 'sys_autocode_cyclemethod', '0', 'admin', '2022-04-26 15:33:02', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (103, '客户类型', 'mes_client_type', '0', 'admin', '2022-05-06 20:53:28', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (104, '供应商级别', 'mes_vendor_level', '0', 'admin', '2022-05-06 22:44:56', 'admin', '2022-05-06 22:45:35', NULL);
INSERT INTO `sys_dict_type` VALUES (105, '单据状态【通用】', 'mes_order_status', '0', 'admin', '2022-05-09 10:34:41', 'admin', '2022-05-09 10:35:10', NULL);
INSERT INTO `sys_dict_type` VALUES (106, '生产工单来源', 'mes_workorder_sourcetype', '0', 'admin', '2022-05-09 11:23:22', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (107, '维护类型表', 'mes_mainten_type', '0', 'admin', '2022-05-10 23:40:36', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (108, '工装夹具状态', 'mes_tool_status', '0', 'admin', '2022-05-11 20:50:46', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (109, '工序关系类型', 'mes_link_type', '0', 'admin', '2022-05-13 21:50:44', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (110, '时间单位', 'mes_time_type', '0', 'admin', '2022-05-14 08:40:53', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (111, '检测项类型', 'mes_index_type', '0', 'admin', '2022-05-17 21:26:05', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (112, '检测类型', 'mes_qc_type', '0', 'admin', '2022-05-18 09:38:32', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (113, '缺陷等级', 'mes_defect_level', '0', 'admin', '2022-05-19 10:23:38', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (114, '检验结果', 'mes_qc_result', '0', 'admin', '2022-05-19 16:37:45', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (116, '倒班方式', 'mes_shift_type', '0', 'admin', '2022-06-06 21:33:17', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (117, '轮班方式', 'mes_shift_method', '0', 'admin', '2022-06-06 21:39:26', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (118, '排班类型', 'mes_calendar_type', '0', 'admin', '2022-06-08 13:26:56', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (119, '设备点检保养项目类型', 'mes_dvsubject_type', '0', 'admin', '2022-06-16 16:50:14', 'admin', '2022-06-16 16:50:29', NULL);
INSERT INTO `sys_dict_type` VALUES (120, '设备点检频率', 'mes_cycle_type', '0', 'admin', '2022-06-16 20:47:19', 'admin', '2022-06-16 20:51:22', NULL);
INSERT INTO `sys_dict_type` VALUES (121, '设备点检保养计划类型', 'dv_plan_type', '0', 'admin', '2022-06-19 17:03:18', 'admin', '2022-06-19 17:04:17', NULL);
INSERT INTO `sys_dict_type` VALUES (122, '条码格式', 'mes_barcode_formart', '0', 'admin', '2022-08-01 11:05:54', 'admin', '2022-08-01 11:06:15', NULL);
INSERT INTO `sys_dict_type` VALUES (123, '条码类型', 'mes_barcode_type', '0', 'admin', '2022-08-01 11:08:27', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (124, '维修结果', 'mes_repair_result', '0', 'admin', '2022-08-06 11:27:05', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (125, '民族', 'nation_type', '0', 'admin', '2022-08-15 14:25:55', 'admin', '2022-08-15 14:26:34', '民族');
INSERT INTO `sys_dict_type` VALUES (126, '过程质量检验类型', 'mes_ipqc_type', '0', 'admin', '2022-08-29 20:18:48', 'admin', '2022-08-29 20:18:59', NULL);
INSERT INTO `sys_dict_type` VALUES (127, '生产报工类型', 'mes_feedback_type', '0', 'admin', '2022-10-02 15:51:21', '', NULL, NULL);
INSERT INTO `sys_dict_type` VALUES (128, '设备状态', 'mes_machinery_status', '0', 'admin', '2022-10-09 19:23:54', '', NULL, NULL);

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`  (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务调度表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_job
-- ----------------------------
INSERT INTO `sys_job` VALUES (1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin', '2024-11-16 21:27:26', '', NULL, '');
INSERT INTO `sys_job` VALUES (2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', 'admin', '2024-11-16 21:27:26', '', NULL, '');
INSERT INTO `sys_job` VALUES (3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?', '3', '1', '1', 'admin', '2024-11-16 21:27:26', '', NULL, '');

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`  (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '异常信息',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务调度日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_job_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor`  (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '提示消息',
  `login_time` datetime NULL DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 108 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统访问记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_logininfor
-- ----------------------------
INSERT INTO `sys_logininfor` VALUES (100, 'admin', '127.0.0.1', '内网IP', 'Chrome 13', 'Windows 10', '0', '登录成功', '2024-11-16 21:44:19');
INSERT INTO `sys_logininfor` VALUES (101, 'admin', '127.0.0.1', '内网IP', 'Chrome 13', 'Windows 10', '0', '登录成功', '2024-11-16 21:51:51');
INSERT INTO `sys_logininfor` VALUES (102, 'admin', '127.0.0.1', '内网IP', 'Chrome 13', 'Windows 10', '1', '验证码已失效', '2024-11-20 15:31:48');
INSERT INTO `sys_logininfor` VALUES (103, 'admin', '127.0.0.1', '内网IP', 'Chrome 13', 'Windows 10', '0', '登录成功', '2024-11-20 15:31:52');
INSERT INTO `sys_logininfor` VALUES (104, 'admin', '127.0.0.1', '内网IP', 'Chrome 13', 'Windows 10', '0', '登录成功', '2024-11-20 17:09:26');
INSERT INTO `sys_logininfor` VALUES (105, 'admin', '127.0.0.1', '内网IP', 'Chrome 13', 'Windows 10', '0', '登录成功', '2024-11-21 16:18:03');
INSERT INTO `sys_logininfor` VALUES (106, 'admin', '127.0.0.1', '内网IP', 'Chrome 13', 'Windows 10', '0', '登录成功', '2024-11-21 17:04:47');
INSERT INTO `sys_logininfor` VALUES (107, 'admin', '127.0.0.1', '内网IP', 'Chrome 13', 'Windows 10', '0', '登录成功', '2024-11-22 16:13:35');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由参数',
  `is_frame` int NULL DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2256 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 1, 'system', NULL, '', 1, 0, 'M', '0', '0', '', 'system', 'admin', '2022-04-07 00:29:31', '', NULL, '系统管理目录');
INSERT INTO `sys_menu` VALUES (2, '系统监控', 0, 2, 'monitor', NULL, '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', '2022-04-07 00:29:31', '', NULL, '系统监控目录');
INSERT INTO `sys_menu` VALUES (3, '系统工具', 0, 3, 'tool', NULL, '', 1, 0, 'M', '0', '0', '', 'tool', 'admin', '2022-04-07 00:29:31', '', NULL, '系统工具目录');
INSERT INTO `sys_menu` VALUES (100, '用户管理', 2210, 4, 'user', 'system/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', '2022-04-07 00:29:31', 'admin', '2022-08-13 21:41:42', '用户管理菜单');
INSERT INTO `sys_menu` VALUES (101, '角色管理', 2210, 3, 'role', 'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', '2022-04-07 00:29:31', 'admin', '2022-08-13 21:42:03', '角色管理菜单');
INSERT INTO `sys_menu` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', '2022-04-07 00:29:31', '', NULL, '菜单管理菜单');
INSERT INTO `sys_menu` VALUES (103, '部门管理', 2210, 1, 'dept', 'system/dept/index', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', '2022-04-07 00:29:31', 'admin', '2022-08-13 21:42:27', '部门管理菜单');
INSERT INTO `sys_menu` VALUES (104, '岗位管理', 2210, 2, 'post', 'system/post/index', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', '2022-04-07 00:29:31', 'admin', '2022-08-13 21:42:38', '岗位管理菜单');
INSERT INTO `sys_menu` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', '2022-04-07 00:29:31', '', NULL, '字典管理菜单');
INSERT INTO `sys_menu` VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', '2022-04-07 00:29:31', '', NULL, '参数设置菜单');
INSERT INTO `sys_menu` VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 'admin', '2022-04-07 00:29:31', '', NULL, '通知公告菜单');
INSERT INTO `sys_menu` VALUES (108, '日志管理', 1, 9, 'log', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', '2022-04-07 00:29:31', '', NULL, '日志管理菜单');
INSERT INTO `sys_menu` VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', '2022-04-07 00:29:31', '', NULL, '在线用户菜单');
INSERT INTO `sys_menu` VALUES (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job', 'admin', '2022-04-07 00:29:31', '', NULL, '定时任务菜单');
INSERT INTO `sys_menu` VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', 1, 0, 'C', '0', '0', 'monitor:druid:list', 'druid', 'admin', '2022-04-07 00:29:31', '', NULL, '数据监控菜单');
INSERT INTO `sys_menu` VALUES (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', 1, 0, 'C', '0', '0', 'monitor:server:list', 'server', 'admin', '2022-04-07 00:29:31', '', NULL, '服务监控菜单');
INSERT INTO `sys_menu` VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 'admin', '2022-04-07 00:29:31', '', NULL, '缓存监控菜单');
INSERT INTO `sys_menu` VALUES (114, '表单构建', 3, 1, 'build', 'tool/build/index', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 'admin', '2022-04-07 00:29:31', '', NULL, '表单构建菜单');
INSERT INTO `sys_menu` VALUES (115, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin', '2022-04-07 00:29:31', '', NULL, '代码生成菜单');
INSERT INTO `sys_menu` VALUES (116, '系统接口', 3, 3, 'swagger', 'tool/swagger/index', '', 1, 0, 'C', '0', '0', 'tool:swagger:list', 'swagger', 'admin', '2022-04-07 00:29:31', '', NULL, '系统接口菜单');
INSERT INTO `sys_menu` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', '2022-04-07 00:29:31', '', NULL, '操作日志菜单');
INSERT INTO `sys_menu` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', '2022-04-07 00:29:31', '', NULL, '登录日志菜单');
INSERT INTO `sys_menu` VALUES (1001, '用户查询', 100, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1002, '用户新增', 100, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1003, '用户修改', 100, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1004, '用户删除', 100, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1005, '用户导出', 100, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1006, '用户导入', 100, 6, '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1007, '重置密码', 100, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1008, '角色查询', 101, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1009, '角色新增', 101, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1010, '角色修改', 101, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1011, '角色删除', 101, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1012, '角色导出', 101, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1013, '菜单查询', 102, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1014, '菜单新增', 102, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1015, '菜单修改', 102, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1016, '菜单删除', 102, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1017, '部门查询', 103, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1018, '部门新增', 103, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1019, '部门修改', 103, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1020, '部门删除', 103, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1021, '岗位查询', 104, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1022, '岗位新增', 104, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1023, '岗位修改', 104, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1024, '岗位删除', 104, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1025, '岗位导出', 104, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1026, '字典查询', 105, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1027, '字典新增', 105, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1028, '字典修改', 105, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1029, '字典删除', 105, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1030, '字典导出', 105, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1031, '参数查询', 106, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1032, '参数新增', 106, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1033, '参数修改', 106, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1034, '参数删除', 106, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1035, '参数导出', 106, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1036, '公告查询', 107, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1037, '公告新增', 107, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1038, '公告修改', 107, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1039, '公告删除', 107, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1040, '操作查询', 500, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1041, '操作删除', 500, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1042, '日志导出', 500, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1043, '登录查询', 501, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1044, '登录删除', 501, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1045, '日志导出', 501, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1046, '在线查询', 109, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1047, '批量强退', 109, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1048, '单条强退', 109, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1049, '任务查询', 110, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1050, '任务新增', 110, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1051, '任务修改', 110, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1052, '任务删除', 110, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1053, '状态修改', 110, 5, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1054, '任务导出', 110, 7, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1055, '生成查询', 115, 1, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1056, '生成修改', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1057, '生成删除', 115, 3, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1058, '导入代码', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1059, '预览代码', 115, 4, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1060, '生成代码', 115, 5, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 'admin', '2022-04-07 00:29:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2000, '主数据', 0, 4, 'mes/md', NULL, NULL, 1, 0, 'M', '0', '0', '', 'build', 'admin', '2022-04-16 14:41:55', 'admin', '2022-08-18 11:44:29', '');
INSERT INTO `sys_menu` VALUES (2001, '物料产品管理', 2000, 1, 'mditem', 'mes/md/mditem/index', NULL, 1, 0, 'C', '0', '0', 'ms:md:mditem', 'excel', 'admin', '2022-04-16 14:44:00', 'admin', '2022-05-04 18:13:08', '');
INSERT INTO `sys_menu` VALUES (2002, '物料产品分类', 2000, 2, 'itemtype', 'mes/md/itemtype/index', NULL, 1, 0, 'C', '0', '0', 'mes:md:itemtype', 'nested', 'admin', '2022-04-16 16:03:18', 'admin', '2022-04-16 16:05:56', '');
INSERT INTO `sys_menu` VALUES (2003, '编码规则', 1, 10, 'autocodeRule', 'system/autocode/index', NULL, 1, 0, 'C', '0', '0', 'system:autocode:rule', 'code', 'admin', '2022-04-24 21:43:28', 'admin', '2022-04-24 21:49:31', '');
INSERT INTO `sys_menu` VALUES (2004, '编码规则查询', 2003, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:autocode:rule:list', '#', 'admin', '2022-04-24 21:46:05', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2005, '编码规则新增', 2003, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:autocode:rule:add', '#', 'admin', '2022-04-24 21:46:30', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2006, '编码规则更新', 2003, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:autocode:rule:edit', '#', 'admin', '2022-04-24 21:46:59', 'admin', '2022-04-24 21:47:42', '');
INSERT INTO `sys_menu` VALUES (2007, '编码规则删除', 2003, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:autocode:rule:remove', '#', 'admin', '2022-04-24 21:47:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2008, '新增物料分类', 2002, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:itemtype:add', '#', 'admin', '2022-04-27 16:46:06', 'admin', '2022-08-16 16:15:39', '');
INSERT INTO `sys_menu` VALUES (2009, '更新物料分类', 2002, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:itemtype:edit', '#', 'admin', '2022-04-27 16:46:51', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2010, '删除物料分类', 2002, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:itemtype:remove', '#', 'admin', '2022-04-27 16:47:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2011, '新增物料产品', 2001, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:mditem:add', '#', 'admin', '2022-04-27 16:47:46', 'admin', '2022-08-17 08:56:36', '');
INSERT INTO `sys_menu` VALUES (2012, '更新物料产品', 2001, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:mditem:edit', '#', 'admin', '2022-04-27 16:48:11', 'admin', '2022-05-04 18:05:40', '');
INSERT INTO `sys_menu` VALUES (2013, '删除物料产品', 2001, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:mditem:remove', '#', 'admin', '2022-04-27 16:48:36', 'admin', '2022-05-04 18:05:55', '');
INSERT INTO `sys_menu` VALUES (2014, '计量单位', 2000, 3, 'unitmeasure', 'mes/md/unitmeasure/index', NULL, 1, 0, 'C', '0', '0', 'mes:md:unitmeasure', 'tab', 'admin', '2022-04-27 18:08:08', 'admin', '2022-04-27 21:27:43', '');
INSERT INTO `sys_menu` VALUES (2015, '新增单位', 2014, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:unitmeasure:add', '#', 'admin', '2022-04-27 18:14:06', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2016, '更新单位', 2014, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:unitmeasure:edit', '#', 'admin', '2022-04-27 18:14:25', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2017, '删除单位', 2014, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:unitmeasure:remove', '#', 'admin', '2022-04-27 18:14:44', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2018, '物料明细查看', 2001, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:mditem:query', '#', 'admin', '2022-05-04 17:51:19', 'admin', '2022-05-04 18:06:04', '');
INSERT INTO `sys_menu` VALUES (2019, '物料分类明细', 2002, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:itemtype:query', '#', 'admin', '2022-05-04 17:52:22', 'admin', '2022-08-16 16:16:59', '');
INSERT INTO `sys_menu` VALUES (2020, '单位明细', 2014, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:unitmeasure:query', '#', 'admin', '2022-05-04 17:54:06', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2021, '物料产品列表查询', 2001, 0, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:mditem:list', '#', 'admin', '2022-05-04 18:04:36', 'admin', '2022-05-04 18:05:23', '');
INSERT INTO `sys_menu` VALUES (2022, '物料分类列表查询', 2002, 0, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:itemtype:list', '#', 'admin', '2022-05-04 18:08:57', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2023, '单位列表查询', 2014, 0, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:unitmeasure:list', '#', 'admin', '2022-05-04 18:09:25', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2024, '客户管理', 2000, 4, 'client', 'mes/md/client/index', NULL, 1, 0, 'C', '0', '0', 'mes:md:client', 'people', 'admin', '2022-05-06 20:37:28', 'admin', '2022-05-06 20:43:03', '');
INSERT INTO `sys_menu` VALUES (2025, '客户列表查询', 2024, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:client:list', '#', 'admin', '2022-05-06 20:38:07', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2026, '客户新增', 2024, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:client:add', '#', 'admin', '2022-05-06 20:38:33', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2027, '客户编辑', 2024, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:client:edit', '#', 'admin', '2022-05-06 20:38:55', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2028, '客户明细', 2024, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:client:query', '#', 'admin', '2022-05-06 20:39:23', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2029, '客户删除', 2024, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:client:remove', '#', 'admin', '2022-05-06 20:39:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2030, '供应商管理', 2000, 5, 'vendor', 'mes/md/vendor/index', NULL, 1, 0, 'C', '0', '0', 'mes:md:vendor', 'peoples', 'admin', '2022-05-06 22:34:09', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2031, '供应商列表查询', 2030, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:vendor:list', '#', 'admin', '2022-05-06 22:34:34', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2032, '供应商查看', 2030, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:vendor:query', '#', 'admin', '2022-05-06 22:35:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2033, '供应商新增', 2030, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:vendor:add', '#', 'admin', '2022-05-06 22:35:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2034, '供应商编辑', 2030, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:vendor:edit', '#', 'admin', '2022-05-06 22:35:59', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2035, '供应商删除', 2030, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:vendor:remove', '#', 'admin', '2022-05-06 22:36:21', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2036, '车间设置', 2000, 6, 'workshop', 'mes/md/workshop/index', NULL, 1, 0, 'C', '0', '0', 'mes:md:workshop', 'zip', 'admin', '2022-05-07 16:40:48', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2037, '车间列表查询', 2036, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workshop:list', '#', 'admin', '2022-05-07 16:41:09', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2038, '车间查看', 2036, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workshop:query', '#', 'admin', '2022-05-07 16:41:36', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2039, '车间新增', 2036, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workshop:add', '#', 'admin', '2022-05-07 16:41:58', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2040, '车间修改', 2036, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workshop:edit', '#', 'admin', '2022-05-07 16:42:18', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2041, '车间删除', 2036, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workshop:remove', '#', 'admin', '2022-05-07 16:42:39', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2042, '仓储管理', 0, 5, 'mes/wm', NULL, NULL, 1, 0, 'M', '0', '0', '', 'zip', 'admin', '2022-05-07 21:06:28', 'admin', '2022-08-13 21:44:45', '');
INSERT INTO `sys_menu` VALUES (2043, '仓库设置', 2042, 1, 'warehouse', 'mes/wm/warehouse/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:warehouse', 'dict', 'admin', '2022-05-07 21:07:56', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2044, '仓库清单查询', 2043, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:warehouse:list', '#', 'admin', '2022-05-07 21:08:25', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2045, '仓库查看', 2043, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:warehouse:query', '#', 'admin', '2022-05-07 21:08:56', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2046, '仓库新增', 2043, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:warehouse:add', '#', 'admin', '2022-05-07 21:09:29', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2047, '仓库编辑', 2043, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:warehouse:edit', '#', 'admin', '2022-05-07 21:09:51', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2048, '仓库删除', 2043, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:warehouse:remove', '#', 'admin', '2022-05-07 21:10:13', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2049, '库区列表查询', 2043, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:list', '#', 'admin', '2022-05-08 18:19:51', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2050, '库区查看', 2043, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:query', '#', 'admin', '2022-05-08 18:20:28', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2051, '库区新增', 2043, 8, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:add', '#', 'admin', '2022-05-08 18:20:57', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2052, '库区编辑', 2043, 9, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:edit', '#', 'admin', '2022-05-08 18:21:23', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2053, '库区删除', 2043, 10, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:remove', '#', 'admin', '2022-05-08 18:21:57', 'admin', '2022-05-08 18:24:34', '');
INSERT INTO `sys_menu` VALUES (2054, '库位列表查询', 2043, 11, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:area:list', '#', 'admin', '2022-05-08 18:22:30', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2055, '库位查看', 2043, 12, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:area:query', '#', 'admin', '2022-05-08 18:22:54', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2056, '库位新增', 2043, 13, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:area:add', '#', 'admin', '2022-05-08 18:23:17', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2057, '库位编辑', 2043, 14, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:area:edit', '#', 'admin', '2022-05-08 18:23:43', 'admin', '2022-05-08 18:23:53', '');
INSERT INTO `sys_menu` VALUES (2058, '库位删除', 2043, 15, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:area:remove', '#', 'admin', '2022-05-08 18:24:58', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2059, '设备管理', 0, 6, 'mes/dv', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'redis', 'admin', '2022-05-08 19:10:32', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2060, '设备类型设置', 2059, 1, 'machinerytype', 'mes/dv/machinerytype/index', NULL, 1, 0, 'C', '0', '0', 'mes:dv:machinerytype', 'swagger', 'admin', '2022-05-08 19:11:58', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2061, '设备类型列表', 2060, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:list', '#', 'admin', '2022-05-08 19:12:35', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2062, '设备类型查看', 2060, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:query', '#', 'admin', '2022-05-08 19:12:58', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2063, '设备类型新增', 2060, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:add', '#', 'admin', '2022-05-08 19:13:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2064, '设备类型编辑', 2060, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:edit', '#', 'admin', '2022-05-08 19:13:33', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2065, '设备类型删除', 2060, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:remove', '#', 'admin', '2022-05-08 19:13:59', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2066, '设备台账', 2059, 2, 'machinery', 'mes/dv/machinery/index', NULL, 1, 0, 'C', '0', '0', 'mes:dv:machinery', 'system', 'admin', '2022-05-08 21:28:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2067, '设备列表查询', 2066, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinery:list', '#', 'admin', '2022-05-08 21:29:14', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2068, '设备查看', 2066, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinery:query', '#', 'admin', '2022-05-08 21:29:35', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2069, '设备新增', 2066, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinery:add', '#', 'admin', '2022-05-08 21:30:00', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2070, '设备编辑', 2066, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinery:edit', '#', 'admin', '2022-05-08 21:30:16', 'admin', '2022-05-08 21:30:40', '');
INSERT INTO `sys_menu` VALUES (2071, '设备删除', 2066, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:machinery:remove', '#', 'admin', '2022-05-08 21:30:32', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2072, '生产管理', 0, 8, 'mes/pro', NULL, NULL, 1, 0, 'M', '0', '0', '', 'switch', 'admin', '2022-05-09 10:58:07', 'admin', '2022-05-11 00:07:21', '');
INSERT INTO `sys_menu` VALUES (2073, '生产工单', 2072, 1, 'workorder', 'mes/pro/workorder/index', NULL, 1, 0, 'C', '0', '0', 'mes:pro:workorder', 'log', 'admin', '2022-05-09 10:59:26', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2074, '工单列表查询', 2073, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorder:list', '#', 'admin', '2022-05-09 11:00:02', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2075, '工单查看', 2073, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorder:query', '#', 'admin', '2022-05-09 11:00:24', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2076, '工单新增', 2073, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorder:add', '#', 'admin', '2022-05-09 11:00:55', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2077, '工单编辑', 2073, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorder:edit', '#', 'admin', '2022-05-09 11:01:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2078, '工单删除', 2073, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorder:remove', '#', 'admin', '2022-05-09 11:01:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2079, 'BOM列表查询', 2001, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:bom:list', '#', 'admin', '2022-05-10 10:17:25', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2080, 'BOM查看', 2001, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:bom:query', '#', 'admin', '2022-05-10 10:17:55', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2081, 'BOM新增', 2001, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:bom:add', '#', 'admin', '2022-05-10 10:18:22', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2082, 'BOM编辑', 2001, 8, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:bom:edit', '#', 'admin', '2022-05-10 10:18:44', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2083, 'BOM删除', 2001, 9, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:bom:remove', '#', 'admin', '2022-05-10 10:19:05', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2084, '工单BOM列表查询', 2073, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorderbom:list', '#', 'admin', '2022-05-10 16:17:25', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2085, '工单BOM查看', 2073, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorderbom:query', '#', 'admin', '2022-05-10 16:17:58', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2086, '工单BOM编辑', 2073, 8, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorderbom:edit', '#', 'admin', '2022-05-10 16:18:28', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2087, '工单BOM删除', 2073, 9, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorderbom:remove', '#', 'admin', '2022-05-10 16:18:59', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2088, '工作站', 2000, 7, 'workstation', 'mes/md/workstation/index', NULL, 1, 0, 'C', '0', '0', 'mes:md:workstation', 'job', 'admin', '2022-05-10 21:44:36', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2089, '工作站列表查询', 2088, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workstation:list', '#', 'admin', '2022-05-10 21:45:11', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2090, '工作站查看', 2088, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workstation:query', '#', 'admin', '2022-05-10 21:45:32', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2091, '工作站新增', 2088, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workstation:add', '#', 'admin', '2022-05-10 21:45:48', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2092, '工作站编辑', 2088, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workstation:edit', '#', 'admin', '2022-05-10 21:46:05', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2093, '工作站删除', 2088, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workstation:remove', '#', 'admin', '2022-05-10 21:46:28', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2094, '工作站导出', 2088, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:md:workstation:export', '#', 'admin', '2022-05-10 21:47:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2095, '工装夹具管理', 0, 7, 'tm', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'lock', 'admin', '2022-05-11 00:07:13', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2096, '类型设置', 2095, 1, 'tooltype', 'mes/tm/tooltype/index', NULL, 1, 0, 'C', '0', '0', 'mes:tm:tooltype', 'tree', 'admin', '2022-05-11 00:08:25', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2097, '类型列表查询', 2096, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tooltype:list', '#', 'admin', '2022-05-11 00:09:01', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2098, '类型查看', 2096, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tooltype:query', '#', 'admin', '2022-05-11 00:09:27', 'admin', '2022-05-11 00:09:48', '');
INSERT INTO `sys_menu` VALUES (2099, '类型新增', 2096, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tooltype:add', '#', 'admin', '2022-05-11 00:10:14', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2100, '类型编辑', 2096, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tooltype:edit', '#', 'admin', '2022-05-11 00:10:35', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2101, '类型删除', 2096, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tooltype:remove', '#', 'admin', '2022-05-11 00:10:56', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2102, '工装夹具台账', 2095, 2, 'tool', 'mes/tm/tool/index', NULL, 1, 0, 'C', '0', '0', 'mes:tm:tool', 'date-range', 'admin', '2022-05-11 21:23:23', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2103, '工装夹具列表查询', 2102, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tool:list', '#', 'admin', '2022-05-11 21:23:53', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2104, '工装夹具查看', 2102, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tool:query', '#', 'admin', '2022-05-11 21:24:15', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2105, '工装夹具新增', 2102, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tool:add', '#', 'admin', '2022-05-11 21:24:35', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2106, '工装夹具编辑', 2102, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tool:edit', '#', 'admin', '2022-05-11 21:24:56', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2107, '工装夹具删除', 2102, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tool:remove', '#', 'admin', '2022-05-11 21:25:23', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2108, '工装夹具导出', 2102, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:tm:tool:export', '#', 'admin', '2022-05-11 21:25:46', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2109, '工序设置', 2072, 2, 'process', 'mes/pro/process/index', NULL, 1, 0, 'C', '0', '0', 'mes:pro:process', 'druid', 'admin', '2022-05-12 00:03:58', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2110, '工序列表查询', 2109, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:process:list', '#', 'admin', '2022-05-12 00:04:23', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2111, '工序查看', 2109, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:process:query', '#', 'admin', '2022-05-12 00:04:39', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2112, '工序新增', 2109, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:process:add', '#', 'admin', '2022-05-12 00:04:54', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2113, '工序编辑', 2109, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:process:edit', '#', 'admin', '2022-05-12 00:05:10', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2114, '工序删除', 2109, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:process:remove', '#', 'admin', '2022-05-12 00:05:28', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2115, '工序导出', 2109, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:process:export', '#', 'admin', '2022-05-12 00:05:41', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2116, '工艺流程', 2072, 3, 'proroute', 'mes/pro/proroute/index', NULL, 1, 0, 'C', '0', '0', 'mes:pro:proroute', 'nested', 'admin', '2022-05-12 23:08:26', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2117, '工艺流程列表查询', 2116, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:proroute:list', '#', 'admin', '2022-05-12 23:08:51', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2118, '工艺流程查看', 2116, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:proroute:query', '#', 'admin', '2022-05-12 23:09:13', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2119, '工艺流程新增', 2116, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:proroute:add', '#', 'admin', '2022-05-12 23:09:36', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2120, '工艺流程编辑', 2116, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:proroute:edit', '#', 'admin', '2022-05-12 23:09:53', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2121, '工艺流程删除', 2116, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:proroute:remove', '#', 'admin', '2022-05-12 23:10:11', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2122, '工艺流程导出', 2116, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:pro:proroute:export', '#', 'admin', '2022-05-12 23:10:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2123, '生产排产', 2072, 4, 'proschedule', 'mes/pro/schedule/index', NULL, 1, 0, 'C', '0', '0', 'mes:pro:schedule', 'build', 'admin', '2022-05-15 15:01:21', 'admin', '2022-05-15 15:01:36', '');
INSERT INTO `sys_menu` VALUES (2124, '质量管理', 0, 9, 'mes/qc', NULL, NULL, 1, 0, 'M', '0', '0', '', 'time-range', 'admin', '2022-05-17 21:51:23', 'admin', '2022-08-13 21:45:33', '');
INSERT INTO `sys_menu` VALUES (2125, '检测项设置', 2124, 2, 'qcindex', 'mes/qc/qcindex/index', NULL, 1, 0, 'C', '0', '0', 'mes:qc:qcindex', 'checkbox', 'admin', '2022-05-17 21:52:33', 'admin', '2022-05-19 11:26:35', '');
INSERT INTO `sys_menu` VALUES (2126, '检测项列表查询', 2125, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcindex:list', '#', 'admin', '2022-05-17 21:53:07', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2127, '检测项查看', 2125, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcindex:query', '#', 'admin', '2022-05-17 21:53:36', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2128, '检测项新增', 2125, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcindex:add', '#', 'admin', '2022-05-17 21:54:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2129, '检测项编辑', 2125, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcindex:edit', '#', 'admin', '2022-05-17 21:54:24', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2130, '检测项删除', 2125, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcindex:remove', '#', 'admin', '2022-05-17 21:54:50', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2131, '检测项导出', 2125, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcindex:export', '#', 'admin', '2022-05-17 21:55:12', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2132, '检测模板', 2124, 3, 'qctemplate', 'mes/qc/qctemplate/index', NULL, 1, 0, 'C', '0', '0', 'mes:qc:qctemplate', 'example', 'admin', '2022-05-17 22:23:48', 'admin', '2022-05-19 11:26:41', '');
INSERT INTO `sys_menu` VALUES (2133, '检测模板列表查询', 2132, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qctemplate:list', '#', 'admin', '2022-05-17 22:24:05', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2134, '检测模板查看', 2132, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qctemplate:query', '#', 'admin', '2022-05-17 22:24:32', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2135, '检测模板新增', 2132, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qctemplate:add', '#', 'admin', '2022-05-17 22:24:50', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2136, '检测模板编辑', 2132, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qctemplate:edit', '#', 'admin', '2022-05-17 22:25:09', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2137, '检测模板删除', 2132, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qctemplate:remove', '#', 'admin', '2022-05-17 22:25:32', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2138, '常见缺陷', 2124, 1, 'qcdefect', 'mes/qc/qcdefect/index', NULL, 1, 0, 'C', '0', '0', 'mes:qc:qcdefect', 'bug', 'admin', '2022-05-19 11:26:15', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2139, '缺陷列表查询', 2138, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcdefect:list', '#', 'admin', '2022-05-19 11:27:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2140, '缺陷查看', 2138, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcdefect:query', '#', 'admin', '2022-05-19 11:27:28', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2141, '缺陷新增', 2138, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcdefect:add', '#', 'admin', '2022-05-19 11:27:47', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2142, '缺陷编辑', 2138, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcdefect:edit', '#', 'admin', '2022-05-19 11:28:11', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2143, '缺陷删除', 2138, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:qcdefect:remove', '#', 'admin', '2022-05-19 11:28:31', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2144, '来料检验', 2124, 4, 'iqc', 'mes/qc/iqc/iqc', NULL, 1, 0, 'C', '0', '0', 'mes:qc:iqc', 'edit', 'admin', '2022-05-19 16:32:24', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2145, '来料检验单列表查询', 2144, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:iqc:list', '#', 'admin', '2022-05-19 16:33:18', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2146, '来料检验单查看', 2144, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:iqc:query', '#', 'admin', '2022-05-19 16:33:41', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2147, '来料检验单新增', 2144, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:iqc:add', '#', 'admin', '2022-05-19 16:34:01', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2148, '来料检验单编辑', 2144, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:iqc:edit', '#', 'admin', '2022-05-19 16:34:19', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2149, '来料检验单删除', 2144, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:iqc:remove', '#', 'admin', '2022-05-19 16:34:35', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2150, '来料检验单导出', 2144, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:iqc:export', '#', 'admin', '2022-05-19 16:34:56', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2151, '物料入库单', 2042, 3, 'itemrecpt', 'mes/wm/itemrecpt/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:itemrecpt', 'form', 'admin', '2022-05-22 17:35:11', 'admin', '2022-06-14 16:22:12', '');
INSERT INTO `sys_menu` VALUES (2152, '物料入库单列表查询', 2151, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:list', '#', 'admin', '2022-05-22 17:35:39', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2153, '物料入库单查看', 2151, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:query', '#', 'admin', '2022-05-22 17:35:53', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2154, '物料入库单新增', 2151, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:add', '#', 'admin', '2022-05-22 17:36:09', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2155, '物料入库单编辑', 2151, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:edit', '#', 'admin', '2022-05-22 17:36:23', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2156, '物料入库单删除', 2151, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:remove', '#', 'admin', '2022-05-22 17:36:39', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2157, '库存现有量', 2042, 2, 'wmstock', 'mes/wm/wmstock/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:wmstock', 'chart', 'admin', '2022-05-30 23:36:40', 'admin', '2022-06-14 16:22:19', '');
INSERT INTO `sys_menu` VALUES (2158, '库存现有量列表查询', 2157, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:wmstock:list', '#', 'admin', '2022-05-30 23:37:07', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2159, '库存现有量导出', 2157, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:wmstock:export', '#', 'admin', '2022-05-30 23:37:30', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2160, '排班管理', 0, 10, 'cal', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'peoples', 'admin', '2022-06-05 21:52:34', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2161, '班组设置    ', 2160, 1, 'team', 'mes/cal/team/index', NULL, 1, 0, 'C', '0', '0', 'mes:cal:team', 'peoples', 'admin', '2022-06-05 21:53:23', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2162, '班组列表查看', 2161, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:team:list', '#', 'admin', '2022-06-05 21:56:58', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2163, '班组查看', 2161, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:team:query', '#', 'admin', '2022-06-05 21:57:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2164, '班组新增', 2161, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:team:add', '#', 'admin', '2022-06-05 21:57:40', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2165, '班组编辑', 2161, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:team:edit', '#', 'admin', '2022-06-05 21:57:59', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2168, '排班计划', 2160, 2, 'plan', 'mes/cal/plan/index', NULL, 1, 0, 'C', '0', '0', 'mes:cal:plan', 'build', 'admin', '2022-06-06 21:47:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2169, '排班计划列表查询', 2168, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:plan:list', '#', 'admin', '2022-06-06 21:47:27', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2170, '排班计划查看', 2168, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:plan:query', '#', 'admin', '2022-06-06 21:47:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2171, '排班计划新增', 2168, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:plan:add', '#', 'admin', '2022-06-06 21:48:13', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2172, '排班计划编辑', 2168, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:plan:edit', '#', 'admin', '2022-06-06 21:48:30', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2173, '排班计划删除', 2168, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:plan:remove', '#', 'admin', '2022-06-06 21:48:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2174, '节假日设置', 2160, 3, 'holiday', 'mes/cal/holiday/index', NULL, 1, 0, 'C', '0', '0', 'mes:cal:holiday', 'date', 'admin', '2022-06-07 23:25:52', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2175, '节假日查看', 2174, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:holiday:query', '#', 'admin', '2022-06-07 23:26:18', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2176, '节假日设置', 2174, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:holiday:edit', '#', 'admin', '2022-06-07 23:26:41', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2177, '排班日历', 2160, 4, 'calendar', 'mes/cal/calendar/index', NULL, 1, 0, 'C', '0', '0', 'mes:cal:calendar', 'date-range', 'admin', '2022-06-09 21:08:06', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2178, '查看排班日历', 2177, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:cal:calendar:list', '#', 'admin', '2022-06-09 21:08:52', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2179, '供应商退货', 2042, 4, 'rtvendor', 'mes/wm/rtvendor/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:rtvendor', 'link', 'admin', '2022-06-13 15:29:43', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2180, '退货单列表查询', 2179, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtvendor:list', '#', 'admin', '2022-06-13 15:30:45', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2181, '退货单信息查看', 2179, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtvendor:query', '#', 'admin', '2022-06-13 15:31:13', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2182, '退货单新增', 2179, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtvendor:add', '#', 'admin', '2022-06-13 15:31:32', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2183, '退货单编辑', 2179, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtvendor:edit', '#', 'admin', '2022-06-13 15:31:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2184, '退货单删除', 2179, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtvendor:remove', '#', 'admin', '2022-06-13 15:32:06', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2185, '点检保养项目', 2059, 3, 'dvsubject', 'mes/dv/subject/index', NULL, 1, 0, 'C', '0', '0', 'mes:dv:dvsubject', 'cascader', 'admin', '2022-06-16 20:17:37', 'admin', '2022-06-16 20:20:44', '');
INSERT INTO `sys_menu` VALUES (2186, '项目列表查询', 2185, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:dvsubject:list', '#', 'admin', '2022-06-16 20:18:10', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2187, '项目信息查看', 2185, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:dvsubject:query', '#', 'admin', '2022-06-16 20:18:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2188, '项目信息新增', 2185, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:dvsubject:add', '#', 'admin', '2022-06-16 20:18:56', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2189, '项目信息编辑', 2185, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:dvsubject:edit', '#', 'admin', '2022-06-16 20:19:18', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2190, '项目删除', 2185, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:dvsubject:remove', '#', 'admin', '2022-06-16 20:19:54', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2191, '点检保养计划', 2059, 4, 'checkplan', 'mes/dv/checkplan/index', NULL, 1, 0, 'C', '0', '0', 'mes:dv:checkplan', 'build', 'admin', '2022-06-16 21:35:31', 'admin', '2022-09-01 22:17:50', '');
INSERT INTO `sys_menu` VALUES (2192, '点检计划列表查询', 2191, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:checkplan:list', '#', 'admin', '2022-06-16 21:36:03', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2193, '点检计划查看', 2191, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:checkplan:query', '#', 'admin', '2022-06-16 21:36:34', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2194, '点检计划新增', 2191, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:checkplan:add', '#', 'admin', '2022-06-16 21:36:56', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2195, '点检计划编辑', 2191, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:checkplan:edit', '#', 'admin', '2022-06-16 21:37:15', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2196, '点检计划删除', 2191, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:checkplan:remove', '#', 'admin', '2022-06-16 21:37:35', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2197, '生产领料', 2042, 5, 'issue', 'mes/wm/issue/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:issueheader', 'edit', 'admin', '2022-07-17 19:24:17', 'admin', '2022-08-22 13:52:30', '');
INSERT INTO `sys_menu` VALUES (2198, '生产领料新增', 2197, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:issueheader:add', '#', 'admin', '2022-07-17 19:24:43', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2199, '生产领料列表查询', 2197, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:issueheader:list', '#', 'admin', '2022-07-17 19:25:12', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2200, '生产领料信息查看', 2197, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:issueheader:query', '#', 'admin', '2022-07-17 19:25:37', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2201, '生产领料编辑', 2197, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:issueheader:edit', '#', 'admin', '2022-07-17 19:25:54', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2202, '生产领料删除', 2197, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:issueheader:remove', '#', 'admin', '2022-07-17 19:26:12', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2203, '条码清单', 2042, 10, 'barcode', 'mes/wm/barcode/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:barcode', 'barcode', 'admin', '2022-08-01 11:19:01', 'admin', '2022-10-10 12:50:47', '');
INSERT INTO `sys_menu` VALUES (2204, '条码清单', 2203, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:barcode:list', '#', 'admin', '2022-08-01 11:19:23', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2205, '维修单', 2059, 5, 'repair', 'mes/dv/repair/index', NULL, 1, 0, 'C', '0', '0', 'mes:dv:repair', 'system', 'admin', '2022-08-06 15:19:26', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2206, '维修单列表查询', 2205, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:repair:list', '#', 'admin', '2022-08-06 15:19:59', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2207, '维修单查看', 2205, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:repair:query', '#', 'admin', '2022-08-06 15:20:19', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2208, '维修单修改', 2205, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:repair:edit', '#', 'admin', '2022-08-06 15:20:41', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2209, '维修单删除', 2205, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:dv:repair:remove', '#', 'admin', '2022-08-06 15:20:58', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2210, '组织架构', 0, 0, 'user', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'tree', 'admin', '2022-08-13 21:41:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2214, '过程检验', 2124, 5, 'ipqc', 'mes/qc/ipqc/index', NULL, 1, 0, 'C', '0', '0', 'mes:qc:ipqc', 'job', 'admin', '2022-08-29 20:46:27', 'admin', '2022-08-29 20:46:45', '');
INSERT INTO `sys_menu` VALUES (2215, '过程检验单列表查询', 2214, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:ipqc:list', '#', 'admin', '2022-08-29 20:47:26', 'admin', '2022-08-29 20:47:46', '');
INSERT INTO `sys_menu` VALUES (2216, '过程检验单详情查看', 2214, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:ipqc:query', '#', 'admin', '2022-08-29 20:48:10', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2217, '过程检验单新增', 2214, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:ipqc:add', '#', 'admin', '2022-08-29 20:48:39', 'admin', '2022-08-29 20:48:56', '');
INSERT INTO `sys_menu` VALUES (2218, '过程检验单编辑', 2214, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:ipqc:edit', '#', 'admin', '2022-08-29 20:49:22', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2219, '过程检验单删除', 2214, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:ipqc:remove', '#', 'admin', '2022-08-29 20:49:44', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2220, '出货检验', 2124, 6, 'oqc', 'mes/qc/oqc/index', NULL, 1, 0, 'C', '0', '0', 'mes:qc:oqc', 'guide', 'admin', '2022-08-31 22:17:39', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2221, '出货检验单列表查询', 2220, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:oqc:list', '#', 'admin', '2022-08-31 22:18:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2222, '出货检验单明细查看', 2220, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:oqc:query', '#', 'admin', '2022-08-31 22:18:47', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2223, '出货检验单新增', 2220, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:oqc:add', '#', 'admin', '2022-08-31 22:19:10', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2224, '出货检验单编辑', 2220, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:oqc:edit', '#', 'admin', '2022-08-31 22:19:27', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2225, '出货检验单删除', 2220, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:qc:oqc:remove', '#', 'admin', '2022-08-31 22:19:45', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2226, '生产退料', 2042, 6, 'rtissue', 'mes/wm/rtissue/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:rtissue', 'logininfor', 'admin', '2022-09-03 23:39:43', 'admin', '2022-09-15 22:59:05', '生产退料单头菜单');
INSERT INTO `sys_menu` VALUES (2227, '生产退料单列表查询', 2226, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtissue:list', '#', 'admin', '2022-09-03 23:39:43', 'admin', '2022-09-15 22:59:42', '');
INSERT INTO `sys_menu` VALUES (2228, '生产退料单头新增', 2226, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtissue:add', '#', 'admin', '2022-09-03 23:39:43', 'admin', '2022-09-15 22:59:55', '');
INSERT INTO `sys_menu` VALUES (2229, '生产退料单头修改', 2226, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtissue:edit', '#', 'admin', '2022-09-03 23:39:43', 'admin', '2022-09-15 23:00:06', '');
INSERT INTO `sys_menu` VALUES (2230, '生产退料单头删除', 2226, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtissue:remove', '#', 'admin', '2022-09-03 23:39:43', 'admin', '2022-09-15 23:00:17', '');
INSERT INTO `sys_menu` VALUES (2231, '生产退料明细查看', 2226, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtissue:query', '#', 'admin', '2022-09-03 23:39:43', 'admin', '2022-09-15 23:00:44', '');
INSERT INTO `sys_menu` VALUES (2232, '产品入库', 2042, 7, 'productrecpt', 'mes/wm/productrecpt/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:productrecpt', 'shopping', 'admin', '2022-09-22 21:02:47', 'admin', '2022-10-06 18:20:41', '');
INSERT INTO `sys_menu` VALUES (2233, '产品入库单列表查询', 2232, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:productrecpt:list', '#', 'admin', '2022-09-22 21:03:12', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2234, '产品入库单明细查看', 2232, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:productrecpt:query', '#', 'admin', '2022-09-22 21:03:34', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2235, '产品入库单编辑', 2232, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:productrecpt:edit', '#', 'admin', '2022-09-22 21:03:57', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2236, '产品入库单删除', 2232, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:productrecpt:remove', '#', 'admin', '2022-09-22 21:04:15', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2237, '生产报工', 2072, 5, 'feedback', 'mes/pro/feedback/index', NULL, 1, 0, 'C', '0', '0', 'mes:pro:feedback', 'chart', 'admin', '2022-10-03 20:13:20', 'admin', '2022-10-03 20:13:43', '');
INSERT INTO `sys_menu` VALUES (2238, '销售出库', 2042, 8, 'productsalse', 'mes/wm/productsalse/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:productsalse', 'guide', 'admin', '2022-10-05 18:54:42', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2239, '出库单列表查询', 2238, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:productsalse:list', '#', 'admin', '2022-10-05 18:55:18', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2240, '销售出库单明细查看', 2238, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:productsalse:query', '#', 'admin', '2022-10-05 18:55:43', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2241, '销售出库单新增', 2238, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:productsalse:add', '#', 'admin', '2022-10-05 18:56:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2242, '销售出库单编辑', 2238, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:productsalse:edit', '#', 'admin', '2022-10-05 18:56:21', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2243, '销售出库单删除', 2238, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:productsalse:remove', '#', 'admin', '2022-10-05 18:56:51', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2244, '销售退货', 2042, 9, 'rtsalse', 'mes/wm/rtsalse/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:rtsalse', 'link', 'admin', '2022-10-06 21:28:47', 'admin', '2022-10-06 22:02:18', '');
INSERT INTO `sys_menu` VALUES (2245, '退货单列表查询', 2244, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtsalse:list', '#', 'admin', '2022-10-06 21:29:27', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2246, '销售退货单明细查看', 2244, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtsalse:query', '#', 'admin', '2022-10-06 21:29:51', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2247, '销售退货单新增', 2244, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtsalse:add', '#', 'admin', '2022-10-06 21:30:18', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2248, '销售退货单编辑', 2244, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtsalse:edit', '#', 'admin', '2022-10-06 21:30:39', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2249, '销售退货单删除', 2244, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:rtsalse:remove', '#', 'admin', '2022-10-06 21:31:03', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2250, '装箱管理', 2042, 11, 'package', 'mes/wm/package/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:package', 'table', 'admin', '2022-10-10 12:48:22', 'admin', '2022-10-10 12:51:46', '');
INSERT INTO `sys_menu` VALUES (2251, '装箱单列表查询', 2250, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:package:list', '#', 'admin', '2022-10-10 12:49:03', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2252, '装箱单明细查看', 2250, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:package:query', '#', 'admin', '2022-10-10 12:49:26', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2253, '装箱单新增', 2250, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:package:add', '#', 'admin', '2022-10-10 12:49:42', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2254, '装箱单编辑', 2250, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:package:edit', '#', 'admin', '2022-10-10 12:50:01', '', NULL, '');
INSERT INTO `sys_menu` VALUES (2255, '装箱单删除', 2250, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:package:remove', '#', 'admin', '2022-10-10 12:50:23', '', NULL, '');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob NULL COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (1, '温馨提醒：2018-07-01 若依新版本发布啦', '2', 0xE696B0E78988E69CACE58685E5AEB9, '0', 'admin', '2024-11-16 21:27:26', '', NULL, '管理员');
INSERT INTO `sys_notice` VALUES (2, '维护通知：2018-07-01 若依系统凌晨维护', '1', 0xE7BBB4E68AA4E58685E5AEB9, '0', 'admin', '2024-11-16 21:27:26', '', NULL, '管理员');

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '模块标题',
  `business_type` int NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求方式',
  `operator_type` int NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '返回参数',
  `status` int NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`oper_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 105 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------
INSERT INTO `sys_oper_log` VALUES (100, '部门管理', 2, 'com.ktg.web.controller.system.SysDeptController.edit()', 'PUT', 1, 'admin', NULL, '/system/dept', '127.0.0.1', '内网IP', '{\"deptName\":\"青岛科技\",\"leader\":\"若依\",\"deptId\":100,\"orderNum\":\"0\",\"delFlag\":\"0\",\"params\":{},\"parentId\":0,\"createBy\":\"admin\",\"children\":[],\"createTime\":1731763645000,\"phone\":\"15888888888\",\"updateBy\":\"admin\",\"ancestors\":\"0\",\"email\":\"ry@qq.com\",\"status\":\"0\"}', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2024-11-16 21:44:53');
INSERT INTO `sys_oper_log` VALUES (101, '部门管理', 2, 'com.ktg.web.controller.system.SysDeptController.edit()', 'PUT', 1, 'admin', NULL, '/system/dept', '127.0.0.1', '内网IP', '{\"deptName\":\"青岛科技\",\"leader\":\"青岛\",\"deptId\":100,\"orderNum\":\"0\",\"delFlag\":\"0\",\"params\":{},\"parentId\":0,\"createBy\":\"admin\",\"children\":[],\"createTime\":1731763645000,\"phone\":\"15888888888\",\"updateBy\":\"admin\",\"ancestors\":\"0\",\"email\":\"qd@qq.com\",\"status\":\"0\"}', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2024-11-16 21:45:05');
INSERT INTO `sys_oper_log` VALUES (102, '部门管理', 2, 'com.ktg.web.controller.system.SysDeptController.edit()', 'PUT', 1, 'admin', NULL, '/system/dept', '127.0.0.1', '内网IP', '{\"deptName\":\"青岛总公司\",\"leader\":\"青岛\",\"deptId\":101,\"orderNum\":\"1\",\"delFlag\":\"0\",\"params\":{},\"parentId\":100,\"createBy\":\"admin\",\"children\":[],\"createTime\":1731763645000,\"phone\":\"15888888888\",\"updateBy\":\"admin\",\"ancestors\":\"0,100\",\"email\":\"qd@qq.com\",\"status\":\"0\"}', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2024-11-16 21:45:29');
INSERT INTO `sys_oper_log` VALUES (103, '部门管理', 2, 'com.ktg.web.controller.system.SysDeptController.edit()', 'PUT', 1, 'admin', NULL, '/system/dept', '127.0.0.1', '内网IP', '{\"deptName\":\"济南分公司\",\"leader\":\"济南\",\"deptId\":102,\"orderNum\":\"2\",\"delFlag\":\"0\",\"params\":{},\"parentId\":100,\"createBy\":\"admin\",\"children\":[],\"createTime\":1731763645000,\"phone\":\"15888888888\",\"updateBy\":\"admin\",\"ancestors\":\"0,100\",\"email\":\"jn@qq.com\",\"status\":\"0\"}', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2024-11-16 21:45:46');
INSERT INTO `sys_oper_log` VALUES (104, '用户管理', 2, 'com.ktg.web.controller.system.SysUserController.edit()', 'PUT', 1, 'admin', NULL, '/system/user', '127.0.0.1', '内网IP', '{\"roles\":[{\"flag\":false,\"roleId\":2,\"admin\":false,\"dataScope\":\"2\",\"params\":{},\"roleSort\":\"2\",\"deptCheckStrictly\":false,\"menuCheckStrictly\":false,\"roleKey\":\"common\",\"roleName\":\"普通角色\",\"status\":\"0\"}],\"phonenumber\":\"15666666666\",\"admin\":false,\"loginDate\":1731763645000,\"remark\":\"测试员\",\"delFlag\":\"0\",\"password\":\"\",\"updateBy\":\"admin\",\"postIds\":[2],\"loginIp\":\"127.0.0.1\",\"email\":\"cs@qq.com\",\"nickName\":\"测试\",\"sex\":\"1\",\"deptId\":105,\"avatar\":\"\",\"dept\":{\"deptName\":\"测试部门\",\"leader\":\"若依\",\"deptId\":105,\"orderNum\":\"3\",\"params\":{},\"parentId\":101,\"children\":[],\"status\":\"0\"},\"params\":{},\"userName\":\"ry\",\"userId\":2,\"createBy\":\"admin\",\"roleIds\":[2],\"createTime\":1731763645000,\"status\":\"0\"}', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2024-11-16 21:46:30');

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, 'ceo', '董事长', 1, '0', 'admin', '2024-11-16 21:27:25', '', NULL, '');
INSERT INTO `sys_post` VALUES (2, 'se', '项目经理', 2, '0', 'admin', '2024-11-16 21:27:25', '', NULL, '');
INSERT INTO `sys_post` VALUES (3, 'hr', '人力资源', 3, '0', 'admin', '2024-11-16 21:27:25', '', NULL, '');
INSERT INTO `sys_post` VALUES (4, 'user', '普通员工', 4, '0', 'admin', '2024-11-16 21:27:25', '', NULL, '');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL, '超级管理员');
INSERT INTO `sys_role` VALUES (2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', '2024-11-16 21:27:25', '', NULL, '普通角色');

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO `sys_role_dept` VALUES (2, 100);
INSERT INTO `sys_role_dept` VALUES (2, 101);
INSERT INTO `sys_role_dept` VALUES (2, 105);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (2, 1);
INSERT INTO `sys_role_menu` VALUES (2, 2);
INSERT INTO `sys_role_menu` VALUES (2, 3);
INSERT INTO `sys_role_menu` VALUES (2, 4);
INSERT INTO `sys_role_menu` VALUES (2, 100);
INSERT INTO `sys_role_menu` VALUES (2, 101);
INSERT INTO `sys_role_menu` VALUES (2, 102);
INSERT INTO `sys_role_menu` VALUES (2, 103);
INSERT INTO `sys_role_menu` VALUES (2, 104);
INSERT INTO `sys_role_menu` VALUES (2, 105);
INSERT INTO `sys_role_menu` VALUES (2, 106);
INSERT INTO `sys_role_menu` VALUES (2, 107);
INSERT INTO `sys_role_menu` VALUES (2, 108);
INSERT INTO `sys_role_menu` VALUES (2, 109);
INSERT INTO `sys_role_menu` VALUES (2, 110);
INSERT INTO `sys_role_menu` VALUES (2, 111);
INSERT INTO `sys_role_menu` VALUES (2, 112);
INSERT INTO `sys_role_menu` VALUES (2, 113);
INSERT INTO `sys_role_menu` VALUES (2, 114);
INSERT INTO `sys_role_menu` VALUES (2, 115);
INSERT INTO `sys_role_menu` VALUES (2, 116);
INSERT INTO `sys_role_menu` VALUES (2, 500);
INSERT INTO `sys_role_menu` VALUES (2, 501);
INSERT INTO `sys_role_menu` VALUES (2, 1000);
INSERT INTO `sys_role_menu` VALUES (2, 1001);
INSERT INTO `sys_role_menu` VALUES (2, 1002);
INSERT INTO `sys_role_menu` VALUES (2, 1003);
INSERT INTO `sys_role_menu` VALUES (2, 1004);
INSERT INTO `sys_role_menu` VALUES (2, 1005);
INSERT INTO `sys_role_menu` VALUES (2, 1006);
INSERT INTO `sys_role_menu` VALUES (2, 1007);
INSERT INTO `sys_role_menu` VALUES (2, 1008);
INSERT INTO `sys_role_menu` VALUES (2, 1009);
INSERT INTO `sys_role_menu` VALUES (2, 1010);
INSERT INTO `sys_role_menu` VALUES (2, 1011);
INSERT INTO `sys_role_menu` VALUES (2, 1012);
INSERT INTO `sys_role_menu` VALUES (2, 1013);
INSERT INTO `sys_role_menu` VALUES (2, 1014);
INSERT INTO `sys_role_menu` VALUES (2, 1015);
INSERT INTO `sys_role_menu` VALUES (2, 1016);
INSERT INTO `sys_role_menu` VALUES (2, 1017);
INSERT INTO `sys_role_menu` VALUES (2, 1018);
INSERT INTO `sys_role_menu` VALUES (2, 1019);
INSERT INTO `sys_role_menu` VALUES (2, 1020);
INSERT INTO `sys_role_menu` VALUES (2, 1021);
INSERT INTO `sys_role_menu` VALUES (2, 1022);
INSERT INTO `sys_role_menu` VALUES (2, 1023);
INSERT INTO `sys_role_menu` VALUES (2, 1024);
INSERT INTO `sys_role_menu` VALUES (2, 1025);
INSERT INTO `sys_role_menu` VALUES (2, 1026);
INSERT INTO `sys_role_menu` VALUES (2, 1027);
INSERT INTO `sys_role_menu` VALUES (2, 1028);
INSERT INTO `sys_role_menu` VALUES (2, 1029);
INSERT INTO `sys_role_menu` VALUES (2, 1030);
INSERT INTO `sys_role_menu` VALUES (2, 1031);
INSERT INTO `sys_role_menu` VALUES (2, 1032);
INSERT INTO `sys_role_menu` VALUES (2, 1033);
INSERT INTO `sys_role_menu` VALUES (2, 1034);
INSERT INTO `sys_role_menu` VALUES (2, 1035);
INSERT INTO `sys_role_menu` VALUES (2, 1036);
INSERT INTO `sys_role_menu` VALUES (2, 1037);
INSERT INTO `sys_role_menu` VALUES (2, 1038);
INSERT INTO `sys_role_menu` VALUES (2, 1039);
INSERT INTO `sys_role_menu` VALUES (2, 1040);
INSERT INTO `sys_role_menu` VALUES (2, 1041);
INSERT INTO `sys_role_menu` VALUES (2, 1042);
INSERT INTO `sys_role_menu` VALUES (2, 1043);
INSERT INTO `sys_role_menu` VALUES (2, 1044);
INSERT INTO `sys_role_menu` VALUES (2, 1045);
INSERT INTO `sys_role_menu` VALUES (2, 1046);
INSERT INTO `sys_role_menu` VALUES (2, 1047);
INSERT INTO `sys_role_menu` VALUES (2, 1048);
INSERT INTO `sys_role_menu` VALUES (2, 1049);
INSERT INTO `sys_role_menu` VALUES (2, 1050);
INSERT INTO `sys_role_menu` VALUES (2, 1051);
INSERT INTO `sys_role_menu` VALUES (2, 1052);
INSERT INTO `sys_role_menu` VALUES (2, 1053);
INSERT INTO `sys_role_menu` VALUES (2, 1054);
INSERT INTO `sys_role_menu` VALUES (2, 1055);
INSERT INTO `sys_role_menu` VALUES (2, 1056);
INSERT INTO `sys_role_menu` VALUES (2, 1057);
INSERT INTO `sys_role_menu` VALUES (2, 1058);
INSERT INTO `sys_role_menu` VALUES (2, 1059);
INSERT INTO `sys_role_menu` VALUES (2, 1060);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 103, 'admin', 'admin', '00', 'ry@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2024-11-22 16:13:36', 'admin', '2024-11-16 21:27:25', '', '2024-11-22 16:13:35', '管理员');
INSERT INTO `sys_user` VALUES (2, 105, 'cs', '测试', '00', 'cs@qq.com', '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2024-11-16 21:27:25', 'admin', '2024-11-16 21:27:25', 'admin', '2024-11-16 21:46:30', '测试员');

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与岗位关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES (1, 1);
INSERT INTO `sys_user_post` VALUES (2, 2);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2);

-- ----------------------------
-- Table structure for tm_tool
-- ----------------------------
DROP TABLE IF EXISTS `tm_tool`;
CREATE TABLE `tm_tool`  (
  `tool_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工装夹具ID',
  `tool_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工装夹具编码',
  `tool_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工装夹具名称',
  `brand` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌',
  `spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '型号',
  `tool_type_id` bigint NOT NULL COMMENT '工装夹具类型ID',
  `tool_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工装夹具类型编码',
  `tool_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工装夹具类型名称',
  `code_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否单独编码管理',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `quantity_avail` int NULL DEFAULT 1 COMMENT '可用数量',
  `mainten_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '保养维护类型',
  `next_mainten_period` int NULL DEFAULT NULL COMMENT '下一次保养周期',
  `next_mainten_date` datetime NULL DEFAULT NULL COMMENT '下一次保养日期',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'STORE' COMMENT '状态[MES_TOOL_STATUS]',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tool_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工装夹具清单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tm_tool
-- ----------------------------

-- ----------------------------
-- Table structure for tm_tool_type
-- ----------------------------
DROP TABLE IF EXISTS `tm_tool_type`;
CREATE TABLE `tm_tool_type`  (
  `tool_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工装夹具类型ID',
  `tool_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型编码',
  `tool_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型名称',
  `code_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Y' COMMENT '是否编码管理',
  `mainten_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '保养维护类型',
  `mainten_period` int NULL DEFAULT NULL COMMENT '保养周期',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tool_type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工装夹具类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tm_tool_type
-- ----------------------------

-- ----------------------------
-- Table structure for wm_barcode
-- ----------------------------
DROP TABLE IF EXISTS `wm_barcode`;
CREATE TABLE `wm_barcode`  (
  `barcode_id` bigint NOT NULL AUTO_INCREMENT COMMENT '条码ID',
  `barcode_formart` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '条码格式',
  `barcode_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '条码类型',
  `barcode_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '条码内容',
  `bussiness_id` bigint NOT NULL COMMENT '业务ID',
  `bussiness_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务编码',
  `bussiness_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务名称',
  `barcode_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '条码地址',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'Y' COMMENT '是否生效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`barcode_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 204 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '条码清单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wm_barcode
-- ----------------------------
INSERT INTO `wm_barcode` VALUES (200, 'QR_CODE', 'ITEM', 'ITEM-IF2024110838', 203, 'IF2024110838', '测试物料2', 'http://127.0.0.1:9000/public-bucket-mes/2024/11/08/ITEM-IF2024110838_20241108163905A003.png', 'Y', '', NULL, NULL, 0, 0, '', '2024-11-08 16:39:06', '', NULL);
INSERT INTO `wm_barcode` VALUES (201, 'UPC_CODE', 'BOX_SMALL', '12345678901', 200, '1001', '测试物料|null|比亚迪|101', 'http://127.0.0.1:9000/public-bucket-mes/2024/11/08/12345678901_20241108164417A004.png', 'Y', '', NULL, NULL, 0, 0, '', '2024-11-08 16:44:17', '', NULL);
INSERT INTO `wm_barcode` VALUES (202, 'QR_CODE', 'PACKAGE', 'PACKAGE-PACK202411080001', 200, 'PACK202411080001', '张三', 'http://127.0.0.1:9000/public-bucket-mes/2024/11/08/PACKAGE-PACK202411080001_20241108164440A005.png', 'Y', '', NULL, NULL, 0, 0, 'admin', '2024-11-08 16:44:41', '', NULL);

-- ----------------------------
-- Table structure for wm_barcode_config
-- ----------------------------
DROP TABLE IF EXISTS `wm_barcode_config`;
CREATE TABLE `wm_barcode_config`  (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `barcode_formart` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '条码格式',
  `barcode_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '条码类型',
  `content_formart` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容格式',
  `content_example` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容样例',
  `auto_gen_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'Y' COMMENT '是否自动生成',
  `default_template` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认的打印模板',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'Y' COMMENT '是否生效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '条码配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wm_barcode_config
-- ----------------------------

-- ----------------------------
-- Table structure for wm_material_stock
-- ----------------------------
DROP TABLE IF EXISTS `wm_material_stock`;
CREATE TABLE `wm_material_stock`  (
  `material_stock_id` bigint NOT NULL AUTO_INCREMENT COMMENT '事务ID',
  `item_type_id` bigint NULL DEFAULT NULL COMMENT '物料类型ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位',
  `batch_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批次号',
  `workorder_id` bigint NULL DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产工单编号',
  `vendor_id` bigint NULL DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商编号',
  `vendor_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商简称',
  `client_id` bigint NULL DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户简称',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint NULL DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint NULL DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库位名称',
  `package_id` bigint NULL DEFAULT NULL COMMENT '容器ID',
  `package_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '容器编号',
  `quantity_onhand` double(12, 2) NULL DEFAULT NULL COMMENT '在库数量',
  `quantity_reserved` double(12, 2) NULL DEFAULT NULL COMMENT '保留数量',
  `recpt_date` datetime NULL DEFAULT NULL COMMENT '入库时间',
  `expire_date` datetime NULL DEFAULT NULL COMMENT '库存有效期',
  `production_date` datetime NULL DEFAULT NULL,
  `frozen_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'N' COMMENT '是否冻结',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`material_stock_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 202 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wm_material_stock
-- ----------------------------
INSERT INTO `wm_material_stock` VALUES (200, 200, 200, '1001', '测试物料', NULL, '11', '101', NULL, NULL, 200, 'V00059', '比亚迪', 'BYD', NULL, NULL, NULL, NULL, 201, 'WH158', '测试仓库', 201, 'L046', '1库区', 202, 'A0036', '1原料位', NULL, NULL, 1.00, NULL, '2024-11-07 00:00:00', '2024-11-16 00:00:00', NULL, 'N', NULL, NULL, 0, 0, '', NULL, '', NULL);
INSERT INTO `wm_material_stock` VALUES (201, 202, 201, 'IF2024110635', '铁板', '100', '毫米', '2', 201, 'MO202411080001', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 200, 'XBK_VIRTUAL', '线边库-虚拟', 200, 'XBKKQ_VIRTUAL', '线边库库区-虚拟', 200, 'XBKKW_VIRTUAL', '线边库库位-虚拟', NULL, NULL, 10.00, NULL, '2024-11-15 16:17:26', NULL, NULL, 'N', NULL, NULL, 0, 0, '', NULL, '', NULL);

-- ----------------------------
-- Table structure for wm_package
-- ----------------------------
DROP TABLE IF EXISTS `wm_package`;
CREATE TABLE `wm_package`  (
  `package_id` bigint NOT NULL AUTO_INCREMENT COMMENT '装箱单ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父箱ID',
  `ancestors` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '所有父节点ID',
  `package_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '装箱单编号',
  `barcode_id` bigint NULL DEFAULT NULL COMMENT '条码ID',
  `barcode_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '条码内容',
  `barcode_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '条码地址',
  `package_date` datetime NOT NULL COMMENT '装箱日期',
  `so_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '销售订单编号',
  `invoice_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发票编号',
  `client_id` bigint NULL DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户简称',
  `package_length` double(12, 4) NULL DEFAULT NULL COMMENT '箱长度',
  `package_width` double(12, 4) NULL DEFAULT NULL COMMENT '箱宽度',
  `package_height` double(12, 4) NULL DEFAULT NULL COMMENT '箱高度',
  `size_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '尺寸单位',
  `net_weight` double(12, 4) NULL DEFAULT NULL COMMENT '净重',
  `cross_weight` double(12, 4) NULL DEFAULT NULL COMMENT '毛重',
  `weight_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '重量单位',
  `inspector` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '检查员用户名',
  `inspector_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '检查员名称',
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PREPARE' COMMENT '状态',
  `enable_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'Y' COMMENT '是否生效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`package_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '装箱单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wm_package
-- ----------------------------
INSERT INTO `wm_package` VALUES (200, 0, '0', 'PACK202411080001', 202, 'PACKAGE-PACK202411080001', 'http://127.0.0.1:9000/public-bucket-mes/2024/11/08/PACKAGE-PACK202411080001_20241108164440A005.png', '2024-11-08 00:00:00', '101', NULL, NULL, '101', '张三', NULL, 11.0000, 11.0000, 11.0000, NULL, 10.0000, 15.0000, NULL, NULL, '张三', 'PREPARE', 'Y', '', NULL, NULL, 0, 0, '', '2024-11-08 16:44:41', '', '2024-11-08 16:51:31');

-- ----------------------------
-- Table structure for wm_package_line
-- ----------------------------
DROP TABLE IF EXISTS `wm_package_line`;
CREATE TABLE `wm_package_line`  (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细行ID',
  `package_id` bigint NOT NULL COMMENT '装箱单ID',
  `material_stock_id` bigint NULL DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位',
  `quantity_package` double(12, 2) NOT NULL COMMENT '装箱数量',
  `workorder_id` bigint NULL DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产工单编号',
  `batch_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint NULL DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint NULL DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint NULL DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库位名称',
  `expire_date` datetime NULL DEFAULT NULL COMMENT '有效期',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '装箱明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wm_package_line
-- ----------------------------
INSERT INTO `wm_package_line` VALUES (200, 200, NULL, 200, '1001', '测试物料', NULL, '千克', 0.00, 200, 'MO202411070001', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, NULL, 0, 0, '', '2024-11-08 16:51:13', '', NULL);

-- ----------------------------
-- Table structure for wm_sn
-- ----------------------------
DROP TABLE IF EXISTS `wm_sn`;
CREATE TABLE `wm_sn`  (
  `sn_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SN码ID',
  `sn_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SN码',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位',
  `batch_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批次号',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预留字段2',
  `attr3` int NULL DEFAULT 0 COMMENT '预留字段3',
  `attr4` int NULL DEFAULT 0 COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sn_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SN码表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wm_sn
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
