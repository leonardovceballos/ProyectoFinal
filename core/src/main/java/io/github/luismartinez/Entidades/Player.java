package io.github.luismartinez.Entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class Player extends Entity {
    public Player(Vector2 position, Texture img, float speed) {
        super(position, img, speed);
    }

    public void moveLeft(float delta){
        sprite.translateX(-this.speed * delta);
    }

    public void moveRight(float delta){
        sprite.translateX(this.speed * delta);
    }
}
