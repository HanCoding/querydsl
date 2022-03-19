package com.study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entity.Item;
import com.study.querydsl.entity.QItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
class QuerydslApplicationTests {

    @Autowired
    private EntityManager em;

    @Test
    void contextLoads() {
        Item item = new Item();
        item.setName("hancoding");
        em.persist(item);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QItem qItem = QItem.item;

        Item result = query.selectFrom(qItem)
                .fetchOne();

        assertThat(result).isEqualTo(item);
    }

}
