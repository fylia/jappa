package org.fylia.jappa.test.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="customer")
public class Customer extends AbstractEntity {
	@Column(length=20, nullable=false)
	private String name;
	@Column(length=256)
	private String address;
	
	public String getAddress() {
		return address;
	}
	public String getName() {
		return name;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public void setName(String name) {
		this.name = name;
	}
}

