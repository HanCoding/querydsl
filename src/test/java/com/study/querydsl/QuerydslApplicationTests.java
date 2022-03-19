package com.study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entity.Item;
import com.study.querydsl.entity.QItem;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired
    private EntityManager em;

    @Test
    void contextLoads() {
        Item item = new Item();
        em.persist(item);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QItem qItem = new QItem("item");

        Item result = query.selectFrom(qItem)
                .fetchOne();

        assertThat(result).isEqualTo(item);
    }

}
