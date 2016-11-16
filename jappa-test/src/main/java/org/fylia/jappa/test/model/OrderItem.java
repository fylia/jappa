package org.fylia.jappa.test.model;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="orderItem")
public class OrderItem {
    @EmbeddedId
    @AttributeOverride(name="articleId", column=@Column(name="artId"))
    private OrderItemId id;
     
    @ManyToOne
	@JoinColumn(name="orderId", nullable=false, updatable=false)
    private Order order;
    
    @ManyToOne 
	@JoinColumn(name="artId", nullable=false, updatable=false)
    private Article article;
    
    public OrderItemId getId() {
		return id;
	}
    
    public void setId(OrderItemId id) {
		this.id = id;
	}
	
	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

}

