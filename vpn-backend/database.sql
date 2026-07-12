-- SQL Schema for Alif Go VPN Server Backend
-- Create a database named 'alif_vpn_db' and run this query

CREATE TABLE IF NOT EXISTS `vpn_servers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `country_name` varchar(100) NOT NULL,
  `country_code` varchar(10) NOT NULL,
  `city` varchar(100) NOT NULL,
  `ip_address` varchar(100) NOT NULL,
  `type` varchar(50) NOT NULL DEFAULT 'Free', -- Free, Premium, Gaming, Streaming
  `latency` int(11) NOT NULL DEFAULT 50,
  `load_percent` int(11) NOT NULL DEFAULT 30,
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `protocol` varchar(50) NOT NULL DEFAULT 'WireGuard', -- WireGuard, OpenVPN
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed initial servers
INSERT INTO `vpn_servers` (`country_name`, `country_code`, `city`, `ip_address`, `type`, `latency`, `load_percent`, `protocol`) VALUES
('United States', 'US', 'New York', '104.244.42.1', 'Free', 45, 32, 'WireGuard'),
('Singapore', 'SG', 'Jurong', '128.199.112.5', 'Free', 21, 15, 'OpenVPN'),
('Bangladesh', 'BD', 'Dhaka', '103.112.113.1', 'Free', 12, 58, 'IKEv2'),
('Japan', 'JP', 'Tokyo', '210.140.10.3', 'Premium', 75, 19, 'WireGuard'),
('United Kingdom', 'GB', 'London', '195.154.122.9', 'Premium', 92, 25, 'OpenVPN'),
('Germany', 'DE', 'Frankfurt', '46.165.230.12', 'Premium', 88, 41, 'WireGuard'),
('South Korea (Seoul)', 'KR', 'Seoul', '182.162.24.8', 'Gaming', 15, 48, 'WireGuard');
