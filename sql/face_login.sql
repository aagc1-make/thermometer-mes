-- 人脸登录（1:1 特征比对）
-- 只保存 128 维特征向量，不保存人脸照片；解绑即删除整行。
CREATE TABLE IF NOT EXISTS `sys_user_face` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `descriptor` text NOT NULL COMMENT '128维人脸特征（逗号分隔，不含原图）',
  `algorithm` varchar(64) NOT NULL DEFAULT 'face-api-0.22.2' COMMENT '特征算法版本',
  `samples` int(11) NOT NULL DEFAULT 0 COMMENT '录入样本帧数',
  `quality` decimal(5,4) DEFAULT NULL COMMENT '录入质量分0-1',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
  `create_by` varchar(64) DEFAULT '' COMMENT '录入人',
  `create_time` datetime DEFAULT NULL COMMENT '录入时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注（用途与授权说明）',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户人脸登录模板（仅特征向量）';
