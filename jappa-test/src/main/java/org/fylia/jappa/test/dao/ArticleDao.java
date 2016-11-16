package org.fylia.jappa.test.dao;

import java.util.List;
import java.util.stream.Collectors;

import org.fylia.jappa.test.dao.impl.ArticleJdbcTemplate;
import org.fylia.jappa.test.dao.impl.ArticleJdbcTemplate.ArticleRowMapper;
import org.fylia.jappa.test.dao.impl.SupplierJdbcTemplate;
import org.fylia.jappa.test.dao.impl.SupplierJdbcTemplate.SupplierRowMapper;
import org.fylia.jappa.test.model.Article;
import org.springframework.stereotype.Component;

/**
 * @author fylia
 */
@Component
public class ArticleDao extends ArticleJdbcTemplate {
	public List<Article> findAllJoinSupplier() {
		String thisColumns = 
				ID_COLUMN_LIST.stream().map(c->"this."+c+" this_"+c).collect(Collectors.joining(",")) +", "+ 
				DETAIL_COLUMN_LIST.stream().map(c->"this."+c+" this_"+c).collect(Collectors.joining(",")) + ", " +
				SupplierJdbcTemplate.ID_COLUMN_LIST.stream().map(c->"supplier."+c+" supplier_"+c).collect(Collectors.joining(",")) + ", "+ 
				SupplierJdbcTemplate.DETAIL_COLUMN_LIST.stream().map(c->"supplier."+c+" supplier_"+c).collect(Collectors.joining(","));
				
		final ArticleRowMapper articleMapper = new ArticleRowMapper("this_");
		final SupplierRowMapper supplierMapper = new SupplierRowMapper("supplier_");
        return getJdbcTemplate().query(
                "select " + thisColumns + " from article this left outer join suppliers supplier on this.supplierId = supplier.idSuppliers", 
                	new Object[] {},
                	(rs, rowNr) -> {
                		Article art = articleMapper.mapRow(rs,rowNr);
                		if (art.getSupplier()!=null) {
                			art.setSupplier(supplierMapper.mapRow(rs,rowNr));
                		}
                		return art;});
	}
}
