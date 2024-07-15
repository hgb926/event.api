package com.study.event.api.event.dto.response;

import lombok.*;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String email;
    private String role; // 권한
    private String token; // 인증 토큰 (json 토큰은 문자열로 만들어져있다)

}

