package org.fylia.jappa.test.model;

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
	
	@Column(length=20, nullable=false)
	private String code;
	
	@Column(length=256)
	private String descriptionNl;
	@Column(length=256)
	private String descriptionFr; 
	@Column
	private BigDecimal cataloguePrice;
	@Column
	private BigDecimal supplierDiscount;
	@Column
	private BigDecimal orderQuantity;
	@Column
	private boolean inventoryItem;
	@Column
	private boolean active;
	
	@ManyToOne
	@JoinColumn(name="supplierId", nullable=false, updatable=true)
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
	public String getDescriptionNl() {
		return descriptionNl;
	}
	public void setDescriptionNl(String descriptionNl) {
		this.descriptionNl = descriptionNl;
	}
	public String getDescriptionFr() {
		return descriptionFr;
	}
	public void setDescriptionFr(String descriptionFr) {
		this.descriptionFr = descriptionFr;
	}
	public BigDecimal getCataloguePrice() {
		return cataloguePrice;
	}
	public void setCataloguePrice(BigDecimal cataloguePrice) {
		this.cataloguePrice = cataloguePrice;
	}
	public BigDecimal getSupplierDiscount() {
		return supplierDiscount;
	}
	public void setSupplierDiscount(BigDecimal supplierDiscount) {
		this.supplierDiscount = supplierDiscount;
	}
	public BigDecimal getOrderQuantity() {
		return orderQuantity;
	}
	public void setOrderQuantity(BigDecimal orderQuantity) {
		this.orderQuantity = orderQuantity;
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
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	
}

