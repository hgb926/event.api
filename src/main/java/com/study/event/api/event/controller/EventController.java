package com.study.event.api.event.controller;

import com.study.event.api.event.dto.request.EventSaveDto;
import com.study.event.api.event.dto.response.EventOneDto;
import com.study.event.api.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;


    // 전체 조회 요청
    @GetMapping("/page/{pageNo}")
    public ResponseEntity<?> getList(
            @RequestParam(required = false) String sort,
            @PathVariable int pageNo) throws InterruptedException {

        if (sort == null) {
            return ResponseEntity.badRequest().body("sort is null");
        }

        Map<String, Object> events = eventService.getEvents(pageNo, sort);

        // 의도적으로 2초간의 로딩을 설정
        Thread.sleep(2000);
        return ResponseEntity.ok().body(events);
    }

    // 등록 요청
    @PostMapping
    public ResponseEntity<?> register(@RequestBody EventSaveDto dto) {
        eventService.saveEvent(dto);
        return ResponseEntity.ok().body("events saved!");
    }

    // 단일 조회 요청
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
