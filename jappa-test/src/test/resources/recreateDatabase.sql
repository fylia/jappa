DROP TABLE ARTICLE IF EXISTS;
DROP TABLE SUPPLIERS IF EXISTS;

CREATE TABLE ARTICLE (
	idArticle INT PRIMARY KEY AUTO_INCREMENT,
	code VARCHAR(20),
	descriptionNl VARCHAR(256),
	descriptionFr VARCHAR(256),
	cataloguePrice DECIMAL(8,3),
	supplierDiscount DECIMAL(8,3),
	orderQuantity DECIMAL(8,3),
	inventoryItem BOOLEAN,
	active INT,
	supplierId INT
);

CREATE TABLE SUPPLIERS (
	idSuppliers INT PRIMARY KEY AUTO_INCREMENT,
	supplier VARCHAR(20),
	contact VARCHAR(256),
	address VARCHAR(256),
	foreignCountry BOOLEAN,
	phone VARCHAR(50),
	mail VARCHAR(256)
);

CREATE INDEX I_ARTICLE_SUPPLIER_ID on ARTICLE (SUPPLIERID ASC);

ALTER TABLE ARTICLE ADD CONSTRAINT F_ARTICLE_SUPPLIER FOREIGN KEY (SUPPLIERID)
               REFERENCES SUPPLIERS (idSuppliers) ON DELETE RESTRICT ON UPDATE RESTRICT;