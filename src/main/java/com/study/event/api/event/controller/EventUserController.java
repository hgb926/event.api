package com.study.event.api.event.controller;

import com.study.event.api.event.dto.request.EventUserSaveDto;
import com.study.event.api.event.dto.request.LoginRequestDto;
import com.study.event.api.event.dto.response.LoginResponseDto;
import com.study.event.api.event.service.EventUserService;
import com.study.event.api.exception.LoginFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class EventUserController {

    private final EventUserService eventUserService;

    // 이메일 중복확인
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(String email) {
        boolean isDuplicate = eventUserService.checkEmailDuplicate(email);

        return ResponseEntity.ok().body(isDuplicate);
    }

    // 인증 코드 검증 API
    @GetMapping("/code")            // 너 누구야,   코드는 뭐야
    public ResponseEntity<?> verifyCode(String email, String code) {
        log.info("{}'s verify code is [  {}  ]", email, code);
        boolean isMatch = eventUserService.isMatchCode(email, code);
        return ResponseEntity.ok().body(isMatch);
    }

    // 회원가입 마무리 처리
    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody EventUserSaveDto dto) {
        log.info("save User Info - {}", dto);
        try {
            eventUserService.confirmSignUp(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok().body("saved success");
    }

    @PostMapping("/sign-in") // login은 password가 날라오기 때문에 post
    public ResponseEntity<?> signIn(@RequestBody LoginRequestDto dto) {

        try {
            LoginResponseDto responseDto = eventUserService.authenticate(dto);// authenticate에 throw가 3개나 있다
            return ResponseEntity.ok().body(responseDto);

        } catch (LoginFailException e) {
            // 서비스에서 예외발생 (로그인 실패)
            String errorMessage = e.getMessage();
            return ResponseEntity.status(422).body(errorMessage);
        }
    }




}
