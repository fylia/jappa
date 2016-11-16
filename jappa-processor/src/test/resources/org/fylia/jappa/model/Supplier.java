package org.fylia.jappa.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="suppliers")
public class Supplier {
	@Id
	@Column(name="idSuppliers")
	private Integer id;
	
	@Column(length=100)
	private String supplier;
	
	@Column(length=256)
	private String name;
	@Column(name="fc")
	private boolean foreignCountry;
	@Column
	private Integer depositDeadline;
	@Column(name="np")
	private BigDecimal nettoPrice;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getSupplier() {
		return supplier;
	}
	public void setSupplier(String supplier) {
		this.supplier = supplier;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isForeignCountry() {
		return foreignCountry;
	}
	public void setForeignCountry(boolean foreignCountry) {
		this.foreignCountry = foreignCountry;
	}
	public Integer getDepositDeadline() {
		return depositDeadline;
	}
	public void setDepositDeadline(Integer depositDeadline) {
		this.depositDeadline = depositDeadline;
	}
	public BigDecimal getNettoPrice() {
		return nettoPrice;
	}
	public void setNettoPrice(BigDecimal nettoPrice) {
		this.nettoPrice = nettoPrice;
	}
}
