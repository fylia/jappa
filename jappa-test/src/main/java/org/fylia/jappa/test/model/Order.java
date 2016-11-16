package org.fylia.jappa.test.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="order")
public class Order {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id")
	private Integer id;
	
	@Column
	private String code;

	/*
	@OneToMany(mappedBy="order")
	private List<OrderItem> orderItems;
	*/
	
	public Integer getId() {
		return id;
		
	}
	public void setId(Integer id) {
		this.id = id;
	}

	/*
	public List<OrderItem> getOrderItems() {
		return orderItems;
	}
	public void setOrderItems(List<OrderItem> orderItems) {
		this.orderItems = orderItems;
	}
	*/

	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
}

