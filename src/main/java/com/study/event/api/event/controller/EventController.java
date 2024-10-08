package com.study.event.api.event.controller;

import com.study.event.api.auth.TokenProvider;
import com.study.event.api.event.dto.request.EventSaveDto;
import com.study.event.api.event.dto.response.EventOneDto;
import com.study.event.api.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.study.event.api.auth.TokenProvider.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;


    // 전체 조회 요청
    @GetMapping("/page/{pageNo}")
    public ResponseEntity<?> getList(
            // 토큰파싱 결과로 로그인에 성공한 회원의 PK
            @AuthenticationPrincipal TokenUserInfo tokenInfo,
            @RequestParam(required = false) String sort,
            @PathVariable int pageNo) throws InterruptedException {

        log.info("token user id : {}", tokenInfo);

        if (sort == null) {
            return ResponseEntity.badRequest().body("sort is null");
        }

        Map<String, Object> events = eventService.getEvents(pageNo, sort, tokenInfo.getUserId());

        // 스켈레톤을 보기 위해 의도적으로 2초간의 로딩을 설정
        // Thread.sleep(2000);
        return ResponseEntity.ok().body(events);
    }

    // 등록 요청
    @PostMapping
    public ResponseEntity<?> register(
            // JwtAuthFilter에서 시큐리티에 등록한 데이터
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody EventSaveDto dto
    ) {

        // Service에서 반환과정 중 new throw가 있기에 try, catch
        try {
            eventService.saveEvent(dto, userInfo.getUserId());
            return ResponseEntity.ok().body("event saved!");
        } catch (IllegalStateException e) {
            log.warn(e.getMessage());
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }


    // 단일 조회 요청
    @PreAuthorize("hasAuthority('PREMIUM') or hasAuthority('ADMIN')")
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEvent(@PathVariable Long eventId) {

        if (eventId == null || eventId < 1) {
            String errorMessage = "Event id is null or negative value";
            log.warn(errorMessage);
            return ResponseEntity.badRequest().body(errorMessage);
        }

        EventOneDto eventDetail = eventService.getEventDetail(eventId);
        return ResponseEntity.ok().body(eventDetail);
    }

    // 삭제 요청
    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> delete(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok().body("event delete!");
    }

    // 수정 요청
    @PatchMapping("/{eventId}")
    public ResponseEntity<?> modify(@RequestBody EventSaveDto dto,
                                    @PathVariable Long eventId) {
        eventService.modifyEvent(dto, eventId);
        return ResponseEntity.ok().body("event modify!");
    }
}
