package org.fylia.jappa.test.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class OrderItemId implements Serializable {
	private static final long serialVersionUID = 1L;
	@Column
	private Integer articleId;
	@Column
	private Integer orderId; 
	
	public Integer getArticleId() {
		return articleId;
	}
	public void setArticleId(Integer articleId) {
		this.articleId = articleId;
	}
	public Integer getOrderId() {
		return orderId;
	}
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}
}

