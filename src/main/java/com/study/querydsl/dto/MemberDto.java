package com.study.querydsl.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String memberName;
    private int age;

    public MemberDto(String memberName, int age) {
        this.memberName = memberName;
        this.age = age;
    }
}
