package org.fylia.jappa.test.dao;

import java.util.List;

import org.fylia.jappa.test.model.Article;
import org.fylia.jappa.test.model.Supplier;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class JdbcTemplatesTest extends AbstractDaoTest {
	@Autowired
	private ArticleDao articleDao;
	@Test
	public void testFill() {
		List<Article> allArticles = articleDao.findAll();
		Assert.assertEquals(6, allArticles.size());

		Article art2 = new Article();
		Supplier supplier = new Supplier();
		supplier.setId(1);
		art2.setSupplier(supplier);
		art2.setCode("1234");
		art2.setActive(true);
		art2.setInventoryItem(true);
		articleDao.merge(art2);
		
		Assert.assertNotNull(art2.getId());
		allArticles = articleDao.findAll();
		Assert.assertEquals(7, allArticles.size());
		
		Article loaded = articleDao.getById(art2.getId());
		Assert.assertEquals(1, loaded.getSupplier().getId().intValue());
		Assert.assertNotNull(loaded);
		Assert.assertEquals(art2.getCode(), loaded.getCode());
		Assert.assertEquals(art2.isActive(), loaded.isActive());
		Assert.assertEquals(art2.isInventoryItem(), loaded.isInventoryItem());
		
		loaded.setActive(false);
		articleDao.merge(loaded);
		Article reloaded = articleDao.getById(art2.getId());
		
		Assert.assertFalse(reloaded.isActive());
		
	}

	@Test
	public void testFindAllJoinSupplier() {
		List<Article> allArticles = articleDao.findAllJoinSupplier();
		Assert.assertEquals(6, allArticles.size());
	}
}
