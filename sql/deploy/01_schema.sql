-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: j2eedb
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cal_holiday`
--

DROP TABLE IF EXISTS `cal_holiday`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cal_holiday` (
  `holiday_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `the_day` datetime DEFAULT NULL COMMENT '日期',
  `holiday_type` varchar(64) DEFAULT NULL COMMENT '日期类型',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`holiday_id`)
) ENGINE=InnoDB AUTO_INCREMENT=71 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节假日设置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cal_plan`
--

DROP TABLE IF EXISTS `cal_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cal_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `plan_code` varchar(64) NOT NULL COMMENT '计划编号',
  `plan_name` varchar(255) NOT NULL COMMENT '计划名称',
  `calendar_type` varchar(64) DEFAULT NULL COMMENT '班组类型',
  `start_date` datetime NOT NULL COMMENT '开始日期',
  `end_date` datetime NOT NULL COMMENT '结束日期',
  `shift_type` varchar(64) DEFAULT NULL COMMENT '轮班方式',
  `shift_method` varchar(64) DEFAULT NULL COMMENT '倒班方式',
  `shift_count` int DEFAULT NULL COMMENT '数',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9992 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排班计划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cal_plan_team`
--

DROP TABLE IF EXISTS `cal_plan_team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cal_plan_team` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `team_id` bigint NOT NULL COMMENT '班组ID',
  `team_code` varchar(64) DEFAULT NULL COMMENT '班组编号',
  `team_name` varchar(64) DEFAULT NULL COMMENT '班组名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9996 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计划班组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cal_shift`
--

DROP TABLE IF EXISTS `cal_shift`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cal_shift` (
  `shift_id` bigint NOT NULL AUTO_INCREMENT COMMENT '班次ID',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `order_num` int NOT NULL COMMENT '序号',
  `shift_name` varchar(64) NOT NULL COMMENT '班次名称',
  `start_time` varchar(10) NOT NULL COMMENT '开始时间',
  `end_time` varchar(10) NOT NULL COMMENT '结束时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`shift_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9994 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计划班次表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cal_team`
--

DROP TABLE IF EXISTS `cal_team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cal_team` (
  `team_id` bigint NOT NULL AUTO_INCREMENT COMMENT '班组ID',
  `team_code` varchar(64) NOT NULL COMMENT '班组编号',
  `team_name` varchar(255) NOT NULL COMMENT '班组名称',
  `calendar_type` varchar(64) DEFAULT NULL COMMENT '班组类型',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`team_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9973 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cal_team_member`
--

DROP TABLE IF EXISTS `cal_team_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cal_team_member` (
  `member_id` bigint NOT NULL AUTO_INCREMENT COMMENT '班组成员ID',
  `team_id` bigint NOT NULL COMMENT '班组ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(64) NOT NULL COMMENT '用户名',
  `nick_name` varchar(64) DEFAULT NULL COMMENT '用户昵称',
  `tel` varchar(64) DEFAULT NULL COMMENT '电话',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9986 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班组成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cal_teamshift`
--

DROP TABLE IF EXISTS `cal_teamshift`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cal_teamshift` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `the_day` varchar(64) NOT NULL COMMENT '日期',
  `team_id` bigint NOT NULL COMMENT '班组ID',
  `team_name` varchar(255) DEFAULT NULL COMMENT '班组名称',
  `shift_id` bigint NOT NULL COMMENT '班次ID',
  `shift_name` varchar(255) DEFAULT NULL COMMENT '班次名称',
  `order_num` int DEFAULT NULL COMMENT '序号',
  `plan_id` bigint DEFAULT NULL COMMENT '计划ID',
  `calendar_type` varchar(64) DEFAULT NULL COMMENT '班组类型',
  `shift_type` varchar(64) DEFAULT NULL COMMENT '轮班方式',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=745 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班组排班表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `db_test`
--

DROP TABLE IF EXISTS `db_test`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_test` (
  `area_id` bigint NOT NULL AUTO_INCREMENT COMMENT '库位ID',
  `area_code` varchar(64) NOT NULL COMMENT '库位编码',
  `area_name` varchar(255) NOT NULL COMMENT '库位名称',
  `location_id` bigint NOT NULL COMMENT '库区ID',
  `area` double(8,2) DEFAULT NULL COMMENT '面积',
  `max_loa` double(8,2) DEFAULT NULL COMMENT '最大载重量',
  `position_x` int DEFAULT NULL COMMENT '库位位置X',
  `position_y` int DEFAULT NULL COMMENT '库位位置y',
  `position_z` int DEFAULT NULL COMMENT '库位位置z',
  `enable_flag` char(1) DEFAULT NULL COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`area_id`),
  UNIQUE KEY `area_code` (`area_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_check_machinery`
--

DROP TABLE IF EXISTS `dv_check_machinery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_check_machinery` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9956 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点检设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_check_plan`
--

DROP TABLE IF EXISTS `dv_check_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_check_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `plan_code` varchar(64) NOT NULL COMMENT '计划编码',
  `plan_name` varchar(255) DEFAULT NULL COMMENT '计划名称',
  `plan_type` varchar(64) NOT NULL COMMENT '计划类型',
  `start_date` datetime DEFAULT NULL COMMENT '开始日期',
  `end_date` datetime DEFAULT NULL COMMENT '结束日期',
  `cycle_type` varchar(64) DEFAULT NULL COMMENT '频率',
  `cycle_count` int DEFAULT NULL COMMENT '次数',
  `status` varchar(64) DEFAULT NULL COMMENT '状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9944 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备点检保养计划头表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_check_record`
--

DROP TABLE IF EXISTS `dv_check_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_check_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `plan_id` bigint DEFAULT NULL COMMENT '计划ID',
  `plan_code` varchar(64) DEFAULT NULL COMMENT '计划编码',
  `plan_name` varchar(255) DEFAULT NULL COMMENT '计划名称',
  `plan_type` varchar(64) DEFAULT NULL COMMENT '计划类型',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `check_time` datetime NOT NULL COMMENT '点检时间',
  `user_id` bigint DEFAULT NULL COMMENT '点检人',
  `user_name` varchar(64) DEFAULT NULL COMMENT '点检人用户名',
  `nick_name` varchar(255) DEFAULT NULL COMMENT '点检人名称',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=216 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备点检记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_check_record_line`
--

DROP TABLE IF EXISTS `dv_check_record_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_check_record_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `record_id` bigint NOT NULL COMMENT '计划ID',
  `subject_id` bigint NOT NULL COMMENT '项目ID',
  `subject_code` varchar(64) NOT NULL COMMENT '项目编码',
  `subject_name` varchar(255) DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) DEFAULT NULL COMMENT '项目类型',
  `subject_content` varchar(500) NOT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) DEFAULT NULL COMMENT '标准',
  `check_status` varchar(64) NOT NULL COMMENT '点检结果',
  `check_result` varchar(500) DEFAULT NULL COMMENT '异常描述',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=209 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备点检记录行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_check_subject`
--

DROP TABLE IF EXISTS `dv_check_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_check_subject` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `subject_id` bigint NOT NULL COMMENT '设备ID',
  `subject_code` varchar(64) NOT NULL COMMENT '项目编码',
  `subject_name` varchar(255) DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) DEFAULT NULL COMMENT '项目类型',
  `subject_content` varchar(500) NOT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) DEFAULT NULL COMMENT '标准',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9965 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点检项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_machinery`
--

DROP TABLE IF EXISTS `dv_machinery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_machinery` (
  `machinery_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备类型ID',
  `machinery_code` varchar(64) NOT NULL COMMENT '设备类型编码',
  `machinery_name` varchar(255) NOT NULL COMMENT '设备类型名称',
  `machinery_brand` varchar(255) DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `machinery_type_id` bigint NOT NULL COMMENT '设备类型ID',
  `machinery_type_code` varchar(64) DEFAULT NULL COMMENT '设备类型编码',
  `machinery_type_name` varchar(255) DEFAULT NULL COMMENT '设备类型名称',
  `workshop_id` bigint NOT NULL COMMENT '所属车间ID',
  `workshop_code` varchar(64) DEFAULT NULL COMMENT '所属车间编码',
  `workshop_name` varchar(255) DEFAULT NULL COMMENT '所属车间名称',
  `status` varchar(64) NOT NULL DEFAULT 'STOP' COMMENT '设备状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `last_mainten_time` datetime DEFAULT NULL COMMENT '最近保养时间',
  `last_check_time` datetime DEFAULT NULL COMMENT '最近点检时间',
  PRIMARY KEY (`machinery_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9916 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_machinery_type`
--

DROP TABLE IF EXISTS `dv_machinery_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_machinery_type` (
  `machinery_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备类型ID',
  `machinery_type_code` varchar(64) NOT NULL COMMENT '设备类型编码',
  `machinery_type_name` varchar(255) NOT NULL COMMENT '设备类型名称',
  `parent_type_id` bigint DEFAULT '0' COMMENT '父类型ID',
  `ancestors` varchar(255) NOT NULL COMMENT '所有父节点ID',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`machinery_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9904 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_mainten_record`
--

DROP TABLE IF EXISTS `dv_mainten_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_mainten_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `plan_id` bigint DEFAULT NULL COMMENT '计划ID',
  `plan_code` varchar(64) DEFAULT NULL COMMENT '计划编码',
  `plan_name` varchar(255) DEFAULT NULL COMMENT '计划名称',
  `plan_type` varchar(64) DEFAULT NULL COMMENT '计划类型',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `mainten_time` datetime NOT NULL COMMENT '保养时间',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `nick_name` varchar(128) DEFAULT NULL COMMENT '昵称',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备保养记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_mainten_record_line`
--

DROP TABLE IF EXISTS `dv_mainten_record_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_mainten_record_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `record_id` bigint NOT NULL COMMENT '计划ID',
  `subject_id` bigint NOT NULL COMMENT '项目ID',
  `subject_code` varchar(64) NOT NULL COMMENT '项目编码',
  `subject_name` varchar(255) DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) DEFAULT NULL COMMENT '项目类型',
  `subject_content` varchar(500) NOT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) DEFAULT NULL COMMENT '标准',
  `mainten_status` varchar(64) NOT NULL COMMENT '保养结果',
  `mainten_result` varchar(500) DEFAULT NULL COMMENT '异常描述',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备保养记录行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_repair`
--

DROP TABLE IF EXISTS `dv_repair`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_repair` (
  `repair_id` bigint NOT NULL AUTO_INCREMENT COMMENT '维修单ID',
  `repair_code` varchar(64) NOT NULL COMMENT '维修单编号',
  `repair_name` varchar(255) DEFAULT NULL COMMENT '维修单名称',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) NOT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) NOT NULL COMMENT '设备名称',
  `machinery_brand` varchar(255) DEFAULT NULL COMMENT '品牌',
  `machinery_spec` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `machinery_type_id` bigint DEFAULT NULL COMMENT '设备类型ID',
  `require_date` datetime DEFAULT NULL COMMENT '报修日期',
  `finish_date` datetime DEFAULT NULL COMMENT '维修完成日期',
  `confirm_date` datetime DEFAULT NULL COMMENT '验收日期',
  `repair_result` varchar(64) DEFAULT NULL COMMENT '维修结果',
  `accepted_id` bigint DEFAULT NULL COMMENT '维修人员ID',
  `accepted_name` varchar(64) DEFAULT NULL COMMENT '维修人员用户名',
  `accepted_by` varchar(64) DEFAULT NULL COMMENT '维修人员名称',
  `confirm_id` bigint DEFAULT NULL COMMENT '验收人员ID',
  `confirm_name` varchar(64) DEFAULT NULL COMMENT '验收人员用户名',
  `confirm_by` varchar(64) DEFAULT NULL COMMENT '验收人员名称',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '来源单据类型',
  `source_doc_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '来源单据编号',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`repair_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备维修单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_repair_line`
--

DROP TABLE IF EXISTS `dv_repair_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_repair_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `repair_id` bigint NOT NULL COMMENT '维修单ID',
  `subject_id` bigint DEFAULT NULL COMMENT '项目ID',
  `subject_code` varchar(64) DEFAULT NULL COMMENT '项目编码',
  `subject_name` varchar(255) DEFAULT NULL COMMENT '项目名称',
  `subject_type` varchar(64) DEFAULT NULL COMMENT '项目类型',
  `subject_content` varchar(500) DEFAULT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) DEFAULT NULL COMMENT '标准',
  `malfunction` varchar(500) NOT NULL COMMENT '故障描述',
  `malfunction_url` varchar(255) DEFAULT NULL COMMENT '故障描述资源',
  `repair_des` varchar(500) DEFAULT NULL COMMENT '维修情况',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备维修单行';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dv_subject`
--

DROP TABLE IF EXISTS `dv_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dv_subject` (
  `subject_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `subject_code` varchar(64) NOT NULL COMMENT '项目编码',
  `subject_name` varchar(255) NOT NULL COMMENT '项目名称',
  `subject_type` varchar(64) DEFAULT '0' COMMENT '项目类型',
  `subject_content` varchar(500) NOT NULL COMMENT '项目内容',
  `subject_standard` varchar(255) DEFAULT NULL COMMENT '标准',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`subject_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9935 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备点检保养项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=185 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` varchar(64) DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4413 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `iot_device`
--

DROP TABLE IF EXISTS `iot_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device` (
  `device_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `device_code` varchar(64) NOT NULL COMMENT '设备编码',
  `device_name` varchar(255) NOT NULL COMMENT '设备名称',
  `device_type_id` bigint DEFAULT NULL COMMENT '设备类型ID',
  `pic_url` varchar(255) DEFAULT NULL COMMENT '图片地址',
  `con_protocol` varchar(64) NOT NULL DEFAULT 'MQTT' COMMENT '接入协议',
  `com_protocol` varchar(64) DEFAULT NULL COMMENT '通讯协议',
  `location` varchar(255) DEFAULT NULL COMMENT '位置',
  `groupcode` varchar(255) DEFAULT NULL COMMENT '分组',
  `topic` varchar(255) DEFAULT NULL COMMENT '数据订阅主题',
  `enable_flag` char(1) DEFAULT 'N' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数采设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `iot_device_property`
--

DROP TABLE IF EXISTS `iot_device_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device_property` (
  `property_id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  `property_code` varchar(64) NOT NULL COMMENT '属性编码',
  `property_name` varchar(255) NOT NULL COMMENT '属性名称',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `value_type` varchar(64) NOT NULL COMMENT '数据类型',
  `json_key` varchar(64) DEFAULT NULL COMMENT 'json_key',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`property_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数采设备属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `iot_device_stproperty`
--

DROP TABLE IF EXISTS `iot_device_stproperty`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device_stproperty` (
  `property_id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  `property_code` varchar(64) NOT NULL COMMENT '属性编码',
  `property_name` varchar(255) NOT NULL COMMENT '属性名称',
  `source_property_id` bigint NOT NULL COMMENT '源属性ID',
  `source_property_code` varchar(64) NOT NULL COMMENT '源属性编码',
  `source_property_name` varchar(255) NOT NULL COMMENT '源属性名称',
  `method_code` varchar(128) NOT NULL COMMENT '流计算函数代码',
  `method_name` varchar(255) NOT NULL COMMENT '流计算函数名称',
  `stparams` varchar(255) NOT NULL COMMENT '流计算参数',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `value_type` varchar(64) NOT NULL COMMENT '数据类型',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`property_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数采设备流计算属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `iot_model`
--

DROP TABLE IF EXISTS `iot_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_model` (
  `model_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型ID',
  `model_code` varchar(64) NOT NULL COMMENT '模型编码',
  `model_name` varchar(255) NOT NULL COMMENT '模型名称',
  `pic_url` varchar(255) DEFAULT NULL COMMENT '图片地址',
  `con_protocol` varchar(64) NOT NULL DEFAULT 'MQTT' COMMENT '接入协议',
  `com_protocol` varchar(64) DEFAULT NULL COMMENT '通讯协议',
  `enable_flag` char(1) DEFAULT 'N' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`model_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备模型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `iot_model_property`
--

DROP TABLE IF EXISTS `iot_model_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_model_property` (
  `property_id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  `property_code` varchar(64) NOT NULL COMMENT '属性编码',
  `property_name` varchar(255) NOT NULL COMMENT '属性名称',
  `model_id` bigint NOT NULL COMMENT '模型ID',
  `value_type` varchar(64) NOT NULL COMMENT '数据类型',
  `json_key` varchar(64) DEFAULT NULL COMMENT 'json_key',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`property_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备模型属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `iot_model_stproperty`
--

DROP TABLE IF EXISTS `iot_model_stproperty`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_model_stproperty` (
  `property_id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  `property_code` varchar(64) NOT NULL COMMENT '属性编码',
  `property_name` varchar(255) NOT NULL COMMENT '属性名称',
  `source_property_id` bigint NOT NULL COMMENT '源属性ID',
  `source_property_code` varchar(64) NOT NULL COMMENT '源属性编码',
  `source_property_name` varchar(255) NOT NULL COMMENT '源属性名称',
  `method_code` varchar(128) NOT NULL COMMENT '流计算函数代码',
  `method_name` varchar(255) NOT NULL COMMENT '流计算函数名称',
  `stparams` varchar(255) NOT NULL COMMENT '流计算参数',
  `model_id` bigint NOT NULL COMMENT '模型ID',
  `value_type` varchar(64) NOT NULL COMMENT '数据类型',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`property_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备模型流计算属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_client`
--

DROP TABLE IF EXISTS `md_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_client` (
  `client_id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `client_code` varchar(64) NOT NULL COMMENT '客户编码',
  `client_name` varchar(255) NOT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `client_en` varchar(255) DEFAULT NULL COMMENT '客户英文名称',
  `client_des` varchar(500) DEFAULT NULL COMMENT '客户简介',
  `client_logo` varchar(255) DEFAULT NULL COMMENT '客户LOGO地址',
  `client_type` varchar(64) DEFAULT 'ENTERPRISE' COMMENT '客户类型',
  `address` varchar(500) DEFAULT NULL COMMENT '客户地址',
  `website` varchar(255) DEFAULT NULL COMMENT '客户官网地址',
  `email` varchar(255) DEFAULT NULL COMMENT '客户邮箱地址',
  `tel` varchar(64) DEFAULT NULL COMMENT '客户电话',
  `contact1` varchar(64) DEFAULT NULL COMMENT '联系人1',
  `contact1_tel` varchar(64) DEFAULT NULL COMMENT '联系人1-电话',
  `contact1_email` varchar(255) DEFAULT NULL COMMENT '联系人1-邮箱',
  `contact2` varchar(64) DEFAULT NULL COMMENT '联系人2',
  `contact2_tel` varchar(64) DEFAULT NULL COMMENT '联系人2-电话',
  `contact2_email` varchar(255) DEFAULT NULL COMMENT '联系人2-邮箱',
  `credit_code` varchar(64) DEFAULT NULL COMMENT '统一社会信用代码',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9755 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_item`
--

DROP TABLE IF EXISTS `md_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `item_or_product` varchar(20) NOT NULL COMMENT '产品物料标识',
  `item_type_id` bigint DEFAULT '0' COMMENT '物料类型ID',
  `item_type_code` varchar(64) DEFAULT '' COMMENT '物料类型编码',
  `item_type_name` varchar(255) DEFAULT '' COMMENT '物料类型名称',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `safe_stock_flag` char(1) NOT NULL DEFAULT 'N' COMMENT '是否设置安全库存',
  `min_stock` double(12,4) DEFAULT '0.0000' COMMENT '最低库存量',
  `max_stock` double(12,4) DEFAULT '0.0000' COMMENT '最大库存量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(64) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `high_value` char(1) NOT NULL DEFAULT 'N',
  `unit_name` varchar(64) DEFAULT NULL,
  `batch_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否批次管理',
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9218 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料产品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_item_batch_config`
--

DROP TABLE IF EXISTS `md_item_batch_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_item_batch_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `produce_date_flag` char(1) DEFAULT NULL COMMENT '生产日期',
  `expire_date_flag` char(1) DEFAULT NULL COMMENT '有效期',
  `recpt_date_flag` char(1) DEFAULT NULL COMMENT '入库日期',
  `vendor_flag` char(1) DEFAULT NULL COMMENT '供应商',
  `client_flag` char(1) DEFAULT NULL COMMENT '客户',
  `co_code_flag` char(1) DEFAULT NULL COMMENT '销售订单编号',
  `po_code_flag` char(1) DEFAULT NULL COMMENT '采购订单编号',
  `workorder_flag` char(1) DEFAULT NULL COMMENT '生产工单',
  `task_flag` char(1) DEFAULT NULL COMMENT '生产任务',
  `workstation_flag` char(1) DEFAULT NULL COMMENT '工作站',
  `tool_flag` char(1) DEFAULT NULL COMMENT '工具',
  `mold_flag` char(1) DEFAULT NULL COMMENT '模具',
  `lot_number_flag` char(1) DEFAULT NULL COMMENT '生产批号',
  `quality_status_flag` char(1) DEFAULT NULL COMMENT '质量状态',
  `enable_flag` char(1) DEFAULT NULL COMMENT '生效状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9128 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料批次属性配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_item_type`
--

DROP TABLE IF EXISTS `md_item_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_item_type` (
  `item_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品物料类型ID',
  `item_type_code` varchar(64) NOT NULL COMMENT '产品物料类型编码',
  `item_type_name` varchar(255) NOT NULL COMMENT '产品物料类型名称',
  `parent_type_id` bigint NOT NULL DEFAULT '0' COMMENT '父类型ID',
  `ancestors` varchar(255) NOT NULL COMMENT '所有层级父节点',
  `item_or_product` varchar(20) NOT NULL COMMENT '产品物料标识',
  `order_num` int DEFAULT '1' COMMENT '排列顺序',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`item_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料产品分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_product_bom`
--

DROP TABLE IF EXISTS `md_product_bom`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_product_bom` (
  `bom_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `item_id` bigint NOT NULL COMMENT '物料产品ID',
  `bom_item_id` bigint NOT NULL COMMENT 'BOM物料ID',
  `bom_item_code` varchar(64) NOT NULL COMMENT 'BOM物料编码',
  `bom_item_name` varchar(255) NOT NULL COMMENT 'BOM物料名称',
  `bom_item_spec` varchar(500) DEFAULT NULL COMMENT 'BOM物料规格',
  `unit_of_measure` varchar(64) NOT NULL COMMENT 'BOM物料单位',
  `item_or_product` varchar(20) NOT NULL COMMENT '产品物料标识',
  `quantity` double(12,4) NOT NULL DEFAULT '0.0000' COMMENT '物料使用比例',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `unit_name` varchar(64) DEFAULT NULL COMMENT 'BOM物料单位名称',
  PRIMARY KEY (`bom_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9324 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品BOM关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_product_sip`
--

DROP TABLE IF EXISTS `md_product_sip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_product_sip` (
  `sip_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `item_id` bigint NOT NULL COMMENT '物料产品ID',
  `order_num` int DEFAULT NULL COMMENT '排列顺序',
  `process_id` bigint DEFAULT NULL COMMENT '对应的工序',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `sip_title` varchar(255) DEFAULT NULL COMMENT '标题',
  `sip_description` varchar(500) DEFAULT NULL COMMENT '详细描述',
  `sip_url` varchar(255) DEFAULT NULL COMMENT '图片地址',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sip_id`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品SIP表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_product_sop`
--

DROP TABLE IF EXISTS `md_product_sop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_product_sop` (
  `sop_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `item_id` bigint NOT NULL COMMENT '物料产品ID',
  `order_num` int DEFAULT NULL COMMENT '排列顺序',
  `process_id` bigint DEFAULT NULL COMMENT '对应的工序',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `sop_title` varchar(255) DEFAULT NULL COMMENT '标题',
  `sop_description` varchar(500) DEFAULT NULL COMMENT '详细描述',
  `sop_url` varchar(255) DEFAULT NULL COMMENT '图片地址',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=207 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品SOP表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_produt_sop`
--

DROP TABLE IF EXISTS `md_produt_sop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_produt_sop` (
  `sop_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `item_id` bigint NOT NULL COMMENT '物料产品ID',
  `order_num` int DEFAULT NULL COMMENT '排列顺序',
  `process_id` bigint DEFAULT NULL COMMENT '对应的工序',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `sop_title` varchar(255) DEFAULT NULL COMMENT '标题',
  `sop_description` varchar(500) DEFAULT NULL COMMENT '详细描述',
  `sop_url` varchar(255) DEFAULT NULL COMMENT '图片地址',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=203 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品SOP表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_unit_measure`
--

DROP TABLE IF EXISTS `md_unit_measure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_unit_measure` (
  `measure_id` bigint NOT NULL AUTO_INCREMENT COMMENT '单位ID',
  `measure_code` varchar(64) NOT NULL COMMENT '单位编码',
  `measure_name` varchar(255) NOT NULL COMMENT '单位名称',
  `primary_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否是主单位',
  `primary_id` bigint DEFAULT NULL COMMENT '主单位ID',
  `change_rate` double(12,4) DEFAULT NULL COMMENT '与主单位换算比例',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`measure_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9007 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='单位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_vendor`
--

DROP TABLE IF EXISTS `md_vendor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_vendor` (
  `vendor_id` bigint NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `vendor_code` varchar(64) NOT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) NOT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `vendor_en` varchar(255) DEFAULT NULL COMMENT '供应商英文名称',
  `vendor_des` varchar(500) DEFAULT NULL COMMENT '供应商简介',
  `vendor_logo` varchar(255) DEFAULT NULL COMMENT '供应商LOGO地址',
  `vendor_level` varchar(64) DEFAULT NULL COMMENT '供应商等级',
  `vendor_score` int DEFAULT NULL COMMENT '供应商评分',
  `address` varchar(500) DEFAULT NULL COMMENT '供应商地址',
  `website` varchar(255) DEFAULT NULL COMMENT '供应商官网地址',
  `email` varchar(255) DEFAULT NULL COMMENT '供应商邮箱地址',
  `tel` varchar(64) DEFAULT NULL COMMENT '供应商电话',
  `contact1` varchar(64) DEFAULT NULL COMMENT '联系人1',
  `contact1_tel` varchar(64) DEFAULT NULL COMMENT '联系人1-电话',
  `contact1_email` varchar(255) DEFAULT NULL COMMENT '联系人1-邮箱',
  `contact2` varchar(64) DEFAULT NULL COMMENT '联系人2',
  `contact2_tel` varchar(64) DEFAULT NULL COMMENT '联系人2-电话',
  `contact2_email` varchar(255) DEFAULT NULL COMMENT '联系人2-邮箱',
  `credit_code` varchar(64) DEFAULT NULL COMMENT '统一社会信用代码',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`vendor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9765 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供应商表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_workshop`
--

DROP TABLE IF EXISTS `md_workshop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_workshop` (
  `workshop_id` bigint NOT NULL AUTO_INCREMENT COMMENT '车间ID',
  `workshop_code` varchar(64) NOT NULL COMMENT '车间编码',
  `workshop_name` varchar(255) NOT NULL COMMENT '车间名称',
  `area` double(12,2) DEFAULT NULL COMMENT '面积',
  `charge` varchar(64) DEFAULT NULL COMMENT '负责人',
  `charge_id` bigint DEFAULT NULL COMMENT '负责人id',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`workshop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9502 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车间表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_workstation`
--

DROP TABLE IF EXISTS `md_workstation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_workstation` (
  `workstation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作站ID',
  `workstation_code` varchar(64) NOT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) NOT NULL COMMENT '工作站名称',
  `workstation_address` varchar(255) DEFAULT NULL COMMENT '工作站地点',
  `workshop_id` bigint DEFAULT NULL COMMENT '所在车间ID',
  `workshop_code` varchar(64) DEFAULT NULL COMMENT '所在车间编码',
  `workshop_name` varchar(255) DEFAULT NULL COMMENT '所在车间名称',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `warehouse_id` bigint NOT NULL COMMENT '线边库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '线边库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '线边库名称',
  `location_id` bigint NOT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint NOT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`workstation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9610 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作站表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_workstation_machine`
--

DROP TABLE IF EXISTS `md_workstation_machine`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_workstation_machine` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `machinery_id` bigint NOT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) DEFAULT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) DEFAULT NULL COMMENT '设备名称',
  `quantity` int DEFAULT '1' COMMENT '数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备资源表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_workstation_tool`
--

DROP TABLE IF EXISTS `md_workstation_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_workstation_tool` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `tool_type_id` bigint NOT NULL COMMENT '工装夹具类型ID',
  `tool_type_code` varchar(64) DEFAULT NULL COMMENT '类型编码',
  `tool_type_name` varchar(255) DEFAULT NULL COMMENT '类型名称',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工装夹具资源表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `md_workstation_worker`
--

DROP TABLE IF EXISTS `md_workstation_worker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `md_workstation_worker` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `post_code` varchar(64) DEFAULT NULL COMMENT '岗位编码',
  `post_name` varchar(255) DEFAULT NULL COMMENT '岗位名称',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力资源表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `print_client`
--

DROP TABLE IF EXISTS `print_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `print_client` (
  `client_id` bigint NOT NULL AUTO_INCREMENT COMMENT '打印机_客户端ID',
  `client_code` varchar(64) NOT NULL COMMENT '打印机_客户端编号',
  `client_name` varchar(255) NOT NULL COMMENT '打印机_客户端名称',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '打印客户端IP',
  `client_port` int DEFAULT NULL COMMENT '打印客户端端口',
  `status` varchar(64) DEFAULT 'READY' COMMENT '打印机客户端状态',
  `client_token` varchar(255) DEFAULT NULL COMMENT '打印机客户端连接token',
  `workshop_id` bigint DEFAULT NULL COMMENT '车间id',
  `workshop_code` varchar(64) DEFAULT NULL COMMENT '车间编码',
  `workshop_name` varchar(255) DEFAULT NULL COMMENT '车间名称',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站id',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) DEFAULT NULL COMMENT '工作站名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`client_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打印机客户端配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `print_printer_config`
--

DROP TABLE IF EXISTS `print_printer_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `print_printer_config` (
  `printer_id` bigint NOT NULL AUTO_INCREMENT COMMENT '打印机ID',
  `client_id` bigint DEFAULT NULL COMMENT '客户端id',
  `printer_code` varchar(64) NOT NULL COMMENT '打印机编号',
  `printer_type` varchar(64) DEFAULT 'LABEL' COMMENT '打印机类型',
  `printer_name` varchar(255) NOT NULL COMMENT '打印机名称',
  `brand` varchar(64) DEFAULT NULL COMMENT '品牌',
  `printer_model` varchar(64) DEFAULT NULL COMMENT '型号',
  `connection_type` varchar(64) DEFAULT NULL COMMENT '连接类型',
  `printer_url` varchar(255) DEFAULT NULL COMMENT '图片URL',
  `printer_ip` varchar(64) DEFAULT NULL COMMENT '打印机IP',
  `printer_port` int DEFAULT NULL COMMENT '打印机端口',
  `enable_flag` char(1) DEFAULT 'Y' COMMENT '启用状态',
  `status` varchar(64) DEFAULT 'READY' COMMENT '打印机状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `default_flag` char(5) DEFAULT NULL COMMENT '是否为默认打印机',
  PRIMARY KEY (`printer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打印机配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `print_template`
--

DROP TABLE IF EXISTS `print_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `print_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编号',
  `template_name` varchar(255) DEFAULT NULL COMMENT '模板名称',
  `template_type` varchar(64) NOT NULL COMMENT '模板类型',
  `template_json` json DEFAULT NULL COMMENT '模板内容',
  `paper_type` varchar(64) DEFAULT 'A4' COMMENT '纸张类型',
  `template_width` int DEFAULT NULL COMMENT '模板宽度',
  `template_height` int DEFAULT NULL COMMENT '模板高度',
  `is_default` char(1) DEFAULT 'Y' COMMENT '是否默认',
  `enable_flag` char(1) DEFAULT 'Y' COMMENT '启用状态',
  `template_pic` varchar(255) DEFAULT NULL COMMENT '缩略图',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=228 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打印模板配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_andon_config`
--

DROP TABLE IF EXISTS `pro_andon_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_andon_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `andon_reason` varchar(500) NOT NULL COMMENT '呼叫原因',
  `andon_level` varchar(64) NOT NULL COMMENT '级别',
  `handler_role_id` bigint DEFAULT NULL COMMENT '处置人角色ID',
  `handler_role_name` varchar(128) DEFAULT NULL COMMENT '处置人角色名称',
  `handler_user_id` bigint DEFAULT NULL COMMENT '处置人',
  `handler_user_name` varchar(64) DEFAULT NULL COMMENT '处置人用户名',
  `handler_nick_name` varchar(64) DEFAULT NULL COMMENT '处置人名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安灯呼叫配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_andon_record`
--

DROP TABLE IF EXISTS `pro_andon_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_andon_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(125) DEFAULT NULL COMMENT '工作站名称',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `nick_name` varchar(125) DEFAULT NULL COMMENT '名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编号',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '生产工单名称',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `andon_reason` varchar(500) NOT NULL COMMENT '呼叫原因',
  `andon_level` varchar(64) DEFAULT 'LEVEL3' COMMENT '级别',
  `handle_time` datetime DEFAULT NULL COMMENT '处置时间',
  `handler_user_id` bigint DEFAULT NULL COMMENT '处置人',
  `handler_user_name` varchar(64) DEFAULT NULL COMMENT '处置人用户名',
  `handler_nick_name` varchar(64) DEFAULT NULL COMMENT '处置人名称',
  `status` varchar(64) DEFAULT 'ACTIVE' COMMENT '处置状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安灯呼叫记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_card`
--

DROP TABLE IF EXISTS `pro_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_card` (
  `card_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流转卡ID',
  `card_code` varchar(64) DEFAULT NULL COMMENT '流转卡编号',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编号',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '生产工单名称',
  `batch_code` varchar(64) DEFAULT NULL COMMENT '批次号',
  `item_id` bigint DEFAULT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `barcode_url` varchar(255) DEFAULT NULL COMMENT '赋码地址',
  `quantity_transfered` double(12,2) DEFAULT NULL COMMENT '流转数量',
  `status` varchar(64) DEFAULT NULL COMMENT '流转卡状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`card_id`)
) ENGINE=InnoDB AUTO_INCREMENT=214 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流转卡表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_card_process`
--

DROP TABLE IF EXISTS `pro_card_process`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_card_process` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `card_id` bigint NOT NULL COMMENT '流转卡ID',
  `card_code` varchar(64) DEFAULT NULL COMMENT '流转卡编号',
  `seq_num` int DEFAULT '1' COMMENT '序号',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `input_time` datetime DEFAULT NULL COMMENT '进入工序时间',
  `output_time` datetime DEFAULT NULL COMMENT '出工序时间',
  `quantity_input` double(12,2) DEFAULT NULL COMMENT '投入数量',
  `quantity_output` double(12,2) DEFAULT NULL COMMENT '产出数量',
  `quantity_unquanlify` double(12,2) DEFAULT NULL COMMENT '不合格品数量',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(125) DEFAULT NULL COMMENT '工作站名称',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `nick_name` varchar(125) DEFAULT NULL COMMENT '名称',
  `ipqc_id` bigint DEFAULT NULL COMMENT '过程检验单ID',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=203 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工序流转卡-工序信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_feedback`
--

DROP TABLE IF EXISTS `pro_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_feedback` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `feedback_type` varchar(64) NOT NULL COMMENT '报工类型',
  `feedback_code` varchar(64) DEFAULT NULL COMMENT '报工单编号',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) DEFAULT NULL COMMENT '工作站名称',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编号',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '生产工单名称',
  `route_id` bigint NOT NULL COMMENT '工艺流程ID',
  `route_code` varchar(64) DEFAULT NULL COMMENT '工艺流程编号',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID',
  `task_code` varchar(64) DEFAULT NULL COMMENT '生产任务编号',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '产品物料名称',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `expire_date` datetime DEFAULT NULL COMMENT '过期日期',
  `lot_number` varchar(128) DEFAULT NULL COMMENT '生产批号',
  `quantity` double(14,2) DEFAULT NULL COMMENT '排产数量',
  `quantity_feedback` double(14,2) DEFAULT NULL COMMENT '本次报工数量',
  `quantity_qualified` double(14,2) DEFAULT NULL COMMENT '合格品数量',
  `quantity_unquanlified` double(14,2) DEFAULT NULL COMMENT '不良品数量',
  `quantity_uncheck` double(14,2) DEFAULT NULL COMMENT '待检测数量',
  `quantity_labor_scrap` double(14,2) DEFAULT NULL COMMENT '工废数量',
  `quantity_material_scrap` double(14,2) DEFAULT NULL COMMENT '料废数量',
  `quantity_other_scrap` double(14,2) DEFAULT NULL COMMENT '其他废品数量',
  `user_name` varchar(64) DEFAULT NULL COMMENT '报工用户名',
  `nick_name` varchar(64) DEFAULT NULL COMMENT '昵称',
  `feedback_channel` varchar(64) DEFAULT NULL COMMENT '报工途径',
  `feedback_time` datetime DEFAULT NULL COMMENT '报工时间',
  `record_user` varchar(64) DEFAULT NULL COMMENT '记录人',
  `record_nick` varchar(64) DEFAULT NULL COMMENT '记录人名称',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=138 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产报工记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_process`
--

DROP TABLE IF EXISTS `pro_process`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_process` (
  `process_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工序ID',
  `process_code` varchar(64) NOT NULL COMMENT '工序编码',
  `process_name` varchar(255) NOT NULL COMMENT '工序名称',
  `attention` varchar(1000) DEFAULT NULL COMMENT '工艺要求',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`process_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9410 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产工序表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_process_content`
--

DROP TABLE IF EXISTS `pro_process_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_process_content` (
  `content_id` bigint NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `order_num` int DEFAULT '0' COMMENT '顺序编号',
  `content_text` varchar(500) DEFAULT NULL COMMENT '内容说明',
  `device` varchar(255) DEFAULT NULL COMMENT '辅助设备',
  `material` varchar(255) DEFAULT NULL COMMENT '辅助材料',
  `doc_url` varchar(255) DEFAULT NULL COMMENT '材料URL',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`content_id`)
) ENGINE=InnoDB AUTO_INCREMENT=226 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产工序内容表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_route`
--

DROP TABLE IF EXISTS `pro_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_route` (
  `route_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工艺路线ID',
  `route_code` varchar(64) NOT NULL COMMENT '工艺路线编号',
  `route_name` varchar(255) NOT NULL COMMENT '工艺路线名称',
  `route_desc` varchar(500) DEFAULT NULL COMMENT '工艺路线说明',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`route_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9454 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工艺路线表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_route_process`
--

DROP TABLE IF EXISTS `pro_route_process`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_route_process` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `order_num` int DEFAULT '1' COMMENT '序号',
  `next_process_id` bigint NOT NULL COMMENT '工序ID',
  `next_process_code` varchar(64) DEFAULT NULL COMMENT '工序编码',
  `next_process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `link_type` varchar(64) DEFAULT 'SS' COMMENT '与下一道工序关系',
  `default_pre_time` int DEFAULT '0' COMMENT '准备时间',
  `default_suf_time` int DEFAULT '0' COMMENT '等待时间',
  `color_code` char(7) DEFAULT '#00AEF3' COMMENT '甘特图显示颜色',
  `key_flag` varchar(64) DEFAULT 'N' COMMENT '关键工序',
  `is_check` char(1) DEFAULT 'N' COMMENT '是否检验',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9470 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工艺组成表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_route_product`
--

DROP TABLE IF EXISTS `pro_route_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_route_product` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` int DEFAULT '1' COMMENT '生产数量',
  `production_time` double(12,2) DEFAULT '1.00' COMMENT '生产用时',
  `time_unit_type` varchar(64) DEFAULT 'MINUTE' COMMENT '时间单位',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9484 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品制程';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_route_product_bom`
--

DROP TABLE IF EXISTS `pro_route_product_bom`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_route_product_bom` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `product_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `quantity` double(12,2) DEFAULT '1.00' COMMENT '用料比例',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9507 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品制程物料BOM表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_shutdown_record`
--

DROP TABLE IF EXISTS `pro_shutdown_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_shutdown_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(125) DEFAULT NULL COMMENT '工作站名称',
  `machinery_id` bigint DEFAULT NULL COMMENT '设备ID',
  `machinery_code` varchar(64) DEFAULT NULL COMMENT '设备编号',
  `machinery_name` varchar(255) DEFAULT NULL COMMENT '设备名称',
  `shutdown_reason` varchar(500) NOT NULL COMMENT '停机原因',
  `operation_time` datetime DEFAULT NULL COMMENT '操作时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停机记录记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_sn_process`
--

DROP TABLE IF EXISTS `pro_sn_process`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_sn_process` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `sn_id` bigint NOT NULL COMMENT 'SNID',
  `sn_code` varchar(64) DEFAULT NULL COMMENT 'SN编号',
  `seq_num` int DEFAULT '1' COMMENT '序号',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `input_time` datetime DEFAULT NULL COMMENT '进入工序时间',
  `output_time` datetime DEFAULT NULL COMMENT '出工序时间',
  `quantity_input` double(12,2) DEFAULT NULL COMMENT '投入数量',
  `quantity_output` double(12,2) DEFAULT NULL COMMENT '产出数量',
  `quantity_unquanlify` double(12,2) DEFAULT NULL COMMENT '不合格品数量',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(125) DEFAULT NULL COMMENT '工作站名称',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `nick_name` varchar(125) DEFAULT NULL COMMENT '名称',
  `ipqc_id` bigint DEFAULT NULL COMMENT '过程检验单ID',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SN流转-工序信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_task`
--

DROP TABLE IF EXISTS `pro_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_code` varchar(64) NOT NULL COMMENT '任务编号',
  `task_name` varchar(255) NOT NULL COMMENT '任务名称',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) NOT NULL COMMENT '生产工单编号',
  `workorder_name` varchar(255) NOT NULL COMMENT '工单名称',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) NOT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) NOT NULL COMMENT '工作站名称',
  `route_id` bigint NOT NULL COMMENT '工艺ID',
  `route_code` varchar(64) DEFAULT NULL COMMENT '工艺编号',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `quantity` double(14,2) NOT NULL DEFAULT '1.00' COMMENT '排产数量',
  `quantity_produced` double(14,2) DEFAULT '0.00' COMMENT '已生产数量',
  `quantity_quanlify` double(14,2) DEFAULT '0.00' COMMENT '合格品数量',
  `quantity_unquanlify` double(14,2) DEFAULT '0.00' COMMENT '不良品数量',
  `quantity_changed` double(14,2) DEFAULT '0.00' COMMENT '调整数量',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `start_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '开始生产时间',
  `duration` int DEFAULT '1' COMMENT '生产时长',
  `end_time` datetime DEFAULT NULL COMMENT '完成生产时间',
  `color_code` char(7) DEFAULT '#00AEF3' COMMENT '甘特图显示颜色',
  `request_date` datetime DEFAULT NULL COMMENT '需求日期',
  `status` varchar(64) DEFAULT 'NORMAL',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `finish_date` datetime DEFAULT NULL COMMENT '完成日期',
  `cancel_date` datetime DEFAULT NULL COMMENT '取消日期',
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=124 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_task_issue`
--

DROP TABLE IF EXISTS `pro_task_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_task_issue` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `task_id` bigint NOT NULL COMMENT '生产任务ID',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID',
  `source_doc_id` bigint NOT NULL COMMENT '单据ID',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '单据编号',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '单据类型',
  `batch_code` varchar(64) DEFAULT NULL COMMENT '投料批次',
  `source_line_id` bigint DEFAULT NULL COMMENT '行ID',
  `item_id` bigint DEFAULT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `quantity_issued` double(12,2) DEFAULT NULL COMMENT '总的投料数量',
  `quantity_available` double(12,2) DEFAULT NULL COMMENT '当前可用数量',
  `quantity_used` double(12,2) DEFAULT NULL COMMENT '当前使用数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产任务投料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_trans_consume`
--

DROP TABLE IF EXISTS `pro_trans_consume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_trans_consume` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `trans_order_id` bigint DEFAULT NULL COMMENT '流转单ID',
  `trans_order_code` varchar(64) DEFAULT NULL COMMENT '流转单编号',
  `task_id` bigint NOT NULL COMMENT '生产任务ID',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `batch_code` varchar(64) DEFAULT NULL COMMENT '批次号',
  `source_doc_id` bigint DEFAULT NULL COMMENT '被消耗单据ID',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '被消耗单据编号',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '被消耗单据类型',
  `source_line_id` bigint DEFAULT NULL COMMENT '被消耗单据行ID',
  `source_batch_code` varchar(64) DEFAULT NULL COMMENT '被消耗物料批次号',
  `item_id` bigint DEFAULT NULL COMMENT '被消耗产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '被消耗产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '被消耗产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `quantity_consumed` double(12,2) DEFAULT NULL COMMENT '消耗数量',
  `consume_date` datetime DEFAULT NULL COMMENT '消耗时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料消耗记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_trans_order`
--

DROP TABLE IF EXISTS `pro_trans_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_trans_order` (
  `trans_order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流转单ID',
  `trans_order_code` varchar(64) DEFAULT NULL COMMENT '流转单编号',
  `task_id` bigint NOT NULL COMMENT '生产任务ID',
  `task_code` varchar(64) DEFAULT NULL COMMENT '生产任务编号',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) DEFAULT NULL COMMENT '工作站名称',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编号',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '生产工单名称',
  `batch_code` varchar(64) DEFAULT NULL COMMENT '批次号',
  `item_id` bigint DEFAULT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `quantity_transfered` double(12,2) DEFAULT NULL COMMENT '流转数量',
  `produce_date` datetime DEFAULT NULL COMMENT '生产日期',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `barcode_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`trans_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流转单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_user_workstation`
--

DROP TABLE IF EXISTS `pro_user_workstation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_user_workstation` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `nick_name` bigint DEFAULT NULL COMMENT '名称',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(125) DEFAULT NULL COMMENT '工作站名称',
  `operation_time` datetime DEFAULT NULL COMMENT '操作时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9998 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户工作站绑定关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_workorder`
--

DROP TABLE IF EXISTS `pro_workorder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_workorder` (
  `workorder_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `workorder_code` varchar(64) NOT NULL COMMENT '工单编码',
  `workorder_name` varchar(255) NOT NULL COMMENT '工单名称',
  `order_source` varchar(64) NOT NULL COMMENT '来源类型',
  `source_code` varchar(64) DEFAULT NULL COMMENT '来源单据',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `product_code` varchar(64) NOT NULL COMMENT '产品编号',
  `product_name` varchar(255) NOT NULL COMMENT '产品名称',
  `product_spc` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(14,2) NOT NULL DEFAULT '0.00' COMMENT '生产数量',
  `quantity_produced` double(14,2) DEFAULT '0.00' COMMENT '已生产数量',
  `quantity_changed` double(14,2) DEFAULT '0.00' COMMENT '调整数量',
  `quantity_scheduled` double(14,2) DEFAULT '0.00' COMMENT '已排产数量',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `request_date` datetime NOT NULL COMMENT '需求日期',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父工单',
  `ancestors` varchar(500) NOT NULL COMMENT '所有父节点ID',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `batch_code` varchar(64) DEFAULT NULL,
  `finish_date` datetime DEFAULT NULL COMMENT '完成时间',
  `workorder_type` varchar(64) DEFAULT 'SELF',
  `vendor_id` bigint DEFAULT NULL,
  `vendor_code` varchar(64) DEFAULT NULL,
  `vendor_name` varchar(255) DEFAULT NULL,
  `cancel_date` datetime DEFAULT NULL COMMENT '取消日期',
  PRIMARY KEY (`workorder_id`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产工单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_workorder_bom`
--

DROP TABLE IF EXISTS `pro_workorder_bom`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_workorder_bom` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'BOM行ID',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID',
  `item_id` bigint NOT NULL COMMENT 'BOM物料ID',
  `item_code` varchar(64) NOT NULL COMMENT 'BOM物料编号',
  `item_name` varchar(255) NOT NULL COMMENT 'BOM物料名称',
  `item_spc` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) NOT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `item_or_product` varchar(20) NOT NULL COMMENT '物料产品标识',
  `quantity` double(14,2) NOT NULL DEFAULT '0.00' COMMENT '预计使用量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=281 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产工单BOM组成表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pro_workrecord`
--

DROP TABLE IF EXISTS `pro_workrecord`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pro_workrecord` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `nick_name` bigint DEFAULT NULL COMMENT '名称',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(125) DEFAULT NULL COMMENT '工作站名称',
  `operation_flag` char(1) NOT NULL COMMENT '操作类型',
  `operation_time` datetime DEFAULT NULL COMMENT '操作时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=231 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上下工记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_defect`
--

DROP TABLE IF EXISTS `qc_defect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_defect` (
  `defect_id` bigint NOT NULL AUTO_INCREMENT COMMENT '缺陷ID',
  `defect_code` varchar(64) NOT NULL COMMENT '缺陷编码',
  `defect_name` varchar(500) NOT NULL COMMENT '缺陷描述',
  `index_type` varchar(64) NOT NULL COMMENT '检测项类型',
  `defect_level` varchar(64) NOT NULL COMMENT '缺陷等级',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`defect_id`)
) ENGINE=InnoDB AUTO_INCREMENT=208 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='常见缺陷表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_defect_record`
--

DROP TABLE IF EXISTS `qc_defect_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_defect_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '缺陷ID',
  `qc_type` varchar(64) NOT NULL COMMENT '检验单类型',
  `qc_id` bigint NOT NULL COMMENT '检验单ID',
  `line_id` bigint NOT NULL COMMENT '检验单行ID',
  `defect_name` varchar(500) NOT NULL COMMENT '缺陷描述',
  `defect_level` varchar(64) NOT NULL COMMENT '缺陷等级',
  `defect_quantity` int DEFAULT '1' COMMENT '缺陷数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检验单缺陷记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_index`
--

DROP TABLE IF EXISTS `qc_index`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_index` (
  `index_id` bigint NOT NULL AUTO_INCREMENT COMMENT '检测项ID',
  `index_code` varchar(64) NOT NULL COMMENT '检测项编码',
  `index_name` varchar(255) NOT NULL COMMENT '检测项名称',
  `index_type` varchar(64) NOT NULL COMMENT '检测项类型',
  `qc_tool` varchar(255) DEFAULT NULL COMMENT '检测工具',
  `qc_result_type` varchar(64) NOT NULL COMMENT '质检值类型',
  `qc_result_spc` varchar(255) DEFAULT NULL COMMENT '值属性',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`index_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9811 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_ipqc`
--

DROP TABLE IF EXISTS `qc_ipqc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_ipqc` (
  `ipqc_id` bigint NOT NULL AUTO_INCREMENT COMMENT '检验单ID',
  `ipqc_code` varchar(64) NOT NULL COMMENT '检验单编号',
  `ipqc_name` varchar(255) DEFAULT NULL COMMENT '检验单名称',
  `ipqc_type` varchar(64) NOT NULL COMMENT '检验类型',
  `template_id` bigint NOT NULL COMMENT '检验模板ID',
  `source_doc_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '来源单据类型',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '来源单据编号',
  `source_line_id` bigint DEFAULT NULL COMMENT '来源单据行ID',
  `workorder_id` bigint NOT NULL COMMENT '工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '工单编码',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '工单名称',
  `task_id` bigint DEFAULT NULL COMMENT '任务ID',
  `task_code` varchar(64) DEFAULT NULL COMMENT '任务编号',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) DEFAULT NULL COMMENT '工作站名称',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_check` double(12,4) DEFAULT '1.0000' COMMENT '检测数量',
  `quantity_unqualified` double(12,4) DEFAULT '0.0000' COMMENT '不合格数',
  `quantity_qualified` double(12,4) DEFAULT NULL COMMENT '合格品数量',
  `quantity_labor_scrap` double(14,2) DEFAULT NULL COMMENT '工废数量',
  `quantity_material_scrap` double(14,2) DEFAULT NULL COMMENT '料废数量',
  `quantity_other_scrap` double(14,2) DEFAULT NULL COMMENT '其他废品数量',
  `cr_rate` double(12,2) DEFAULT '0.00' COMMENT '致命缺陷率',
  `maj_rate` double(12,2) DEFAULT '0.00' COMMENT '严重缺陷率',
  `min_rate` double(12,2) DEFAULT '0.00' COMMENT '轻微缺陷率',
  `cr_quantity` double(12,4) DEFAULT '0.0000' COMMENT '致命缺陷数量',
  `maj_quantity` double(12,4) DEFAULT '0.0000' COMMENT '严重缺陷数量',
  `min_quantity` double(12,4) DEFAULT '0.0000' COMMENT '轻微缺陷数量',
  `check_result` varchar(64) DEFAULT NULL COMMENT '检测结果',
  `inspect_date` datetime DEFAULT NULL COMMENT '检测日期',
  `inspector` varchar(64) DEFAULT NULL COMMENT '检测人员',
  `status` varchar(64) DEFAULT NULL COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ipqc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='过程检验单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_ipqc_line`
--

DROP TABLE IF EXISTS `qc_ipqc_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_ipqc_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `ipqc_id` bigint NOT NULL COMMENT '检验单ID',
  `index_id` bigint NOT NULL COMMENT '检测项ID',
  `index_code` varchar(64) DEFAULT NULL COMMENT '检测项编码',
  `index_name` varchar(255) DEFAULT NULL COMMENT '检测项名称',
  `index_type` varchar(64) DEFAULT NULL COMMENT '检测项类型',
  `qc_tool` varchar(255) DEFAULT NULL COMMENT '检测工具',
  `check_method` varchar(500) DEFAULT NULL COMMENT '检测要求',
  `stander_val` double(12,4) DEFAULT NULL COMMENT '标准值',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `threshold_max` double(12,4) DEFAULT NULL COMMENT '误差上限',
  `threshold_min` double(12,4) DEFAULT NULL COMMENT '误差下限',
  `cr_quantity` double(12,4) DEFAULT '0.0000' COMMENT '致命缺陷数量',
  `maj_quantity` double(12,4) DEFAULT '0.0000' COMMENT '严重缺陷数量',
  `min_quantity` double(12,4) DEFAULT '0.0000' COMMENT '轻微缺陷数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=184 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='过程检验单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_iqc`
--

DROP TABLE IF EXISTS `qc_iqc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_iqc` (
  `iqc_id` bigint NOT NULL AUTO_INCREMENT COMMENT '来料检验单ID',
  `iqc_code` varchar(64) NOT NULL COMMENT '来料检验单编号',
  `iqc_name` varchar(500) NOT NULL COMMENT '来料检验单名称',
  `template_id` bigint NOT NULL COMMENT '检验模板ID',
  `source_doc_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '来源单据类型',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '来源单据编号',
  `source_line_id` bigint DEFAULT NULL COMMENT '来源单据行ID',
  `vendor_id` bigint NOT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) NOT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) NOT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `vendor_batch` varchar(64) DEFAULT NULL COMMENT '供应商批次号',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_min_check` int DEFAULT '1' COMMENT '最低检测数',
  `quantity_max_unqualified` int DEFAULT '0' COMMENT '最大不合格数',
  `quantity_recived` double(12,2) NOT NULL COMMENT '本次接收数量',
  `quantity_check` int DEFAULT NULL COMMENT '本次检测数量',
  `quantity_qualified` int DEFAULT '0' COMMENT '合格数',
  `quantity_unqualified` int DEFAULT '0' COMMENT '不合格数',
  `cr_rate` double(12,2) DEFAULT '0.00' COMMENT '致命缺陷率',
  `maj_rate` double(12,2) DEFAULT '0.00' COMMENT '严重缺陷率',
  `min_rate` double(12,2) DEFAULT '0.00' COMMENT '轻微缺陷率',
  `cr_quantity` int DEFAULT '0' COMMENT '致命缺陷数量',
  `maj_quantity` int DEFAULT '0' COMMENT '严重缺陷数量',
  `min_quantity` int DEFAULT '0' COMMENT '轻微缺陷数量',
  `check_result` varchar(64) DEFAULT NULL COMMENT '检测结果',
  `recive_date` datetime DEFAULT NULL COMMENT '来料日期',
  `inspect_date` datetime DEFAULT NULL COMMENT '检测日期',
  `inspector` varchar(64) DEFAULT NULL COMMENT '检测人员',
  `status` varchar(64) DEFAULT NULL COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`iqc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=245 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='来料检验单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_iqc_defect`
--

DROP TABLE IF EXISTS `qc_iqc_defect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_iqc_defect` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '缺陷ID',
  `iqc_id` bigint NOT NULL COMMENT '来料检验单ID',
  `line_id` bigint NOT NULL COMMENT '来料检验单行ID',
  `defect_name` varchar(500) NOT NULL COMMENT '缺陷描述',
  `defect_level` varchar(64) NOT NULL COMMENT '缺陷等级',
  `defect_quantity` int DEFAULT '1' COMMENT '缺陷数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='来料检验单缺陷记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_iqc_line`
--

DROP TABLE IF EXISTS `qc_iqc_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_iqc_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `iqc_id` bigint NOT NULL COMMENT '检验单ID',
  `index_id` bigint NOT NULL COMMENT '检测项ID',
  `index_code` varchar(64) DEFAULT NULL COMMENT '检测项编码',
  `index_name` varchar(255) DEFAULT NULL COMMENT '检测项名称',
  `index_type` varchar(64) DEFAULT NULL COMMENT '检测项类型',
  `qc_tool` varchar(255) DEFAULT NULL COMMENT '检测工具',
  `check_method` varchar(500) DEFAULT NULL COMMENT '检测要求',
  `stander_val` double(12,4) DEFAULT NULL COMMENT '标准值',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `threshold_max` double(12,4) DEFAULT NULL COMMENT '误差上限',
  `threshold_min` double(12,4) DEFAULT NULL COMMENT '误差下限',
  `cr_quantity` int DEFAULT '0' COMMENT '致命缺陷数量',
  `maj_quantity` int DEFAULT '0' COMMENT '严重缺陷数量',
  `min_quantity` int DEFAULT '0' COMMENT '轻微缺陷数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=740 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='来料检验单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_oqc`
--

DROP TABLE IF EXISTS `qc_oqc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_oqc` (
  `oqc_id` bigint NOT NULL AUTO_INCREMENT COMMENT '出货检验单ID',
  `oqc_code` varchar(64) NOT NULL COMMENT '出货检验单编号',
  `oqc_name` varchar(500) DEFAULT NULL COMMENT '出货检验单名称',
  `template_id` bigint NOT NULL COMMENT '检验模板ID',
  `source_doc_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '来源单据类型',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '来源单据编号',
  `source_line_id` bigint DEFAULT NULL COMMENT '来源单据行ID',
  `client_id` bigint NOT NULL COMMENT '客户ID',
  `client_code` varchar(64) NOT NULL COMMENT '客户编码',
  `client_name` varchar(255) NOT NULL COMMENT '客户名称',
  `batch_code` varchar(64) DEFAULT NULL COMMENT '批次号',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `quantity_min_check` double(12,4) DEFAULT '1.0000' COMMENT '最低检测数',
  `quantity_max_unqualified` double(12,4) DEFAULT '0.0000' COMMENT '最大不合格数',
  `quantity_out` double(12,4) NOT NULL COMMENT '发货数量',
  `quantity_check` double(12,4) NOT NULL COMMENT '本次检测数量',
  `quantity_unqualified` double(12,4) DEFAULT '0.0000' COMMENT '不合格数',
  `quantity_qualified` double(12,4) DEFAULT '0.0000' COMMENT '合格数量',
  `cr_rate` double(12,4) DEFAULT '0.0000' COMMENT '致命缺陷率',
  `maj_rate` double(12,4) DEFAULT '0.0000' COMMENT '严重缺陷率',
  `min_rate` double(12,4) DEFAULT '0.0000' COMMENT '轻微缺陷率',
  `cr_quantity` double(12,4) DEFAULT '0.0000' COMMENT '致命缺陷数量',
  `maj_quantity` double(12,4) DEFAULT '0.0000' COMMENT '严重缺陷数量',
  `min_quantity` double(12,4) DEFAULT '0.0000' COMMENT '轻微缺陷数量',
  `check_result` varchar(64) DEFAULT NULL COMMENT '检测结果',
  `out_date` datetime DEFAULT NULL COMMENT '出货日期',
  `inspect_date` datetime DEFAULT NULL COMMENT '检测日期',
  `inspector` varchar(64) DEFAULT NULL COMMENT '检测人员',
  `status` varchar(64) DEFAULT NULL COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`oqc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出货检验单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_oqc_line`
--

DROP TABLE IF EXISTS `qc_oqc_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_oqc_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `oqc_id` bigint NOT NULL COMMENT '检验单ID',
  `index_id` bigint NOT NULL COMMENT '检测项ID',
  `index_code` varchar(64) DEFAULT NULL COMMENT '检测项编码',
  `index_name` varchar(255) DEFAULT NULL COMMENT '检测项名称',
  `index_type` varchar(64) DEFAULT NULL COMMENT '检测项类型',
  `qc_tool` varchar(255) DEFAULT NULL COMMENT '检测工具',
  `check_method` varchar(500) DEFAULT NULL COMMENT '检测要求',
  `stander_val` double(12,4) DEFAULT NULL COMMENT '标准值',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `threshold_max` double(12,4) DEFAULT NULL COMMENT '误差上限',
  `threshold_min` double(12,4) DEFAULT NULL COMMENT '误差下限',
  `cr_quantity` double(12,4) DEFAULT '0.0000' COMMENT '致命缺陷数量',
  `maj_quantity` double(12,4) DEFAULT '0.0000' COMMENT '严重缺陷数量',
  `min_quantity` double(12,4) DEFAULT '0.0000' COMMENT '轻微缺陷数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出货检验单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_result`
--

DROP TABLE IF EXISTS `qc_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_result` (
  `result_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `result_code` varchar(64) NOT NULL COMMENT '样品编号',
  `source_doc_id` bigint DEFAULT NULL COMMENT '关联的质检单ID',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '关联的质检单编号',
  `source_doc_name` varchar(255) DEFAULT NULL COMMENT '关联的质检单名称',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '关联的质检单类型',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `sn_code` varchar(255) DEFAULT NULL COMMENT '对应的物资SN',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`result_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测结果记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_result_detail`
--

DROP TABLE IF EXISTS `qc_result_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_result_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水号',
  `result_id` bigint NOT NULL COMMENT '结果记录ID',
  `index_id` bigint NOT NULL COMMENT '检测项ID',
  `index_type` varchar(64) DEFAULT NULL COMMENT '检测项类型',
  `qc_tool` varchar(255) DEFAULT NULL COMMENT '检测工具',
  `check_method` varchar(500) DEFAULT NULL COMMENT '检测要求',
  `stander_val` double(12,4) DEFAULT NULL COMMENT '标准值',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `threshold_max` double(12,4) DEFAULT NULL COMMENT '误差上限',
  `threshold_min` double(12,4) DEFAULT NULL COMMENT '误差下限',
  `qc_result_type` varchar(64) NOT NULL COMMENT '质检值类型',
  `qc_result_spc` varchar(255) DEFAULT NULL COMMENT '值属性',
  `qc_val_float` float(14,4) DEFAULT NULL COMMENT '浮点值',
  `qc_val_integer` int DEFAULT NULL COMMENT '整数',
  `qc_val_text` varchar(500) DEFAULT NULL COMMENT '文字',
  `qc_val_dict` varchar(64) DEFAULT NULL COMMENT '字典项',
  `qc_val_file` varchar(255) DEFAULT NULL COMMENT '文件',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=330 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测结果明细记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_rqc`
--

DROP TABLE IF EXISTS `qc_rqc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_rqc` (
  `rqc_id` bigint NOT NULL AUTO_INCREMENT COMMENT '检验单ID',
  `rqc_code` varchar(64) NOT NULL COMMENT '检验单编号',
  `rqc_name` varchar(500) DEFAULT NULL COMMENT '检验单名称',
  `template_id` bigint NOT NULL COMMENT '检验模板ID',
  `source_doc_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '来源单据类型',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '来源单据编号',
  `source_line_id` bigint DEFAULT NULL COMMENT '来源单据行ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(128) DEFAULT NULL COMMENT '批次号',
  `quantity_check` double(12,4) DEFAULT '1.0000' COMMENT '检测数量',
  `quantity_unqualified` double(12,4) DEFAULT '0.0000' COMMENT '不合格数',
  `quantity_qualified` double(12,4) DEFAULT NULL COMMENT '合格品数量',
  `check_result` varchar(64) DEFAULT NULL COMMENT '检测结果',
  `inspect_date` datetime DEFAULT NULL COMMENT '检测日期',
  `user_id` bigint DEFAULT NULL COMMENT '检测人员ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '检测人员名称',
  `nick_name` varchar(64) DEFAULT NULL COMMENT '检测人员',
  `status` varchar(64) DEFAULT NULL COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rqc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退料检验单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_rqc_line`
--

DROP TABLE IF EXISTS `qc_rqc_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_rqc_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `rqc_id` bigint NOT NULL COMMENT '检验单ID',
  `index_id` bigint NOT NULL COMMENT '检测项ID',
  `index_code` varchar(64) DEFAULT NULL COMMENT '检测项编码',
  `index_name` varchar(255) DEFAULT NULL COMMENT '检测项名称',
  `index_type` varchar(64) DEFAULT NULL COMMENT '检测项类型',
  `qc_tool` varchar(255) DEFAULT NULL COMMENT '检测工具',
  `check_method` varchar(500) DEFAULT NULL COMMENT '检测要求',
  `stander_val` double(12,4) DEFAULT NULL COMMENT '标准值',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `threshold_max` double(12,4) DEFAULT NULL COMMENT '误差上限',
  `threshold_min` double(12,4) DEFAULT NULL COMMENT '误差下限',
  `cr_quantity` double(12,4) DEFAULT '0.0000' COMMENT '致命缺陷数量',
  `maj_quantity` double(12,4) DEFAULT '0.0000' COMMENT '严重缺陷数量',
  `min_quantity` double(12,4) DEFAULT '0.0000' COMMENT '轻微缺陷数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退料检验单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_template`
--

DROP TABLE IF EXISTS `qc_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '检测模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '检测模板编号',
  `template_name` varchar(255) NOT NULL COMMENT '检测模板名称',
  `qc_types` varchar(255) NOT NULL COMMENT '检测种类',
  `enable_flag` char(1) DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9825 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_template_index`
--

DROP TABLE IF EXISTS `qc_template_index`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_template_index` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `template_id` bigint NOT NULL COMMENT '检测模板ID',
  `index_id` bigint NOT NULL COMMENT '检测项ID',
  `index_code` varchar(64) NOT NULL COMMENT '检测项编码',
  `index_name` varchar(255) NOT NULL COMMENT '检测项名称',
  `index_type` varchar(64) NOT NULL COMMENT '检测项类型',
  `qc_tool` varchar(255) DEFAULT NULL COMMENT '检测工具',
  `check_method` varchar(500) DEFAULT NULL COMMENT '检测方法',
  `stander_val` double(12,4) DEFAULT NULL COMMENT '标准值',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `threshold_max` double(12,4) DEFAULT NULL COMMENT '误差上限',
  `threshold_min` double(12,4) DEFAULT NULL COMMENT '误差下限',
  `doc_url` varchar(255) DEFAULT NULL COMMENT '说明图',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9856 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测模板-检测项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qc_template_product`
--

DROP TABLE IF EXISTS `qc_template_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_template_product` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `template_id` bigint NOT NULL COMMENT '检测模板ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_check` int DEFAULT '1' COMMENT '最低检测数',
  `quantity_unqualified` int DEFAULT '0' COMMENT '最大不合格数',
  `cr_rate` double(12,2) DEFAULT '0.00' COMMENT '最大致命缺陷率',
  `maj_rate` double(12,2) DEFAULT '0.00' COMMENT '最大严重缺陷率',
  `min_rate` double(12,2) DEFAULT '100.00' COMMENT '最大轻微缺陷率',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9878 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测模板-产品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_chart`
--

DROP TABLE IF EXISTS `report_chart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_chart` (
  `chart_id` bigint NOT NULL AUTO_INCREMENT COMMENT '图表ID',
  `chart_code` varchar(64) NOT NULL COMMENT '图表编号',
  `chart_name` varchar(64) NOT NULL COMMENT '图表名称',
  `chart_type` varchar(64) DEFAULT NULL COMMENT '图表类型',
  `business_type` varchar(64) NOT NULL COMMENT '业务类型',
  `api` varchar(256) DEFAULT NULL COMMENT '接口地址',
  `options` text NOT NULL COMMENT '图表options参数',
  `chart_pic` varchar(128) DEFAULT NULL COMMENT '缩略图地址',
  `enable_flag` char(1) DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`chart_id`)
) ENGINE=InnoDB AUTO_INCREMENT=210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图形报表组件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_chart_role`
--

DROP TABLE IF EXISTS `report_chart_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_chart_role` (
  `chart_id` bigint NOT NULL COMMENT '图表ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`chart_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图形报表角色权限';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_attachment`
--

DROP TABLE IF EXISTS `sys_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_attachment` (
  `attachment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `source_doc_id` bigint DEFAULT NULL COMMENT '关联的业务单据ID',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '业务单据类型',
  `file_url` varchar(255) NOT NULL COMMENT '访问URL',
  `base_path` varchar(64) DEFAULT NULL COMMENT '域名',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `orignal_name` varchar(255) DEFAULT NULL COMMENT '原来的文件名',
  `file_type` varchar(64) DEFAULT NULL COMMENT '文件类型',
  `file_size` double(12,2) DEFAULT NULL COMMENT '文件大小',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='附件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_auto_code_part`
--

DROP TABLE IF EXISTS `sys_auto_code_part`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_auto_code_part` (
  `part_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分段ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `part_index` int NOT NULL COMMENT '分段序号',
  `part_type` varchar(20) NOT NULL COMMENT '分段类型，INPUTCHAR：输入字符，NOWDATE：当前日期时间，FIXCHAR：固定字符，SERIALNO：流水号',
  `part_code` varchar(64) DEFAULT NULL COMMENT '分段编号',
  `part_name` varchar(255) DEFAULT NULL COMMENT '分段名称',
  `part_length` int NOT NULL COMMENT '分段长度',
  `date_format` varchar(20) DEFAULT NULL,
  `input_character` varchar(64) DEFAULT NULL COMMENT '输入字符',
  `fix_character` varchar(64) DEFAULT NULL COMMENT '固定字符',
  `seria_start_no` int DEFAULT NULL COMMENT '流水号起始值',
  `seria_step` int DEFAULT NULL COMMENT '流水号步长',
  `seria_now_no` int DEFAULT NULL COMMENT '流水号当前值',
  `cycle_flag` char(1) DEFAULT NULL COMMENT '流水号是否循环',
  `cycle_method` varchar(20) DEFAULT NULL COMMENT '循环方式，YEAR：按年，MONTH：按月，DAY：按天，HOUR：按小时，MINITE：按分钟，OTHER：按传入字符变',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`part_id`)
) ENGINE=InnoDB AUTO_INCREMENT=357 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码生成规则组成表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_auto_code_result`
--

DROP TABLE IF EXISTS `sys_auto_code_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_auto_code_result` (
  `code_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `gen_date` varchar(20) NOT NULL COMMENT '生成日期时间',
  `gen_index` int DEFAULT NULL COMMENT '最后产生的序号',
  `last_result` varchar(64) DEFAULT NULL COMMENT '最后产生的值',
  `last_serial_no` int DEFAULT NULL COMMENT '最后产生的流水号',
  `last_input_char` varchar(64) DEFAULT NULL COMMENT '最后传入的参数',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`code_id`)
) ENGINE=InnoDB AUTO_INCREMENT=657 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码生成记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_auto_code_rule`
--

DROP TABLE IF EXISTS `sys_auto_code_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_auto_code_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(255) NOT NULL COMMENT '规则名称',
  `rule_desc` varchar(500) DEFAULT NULL COMMENT '描述',
  `max_length` int DEFAULT NULL COMMENT '最大长度',
  `is_padded` char(1) NOT NULL COMMENT '是否补齐',
  `padded_char` varchar(20) DEFAULT NULL COMMENT '补齐字符',
  `padded_method` char(1) DEFAULT 'L' COMMENT '补齐方式',
  `enable_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rule_id`)
) ENGINE=InnoDB AUTO_INCREMENT=265 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码生成规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `status` char(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `dept_code` varchar(64) NOT NULL COMMENT '部门编码',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=117 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9006 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=901 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job`
--

DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job_log`
--

DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) DEFAULT NULL COMMENT '日志信息',
  `status` char(1) DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) DEFAULT '' COMMENT '异常信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB AUTO_INCREMENT=173347 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5653 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) DEFAULT NULL COMMENT '路由参数',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2412 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_message`
--

DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `message_type` varchar(64) NOT NULL COMMENT '消息类型',
  `message_level` varchar(64) NOT NULL COMMENT '消息级别',
  `message_title` varchar(64) DEFAULT NULL COMMENT '标题',
  `message_content` longblob COMMENT '内容',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID',
  `sender_name` varchar(64) DEFAULT NULL COMMENT '发送人名称',
  `sender_nick` varchar(64) DEFAULT NULL COMMENT '发送人昵称',
  `recipient_id` bigint NOT NULL COMMENT '接收人ID',
  `recipient_name` varchar(64) DEFAULT NULL COMMENT '接收人名称',
  `recipient_nick` varchar(64) DEFAULT NULL COMMENT '接收人昵称',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `call_back` varchar(255) DEFAULT NULL COMMENT '回调地址',
  `status` varchar(64) NOT NULL DEFAULT 'UNREAD' COMMENT '状态',
  `deleted_flag` char(1) NOT NULL DEFAULT 'N' COMMENT '是否删除',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`message_id`)
) ENGINE=InnoDB AUTO_INCREMENT=230 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) NOT NULL COMMENT '公告标题',
  `notice_type` char(1) NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`oper_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13773 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_todo_list`
--

DROP TABLE IF EXISTS `sys_todo_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_todo_list` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '代办ID',
  `task_name` varchar(256) NOT NULL COMMENT '代办名称',
  `task_type` int NOT NULL COMMENT '代办类型（0-流程待办，1-业务待办）',
  `sys_code` varchar(64) NOT NULL COMMENT '任务来源系统',
  `url` varchar(256) NOT NULL COMMENT 'Web端回调地址',
  `app_url` varchar(256) NOT NULL COMMENT 'APP端回调地址',
  `h5_url` varchar(256) NOT NULL COMMENT 'H5端回调地址（小程序、公众号等）',
  `priority` varchar(64) DEFAULT 'NORMAL' COMMENT '优先级',
  `business_id` bigint NOT NULL COMMENT '业务ID',
  `business_code` varchar(64) NOT NULL COMMENT '业务编码',
  `business_category` varchar(64) NOT NULL COMMENT '业务大类',
  `business_type` varchar(64) NOT NULL COMMENT '业务分类（小类）',
  `business_name` varchar(256) DEFAULT NULL COMMENT '业务名称',
  `owner_id` bigint NOT NULL COMMENT '代办所属人ID',
  `owner_name` varchar(64) NOT NULL COMMENT '代办所属人用户名',
  `owner_nick` varchar(64) NOT NULL COMMENT '代办所属人名称',
  `creator_id` bigint DEFAULT NULL COMMENT '发起人ID',
  `creator_name` varchar(64) DEFAULT NULL COMMENT '发起人用户名',
  `creator_nick` varchar(64) DEFAULT NULL COMMENT '发起人名称',
  `process_status` varchar(64) DEFAULT NULL COMMENT '流转状态（PASS-正常流转，REJECT-退回上一步，FIRST-退回发起人）',
  `node_name` varchar(128) DEFAULT NULL COMMENT '流程节点名称',
  `status` int DEFAULT '0' COMMENT '状态（0-正常，1-已完成，2-已删除）',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用代办表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=207 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_tab`
--

DROP TABLE IF EXISTS `test_tab`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_tab` (
  `user_id` int DEFAULT NULL,
  `user_name` varchar(64) DEFAULT NULL,
  `sex` varchar(5) DEFAULT NULL,
  `age` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tm_tool`
--

DROP TABLE IF EXISTS `tm_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tm_tool` (
  `tool_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工装夹具ID',
  `tool_code` varchar(64) DEFAULT NULL COMMENT '工装夹具编码',
  `tool_name` varchar(255) NOT NULL COMMENT '工装夹具名称',
  `brand` varchar(255) DEFAULT NULL COMMENT '品牌',
  `spec` varchar(255) DEFAULT NULL COMMENT '型号',
  `tool_type_id` bigint NOT NULL COMMENT '工装夹具类型ID',
  `tool_type_code` varchar(64) DEFAULT NULL COMMENT '工装夹具类型编码',
  `tool_type_name` varchar(255) DEFAULT NULL COMMENT '工装夹具类型名称',
  `code_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否单独编码管理',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `quantity_avail` int DEFAULT '1' COMMENT '可用数量',
  `mainten_type` varchar(20) DEFAULT NULL COMMENT '保养维护类型',
  `next_mainten_period` int DEFAULT NULL COMMENT '下一次保养周期',
  `next_mainten_date` datetime DEFAULT NULL COMMENT '下一次保养日期',
  `status` varchar(64) DEFAULT 'STORE' COMMENT '状态[MES_TOOL_STATUS]',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tool_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工装夹具清单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tm_tool_type`
--

DROP TABLE IF EXISTS `tm_tool_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tm_tool_type` (
  `tool_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工装夹具类型ID',
  `tool_type_code` varchar(64) NOT NULL COMMENT '类型编码',
  `tool_type_name` varchar(255) NOT NULL COMMENT '类型名称',
  `code_flag` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否编码管理',
  `mainten_type` varchar(20) DEFAULT NULL COMMENT '保养维护类型',
  `mainten_period` int DEFAULT NULL COMMENT '保养周期',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tool_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工装夹具类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ureport_file_tbl`
--

DROP TABLE IF EXISTS `ureport_file_tbl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ureport_file_tbl` (
  `id_` int NOT NULL AUTO_INCREMENT,
  `name_` varchar(100) NOT NULL,
  `content_` mediumblob,
  `create_time_` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time_` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_stock_alert`
--

DROP TABLE IF EXISTS `v_stock_alert`;
/*!50001 DROP VIEW IF EXISTS `v_stock_alert`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_stock_alert` AS SELECT 
 1 AS `item_id`,
 1 AS `item_code`,
 1 AS `item_name`,
 1 AS `specification`,
 1 AS `unit_name`,
 1 AS `min_stock`,
 1 AS `current_stock`,
 1 AS `gap`,
 1 AS `suggest_purchase`,
 1 AS `alert_level`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `wm_arrival_notice`
--

DROP TABLE IF EXISTS `wm_arrival_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_arrival_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知单ID',
  `notice_code` varchar(64) NOT NULL COMMENT '通知单编号',
  `notice_name` varchar(255) NOT NULL COMMENT '通知单名称',
  `po_code` varchar(64) DEFAULT NULL COMMENT '采购订单编号',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) DEFAULT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `arrival_date` datetime DEFAULT NULL COMMENT '到货日期',
  `contact` varchar(64) DEFAULT NULL COMMENT '联系人',
  `tel` varchar(128) DEFAULT NULL COMMENT '联系方式',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=67 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='到货通知单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_arrival_notice_line`
--

DROP TABLE IF EXISTS `wm_arrival_notice_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_arrival_notice_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `notice_id` bigint DEFAULT NULL COMMENT '通知单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `quantity_arrival` double(12,2) NOT NULL COMMENT '到货数量',
  `quantity_quanlified` double(12,2) DEFAULT NULL COMMENT '合格数量',
  `iqc_check` char(1) DEFAULT NULL COMMENT '是否来料检验',
  `iqc_id` bigint DEFAULT NULL COMMENT '来料检验单ID',
  `iqc_code` varchar(64) DEFAULT NULL COMMENT '来料检验单编号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=237 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='到货通知单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_barcode`
--

DROP TABLE IF EXISTS `wm_barcode`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_barcode` (
  `barcode_id` bigint NOT NULL AUTO_INCREMENT COMMENT '条码ID',
  `barcode_formart` varchar(64) NOT NULL COMMENT '条码格式',
  `barcode_type` varchar(64) NOT NULL COMMENT '条码类型',
  `barcode_content` varchar(255) NOT NULL COMMENT '条码内容',
  `bussiness_id` bigint NOT NULL COMMENT '业务ID',
  `bussiness_code` varchar(64) DEFAULT NULL COMMENT '业务编码',
  `bussiness_name` varchar(255) DEFAULT NULL COMMENT '业务名称',
  `barcode_url` varchar(255) DEFAULT NULL COMMENT '条码地址',
  `enable_flag` char(1) DEFAULT 'Y' COMMENT '是否生效',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`barcode_id`)
) ENGINE=InnoDB AUTO_INCREMENT=317 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='条码清单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_barcode_config`
--

DROP TABLE IF EXISTS `wm_barcode_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_barcode_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `barcode_formart` varchar(64) NOT NULL COMMENT '条码格式',
  `barcode_type` varchar(64) NOT NULL COMMENT '条码类型',
  `content_formart` varchar(255) NOT NULL COMMENT '内容格式',
  `content_example` varchar(255) DEFAULT NULL COMMENT '内容样例',
  `auto_gen_flag` char(1) DEFAULT 'Y' COMMENT '是否自动生成',
  `default_template` varchar(255) DEFAULT NULL COMMENT '默认的打印模板',
  `enable_flag` char(1) DEFAULT 'Y' COMMENT '是否生效',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=217 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='条码配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_batch`
--

DROP TABLE IF EXISTS `wm_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `batch_code` varchar(64) NOT NULL COMMENT '批次编号',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `produce_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期',
  `recpt_date` datetime DEFAULT NULL COMMENT '入库日期',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) DEFAULT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `co_code` varchar(64) DEFAULT NULL COMMENT '销售订单编号',
  `po_code` varchar(64) DEFAULT NULL COMMENT '采购订单编号',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编码',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID',
  `task_code` varchar(64) DEFAULT NULL COMMENT '生产任务编号',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编码',
  `tool_id` bigint DEFAULT NULL COMMENT '工具ID',
  `tool_code` varchar(64) DEFAULT NULL COMMENT '工具编号',
  `mold_id` bigint DEFAULT NULL COMMENT '模具ID',
  `mold_code` varchar(64) DEFAULT NULL COMMENT '模具编号',
  `lot_number` varchar(128) DEFAULT NULL COMMENT '生产批号',
  `quality_status` varchar(64) DEFAULT NULL COMMENT '质量状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`batch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=148 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批次记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_issue_detail`
--

DROP TABLE IF EXISTS `wm_issue_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_issue_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细行ID',
  `issue_id` bigint DEFAULT NULL COMMENT '领料单ID',
  `line_id` bigint NOT NULL COMMENT '行ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '领料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=152 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产领料单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_issue_header`
--

DROP TABLE IF EXISTS `wm_issue_header`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_issue_header` (
  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '领料单ID',
  `issue_code` varchar(64) NOT NULL COMMENT '领料单编号',
  `issue_name` varchar(255) NOT NULL COMMENT '领料单名称',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) DEFAULT NULL COMMENT '工作站名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编码',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID',
  `task_code` varchar(64) DEFAULT NULL COMMENT '生产任务编码',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `issue_date` datetime DEFAULT NULL COMMENT '领料日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `required_time` datetime DEFAULT NULL COMMENT '需求时间',
  PRIMARY KEY (`issue_id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产领料单头表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_issue_line`
--

DROP TABLE IF EXISTS `wm_issue_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_issue_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `issue_id` bigint DEFAULT NULL COMMENT '领料单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity_issued` double(12,2) NOT NULL COMMENT '领料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=151 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产领料单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_item_consume`
--

DROP TABLE IF EXISTS `wm_item_consume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_item_consume` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '生产工单名称',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID',
  `task_code` varchar(64) DEFAULT NULL COMMENT '生产任务编号',
  `task_name` varchar(255) DEFAULT NULL COMMENT '生产任务名称',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) DEFAULT NULL COMMENT '工作站名称',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `feedback_id` bigint DEFAULT NULL COMMENT '关联的报工单',
  `consume_date` datetime DEFAULT NULL COMMENT '消耗日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料消耗记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_item_consume_detail`
--

DROP TABLE IF EXISTS `wm_item_consume_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_item_consume_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint DEFAULT NULL COMMENT '行ID',
  `record_id` bigint DEFAULT NULL COMMENT '消耗记录ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '消耗数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=225 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料消耗记录明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_item_consume_line`
--

DROP TABLE IF EXISTS `wm_item_consume_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_item_consume_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `record_id` bigint DEFAULT NULL COMMENT '消耗记录ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_consume` double(12,2) NOT NULL COMMENT '消耗数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料消耗记录行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_item_recpt`
--

DROP TABLE IF EXISTS `wm_item_recpt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_item_recpt` (
  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `recpt_code` varchar(64) NOT NULL COMMENT '入库单编号',
  `recpt_name` varchar(255) NOT NULL COMMENT '入库单名称',
  `iqc_id` bigint DEFAULT NULL COMMENT '来料检验单ID',
  `iqc_code` varchar(64) DEFAULT NULL COMMENT '来料检验单编号',
  `notice_id` bigint DEFAULT NULL COMMENT '到货通知单ID',
  `notice_code` varchar(64) DEFAULT NULL COMMENT '到货通知单编号',
  `po_code` varchar(64) DEFAULT NULL COMMENT '采购订单编号',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) DEFAULT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `recpt_date` datetime DEFAULT NULL COMMENT '入库日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料入库单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_recpt_auto_iqc` AFTER UPDATE ON `wm_item_recpt` FOR EACH ROW BEGIN
  DECLARE v_tpl BIGINT DEFAULT NULL;
  DECLARE v_ipqc BIGINT DEFAULT NULL;
  DECLARE v_line BIGINT DEFAULT NULL;
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR SELECT line_id FROM wm_item_recpt_line WHERE recpt_id = NEW.recpt_id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  IF NEW.status = 'FINISHED' AND (OLD.status IS NULL OR OLD.status <> 'FINISHED') THEN
    SELECT template_id INTO v_tpl FROM qc_template
     WHERE FIND_IN_SET('IQC', qc_types) AND enable_flag = 'Y' ORDER BY template_id LIMIT 1;

    IF v_tpl IS NOT NULL THEN
      OPEN cur;
      read_loop: LOOP
        FETCH cur INTO v_line;
        IF done = 1 THEN LEAVE read_loop; END IF;

        IF NOT EXISTS (SELECT 1 FROM qc_iqc WHERE source_doc_type='RECPT_LINE' AND source_doc_id=v_line) THEN
          INSERT INTO qc_iqc (
            iqc_code, iqc_name, template_id,
            source_doc_id, source_doc_type, source_doc_code,
            vendor_id, vendor_code, vendor_name,
            item_id, item_code, item_name,
            unit_of_measure, unit_name, specification,
            quantity_recived, quantity_check, status, remark, create_by, create_time
          )
          SELECT
            CONCAT('IQC-A-', l.line_id), CONCAT(l.item_name, ' 入库自动复检'), v_tpl,
            l.line_id, 'RECPT_LINE', NEW.recpt_code,
            IFNULL(NEW.vendor_id, 0), IFNULL(NEW.vendor_code, 'V-UNKNOWN'), IFNULL(NEW.vendor_name, '未知供应商'),
            l.item_id, l.item_code, l.item_name,
            l.unit_of_measure, l.unit_name, l.specification,
            IFNULL(l.quantity_recived, 0), IFNULL(l.quantity_recived, 0),
            'PREPARE', '采购入库完成自动触发来料复检', 'system', NOW()
          FROM wm_item_recpt_line l WHERE l.line_id = v_line;

          SET v_ipqc = LAST_INSERT_ID();

          INSERT INTO qc_iqc_line (
            iqc_id, index_id, index_code, index_name, index_type,
            qc_tool, check_method, stander_val, unit_of_measure,
            threshold_max, threshold_min, cr_quantity, maj_quantity, min_quantity,
            remark, create_by, create_time
          )
          SELECT v_ipqc, ti.index_id, ti.index_code, ti.index_name, ti.index_type,
                 ti.qc_tool, ti.check_method, ti.stander_val, ti.unit_of_measure,
                 ti.threshold_max, ti.threshold_min, 0, 0, 0, '', 'system', NOW()
          FROM qc_template_index ti WHERE ti.template_id = v_tpl;
        END IF;
      END LOOP;
      CLOSE cur;
    END IF;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `wm_item_recpt_detail`
--

DROP TABLE IF EXISTS `wm_item_recpt_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_item_recpt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint DEFAULT NULL COMMENT '行ID',
  `recpt_id` bigint DEFAULT NULL COMMENT '入库单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=242 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料入库单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_item_recpt_line`
--

DROP TABLE IF EXISTS `wm_item_recpt_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_item_recpt_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `recpt_id` bigint DEFAULT NULL COMMENT '入库单ID',
  `notice_line_id` bigint DEFAULT NULL COMMENT '到货通知单行ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity_recived` double(12,2) NOT NULL COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `produce_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期',
  `lot_number` varchar(128) DEFAULT NULL COMMENT '生产批号',
  `iqc_check` char(1) DEFAULT NULL COMMENT '是否来料检验',
  `iqc_id` bigint DEFAULT NULL COMMENT '来料检验单ID',
  `iqc_code` varchar(64) DEFAULT NULL COMMENT '来料检验单编号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=233 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料入库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_material_stock`
--

DROP TABLE IF EXISTS `wm_material_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_material_stock` (
  `material_stock_id` bigint NOT NULL AUTO_INCREMENT COMMENT '事务ID',
  `item_type_id` bigint DEFAULT NULL COMMENT '物料类型ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '入库批次号',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编号',
  `vendor_name` varchar(255) DEFAULT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(64) DEFAULT NULL COMMENT '供应商简称',
  `quantity_onhand` double(12,2) DEFAULT NULL COMMENT '在库数量',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编号',
  `recpt_date` datetime DEFAULT NULL COMMENT '入库时间',
  `expire_date` datetime DEFAULT NULL COMMENT '库存有效期',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `frozen_flag` char(1) NOT NULL DEFAULT 'N',
  `production_date` datetime DEFAULT NULL,
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  PRIMARY KEY (`material_stock_id`)
) ENGINE=InnoDB AUTO_INCREMENT=246 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_materialrequest_notice`
--

DROP TABLE IF EXISTS `wm_materialrequest_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_materialrequest_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID',
  `workstaiton_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编号',
  `user_id` bigint DEFAULT NULL COMMENT '需求人',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `nick_name` varchar(128) DEFAULT NULL COMMENT '昵称',
  `request_time` datetime DEFAULT NULL COMMENT '需求时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始备料时间',
  `end_time` datetime DEFAULT NULL COMMENT '完成时间',
  `status` varchar(64) DEFAULT NULL COMMENT '状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备料通知单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_materialrequest_notice_line`
--

DROP TABLE IF EXISTS `wm_materialrequest_notice_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_materialrequest_notice_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `notice_id` bigint NOT NULL COMMENT '通知ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '需求数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备料通知单明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_misc_issue`
--

DROP TABLE IF EXISTS `wm_misc_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_misc_issue` (
  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '杂项出库单ID',
  `issue_code` varchar(64) NOT NULL COMMENT '杂项出库单编号',
  `issue_name` varchar(255) NOT NULL COMMENT '杂项出库单名称',
  `misc_type` varchar(64) DEFAULT NULL COMMENT '杂项事务类型',
  `source_doc_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '来源单据编号',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '来源单据类型',
  `issue_date` datetime DEFAULT NULL COMMENT '出库日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`issue_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='杂项出库单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_misc_issue_detail`
--

DROP TABLE IF EXISTS `wm_misc_issue_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_misc_issue_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint DEFAULT NULL COMMENT '行ID',
  `issue_id` bigint DEFAULT NULL COMMENT '杂项出库单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='杂项出库单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_misc_issue_line`
--

DROP TABLE IF EXISTS `wm_misc_issue_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_misc_issue_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `issue_id` bigint DEFAULT NULL COMMENT '杂项出库单ID',
  `source_doc_line_id` bigint DEFAULT NULL COMMENT '来源单据行ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity_issued` double(12,2) NOT NULL COMMENT '出库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='杂项出库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_misc_recpt`
--

DROP TABLE IF EXISTS `wm_misc_recpt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_misc_recpt` (
  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '杂项入库单ID',
  `recpt_code` varchar(64) NOT NULL COMMENT '杂项入库单编号',
  `recpt_name` varchar(255) NOT NULL COMMENT '杂项入库单名称',
  `misc_type` varchar(64) DEFAULT NULL COMMENT '杂项事务类型',
  `source_doc_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '来源单据编号',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '来源单据类型',
  `recpt_date` datetime DEFAULT NULL COMMENT '入库日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`)
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='杂项入库单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_misc_recpt_detail`
--

DROP TABLE IF EXISTS `wm_misc_recpt_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_misc_recpt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint DEFAULT NULL COMMENT '行ID',
  `recpt_id` bigint DEFAULT NULL COMMENT '杂项入库单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='杂项入库单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_misc_recpt_line`
--

DROP TABLE IF EXISTS `wm_misc_recpt_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_misc_recpt_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `recpt_id` bigint DEFAULT NULL COMMENT '杂项入库单ID',
  `source_doc_line_id` bigint DEFAULT NULL COMMENT '来源单据行ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity_recived` double(12,2) NOT NULL COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='杂项入库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_outsource_issue`
--

DROP TABLE IF EXISTS `wm_outsource_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_outsource_issue` (
  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '领料单ID',
  `issue_code` varchar(64) NOT NULL COMMENT '领料单编号',
  `issue_name` varchar(255) NOT NULL COMMENT '领料单名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编码',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) DEFAULT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `issue_date` datetime DEFAULT NULL COMMENT '领料日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`issue_id`)
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外协领料单头表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_outsource_issue_detail`
--

DROP TABLE IF EXISTS `wm_outsource_issue_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_outsource_issue_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint NOT NULL COMMENT '行ID',
  `issue_id` bigint DEFAULT NULL COMMENT '领料单ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '退货数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外协领料单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_outsource_issue_line`
--

DROP TABLE IF EXISTS `wm_outsource_issue_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_outsource_issue_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `issue_id` bigint DEFAULT NULL COMMENT '领料单ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_issued` double(12,2) NOT NULL COMMENT '领料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外协领料单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_outsource_recpt`
--

DROP TABLE IF EXISTS `wm_outsource_recpt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_outsource_recpt` (
  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `recpt_code` varchar(64) NOT NULL COMMENT '入库单编号',
  `recpt_name` varchar(255) NOT NULL COMMENT '入库单名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '外协工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '外协工单编号',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) DEFAULT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `recpt_date` datetime DEFAULT NULL COMMENT '入库日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外协入库单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_outsource_recpt_detail`
--

DROP TABLE IF EXISTS `wm_outsource_recpt_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_outsource_recpt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint NOT NULL COMMENT '行ID',
  `recpt_id` bigint DEFAULT NULL COMMENT '领料单ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外协入库单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_outsource_recpt_line`
--

DROP TABLE IF EXISTS `wm_outsource_recpt_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_outsource_recpt_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `recpt_id` bigint DEFAULT NULL COMMENT '入库单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_recived` double(12,2) NOT NULL COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `produce_date` datetime DEFAULT NULL COMMENT '生产日期',
  `lot_number` varchar(128) DEFAULT NULL COMMENT '生产批号',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期',
  `quality_status` varchar(64) DEFAULT 'NT' COMMENT '质量状态',
  `iqc_check` char(1) DEFAULT NULL COMMENT '是否检验',
  `iqc_id` bigint DEFAULT NULL COMMENT '来料检验单ID',
  `iqc_code` varchar(64) DEFAULT NULL COMMENT '来料检验单编号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外协入库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_package`
--

DROP TABLE IF EXISTS `wm_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_package` (
  `package_id` bigint NOT NULL AUTO_INCREMENT COMMENT '装箱单ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父箱ID',
  `ancestors` varchar(255) NOT NULL DEFAULT '0' COMMENT '所有父节点ID',
  `package_code` varchar(64) DEFAULT NULL COMMENT '装箱单编号',
  `barcode_id` bigint DEFAULT NULL COMMENT '条码ID',
  `barcode_content` varchar(255) DEFAULT NULL COMMENT '条码内容',
  `barcode_url` varchar(255) DEFAULT NULL COMMENT '条码地址',
  `package_date` datetime NOT NULL COMMENT '装箱日期',
  `so_code` varchar(64) DEFAULT NULL COMMENT '销售订单编号',
  `invoice_code` varchar(255) DEFAULT NULL COMMENT '发票编号',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `package_length` double(12,4) DEFAULT NULL COMMENT '箱长度',
  `package_width` double(12,4) DEFAULT NULL COMMENT '箱宽度',
  `package_height` double(12,4) DEFAULT NULL COMMENT '箱高度',
  `size_unit` varchar(64) DEFAULT NULL COMMENT '尺寸单位',
  `net_weight` double(12,4) DEFAULT NULL COMMENT '净重',
  `cross_weight` double(12,4) DEFAULT NULL COMMENT '毛重',
  `weight_unit` varchar(64) DEFAULT NULL COMMENT '重量单位',
  `inspector` varchar(64) DEFAULT NULL COMMENT '检查员用户名',
  `inspector_name` varchar(64) DEFAULT NULL COMMENT '检查员名称',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '状态',
  `enable_flag` char(1) DEFAULT 'Y' COMMENT '是否生效',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`package_id`)
) ENGINE=InnoDB AUTO_INCREMENT=207 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='装箱单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_package_line`
--

DROP TABLE IF EXISTS `wm_package_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_package_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细行ID',
  `package_id` bigint NOT NULL COMMENT '装箱单ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_package` double(12,2) NOT NULL COMMENT '装箱数量',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编号',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='装箱明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_pback_header`
--

DROP TABLE IF EXISTS `wm_pback_header`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_pback_header` (
  `pback_id` bigint NOT NULL AUTO_INCREMENT COMMENT '生产退料单ID',
  `pback_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '生产退料单编号',
  `pback_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '生产退料单名称',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '工作站名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '生产工单编码',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID',
  `task_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '生产任务编码',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '库位名称',
  `pback_date` datetime DEFAULT NULL COMMENT '退料日期',
  `status` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`pback_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='生产退料单头表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_pback_line`
--

DROP TABLE IF EXISTS `wm_pback_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_pback_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `pback_id` bigint DEFAULT NULL COMMENT '生产退料单ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '单位',
  `quantity_pback` double(12,2) NOT NULL COMMENT '退料数量',
  `batch_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '退料批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='生产退料单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_produce`
--

DROP TABLE IF EXISTS `wm_product_produce`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_produce` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '生产工单名称',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID',
  `task_code` varchar(64) DEFAULT NULL COMMENT '生产任务编号',
  `task_name` varchar(255) DEFAULT NULL COMMENT '生产任务名称',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) DEFAULT NULL COMMENT '工作站名称',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编号',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称',
  `produce_date` datetime DEFAULT NULL COMMENT '生产日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `feedback_id` bigint DEFAULT NULL COMMENT '报工单ID',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品产出记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_produce_detail`
--

DROP TABLE IF EXISTS `wm_product_produce_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_produce_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `line_id` bigint DEFAULT NULL COMMENT '明细ID',
  `record_id` bigint DEFAULT NULL COMMENT '产出记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '产出数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品产出记录明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_produce_line`
--

DROP TABLE IF EXISTS `wm_product_produce_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_produce_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `record_id` bigint DEFAULT NULL COMMENT '产出记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_produce` double(12,2) NOT NULL COMMENT '产出数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `quality_status` varchar(64) DEFAULT NULL COMMENT '质量状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `expire_date` datetime DEFAULT NULL COMMENT '过期日期',
  `lot_number` varchar(128) DEFAULT NULL COMMENT '生产批号',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品产出记录表行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_feedback_auto_ipqc` AFTER INSERT ON `wm_product_produce_line` FOR EACH ROW BEGIN
  DECLARE v_tpl BIGINT DEFAULT NULL;
  DECLARE v_ipqc BIGINT DEFAULT NULL;
  DECLARE v_feedback BIGINT DEFAULT NULL;
  DECLARE v_check CHAR(1) DEFAULT 'N';

  SELECT MAX(feedback_id) INTO v_feedback FROM wm_product_produce WHERE record_id = NEW.record_id;
  SELECT IFNULL(MAX(r.is_check), 'N') INTO v_check
    FROM pro_feedback f JOIN pro_route_process r ON r.route_id=f.route_id AND r.process_id=f.process_id
    WHERE f.record_id=v_feedback;

  IF NEW.quality_status = 'NT' AND v_check = 'Y' THEN
    SELECT MIN(t.template_id) INTO v_tpl
      FROM qc_template t JOIN qc_template_product p ON p.template_id=t.template_id
      WHERE p.item_id=NEW.item_id AND FIND_IN_SET('IPQC', t.qc_types) AND t.enable_flag='Y';
    IF v_tpl IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IPQC template missing for product';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM qc_template_index WHERE template_id=v_tpl) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IPQC template has no inspection items';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM qc_ipqc WHERE source_doc_type='FEEDBACK'
                   AND source_doc_id=NEW.record_id AND source_line_id=NEW.line_id) THEN
      INSERT INTO qc_ipqc (
        ipqc_code, ipqc_name, ipqc_type, template_id,
        source_doc_id, source_doc_type, source_doc_code, source_line_id,
        workorder_id, workorder_code, workorder_name, task_id, task_code,
        workstation_id, workstation_code, workstation_name, process_id, process_code, process_name,
        item_id, item_code, item_name, unit_of_measure, unit_name, specification,
        quantity_check, quantity_qualified, quantity_unqualified, status, remark, create_by, create_time
      )
      SELECT CONCAT('IPQC-A-',NEW.line_id), CONCAT(f.process_name,' 自动待检'), 'IPQC',v_tpl,
        NEW.record_id,'FEEDBACK',f.feedback_code,NEW.line_id,
        f.workorder_id,f.workorder_code,f.workorder_name,f.task_id,f.task_code,
        f.workstation_id,f.workstation_code,f.workstation_name,f.process_id,f.process_code,f.process_name,
        NEW.item_id,NEW.item_code,NEW.item_name,NEW.unit_of_measure,NEW.unit_name,NEW.specification,
        NEW.quantity_produce,0,0,'PREPARE','报工执行后自动生成待检任务','system',NOW()
      FROM pro_feedback f WHERE f.record_id=v_feedback;
      SET v_ipqc=LAST_INSERT_ID();
      INSERT INTO qc_ipqc_line (
        ipqc_id,index_id,index_code,index_name,index_type,qc_tool,check_method,stander_val,unit_of_measure,
        threshold_max,threshold_min,cr_quantity,maj_quantity,min_quantity,remark,attr3,attr4,create_by,create_time
      )
      SELECT v_ipqc,index_id,index_code,index_name,index_type,qc_tool,check_method,stander_val,unit_of_measure,
        threshold_max,threshold_min,0,0,0,'',0,0,'system',NOW()
      FROM qc_template_index WHERE template_id=v_tpl;
    END IF;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `wm_product_recpt`
--

DROP TABLE IF EXISTS `wm_product_recpt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_recpt` (
  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `recpt_code` varchar(64) NOT NULL COMMENT '入库单编号',
  `recpt_name` varchar(255) DEFAULT NULL COMMENT '入库单名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '生产工单名称',
  `item_id` bigint DEFAULT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `recpt_date` datetime DEFAULT NULL COMMENT '入库日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`)
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品入库录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_recpt_detail`
--

DROP TABLE IF EXISTS `wm_product_recpt_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_recpt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint NOT NULL COMMENT '行ID',
  `recpt_id` bigint DEFAULT NULL COMMENT '入库记录ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品入库记录明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_recpt_line`
--

DROP TABLE IF EXISTS `wm_product_recpt_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_recpt_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `recpt_id` bigint DEFAULT NULL COMMENT '入库记录ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_recived` double(12,2) NOT NULL COMMENT '入库数量',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) DEFAULT NULL COMMENT '生产工单名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品入库记录表行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_sales`
--

DROP TABLE IF EXISTS `wm_product_sales`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_sales` (
  `sales_id` bigint NOT NULL AUTO_INCREMENT COMMENT '出库单ID',
  `sales_code` varchar(64) NOT NULL COMMENT '出库单编号',
  `sales_name` varchar(255) NOT NULL COMMENT '出库单名称',
  `notice_id` bigint DEFAULT NULL COMMENT '通知单ID',
  `notice_code` varchar(64) DEFAULT NULL COMMENT '通知单编号',
  `so_code` varchar(64) DEFAULT NULL COMMENT '销售订单编号',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `recipient` varchar(128) DEFAULT NULL COMMENT '收货人',
  `tel` varchar(128) DEFAULT NULL COMMENT '联系方式',
  `address` varchar(256) DEFAULT NULL COMMENT '收货地址',
  `carrier` varchar(128) DEFAULT NULL COMMENT '承运商',
  `shipping_number` varchar(128) DEFAULT NULL COMMENT '运输单号',
  `sales_date` datetime DEFAULT NULL COMMENT '出库日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sales_id`)
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售出库单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_sales_detail`
--

DROP TABLE IF EXISTS `wm_product_sales_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_sales_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint NOT NULL COMMENT '行ID',
  `sales_id` bigint DEFAULT NULL COMMENT '销售出库单ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '出库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品销售出库记录明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_sales_line`
--

DROP TABLE IF EXISTS `wm_product_sales_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_sales_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `sales_id` bigint DEFAULT NULL COMMENT '出库记录ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_sales` double(12,2) NOT NULL COMMENT '出库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `oqc_check` char(1) DEFAULT NULL COMMENT '是否检验',
  `oqc_id` bigint DEFAULT NULL COMMENT '出货检验单ID',
  `oqc_code` varchar(64) DEFAULT NULL COMMENT '出货检验单编号',
  `quality_status` varchar(64) DEFAULT NULL COMMENT '质量状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品销售出库行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_salse`
--

DROP TABLE IF EXISTS `wm_product_salse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_salse` (
  `salse_id` bigint NOT NULL AUTO_INCREMENT COMMENT '出库单ID',
  `salse_code` varchar(64) NOT NULL COMMENT '出库单编号',
  `salse_name` varchar(255) NOT NULL COMMENT '出库单名称',
  `oqc_id` bigint DEFAULT NULL COMMENT '出货检验单ID',
  `oqc_code` varchar(64) DEFAULT NULL COMMENT '出货检验单编号',
  `so_code` varchar(64) DEFAULT NULL COMMENT '销售订单编号',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `salse_date` datetime DEFAULT NULL COMMENT '出库日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`salse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售出库单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_product_salse_line`
--

DROP TABLE IF EXISTS `wm_product_salse_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_product_salse_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `salse_id` bigint DEFAULT NULL COMMENT '出库记录ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_salse` double(12,2) NOT NULL COMMENT '出库数量',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `oqc_check` char(1) DEFAULT 'N',
  `oqc_id` bigint DEFAULT NULL,
  `oqc_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品销售出库行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_issue`
--

DROP TABLE IF EXISTS `wm_rt_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_issue` (
  `rt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '退料单ID',
  `rt_code` varchar(64) NOT NULL COMMENT '退料单编号',
  `rt_name` varchar(255) DEFAULT NULL COMMENT '退料单名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) DEFAULT NULL COMMENT '生产工单编码',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID',
  `workstation_code` varchar(64) DEFAULT NULL COMMENT '工作站编号',
  `workstation_name` varchar(255) DEFAULT NULL COMMENT '工作站名称',
  `rt_type` varchar(64) DEFAULT NULL COMMENT '退料类型',
  `rt_date` datetime DEFAULT NULL COMMENT '退料日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rt_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产退料单头表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_issue_detail`
--

DROP TABLE IF EXISTS `wm_rt_issue_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_issue_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细行ID',
  `rt_id` bigint DEFAULT NULL COMMENT '退料单ID',
  `line_id` bigint NOT NULL COMMENT '行ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '领料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产退料单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_issue_line`
--

DROP TABLE IF EXISTS `wm_rt_issue_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_issue_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `rt_id` bigint DEFAULT NULL COMMENT '退料单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity_rt` double(12,2) NOT NULL COMMENT '退料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `ipqc_id` bigint DEFAULT NULL COMMENT '对应的质检单ID',
  `ipqc_code` varchar(64) DEFAULT NULL COMMENT '对应的质检单编号',
  `qc_flag` char(1) DEFAULT 'N' COMMENT '是否检验',
  `quality_status` varchar(64) DEFAULT NULL COMMENT '质量状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产退料单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_sales`
--

DROP TABLE IF EXISTS `wm_rt_sales`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_sales` (
  `rt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '退货单ID',
  `rt_code` varchar(64) NOT NULL COMMENT '退货单编号',
  `rt_name` varchar(255) NOT NULL COMMENT '退货单名称',
  `so_code` varchar(64) DEFAULT NULL COMMENT '销售订单编号',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `rt_date` datetime DEFAULT NULL COMMENT '退货日期',
  `rt_reason` varchar(255) DEFAULT NULL COMMENT '退货原因',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rt_id`)
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品销售退货单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_sales_detail`
--

DROP TABLE IF EXISTS `wm_rt_sales_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_sales_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint NOT NULL COMMENT '行ID',
  `rt_id` bigint DEFAULT NULL COMMENT '销售退货单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '退货数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售退货记录明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_sales_line`
--

DROP TABLE IF EXISTS `wm_rt_sales_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_sales_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `rt_id` bigint DEFAULT NULL COMMENT '退货单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `quantity_rted` double(12,2) NOT NULL COMMENT '退货数量',
  `quality_status` varchar(64) DEFAULT NULL COMMENT '质量状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品销售退货行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_vendor`
--

DROP TABLE IF EXISTS `wm_rt_vendor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_vendor` (
  `rt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '退货单ID',
  `rt_code` varchar(64) NOT NULL COMMENT '退货单编号',
  `rt_name` varchar(255) NOT NULL COMMENT '退货单名称',
  `po_code` varchar(64) DEFAULT NULL COMMENT '采购订单编号',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) DEFAULT NULL COMMENT '供应商名称',
  `vendor_nick` varchar(255) DEFAULT NULL COMMENT '供应商简称',
  `rt_reason` varchar(255) DEFAULT NULL COMMENT '退货原因',
  `transport_code` varchar(128) DEFAULT NULL COMMENT '运输单号',
  `transport_tel` varchar(128) DEFAULT NULL COMMENT '联系方式',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `rt_date` datetime DEFAULT NULL COMMENT '退货日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rt_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购退货表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_vendor_detail`
--

DROP TABLE IF EXISTS `wm_rt_vendor_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_vendor_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint DEFAULT NULL COMMENT '行ID',
  `rt_id` bigint DEFAULT NULL COMMENT '退货单ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '退货数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购退货单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_rt_vendor_line`
--

DROP TABLE IF EXISTS `wm_rt_vendor_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_rt_vendor_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `rt_id` bigint DEFAULT NULL COMMENT '退货单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `quantity_rted` double(12,2) NOT NULL COMMENT '退货数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供应商退货行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_sales_notice`
--

DROP TABLE IF EXISTS `wm_sales_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_sales_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '出库单ID',
  `notice_code` varchar(64) NOT NULL COMMENT '出库单编号',
  `notice_name` varchar(255) NOT NULL COMMENT '出库单名称',
  `so_code` varchar(64) DEFAULT NULL COMMENT '销售订单编号',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) DEFAULT NULL COMMENT '客户简称',
  `sales_date` datetime DEFAULT NULL COMMENT '发货日期',
  `recipient` varchar(64) DEFAULT NULL COMMENT '收货人',
  `tel` varchar(64) DEFAULT NULL COMMENT '联系方式',
  `address` varchar(255) DEFAULT NULL COMMENT '收货地址',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发货通知单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_sales_notice_line`
--

DROP TABLE IF EXISTS `wm_sales_notice_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_sales_notice_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `notice_id` bigint DEFAULT NULL COMMENT '通知单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `quantity_sales` double(12,2) NOT NULL COMMENT '发货数量',
  `oqc_check` char(1) DEFAULT NULL COMMENT '是否检验',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发货通知单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_sn`
--

DROP TABLE IF EXISTS `wm_sn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_sn` (
  `sn_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SN码ID',
  `sn_code` varchar(64) NOT NULL COMMENT 'SN码',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `gen_date` datetime DEFAULT NULL COMMENT '生成时间',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sn_id`)
) ENGINE=InnoDB AUTO_INCREMENT=210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SN码表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_stock_taking`
--

DROP TABLE IF EXISTS `wm_stock_taking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_stock_taking` (
  `taking_id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点单ID',
  `taking_code` varchar(64) NOT NULL COMMENT '盘点单编号',
  `taking_name` varchar(255) DEFAULT NULL COMMENT '盘点单名称',
  `taking_date` datetime NOT NULL COMMENT '盘点日期',
  `taking_type` varchar(64) NOT NULL COMMENT '盘点类型',
  `user_id` bigint DEFAULT NULL COMMENT '盘点人ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '盘点人用户名',
  `nick_name` varchar(64) DEFAULT NULL COMMENT '盘点人',
  `blind_flag` char(1) DEFAULT 'N' COMMENT '是否盲盘',
  `frozen_flag` char(1) DEFAULT 'Y' COMMENT '是否库存冻结',
  `plan_id` bigint DEFAULT NULL COMMENT '方案ID',
  `plan_code` varchar(64) DEFAULT NULL COMMENT '方案编号',
  `plan_name` varchar(128) DEFAULT NULL COMMENT '方案名称',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`taking_id`)
) ENGINE=InnoDB AUTO_INCREMENT=213 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_stock_taking_line`
--

DROP TABLE IF EXISTS `wm_stock_taking_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_stock_taking_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `taking_id` bigint DEFAULT NULL COMMENT '报废单ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(128) DEFAULT NULL COMMENT '批次编号',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `taking_quantity` int DEFAULT NULL COMMENT '盘点数量',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `taking_status` varchar(64) NOT NULL DEFAULT 'LOSS' COMMENT '盘点状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=256 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_stock_taking_param`
--

DROP TABLE IF EXISTS `wm_stock_taking_param`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_stock_taking_param` (
  `param_id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点单ID',
  `plan_id` bigint NOT NULL COMMENT '盘点单编号',
  `param_type` varchar(64) DEFAULT NULL COMMENT '条件类型',
  `param_value_id` bigint DEFAULT NULL COMMENT '条件值ID',
  `param_value_code` varchar(64) DEFAULT NULL COMMENT '条件值编码',
  `param_value_name` varchar(128) DEFAULT NULL COMMENT '条件值名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`param_id`)
) ENGINE=InnoDB AUTO_INCREMENT=203 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点方案参数表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_stock_taking_plan`
--

DROP TABLE IF EXISTS `wm_stock_taking_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_stock_taking_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点单ID',
  `plan_code` varchar(64) NOT NULL COMMENT '盘点单编号',
  `plan_name` varchar(255) DEFAULT NULL COMMENT '盘点单名称',
  `taking_type` varchar(64) NOT NULL COMMENT '盘点类型',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `blind_flag` char(1) DEFAULT 'N' COMMENT '是否盲盘',
  `frozen_flag` char(1) DEFAULT 'Y' COMMENT '是否库存冻结',
  `enable_flag` char(1) DEFAULT 'Y' COMMENT '是否启用',
  `data_sql` text COMMENT '数据过滤SQL',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点方案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_stock_taking_result`
--

DROP TABLE IF EXISTS `wm_stock_taking_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_stock_taking_result` (
  `result_id` bigint NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `taking_id` bigint DEFAULT NULL COMMENT '盘点单ID',
  `line_id` bigint DEFAULT NULL COMMENT '行ID',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(128) DEFAULT NULL COMMENT '批次编号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`result_id`)
) ENGINE=InnoDB AUTO_INCREMENT=203 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点结果表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_storage_area`
--

DROP TABLE IF EXISTS `wm_storage_area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_storage_area` (
  `area_id` bigint NOT NULL AUTO_INCREMENT COMMENT '库位ID',
  `area_code` varchar(64) NOT NULL COMMENT '库位编码',
  `area_name` varchar(255) NOT NULL COMMENT '库位名称',
  `location_id` bigint NOT NULL COMMENT '库区ID',
  `area` double(8,2) DEFAULT NULL COMMENT '面积',
  `max_loa` double(8,2) DEFAULT NULL COMMENT '最大载重量',
  `position_x` int DEFAULT NULL COMMENT '库位位置X',
  `position_y` int DEFAULT NULL COMMENT '库位位置y',
  `position_z` int DEFAULT NULL COMMENT '库位位置z',
  `enable_flag` char(1) DEFAULT NULL COMMENT '是否启用',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `frozen_flag` char(1) NOT NULL DEFAULT 'N',
  `product_mixing` char(1) DEFAULT 'Y' COMMENT '是否允许产品混放',
  `batch_mixing` char(1) DEFAULT 'Y' COMMENT '是否允许批次混放',
  PRIMARY KEY (`area_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9730 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_storage_location`
--

DROP TABLE IF EXISTS `wm_storage_location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_storage_location` (
  `location_id` bigint NOT NULL AUTO_INCREMENT COMMENT '库区ID',
  `location_code` varchar(64) NOT NULL COMMENT '库区编码',
  `location_name` varchar(255) NOT NULL COMMENT '库区名称',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `area` double(12,2) DEFAULT NULL COMMENT '面积',
  `area_flag` char(1) DEFAULT 'N' COMMENT '是否开启库位管理',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `frozen_flag` char(1) NOT NULL DEFAULT 'N',
  PRIMARY KEY (`location_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9720 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库区表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_transaction`
--

DROP TABLE IF EXISTS `wm_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_transaction` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT COMMENT '事务ID',
  `transaction_type` varchar(64) NOT NULL COMMENT '事务类型',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(128) DEFAULT NULL COMMENT '单位名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `pakcage_id` bigint DEFAULT NULL COMMENT '容器ID',
  `package_code` varchar(64) DEFAULT NULL COMMENT '容器编号',
  `source_doc_type` varchar(64) DEFAULT NULL COMMENT '单据类型',
  `source_doc_id` bigint DEFAULT NULL COMMENT '单据ID',
  `source_doc_code` varchar(64) DEFAULT NULL COMMENT '单据编号',
  `source_doc_line_id` bigint DEFAULT NULL COMMENT '单据行ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID',
  `transaction_flag` int DEFAULT '1' COMMENT '库存方向',
  `transaction_quantity` double(12,2) DEFAULT NULL COMMENT '事务数量',
  `transaction_date` datetime DEFAULT NULL COMMENT '事务日期',
  `related_transaction_id` bigint DEFAULT NULL COMMENT '关联的事务ID',
  `erp_date` datetime DEFAULT NULL COMMENT 'ERP账期',
  `recpt_date` datetime DEFAULT NULL COMMENT '接收日期',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`transaction_id`)
) ENGINE=InnoDB AUTO_INCREMENT=892 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存事务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_transfer`
--

DROP TABLE IF EXISTS `wm_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_transfer` (
  `transfer_id` bigint NOT NULL AUTO_INCREMENT COMMENT '转移单ID',
  `transfer_code` varchar(64) NOT NULL COMMENT '转移单编号',
  `transfer_name` varchar(255) DEFAULT NULL COMMENT '转移单名称',
  `transfer_type` varchar(64) NOT NULL COMMENT '转移单类型',
  `delivery_flag` char(1) DEFAULT 'N' COMMENT '是否配送',
  `recipient` varchar(64) DEFAULT NULL COMMENT '收货人',
  `tel` varchar(128) DEFAULT NULL COMMENT '联系方式',
  `destination` varchar(255) DEFAULT NULL COMMENT '目的地',
  `carrier` varchar(64) DEFAULT NULL COMMENT '承运商',
  `shipping_number` varchar(128) DEFAULT NULL COMMENT '运输单号',
  `transfer_date` datetime DEFAULT NULL COMMENT '转移日期',
  `status` varchar(64) DEFAULT 'PREPARE' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `confirm_flag` char(1) DEFAULT 'N' COMMENT '是否已确认',
  PRIMARY KEY (`transfer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=208 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='转移单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_transfer_detail`
--

DROP TABLE IF EXISTS `wm_transfer_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_transfer_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `line_id` bigint NOT NULL COMMENT '行ID',
  `transfer_id` bigint DEFAULT NULL COMMENT '转移单ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity` double(12,2) NOT NULL COMMENT '转移数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB AUTO_INCREMENT=207 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='转移调拨单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_transfer_line`
--

DROP TABLE IF EXISTS `wm_transfer_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_transfer_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细行ID',
  `transfer_id` bigint NOT NULL COMMENT '装箱单ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) DEFAULT NULL COMMENT '单位',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `quantity_transfer` double(12,2) NOT NULL COMMENT '转移数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(255) DEFAULT NULL COMMENT '批次号',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `location_code` varchar(64) DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `area_code` varchar(64) DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) DEFAULT NULL COMMENT '库位名称',
  `quality_status` varchar(64) DEFAULT NULL COMMENT '质量状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`)
) ENGINE=InnoDB AUTO_INCREMENT=209 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='转移单行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wm_warehouse`
--

DROP TABLE IF EXISTS `wm_warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wm_warehouse` (
  `warehouse_id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
  `warehouse_code` varchar(64) NOT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) NOT NULL COMMENT '仓库名称',
  `warehouse_type` varchar(32) DEFAULT NULL COMMENT '浠撳簱绫诲瀷(RAW/LINE/FG)',
  `location` varchar(500) DEFAULT NULL COMMENT '位置',
  `area` double(12,2) DEFAULT NULL COMMENT '面积',
  `charge` varchar(64) DEFAULT NULL COMMENT '负责人',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `attr1` varchar(64) DEFAULT NULL COMMENT '预留字段1',
  `attr2` varchar(255) DEFAULT NULL COMMENT '预留字段2',
  `attr3` int DEFAULT '0' COMMENT '预留字段3',
  `attr4` int DEFAULT '0' COMMENT '预留字段4',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `frozen_flag` char(1) NOT NULL DEFAULT 'N',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  PRIMARY KEY (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9706 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'j2eedb'
--

--
-- Final view structure for view `v_stock_alert`
--

/*!50001 DROP VIEW IF EXISTS `v_stock_alert`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_stock_alert` AS select `i`.`item_id` AS `item_id`,`i`.`item_code` AS `item_code`,`i`.`item_name` AS `item_name`,`i`.`specification` AS `specification`,`i`.`unit_name` AS `unit_name`,`i`.`min_stock` AS `min_stock`,ifnull(sum(`s`.`quantity_onhand`),0) AS `current_stock`,(ifnull(sum(`s`.`quantity_onhand`),0) - `i`.`min_stock`) AS `gap`,greatest(((`i`.`min_stock` * 2) - ifnull(sum(`s`.`quantity_onhand`),0)),0) AS `suggest_purchase`,(case when (ifnull(sum(`s`.`quantity_onhand`),0) <= 0) then 'CRITICAL' when (ifnull(sum(`s`.`quantity_onhand`),0) < `i`.`min_stock`) then 'WARNING' else 'OK' end) AS `alert_level` from (`md_item` `i` left join `wm_material_stock` `s` on((`s`.`item_id` = `i`.`item_id`))) where (`i`.`min_stock` > 0) group by `i`.`item_id`,`i`.`item_code`,`i`.`item_name`,`i`.`specification`,`i`.`unit_name`,`i`.`min_stock` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-26 10:31:52
