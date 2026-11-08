
USE roomade;

CREATE TABLE IF NOT EXISTS items (
    item_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    product_name VARCHAR(100) DEFAULT NULL,
    category ENUM('FOOD', 'SUPPLY') NOT NULL,
    quantity DECIMAL(10,2) DEFAULT NULL,
    expiry_date DATE DEFAULT NULL,

    ownership_type ENUM('PERSONAL', 'SHARED') NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_item_quantity
        CHECK (quantity IS NULL OR quantity >= 0),

    CONSTRAINT chk_supply_expiry
        CHECK (
            category = 'FOOD'
            OR expiry_date IS NULL
        )
) ENGINE=InnoDB;
