package com.study.event.api.event.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.study.event.api.event.entity.Event;
import lombok.*;

import java.time.LocalDate;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOneDto {

    @JsonProperty("event-id")
    private String id;

    private String title;

    private String desc;

    @JsonProperty("img-url")
    private String image;

    @JsonProperty("start-date")
    @JsonFormat(pattern = "yyyy년 MM월 dd일")
    private LocalDate date;

    public EventOneDto(Event event) {
        this.id = event.getId().toString();
        this.title = event.getTitle();
        this.desc = event.getDescription();
        this.image = event.getImage();
        this.date = event.getDate();
    }
}
