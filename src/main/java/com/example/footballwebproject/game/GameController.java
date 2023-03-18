package com.example.footballwebproject.game;

import com.example.footballwebproject.entity.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    // 게임조회 - 전체
    @GetMapping
    public List<Game> getGames(){
        return gameService.getGames();
    }

     //게임조회 - 상세
    @GetMapping("/{gameid}")
    public GameResponseDto getGame(@PathVariable Long id){
        return gameService.getGame(id);
    }
}