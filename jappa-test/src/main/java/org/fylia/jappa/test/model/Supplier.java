package org.fylia.jappa.test.model;

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
	
	@Column(length=100, nullable=false)
	private String supplier;
	 
	@Column(length=256)
	private String contact;
	@Column(length=1024)
	private String address;
	@Column(nullable=false)
	private boolean foreignCountry;
	
	@Column(length=50)
	private String phone;
	@Column(length=256)
	private String mail;
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
	public String getContact() {
		return contact;
	}
	public void setContact(String contact) {
		this.contact = contact;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public boolean isForeignCountry() {
		return foreignCountry;
	}
	public void setForeignCountry(boolean foreignCountry) {
		this.foreignCountry = foreignCountry;
	}
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
}
