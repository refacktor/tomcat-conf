create database logs;
use logs;

CREATE TABLE `log_access` (
  `ts` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `remote_ip` varchar(16) NOT NULL,
  `url` text,
  `query_string` text,
  `http_status` int(11) NOT NULL,
  `bytes_sent` int(11) NOT NULL,
  `req_time` float NOT NULL,
  `rsp_time` float NOT NULL,
  `session_id` varchar(99) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `agent_proxy` bit(1) DEFAULT NULL,
  `server_ts` datetime(3) NOT NULL,
  `local_ip` varchar(16) NOT NULL,
  `method` varchar(10) NOT NULL,
  `protocol` varchar(30) NOT NULL,
  `thread_name` varchar(99) NOT NULL,
  `referer` text DEFAULT NULL,
  `user_agent` text DEFAULT NULL,
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `ts` (`ts`),
  KEY `user_log` (`user_id`,`url`(8),`ts`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_application` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `level` varchar(7) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `dbTimeStamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `millis` bigint(20) NOT NULL,
  `loggerName` varchar(255) NOT NULL,
  `message` longtext NOT NULL,
  `sequenceNumber` int(11) NOT NULL,
  `sourceClassName` varchar(255) NOT NULL,
  `sourceMethodName` varchar(255) NOT NULL,
  `threadID` int(11) NOT NULL,
  `thrown` longtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

grant all on logs.* to ctcapi;
