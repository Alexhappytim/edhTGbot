package com.alexhappytim.mtg.dto;

import lombok.Data;
import java.util.List;

@Data
public class TournamentCasualDTO {
    private Long id;
    private String name;
    private UserDTO owner;
    private List<UserDTO> users;
    private List<UserDTO> readyUsers;
    private List<CasualGroupDTO> groups;
}
