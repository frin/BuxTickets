CREATE TABLE IF NOT EXISTS `ticketactions` (
  `ticketactionid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `ticketid` int(10) unsigned NOT NULL,
  `uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `type` enum('comment','status','assign','move') COLLATE utf8_unicode_ci NOT NULL,
  `content` text COLLATE utf8_unicode_ci NOT NULL,
  `world` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `x` decimal(12,4) NOT NULL,
  `y` decimal(12,4) NOT NULL,
  `z` decimal(12,4) NOT NULL,
  `new_uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `seen` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`ticketactionid`),
  KEY `ticketid` (`ticketid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

CREATE TABLE IF NOT EXISTS `tickets` (
  `ticketid` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `owner` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `content` text COLLATE utf8_unicode_ci NOT NULL,
  `group` varchar(50) COLLATE utf8_unicode_ci DEFAULT NULL,
  `world` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `x` decimal(12,4) NOT NULL,
  `y` decimal(12,4) NOT NULL,
  `z` decimal(12,4) NOT NULL,
  `status` enum('open','closed') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'open',
  `assigned_uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `updated_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`ticketid`),
  KEY `uuid` (`uuid`),
  KEY `status` (`status`),
  KEY `assigned_uuid` (`assigned_uuid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;
