package com.study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String memberName;
    private int age;

    @QueryProjection
    public MemberDto(String memberName, int age) {
        this.memberName = memberName;
        this.age = age;
    }
}