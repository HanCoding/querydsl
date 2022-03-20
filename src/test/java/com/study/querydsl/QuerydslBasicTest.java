package com.study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static com.study.querydsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    private EntityManager em;

    private JPAQueryFactory queryFactory;


    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberC", 30, teamB);
        Member memberD = new Member("memberD", 40, teamB);
        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);
    }

    @Test
    public void startJPQL() {
        // memberA 조회
        Member findMember = em.createQuery("select m from Member m where m.memberName = :memberName", Member.class)
                .setParameter("memberName", "memberA")
                .getSingleResult();

        assertThat(findMember.getMemberName()).isEqualTo("memberA");
    }

    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.memberName.eq("memberA"))
                .fetchOne();

        assertThat(findMember.getMemberName()).isEqualTo("memberA");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.memberName.eq("memberA")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getMemberName()).isEqualTo("memberA");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.memberName.eq("memberA"), 
                        member.age.eq(10))
                .fetchOne();

        assertThat(findMember.getMemberName()).isEqualTo("memberA");
    }
    
    @Test
    public void resultFetchTest() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory.selectFrom(QMember.member)
//                .fetchOne();
//
//        Member fetchLimitFirst = queryFactory.selectFrom(QMember.member)
//                .limit(1)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        long total = results.getTotal();
//        List<Member> resultList = results.getResults();
//        long limit = results.getLimit();

        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 단, 2에 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100, null));
        em.persist(new Member("memberF", 100, null));
        em.persist(new Member("memberG", 100, null));

        List<Member> results = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.memberName.asc().nullsLast())
                .fetch();

        Member memberF = results.get(0);
        Member memberG = results.get(1);
        Member memberNull = results.get(2);

        assertThat(memberF.getMemberName()).isEqualTo("memberF");
        assertThat(memberG.getMemberName()).isEqualTo("memberG");
        assertThat(memberNull.getMemberName()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.memberName.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.memberName.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(memberQueryResults.getTotal()).isEqualTo(4);
        assertThat(memberQueryResults.getLimit()).isEqualTo(2);
        assertThat(memberQueryResults.getOffset()).isEqualTo(1);
        assertThat(memberQueryResults.getResults().size()).isEqualTo(2);
    }
}