package com.study.event.api.event.service;

import com.study.event.api.auth.TokenProvider;
import com.study.event.api.event.dto.request.EventUserSaveDto;
import com.study.event.api.event.dto.request.LoginRequestDto;
import com.study.event.api.event.dto.response.LoginResponseDto;
import com.study.event.api.event.entity.EmailVerification;
import com.study.event.api.event.entity.EventUser;
import com.study.event.api.event.repository.EmailVerificationRepository;
import com.study.event.api.event.repository.EventUserRepository;
import com.study.event.api.exception.LoginFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventUserService {

    private final EmailVerificationRepository emailVerificationRepository;
    @Value("${study.mail.host}")
    private String mailHost;

    // 패스워드 암호화 객체
    private final PasswordEncoder encoder;

    private final EventUserRepository eventUserRepository;
    private final EmailVerificationRepository errorEmailVerificationRepository;

    // 이메일 전송객체 주입
    private final JavaMailSender mailSender;

    // 토큰 생성 객체
    private final TokenProvider tokenProvider;

    // 이메일 중복확인 처리
    public boolean checkEmailDuplicate(String email) {
        boolean exists = eventUserRepository.existsByEmail(email);
        log.info("Checking email {} is duplicate : {}", email, exists);

        // 중복인데 회원가입이 마무리되지 않은 회원은 중복이 아니라고 판단
        if (exists && notFinish(email)) {
            // 인증메일 재발송
            return false;
        }



        // 중복이 아니면 선제적으로 회원가입을 시킴
        // 일련의 후속 처리 (데이터베이스 처리, 이메일 보내는 것...)
        if (!exists) processSignUp(email);

        return exists;
    }

    private boolean notFinish(String email) {
        EventUser foundUser = eventUserRepository.findByEmail(email).orElseThrow();

        if (!foundUser.isEmailVerified() || foundUser.getPassword() == null) {
            // 기존 인증코드가 있는 경우 삭제
            EmailVerification ev = emailVerificationRepository.findByEventUser(foundUser).orElse(null);

            if (ev != null) {
                emailVerificationRepository.delete(ev);
            }

            generateAndSendCode(email,foundUser);
            return true;
        }
        return false;
    }


    // 이메일 인증코드 보내기
    public String sendVerificationEmail(String email) {

        // 검증코드 생성하기
        String code = generateVerificationCode();

        // 이메일을 전송할 객체 생성
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // 누구에게 이메일을 보낼 것인지
            messageHelper.setTo(email);
            // 이메일 제목 설정
            messageHelper.setSubject("[인증메일] 중앙정보");
            // 이메일 내용
            messageHelper.setText(
                    "인증 코드: <b style=\"font-weight: 700; letter-spacing: 5px; font-size: 30px;\">" + code + "</b>"
                    , true
            );
            // 전송자의 이메일 주소
            messageHelper.setFrom(mailHost);

            // 이메일 보내기
            mailSender.send(mimeMessage);
            log.info("{} 님에게 이메일 전송!", email);
            return code;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 검증 코드 생성 로직 1000~9999 사이 4자리 숫자
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 9000 + 1000));
    }

    public void processSignUp(String email) {

        // 1. 임시 회원가입
        EventUser newEventUser = EventUser.builder()
                .email(email)
                .build();
        EventUser savedUser = eventUserRepository.save(newEventUser);

        // 2. 이메일 인증 코드 발송
        generateAndSendCode(email, savedUser);

    }

    private void generateAndSendCode(String email, EventUser eventUser) {
        String code = sendVerificationEmail(email);

        // 3. 인증 코드를 데이터베이스에 저장
        EmailVerification verification = EmailVerification.builder()
                .verificationCode(code) // 인증코드
                .expiryDate(LocalDateTime.now().plusMinutes(5)) // 만료 시간 (5분 뒤)
                .eventUser(eventUser) // FK. 누가
                // JPA에서는 FK를 줄때 id만 주는게 아니라 엔터티를 통째로!!
                .build();
        emailVerificationRepository.save(verification);
    }

    // 인증코드 체크
    public boolean isMatchCode(String email, String code) {

        // 이메일을 통해 회원정보를 탐색
        EventUser eventUser = eventUserRepository.findByEmail(email).orElse(null);
        if (eventUser != null) {
            // 인증코드가 있는지 탐색
            EmailVerification ev = emailVerificationRepository.findByEventUser(eventUser).orElse(null);

            // 인증코드가 있고 만료시간이 지나지 않았고 코드번호가 일치할 경우 인증 성공
            if (
                    ev != null
                            && ev.getExpiryDate().isAfter(LocalDateTime.now())
                            && code.equals(ev.getVerificationCode())
            ) {
                // 이메일 인증 여부 true로 수정, 찾고 -> 세터로 변경 -> 세이브
                eventUser.setEmailVerified(true);
                eventUserRepository.save(eventUser); // UPDATE문이 나감

                // 인증 성공했으니, 인증코드 데이터베이스에서 삭제
                emailVerificationRepository.delete(ev);
                return true;
            } else { // 인증코드가 틀렸거나 만료된 경우
                // 인증코드 재발송
                // 원래 인증코드 삭제
                emailVerificationRepository.delete(ev);
                // 새 인증코드 발급 이메일 재전송
                // 데이터베이스에 새 인증코드 저장
                generateAndSendCode(email, eventUser);
                return false;
            }
        }

        return false;
    }

    // 회원가입 마무리
    public void confirmSignUp(EventUserSaveDto dto) {

        // 기존 회원 정보 조회
        EventUser foundUser = eventUserRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(
                        () -> new RuntimeException("회원 정보가 존재하지 않습니다.")
                );

        // 데이터 반영 (패스워드, 가입시간)
        String password = dto.getPassword();
        String encodedPassword = encoder.encode(password); // 암호화
        foundUser.confirm(encodedPassword);
        eventUserRepository.save(foundUser);
    }

    // 회원 인증 처리 (login)
    public LoginResponseDto authenticate(final LoginRequestDto dto) {

        // 이메일을 통해 회원정보 조회
        EventUser eventUser = eventUserRepository.findByEmail(dto.getEmail())
                .orElseThrow(
                        () -> new LoginFailException("가입된 회원이 아닙니다.")
                );

        // 이메일 인증을 안했거나 패스워드를 설정하지 않은 회원
        if (!eventUser.isEmailVerified() || eventUser.getPassword() == null) {
            throw new LoginFailException("회원가입이 중단된 회원입니다. 다시 가입해주세요.");
        }

        // 패스워드 검증
        String inputPassword = dto.getPassword(); // 방금 입력받은 pw
        String encodedPassword = eventUser.getPassword();  // DB에 저장된 암호화된 pw

        if (!encoder.matches(inputPassword, encodedPassword)) { // 일치하지 않으면~
            throw new LoginFailException("비밀번호가 틀렸습니다.");
        }

        // 로그인 성공
        // 인증 정보를 어떻게 관리할 것인가? (핵심) 세션 or 토큰 or 쿠키
        // 인증정보(이메일, 닉네임, 프사, 토큰정보)를 클라이언트에게 전송

        // 토큰 생성
        String token = tokenProvider.createToken(eventUser);

        return LoginResponseDto.builder()
                .email(dto.getEmail())
                .role(eventUser.getRole().toString())
                .token(token)
                .build();

    }

    // 등업 처리
    public LoginResponseDto promoteToPremium(String userId) {
        // 회원 탐색
        EventUser eventUser = eventUserRepository.findById(userId).orElseThrow();

        // 등급 변경
        eventUser.promoteToPremium(); // setter를 사용하지 않고, 메서드로 세터의 역할 대체
        EventUser promotedUser = eventUserRepository.save(eventUser);

        // 토큰 재발급 해야함. (claims에 들어있는 role은 갱신이 안되기때문에)
        String token = tokenProvider.createToken(promotedUser);

        return LoginResponseDto.builder()
                .token(token)
                .role(promotedUser.getRole().toString())
                .email(promotedUser.getEmail())
                .build();
    }
}




