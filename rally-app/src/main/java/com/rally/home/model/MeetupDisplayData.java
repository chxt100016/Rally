package com.rally.home.model;

import com.rally.domain.meetup.model.MeetupCardDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MeetupDisplayData extends BaseDisplayData {
    private List<MeetupCardDTO> meetups;
}
