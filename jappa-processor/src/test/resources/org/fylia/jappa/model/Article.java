package org.fylia.jappa.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="article")
public class Article {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="idArticle")
	private Integer id;
	
	@Column(length=20)
	private String code;
	
	@Column(length=256)
	private String description;
	@Column
	private BigDecimal cataloguePrice;
	@Column
	private boolean inventoryItem;
	
	@ManyToOne
	@JoinColumn(name="supplierId", nullable=false, updatable=false)
	private Supplier supplier;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public BigDecimal getCataloguePrice() {
		return cataloguePrice;
	}
	public void setCataloguePrice(BigDecimal cataloguePrice) {
		this.cataloguePrice = cataloguePrice;
	}
	public boolean isInventoryItem() {
		return inventoryItem;
	}
	public void setInventoryItem(boolean inventoryItem) {
		this.inventoryItem = inventoryItem;
	}
	public Supplier getSupplier() {
		return supplier;
	}
	public void setSupplier(Supplier supplier) {
		this.supplier = supplier;
	}
}
