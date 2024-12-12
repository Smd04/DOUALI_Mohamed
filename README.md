# DOUALI_Mohamed

CREATE TABLE `customer` (
  `id` int(50) NOT NULL,
  `nom` varchar(50) NOT NULL,
  `email` varchar(50) NOT NULL,
  `phone` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


INSERT INTO `customer` (`id`, `nom`, `email`, `phone`) VALUES
(1, 'Mohamed Douali', 'Mohamed@example.com', '123-456-7890'),
(2, 'Bader Douali', 'Bader@example.com', '987-654-3210'),
(3, 'Simo Douali', 'Simo@example.com', '555-123-4567');


CREATE TABLE `orders` (
  `id` int(50) NOT NULL,
  `date` date NOT NULL,
  `amount` int(50) NOT NULL,
  `customer_id` int(50) NOT NULL,
  `status` tinyint(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
