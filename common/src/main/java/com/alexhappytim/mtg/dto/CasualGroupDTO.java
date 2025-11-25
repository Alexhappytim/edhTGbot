package com.alexhappytim.mtg.dto;

import lombok.Data;
import java.util.List;

@Data
public class CasualGroupDTO {
    private Long id;
    private int groupNumber;
    private List<UserDTO> players;
}
