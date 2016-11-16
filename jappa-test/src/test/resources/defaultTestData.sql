INSERT INTO SUPPLIERS(idSuppliers, supplier, contact, address, foreignCountry, phone, mail) VALUES
	(1, 'Sup1', 'John', 'Highway 10 new street', false, '012/345766','j@hi.way'),
	(2, 'Other', 'Francis', 'Compagnystreet 13, 88547 daoijdoiza', true, '003(45)4x567 447854', 'fr@comp.com');

INSERT INTO ARTICLE(idArticle, code, descriptionNl, descriptionFr, cataloguePrice, supplierDiscount, orderQuantity, inventoryItem, active, supplierId) VALUES
	(1, 'art1','Article nr 1', 'Article no 1', 12.47, 50, 8, true, 1, 1),
	(2, 'art2','Article nr 2', 'Article no 2', 1.14, 18, 10, true, 0, 1),
	(3, 'art3','Article nr 3', 'Article no 3', 2.14, 1, 1, false, 1, 1),
	(4, 'art4','Article nr 4', 'Article no 4', 3.14, 1, 1, true, 1, 1),
	(5, '1024.01','Francis Article nr 1', 'Article no 1 de Francis', 12.47, 50, 8, true, 1, 1),
	(6, '4152.02','Francis Article nr 2', 'Article no 2 de Francis', 1.14, 18, 10, true, 0, 1);


