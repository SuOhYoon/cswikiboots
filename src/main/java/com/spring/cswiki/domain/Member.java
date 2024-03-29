package com.spring.cswiki.domain;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    private String u_id;
    private Integer p_id;
    private String email;
    private String name;
    private String pwd;
    private Date reg_date;
    private int ban;
    private Date ban_date;
    private Date banExpire;
    private String ban_reason;
}
