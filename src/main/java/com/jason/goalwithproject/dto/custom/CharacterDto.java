package com.jason.goalwithproject.dto.custom;

import com.jason.goalwithproject.domain.user.UserCharacter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterDto {
    private int id;
    private String character;
    private String name;

    public CharacterDto(UserCharacter userCharacter) {
        this.id = userCharacter.getCharacterImage().getId();
        this.character = userCharacter.getCharacterImage().getImage();
        this.name = userCharacter.getCharacterImage().getName();
    }
}
