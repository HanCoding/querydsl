package com.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberDto;
import com.study.querydsl.dto.QMemberDto;
import com.study.querydsl.dto.UserDto;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.QTeam;
import com.study.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.study.querydsl.entity.QMember.*;
import static com.study.querydsl.entity.QTeam.*;
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

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.count())).isEqualTo(4);

    }


    /**
     * 팀의 이름과 각 팀의 평균 연령 구하기
     */
    @Test
    public void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     * TeamA에 소속된 모든 회원
     */
    @Test
    public void joinTest() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
    }

    /**
     * 세타 조인
     * 회원 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void thetaJoin() {
        em.persist(new Member("teamA", 100, null));
        em.persist(new Member("teamB", 100, null));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.memberName.eq(team.name))
                .fetch();

        for (Member member : result) {
            System.out.println("====== member = " + member);

        }
    }

    /**
     * 회원과 팀을 조인하면서
     * 팀 이름이 teamA인 팀만 조인, 그리고 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void joinOnFlitering() {

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);

        }
    }

    /**
     * 연관 관계가 없는 엔티티 외부 조인
     * 회원 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void joinOnNoRelation() {
        em.persist(new Member("teamA", 100, null));
        em.persist(new Member("teamB", 100, null));

        List<Tuple> tuple = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.memberName.eq(team.name))
                .orderBy(member.id.asc())
                .fetch();

        for (Tuple tuple1 : tuple) {
            System.out.println("tuple = " + tuple1);

        }
    }

    @PersistenceUnit
    private EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

//        Member member = queryFactory
//                .selectFrom(QMember.member)
//                .join(member.team, team).fetchJoin()
//                .where(QMember.member.memberName.eq("memberA"))
//                .fetchOne();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.memberName.eq("memberA"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryAvg() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .contains(40);

    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(" s = " + s);

        }
    }

    @Test
    public void basicCaseBuilder() {
        List<String> result = queryFactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(10, 20)).then("10대")
                                .when(member.age.between(20, 30)).then("20대")
                                .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(" ===== " + s);

        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.memberName, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);

        }
    }

    @Test
    public void concat() {
        // memberName_age 생성
        List<String> result = queryFactory
                .select(member.memberName.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.memberName.eq("memberA"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);

        }
    }
    
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.memberName)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // tuple은 repository에서만 사용할 것을 권장
    @Test
    public void tupleProjection() {
        List<Tuple> fetch = queryFactory
                .select(member.memberName, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            String memberName = tuple.get(member.memberName);
            System.out.println("memberName = " + memberName);
            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);

        }
    }
    
    @Test
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new com.study.querydsl.dto.MemberDto(m.memberName, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDto> fetch = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.memberName,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() {
        List<MemberDto> fetch = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.memberName,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> fetch = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.memberName,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() {

        QMember memberSub = new QMember("memberSub");

        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.memberName.as("name"),
                        ExpressionUtils.as(JPAExpressions
                            .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> fetch = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.memberName,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDto() {
        List<MemberDto> fetch = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.memberName,
                        member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto : " + memberDto);

        }
    }

    @Test
    public void findQueryProjection() {
        List<MemberDto> memberDto = queryFactory
                .select(new QMemberDto(member.memberName, member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void dynamicQueryBooleanBuilder() {
        String memberNameParam = "memberA";
        Integer ageParam = null;

        List<Member> result = searchMember1(memberNameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String memberNameCondition, Integer ageCondition) {

        BooleanBuilder builder = new BooleanBuilder();
        if(memberNameCondition != null) {
            builder.and(member.memberName.eq(memberNameCondition));
        }
        if (ageCondition != null) {
            builder.and(member.age.eq(ageCondition));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQueryWhereParam() {
        String memberNameParam = "memberA";
        Integer ageParam = null;

        List<Member> result = searchMember2(memberNameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String memberNameCondition, Integer ageCondition) {
        return queryFactory
                .selectFrom(member)
                .where(memberNameEq(memberNameCondition), ageEq(ageCondition))
                .fetch();
    }

    private Predicate ageEq(Integer ageCondition) {
        return ageCondition != null ? member.age.eq(ageCondition) : null ;
    }

    private Predicate memberNameEq(String memberNameCondition) {
        return memberNameCondition != null ? member.memberName.eq(memberNameCondition) : null;
    }
}