package com.jason.goalwithproject.dto.custom;

import com.jason.goalwithproject.domain.user.UserCharacter;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CharacterDto {
    private Long id;
    private String character;
    private String name;

    public CharacterDto(UserCharacter userCharacter) {
        this.id = userCharacter.getId();
        this.character = userCharacter.getCharacterImage().getImage();
        this.name = userCharacter.getCharacterImage().getName();
    }
}
